package burp.util;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.persistence.PersistedObject;

public class JsmonConfig {
    private static final String API_KEY_KEY = "apiKey";
    private static final String WORKSPACE_ID_KEY = "workspaceId";
    private static final String SCOPED_DOMAIN_KEY = "scopedDomain";
    private static final String AUTOMATE_SCAN_KEY = "automateScan";
    private static final String GITHUB_TOKEN_KEY = "githubToken";
    
    private MontoyaApi api;
    private PersistedObject persistedObject;
    
    private String apiKey;
    private String workspaceId;
    private String scopedDomain;
    private boolean automateScan = false; // Default to false - automatic scanning is off by default
    private String githubToken;
    
    public JsmonConfig(MontoyaApi api) {
        this.api = api;
        if (api != null) {
            // extensionData() stores data in the project file, making it project-specific
            // Each Burp Suite project will have its own separate JSMon configuration
            // Note: Project must be saved to disk (not temporary) for persistence to work
            this.persistedObject = api.persistence().extensionData();
            loadFromPersistence();
        }
    }
    
    private void loadFromPersistence() {
        if (persistedObject == null) {
            if (api != null && api.logging() != null) {
                api.logging().logToOutput("JSMon: Persistence object is null - using default configuration");
            }
            return;
        }
        
        try {
            // Try to load each value - if it doesn't exist, it will return null/false
            // Note: getString() and getBoolean() may return null/false if key doesn't exist
            String loadedApiKey = persistedObject.getString(API_KEY_KEY);
            String loadedWorkspaceId = persistedObject.getString(WORKSPACE_ID_KEY);
            String loadedScopedDomain = persistedObject.getString(SCOPED_DOMAIN_KEY);
            Boolean loadedAutomateScan = persistedObject.getBoolean(AUTOMATE_SCAN_KEY);
            String loadedGithubToken = persistedObject.getString(GITHUB_TOKEN_KEY);
            
            // Only assign if values were actually loaded (not null for strings, not false for boolean if it was set)
            if (loadedApiKey != null) {
                this.apiKey = loadedApiKey;
            }
            if (loadedWorkspaceId != null) {
                this.workspaceId = loadedWorkspaceId;
            }
            if (loadedScopedDomain != null) {
                this.scopedDomain = loadedScopedDomain;
            }
            if (loadedAutomateScan != null) {
                this.automateScan = loadedAutomateScan;
            }
            if (loadedGithubToken != null) {
                this.githubToken = loadedGithubToken;
            }
            
            // Log successful load for debugging
            if (api != null && api.logging() != null) {
                if (apiKey != null || workspaceId != null) {
                    api.logging().logToOutput("JSMon: Loaded saved configuration from project data (API key: " + 
                        (apiKey != null ? "present" : "not set") + ", Workspace: " + 
                        (workspaceId != null ? "present" : "not set") + ")");
                } else {
                    api.logging().logToOutput("JSMon: No saved configuration found in project data");
                }
            }
        } catch (Exception e) {
            // If loading fails, use defaults and log the error
            this.apiKey = null;
            this.workspaceId = null;
            this.scopedDomain = null;
            this.automateScan = false;
            if (api != null && api.logging() != null) {
                api.logging().logToError("JSMon: Error loading configuration: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private void saveToPersistence() {
        if (persistedObject == null) {
            if (api != null && api.logging() != null) {
                api.logging().logToError("JSMon: Cannot save configuration - persistence object is null. " +
                    "Make sure you're using a disk-based project (not temporary project).");
            }
            return;
        }
        
        try {
            // Save each value - delete if null/empty to clean up
            if (apiKey != null && !apiKey.isEmpty()) {
                persistedObject.setString(API_KEY_KEY, apiKey);
            } else {
                try {
                    persistedObject.deleteString(API_KEY_KEY);
                } catch (Exception e) {
                    // Ignore if key doesn't exist
                }
            }
            
            if (workspaceId != null && !workspaceId.isEmpty()) {
                persistedObject.setString(WORKSPACE_ID_KEY, workspaceId);
            } else {
                try {
                    persistedObject.deleteString(WORKSPACE_ID_KEY);
                } catch (Exception e) {
                    // Ignore if key doesn't exist
                }
            }
            
            if (scopedDomain != null && !scopedDomain.isEmpty()) {
                persistedObject.setString(SCOPED_DOMAIN_KEY, scopedDomain);
            } else {
                try {
                    persistedObject.deleteString(SCOPED_DOMAIN_KEY);
                } catch (Exception e) {
                    // Ignore if key doesn't exist
                }
            }
            
            persistedObject.setBoolean(AUTOMATE_SCAN_KEY, automateScan);
            
            if (githubToken != null && !githubToken.isEmpty()) {
                persistedObject.setString(GITHUB_TOKEN_KEY, githubToken);
            } else {
                try {
                    persistedObject.deleteString(GITHUB_TOKEN_KEY);
                } catch (Exception e) {
                    // Ignore if key doesn't exist
                }
            }
            
            // Log successful save for debugging
            if (api != null && api.logging() != null) {
                api.logging().logToOutput("JSMon: Configuration saved to project data (API key: " + 
                    (apiKey != null && !apiKey.isEmpty() ? "saved" : "cleared") + ", Workspace: " + 
                    (workspaceId != null && !workspaceId.isEmpty() ? "saved" : "cleared") + ")");
            }
        } catch (Exception e) {
            // If saving fails, log the error
            if (api != null && api.logging() != null) {
                api.logging().logToError("JSMon: Error saving configuration: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    public String getApiKey() {
        return apiKey;
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
        saveToPersistence();
    }
    
    public String getWorkspaceId() {
        return workspaceId;
    }
    
    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
        saveToPersistence();
    }
    
    public String getScopedDomain() {
        return scopedDomain;
    }
    
    public void setScopedDomain(String scopedDomain) {
        this.scopedDomain = scopedDomain;
        saveToPersistence();
    }
    
    public boolean isAutomateScan() {
        return automateScan;
    }
    
    public void setAutomateScan(boolean automateScan) {
        this.automateScan = automateScan;
        saveToPersistence();
    }
    
    public String getGithubToken() {
        return githubToken;
    }
    
    public void setGithubToken(String githubToken) {
        this.githubToken = githubToken;
        saveToPersistence();
    }
}


package burp;

import burp.api.JsmonApiClient;
import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.logging.Logging;
import burp.model.JsUrlEntry;
import burp.model.UserProfile;
import burp.model.Workspace;
import burp.ui.JsmonTab;
import burp.util.JsmonConfig;
import burp.util.JsmonUrlProcessor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JsmonExtension implements BurpExtension, HttpHandler {
    
    private MontoyaApi api;
    private Logging logging;
    private JsmonTab tab;
    private JsmonConfig config;
    private JsmonApiClient apiClient;
    private JsmonUrlProcessor urlProcessor;
    private Set<String> processedUrls = ConcurrentHashMap.newKeySet();
    private final Object sendLock = new Object();
    private static final long MIN_SEND_INTERVAL_MS = 200; // 5 requests/second max
    private long lastSendTimestampMs = 0L;
    private volatile Thread currentScanThread = null;
    
    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        this.logging = api.logging();
        this.config = new JsmonConfig(api);
        this.apiClient = new JsmonApiClient(logging);
        this.urlProcessor = new JsmonUrlProcessor(logging);
        
        // Set extension name
        api.extension().setName("Jsmon Extension");
        
        // Register HTTP handler
        api.http().registerHttpHandler(this);
        
        // Register unloading handler
        api.extension().registerUnloadingHandler(() -> {
            // Extension unloaded - configuration is already saved automatically
        });
        
        // Create UI tab
        this.tab = new JsmonTab(api, this);
        api.userInterface().registerSuiteTab("Jsmon", tab);
    }
    
    
    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent request) {
        return RequestToBeSentAction.continueWith((burp.api.montoya.http.message.requests.HttpRequest) request);
    }
    
    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived response) {
        if (!config.isAutomateScan()) {
            return ResponseReceivedAction.continueWith(response);
        }
        
        HttpRequest httpRequest = response.initiatingRequest();
        // Get full URL including path (not just endpoint) for extension matching
        String url = httpRequest.url().toString();
        
        // Check if URL matches scoped domain
        String scopedDomain = config.getScopedDomain();
        if (scopedDomain != null && !scopedDomain.isEmpty()) {
            if (!urlProcessor.isUrlInScope(url, scopedDomain)) {
                return ResponseReceivedAction.continueWith(response);
            }
        }
        
        // Extract Content-Type header directly (avoid fetching full response object)
        String contentType = null;
        
        // Try to get Content-Type header directly from HttpResponseReceived (no need for full response object)
        try {
            // Method 1: Try headerValue() method directly
            java.lang.reflect.Method headerValueMethod = response.getClass().getMethod("headerValue", String.class);
            contentType = (String) headerValueMethod.invoke(response, "Content-Type");
        } catch (Exception e) {
            // Method 2: Try headers() method and iterate
            try {
            java.lang.reflect.Method headersMethod = response.getClass().getMethod("headers");
            Object headersObj = headersMethod.invoke(response);
            if (headersObj != null && headersObj instanceof java.lang.Iterable) {
                for (Object header : (java.lang.Iterable<?>) headersObj) {
                    try {
                        java.lang.reflect.Method nameMethod = header.getClass().getMethod("name");
                        java.lang.reflect.Method valueMethod = header.getClass().getMethod("value");
                        String headerName = (String) nameMethod.invoke(header);
                        if ("Content-Type".equalsIgnoreCase(headerName)) {
                            contentType = (String) valueMethod.invoke(header);
                            break;
                        }
                        } catch (Exception e2) {
                        // Continue to next header
                        }
                    }
                }
            } catch (Exception e2) {
                // All methods failed
            }
        }
        
        if (logging != null && contentType != null) {
            logging.logToOutput("Jsmon: Extracted Content-Type header for '" + url + "': " + contentType);
        }
        
        // Check if it's a scannable file (check URL extension and Content-Type)
        // Use Content-Type string to avoid duplicate extraction (we already have it)
        boolean isScannable = urlProcessor.isScannableFile(url, contentType);
        if (logging != null) {
            logging.logToOutput("Jsmon: Scannable check for '" + url + "' (Content-Type: " + (contentType != null ? contentType : "null") + "): " + isScannable);
        }
        
        if (isScannable) {
            // Avoid processing the same URL multiple times
            if (processedUrls.add(url)) {
                logging.logToOutput("Jsmon: Detected scannable file: " + url + (contentType != null ? " (Content-Type: " + contentType + ")" : ""));
                
                // Check if VPN mode is enabled - if so, send response body directly
                if (config.isVpnMode()) {
                    // Extract response body for VPN mode
                    String responseBody = null;
                    try {
                        responseBody = response.bodyToString();
                    } catch (Exception e) {
                        logging.logToError("Jsmon: VPN Mode - Error extracting response body: " + e.getMessage());
                    }
                    processScannableFileVpnMode(httpRequest, responseBody);
                } else {
                    // Process new scannable files as they come in (only need request headers and URL)
                    processScannableFile(httpRequest);
                }
            }
        }
        
        return ResponseReceivedAction.continueWith(response);
    }
    
    private void processScannableFile(HttpRequest request) {
        // Check if automatic scanning is still enabled before processing
        if (!config.isAutomateScan()) {
            return;
        }
        
        String apiKey = config.getApiKey();
        String workspaceId = config.getWorkspaceId();
        
        if (apiKey == null || apiKey.isEmpty() || workspaceId == null || workspaceId.isEmpty()) {
            logging.logToOutput("Jsmon: Skipping file - API key or workspace ID not configured");
            return;
        }
        
        // Get full URL including path (not just endpoint) for extension matching
        String url = request.url().toString();
        
        // Log to UI if tab is available
        if (tab != null) {
            tab.appendStatusMessage("🔄 Auto-scanning: " + url);
        }
        
        // Execute Jsmon API call with headers
        burp.api.JsmonApiClient.SendResult result = executeSingleSend(() ->
                apiClient.sendToJsmon(url, workspaceId, apiKey, request)
        );
        
        // Log result to UI
        if (tab != null) {
            if (result.isSuccess()) {
                tab.appendStatusMessage("  ✓ Success: " + url);
                // Fetch secrets after successful scan
                tab.fetchAndDisplaySecrets();
                // Refresh user profile to update JSScan credits
                tab.fetchAndDisplayUserProfile();
            } else {
                String errorMsg = result.getErrorMessage();
                if (errorMsg != null && !errorMsg.isEmpty()) {
                    tab.appendStatusMessage("  ✗ Failed: " + url + " - " + errorMsg);
            } else {
                tab.appendStatusMessage("  ✗ Failed: " + url);
                }
            }
        }
    }
    
    private void processScannableFileVpnMode(HttpRequest request, String responseBody) {
        // Check if automatic scanning is still enabled before processing
        if (!config.isAutomateScan()) {
            return;
        }
        
        String apiKey = config.getApiKey();
        String workspaceId = config.getWorkspaceId();
        
        if (apiKey == null || apiKey.isEmpty() || workspaceId == null || workspaceId.isEmpty()) {
            logging.logToOutput("Jsmon: VPN Mode - Skipping file - API key or workspace ID not configured");
            return;
        }
        
        if (responseBody == null || responseBody.isEmpty()) {
            logging.logToOutput("Jsmon: VPN Mode - Skipping file - Response body is empty");
            return;
        }
        
        // Get full URL including path (not just endpoint) for extension matching
        String url = request.url().toString();
        
        // Log to UI if tab is available
        if (tab != null) {
            tab.appendStatusMessage("🔄 VPN Mode - Auto-scanning: " + url);
        }
        
        // Execute Jsmon API call with file content (VPN mode)
        burp.api.JsmonApiClient.SendResult result = executeSingleSend(() ->
                apiClient.sendDirectFileScan(url, responseBody, workspaceId, apiKey)
        );
        
        // Log result to UI
        if (tab != null) {
            if (result.isSuccess()) {
                tab.appendStatusMessage("  ✓ VPN Mode - Success: " + url);
                // Fetch secrets after successful scan
                tab.fetchAndDisplaySecrets();
                // Refresh user profile to update JSScan credits
                tab.fetchAndDisplayUserProfile();
            } else {
                String errorMsg = result.getErrorMessage();
                if (errorMsg != null && !errorMsg.isEmpty()) {
                    tab.appendStatusMessage("  ✗ VPN Mode - Failed: " + url + " - " + errorMsg);
                } else {
                    tab.appendStatusMessage("  ✗ VPN Mode - Failed: " + url);
                }
            }
        }
    }
    
    // Getters and setters for configuration
    public void setApiKey(String apiKey) {
        config.setApiKey(apiKey);
    }
    
    public String getApiKey() {
        return config.getApiKey();
    }
    
    public void setWorkspaceId(String workspaceId) {
        config.setWorkspaceId(workspaceId);
    }
    
    public String getWorkspaceId() {
        return config.getWorkspaceId();
    }
    
    public void setScopedDomain(String scopedDomain) {
        config.setScopedDomain(scopedDomain);
    }
    
    public String getScopedDomain() {
        return config.getScopedDomain();
    }
    
    public void setAutomateScan(boolean automateScan) {
        boolean wasEnabled = config.isAutomateScan();
        config.setAutomateScan(automateScan);
        
        // If disabling automatic scanning, interrupt any running scan thread
        if (wasEnabled && !automateScan) {
            if (currentScanThread != null && currentScanThread.isAlive()) {
                currentScanThread.interrupt();
                logging.logToOutput("Jsmon: Automatic scanning disabled - stopping current scan");
            }
        }
    }
    
    public String getGithubToken() {
        return config.getGithubToken();
    }
    
    public void setGithubToken(String githubToken) {
        config.setGithubToken(githubToken);
    }
    
    public boolean isAutomateScan() {
        return config.isAutomateScan();
    }
    
    public boolean isVpnMode() {
        return config.isVpnMode();
    }
    
    public void setVpnMode(boolean vpnMode) {
        config.setVpnMode(vpnMode);
    }
    
    /**
     * Trigger initial scan of existing history when automatic scanning is enabled
     * Called after configuration is saved
     */
    public void triggerInitialScanIfEnabled() {
        if (config.isAutomateScan()) {
            String apiKey = config.getApiKey();
            String workspaceId = config.getWorkspaceId();
            if (apiKey != null && !apiKey.isEmpty() && workspaceId != null && !workspaceId.isEmpty()) {
            // Clear processed URLs to allow re-scanning
            clearProcessedUrls();
            
            // Scan existing history in background thread with UI status updates
            Thread scanThread = new Thread(() -> {
                // Check if automatic scanning is still enabled before starting
                if (!config.isAutomateScan()) {
                    return;
                }
                
                if (tab != null) {
                    tab.appendStatusMessage("🚀 Automatic scanning enabled - scanning existing files...");
                        String scopedDomain = config.getScopedDomain();
                    if (scopedDomain != null && !scopedDomain.isEmpty()) {
                        tab.appendStatusMessage("  Scoped domain: " + scopedDomain);
                    } else {
                        tab.appendStatusMessage("  No domain scope - scanning all scannable files");
                    }
                }
                logging.logToOutput("Jsmon: Automatic scanning enabled - scanning existing files in history...");
                
                // Pass callback to update UI in real-time
                int count = scanHttpHistory((statusMessage) -> {
                    // Check if automatic scanning was disabled during scan
                    if (!config.isAutomateScan()) {
                        if (tab != null) {
                            tab.appendStatusMessage("  ⚠ Scan stopped - automatic scanning was disabled");
                        }
                        logging.logToOutput("Jsmon: Scan stopped - automatic scanning was disabled");
                        return;
                    }
                    if (tab != null) {
                        tab.appendStatusMessage("  " + statusMessage);
                    }
                });
                
                // Only show completion message if automatic scanning is still enabled
                if (config.isAutomateScan()) {
                    if (tab != null) {
                        tab.appendStatusMessage("✓ Automatic scanning initialized - " + count + " existing file(s) processed");
                        // Note: Secrets are automatically fetched by scanHttpHistory if at least one scan was attempted (success or failure)
                    }
                    logging.logToOutput("Jsmon: Automatic scanning initialized - " + count + " existing file(s) processed");
                }
            });
            scanThread.setDaemon(true);
            currentScanThread = scanThread;
            scanThread.start();
            }
        }
    }
    
    public List<Workspace> fetchWorkspaces() {
        return apiClient.fetchWorkspaces(config.getApiKey());
    }
    
    public String createWorkspace(String workspaceName) {
        return apiClient.createWorkspace(config.getApiKey(), workspaceName);
    }

    /**
     * Fetch the user's profile (name, email, remaining limits) via viewProfile API
     */
    public UserProfile fetchUserProfile() {
        return apiClient.fetchUserProfile(config.getApiKey());
    }
    
    public void clearProcessedUrls() {
        processedUrls.clear();
    }
    
    public int scanHttpHistory(java.util.function.Consumer<String> statusCallback) {
        String apiKey = config.getApiKey();
        String workspaceId = config.getWorkspaceId();
        String scopedDomain = config.getScopedDomain();
        
        if (apiKey == null || apiKey.isEmpty() || workspaceId == null || workspaceId.isEmpty()) {
            logging.logToOutput("Jsmon: Cannot scan - API key or workspace ID not configured");
            if (statusCallback != null) {
                statusCallback.accept("✗ Cannot scan - API key or workspace ID not configured");
            }
            return 0;
        }
        
        int scannedCount = 0;
        int failedCount = 0;
        Set<String> scannedUrls = ConcurrentHashMap.newKeySet();
        java.util.List<String> scannableFiles = new ArrayList<>();
        java.util.Map<String, burp.api.montoya.proxy.ProxyHttpRequestResponse> urlToProxyEntryMap = new java.util.HashMap<>();
        
        try {
            // Get all HTTP history entries from Burp's proxy history
            List<burp.api.montoya.proxy.ProxyHttpRequestResponse> proxyHistory = api.proxy().history();
            
            // Checking proxy history for scannable files
            if (statusCallback != null) {
                statusCallback.accept("Found " + proxyHistory.size() + " entries in proxy history");
            }
            
            // First pass: collect all scannable files and store their requests
            for (burp.api.montoya.proxy.ProxyHttpRequestResponse proxyEntry : proxyHistory) {
                try {
                    // Get the URL from the proxy entry
                    String url = proxyEntry.url().toString();
                    
                    // Check if URL matches scoped domain
                    if (scopedDomain != null && !scopedDomain.isEmpty()) {
                        if (!urlProcessor.isUrlInScope(url, scopedDomain)) {
                            continue;
                        }
                    }
                    
                    // Check if it's a scannable file by URL extension OR Content-Type
                    // Extract Content-Type header directly (avoid fetching full response object)
                    String contentType = null;
                    
                    try {
                        // Try to get response headers directly from proxy entry
                        java.lang.reflect.Method responseMethod = proxyEntry.getClass().getMethod("httpResponse");
                        Object responseObj = responseMethod.invoke(proxyEntry);
                        if (responseObj != null) {
                            // Try headerValue() method on response
                            try {
                                java.lang.reflect.Method headerValueMethod = responseObj.getClass().getMethod("headerValue", String.class);
                                contentType = (String) headerValueMethod.invoke(responseObj, "Content-Type");
                            } catch (Exception e2) {
                                // Try headers() method
                                try {
                                    java.lang.reflect.Method headersMethod = responseObj.getClass().getMethod("headers");
                                    Object headersObj = headersMethod.invoke(responseObj);
                                    if (headersObj != null && headersObj instanceof java.lang.Iterable) {
                                        for (Object header : (java.lang.Iterable<?>) headersObj) {
                                            try {
                                                java.lang.reflect.Method nameMethod = header.getClass().getMethod("name");
                                                java.lang.reflect.Method valueMethod = header.getClass().getMethod("value");
                                                String headerName = (String) nameMethod.invoke(header);
                                                if ("Content-Type".equalsIgnoreCase(headerName)) {
                                                    contentType = (String) valueMethod.invoke(header);
                                                    break;
                                                }
                                            } catch (Exception e3) {
                                                // Continue
                                            }
                                        }
                                    }
                                } catch (Exception e3) {
                                    // Headers not available
                                }
                            }
                        }
                    } catch (Exception e1) {
                        // Try alternative method names
                        try {
                            java.lang.reflect.Method responseMethod = proxyEntry.getClass().getMethod("response");
                            Object responseObj = responseMethod.invoke(proxyEntry);
                            if (responseObj != null) {
                                try {
                                    java.lang.reflect.Method headerValueMethod = responseObj.getClass().getMethod("headerValue", String.class);
                                    contentType = (String) headerValueMethod.invoke(responseObj, "Content-Type");
                                } catch (Exception e2) {
                                    // Headers not available
                                }
                            }
                        } catch (Exception e2) {
                            // Response not available, will check extension only
                        }
                    }
                    
                    // Use urlProcessor to check BOTH extension AND Content-Type (OR logic)
                    // This ensures consistent matching with automatic scanning
                    // Use Content-Type string to avoid duplicate extraction (we already have it)
                    boolean isScannable = urlProcessor.isScannableFile(url, contentType);
                    
                    if (logging != null && isScannable) {
                        logging.logToOutput("Jsmon: History scan - Detected scannable file: " + url + (contentType != null ? " (Content-Type: " + contentType + ")" : ""));
                    }
                    
                    if (isScannable && scannedUrls.add(url)) {
                        scannableFiles.add(url);
                        
                        // Store the proxy entry for later use to extract headers if possible
                        urlToProxyEntryMap.put(url, proxyEntry);
                    }
                } catch (Exception e) {
                    logging.logToError("Jsmon: Error processing entry: " + e.getMessage());
                    continue;
                }
            }
            
            if (scannableFiles.isEmpty()) {
                if (statusCallback != null) {
                    statusCallback.accept("✗ No scannable files found to scan");
                }
                logging.logToOutput("Jsmon: No scannable files found to scan");
                return 0;
            }
            
            if (statusCallback != null) {
                statusCallback.accept("Found " + scannableFiles.size() + " file(s) to scan");
            }
            logging.logToOutput("Jsmon: Found " + scannableFiles.size() + " file(s) to scan");
            
            // Second pass: scan each file sequentially with real-time updates
            for (int i = 0; i < scannableFiles.size(); i++) {
                // Check if automatic scanning was disabled (for automatic scans) or thread was interrupted
                if (config.isAutomateScan() == false || Thread.currentThread().isInterrupted()) {
                    if (statusCallback != null) {
                        statusCallback.accept("⚠ Scan stopped by user");
                    }
                    logging.logToOutput("Jsmon: Scan stopped - automatic scanning disabled or interrupted");
                    break;
                }
                
                String url = scannableFiles.get(i);
                int fileNum = i + 1;
                
                // Show scanning status
                if (statusCallback != null) {
                    statusCallback.accept("[" + fileNum + "/" + scannableFiles.size() + "] Scanning: " + url);
                }
                logging.logToOutput("Jsmon: [" + fileNum + "/" + scannableFiles.size() + "] Scanning: " + url);
                
                // Get request headers from proxy entry (avoid fetching full request body)
                burp.api.montoya.proxy.ProxyHttpRequestResponse proxyEntry = urlToProxyEntryMap.get(url);
                HttpRequest request = null;
                if (proxyEntry != null) {
                    // Try to get request from proxy entry - only need headers, not full body
                    try {
                        request = (HttpRequest) proxyEntry.getClass().getMethod("httpRequest").invoke(proxyEntry);
                    } catch (Exception e1) {
                        try {
                            request = (HttpRequest) proxyEntry.getClass().getMethod("request").invoke(proxyEntry);
                        } catch (Exception e2) {
                            try {
                                request = (HttpRequest) proxyEntry.getClass().getMethod("finalRequest").invoke(proxyEntry);
                            } catch (Exception e3) {
                                // Will use URL fallback
                            }
                        }
                    }
                }
                // Fallback: create minimal request from URL if proxy entry not found or request extraction failed
                // This creates a request with just the URL (no headers, but that's okay - buildHeadersJson handles null)
                if (request == null) {
                    request = HttpRequest.httpRequestFromUrl(url);
                }
                final HttpRequest requestForSend = request;
                            
                // Send to Jsmon API and capture result
                processedUrls.add(url);
                
                burp.api.JsmonApiClient.SendResult result;
                
                // Check if VPN mode is enabled
                if (config.isVpnMode()) {
                    // VPN Mode: Extract response body and send directly
                    String responseBody = null;
                    if (proxyEntry != null) {
                        try {
                            // Try to get response from proxy entry using reflection (consistent with existing code)
                            Object responseObj = null;
                            try {
                                java.lang.reflect.Method responseMethod = proxyEntry.getClass().getMethod("httpResponse");
                                responseObj = responseMethod.invoke(proxyEntry);
                            } catch (Exception ex1) {
                                try {
                                    java.lang.reflect.Method responseMethod = proxyEntry.getClass().getMethod("response");
                                    responseObj = responseMethod.invoke(proxyEntry);
                                } catch (Exception ex2) {
                                    // Will be null
                                }
                            }
                            
                            if (responseObj != null) {
                                // Try bodyToString() method
                                try {
                                    java.lang.reflect.Method bodyMethod = responseObj.getClass().getMethod("bodyToString");
                                    responseBody = (String) bodyMethod.invoke(responseObj);
                                    logging.logToOutput("Jsmon: VPN Mode - Extracted response body (" + (responseBody != null ? responseBody.length() : 0) + " chars) for: " + url);
                                } catch (Exception bodyEx) {
                                    logging.logToError("Jsmon: VPN Mode - bodyToString() failed for: " + url + " - " + bodyEx.getMessage());
                                }
                            } else {
                                logging.logToOutput("Jsmon: VPN Mode - Response object is null for: " + url);
                            }
                        } catch (Exception e) {
                            logging.logToError("Jsmon: VPN Mode - Could not extract response body for: " + url + " - " + e.getMessage());
                        }
                    } else {
                        logging.logToOutput("Jsmon: VPN Mode - proxyEntry is null for: " + url);
                    }
                    
                    if (responseBody != null && !responseBody.isEmpty()) {
                        final String responseBodyForSend = responseBody;
                        result = executeSingleSend(() ->
                                apiClient.sendDirectFileScan(url, responseBodyForSend, workspaceId, apiKey)
                        );
                        if (statusCallback != null && result.isSuccess()) {
                            statusCallback.accept("[" + fileNum + "/" + scannableFiles.size() + "] VPN Mode - Sending: " + url);
                        }
                    } else {
                        logging.logToOutput("Jsmon: VPN Mode - Skipping (no response body): " + url);
                        result = new burp.api.JsmonApiClient.SendResult(false, "No response body available");
                    }
                } else {
                    // Normal mode: Send URL only
                    result = executeSingleSend(() ->
                            apiClient.sendToJsmon(url, workspaceId, apiKey, requestForSend)
                    );
                }
                
                if (result.isSuccess()) {
                    scannedCount++;
                    String modePrefix = config.isVpnMode() ? "VPN Mode - " : "";
                    if (statusCallback != null) {
                        statusCallback.accept("[" + fileNum + "/" + scannableFiles.size() + "] ✓ " + modePrefix + "Success: " + url);
                    }
                    logging.logToOutput("Jsmon: [" + fileNum + "/" + scannableFiles.size() + "] ✓ " + modePrefix + "Successfully scanned: " + url);
                    // Refresh user profile to update JSScan credits after each successful scan
                    if (tab != null) {
                        tab.fetchAndDisplayUserProfile();
                    }
                } else {
                    failedCount++;
                    String modePrefix = config.isVpnMode() ? "VPN Mode - " : "";
                    String errorMsg = result.getErrorMessage();
                    String failureMessage = "[" + fileNum + "/" + scannableFiles.size() + "] ✗ " + modePrefix + "Failed: " + url;
                    if (errorMsg != null && !errorMsg.isEmpty()) {
                        failureMessage += " - " + errorMsg;
                    }
                    if (statusCallback != null) {
                        statusCallback.accept(failureMessage);
                    }
                    logging.logToError("Jsmon: " + failureMessage);
                }
            }
            
            // Final summary
            String summary = "Scan completed: " + scannedCount + " succeeded, " + failedCount + " failed";
            if (statusCallback != null) {
                statusCallback.accept("✓ " + summary);
            }
            logging.logToOutput("Jsmon: Manual scan completed. " + summary);
            
            // Fetch secrets after scan completes if at least one scan was attempted (success or failure)
            if ((scannedCount > 0 || failedCount > 0) && tab != null) {
                tab.fetchAndDisplaySecrets();
            }
            
        } catch (Exception e) {
            logging.logToError("Jsmon: Error scanning HTTP history: " + e.getMessage());
            e.printStackTrace();
            if (statusCallback != null) {
                statusCallback.accept("✗ Error: " + e.getMessage());
            }
        }
        
        return scannedCount;
    }
    
    /**
     * Fetch total counts for all intelligence fields
     * Returns a map of field name to count
     */
    public java.util.Map<String, Integer> fetchTotalCounts(String workspaceId, String apiKey) {
        return apiClient.fetchTotalCounts(workspaceId, apiKey);
    }
    
    /**
     * Fetch secrets from Jsmon using the REST API (keysAndSecrets endpoint)
     * @param page Page number to fetch (1-based)
     */
    public String fetchSecrets(String workspaceId, String apiKey, int page) {
        return apiClient.fetchSecrets(workspaceId, apiKey, page);
    }
    
    /**
     * Fetch secrets from Jsmon (backward compatibility - fetches page 1)
     */
    public String fetchSecrets(String workspaceId, String apiKey) {
        return fetchSecrets(workspaceId, apiKey, 1);
    }

    /**
     * Fetch URLs from Jsmon intelligence API
     * @param page Page number to fetch (1-based)
     * Returns a list of URL entries with timestamps
     */
    public List<JsUrlEntry> fetchJsUrls(String workspaceId, String apiKey, int page) {
        return apiClient.fetchIntelligenceData(workspaceId, apiKey, "jsurls", page);
    }
    
    /**
     * Fetch URLs (backward compatibility - fetches page 1)
     */
    public List<JsUrlEntry> fetchJsUrls(String workspaceId, String apiKey) {
        return fetchJsUrls(workspaceId, apiKey, 1);
    }
    
    /**
     * Fetch API paths from Jsmon intelligence API
     * @param page Page number to fetch (1-based)
     * Returns a list of API path entries with timestamps
     */
    public List<JsUrlEntry> fetchApiPaths(String workspaceId, String apiKey, int page) {
        return apiClient.fetchIntelligenceData(workspaceId, apiKey, "apipaths", page);
    }
    
    /**
     * Fetch API paths (backward compatibility - fetches page 1)
     */
    public List<JsUrlEntry> fetchApiPaths(String workspaceId, String apiKey) {
        return fetchApiPaths(workspaceId, apiKey, 1);
    }
    
    /**
     * Fetch URLs from Jsmon intelligence API
     * @param page Page number to fetch (1-based)
     * Returns a list of URL entries with timestamps
     */
    public List<JsUrlEntry> fetchUrls(String workspaceId, String apiKey, int page) {
        return apiClient.fetchIntelligenceData(workspaceId, apiKey, "urls", page);
    }
    
    /**
     * Fetch Domains from Jsmon intelligence API
     * @param page Page number to fetch (1-based)
     * Returns a list of domain entries with timestamps
     */
    public List<JsUrlEntry> fetchDomains(String workspaceId, String apiKey, int page) {
        return apiClient.fetchIntelligenceData(workspaceId, apiKey, "domains", page);
    }
    
    /**
     * Fetch IP addresses from Jsmon intelligence API
     * @param page Page number to fetch (1-based)
     * Returns a list of IP address entries with timestamps
     */
    public List<JsUrlEntry> fetchIpAddresses(String workspaceId, String apiKey, int page) {
        return apiClient.fetchIntelligenceData(workspaceId, apiKey, "ipaddresses", page);
    }
    
    /**
     * Fetch Emails from Jsmon intelligence API
     * @param page Page number to fetch (1-based)
     * Returns a list of email entries with timestamps
     */
    public List<JsUrlEntry> fetchEmails(String workspaceId, String apiKey, int page) {
        return apiClient.fetchIntelligenceData(workspaceId, apiKey, "emails", page);
    }
    
    /**
     * Fetch S3 buckets from Jsmon intelligence API
     * @param page Page number to fetch (1-based)
     * Returns a list of S3 bucket entries with timestamps
     */
    public List<JsUrlEntry> fetchS3Buckets(String workspaceId, String apiKey, int page) {
        return apiClient.fetchIntelligenceData(workspaceId, apiKey, "s3domains", page);
    }
    
    /**
     * Fetch Invalid Node Modules from Jsmon intelligence API
     * @param page Page number to fetch (1-based)
     * Returns a list of invalid node module entries with timestamps
     */
    public List<JsUrlEntry> fetchInvalidNodeModules(String workspaceId, String apiKey, int page) {
        return apiClient.fetchIntelligenceData(workspaceId, apiKey, "invalidnodemodules", page);
    }

    private burp.api.JsmonApiClient.SendResult executeSingleSend(java.util.function.Supplier<burp.api.JsmonApiClient.SendResult> sendAction) {
        synchronized (sendLock) {
            long now = System.currentTimeMillis();
            long waitMs = MIN_SEND_INTERVAL_MS - (now - lastSendTimestampMs);
            if (waitMs > 0) {
                try {
                    Thread.sleep(waitMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return new burp.api.JsmonApiClient.SendResult(false, "Send interrupted while rate limiting");
                }
            }

            lastSendTimestampMs = System.currentTimeMillis();
            return sendAction.get();
        }
    }
}


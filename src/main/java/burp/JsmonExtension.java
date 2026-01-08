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
    private volatile Thread currentScanThread = null;
    
    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        this.logging = api.logging();
        this.config = new JsmonConfig(api);
        this.apiClient = new JsmonApiClient(logging);
        this.urlProcessor = new JsmonUrlProcessor(logging);
        
        // Set extension name
        api.extension().setName("JSMon Extension");
        
        // Register HTTP handler
        api.http().registerHttpHandler(this);
        
        // Register unloading handler
        api.extension().registerUnloadingHandler(() -> {
            // Extension unloaded - configuration is already saved automatically
        });
        
        // Create UI tab
        this.tab = new JsmonTab(api, this);
        api.userInterface().registerSuiteTab("JSMon", tab);
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
        String url = httpRequest.url();
        
        // Check if URL matches scoped domain
        String scopedDomain = config.getScopedDomain();
        if (scopedDomain != null && !scopedDomain.isEmpty()) {
            if (!urlProcessor.isUrlInScope(url, scopedDomain)) {
                return ResponseReceivedAction.continueWith(response);
            }
        }
        
        // Get HttpResponse from HttpResponseReceived and extract Content-Type
        burp.api.montoya.http.message.responses.HttpResponse httpResponse = null;
        String contentType = null;
        
        // Try to get HttpResponse from HttpResponseReceived using reflection (Montoya API doesn't expose this directly)
        try {
            // Method 1: Try response() method using reflection
            java.lang.reflect.Method responseMethod = response.getClass().getMethod("response");
            Object responseObj = responseMethod.invoke(response);
            if (responseObj instanceof burp.api.montoya.http.message.responses.HttpResponse) {
                httpResponse = (burp.api.montoya.http.message.responses.HttpResponse) responseObj;
            }
        } catch (Exception e) {
            // Try alternative method names
            try {
                java.lang.reflect.Method[] methods = response.getClass().getMethods();
                for (java.lang.reflect.Method method : methods) {
                    if (method.getParameterCount() == 0 
                        && burp.api.montoya.http.message.responses.HttpResponse.class.isAssignableFrom(method.getReturnType())) {
                        try {
                            Object responseObj = method.invoke(response);
                            if (responseObj instanceof burp.api.montoya.http.message.responses.HttpResponse) {
                                httpResponse = (burp.api.montoya.http.message.responses.HttpResponse) responseObj;
                                break;
                            }
                        } catch (Exception e2) {
                            // Continue
                        }
                    }
                }
            } catch (Exception e2) {
                if (logging != null) {
                    logging.logToError("JSMon: Failed to extract HttpResponse from HttpResponseReceived for '" + url + "': " + e2.getMessage());
                }
            }
        }
        
        // Extract Content-Type from HttpResponse (more reliable than reflection on HttpResponseReceived)
        if (httpResponse != null) {
            contentType = httpResponse.headerValue("Content-Type");
            if (logging != null) {
                logging.logToOutput("JSMon: Extracted Content-Type from HttpResponse for '" + url + "': " + (contentType != null ? contentType : "null"));
            }
        } else {
            // Fallback: Try to get Content-Type directly from HttpResponseReceived using reflection
            try {
                java.lang.reflect.Method headerValueMethod = response.getClass().getMethod("headerValue", String.class);
                contentType = (String) headerValueMethod.invoke(response, "Content-Type");
            } catch (Exception e) {
                // Try headers() method as last resort
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
            if (logging != null) {
                logging.logToOutput("JSMon: Extracted Content-Type from HttpResponseReceived (fallback) for '" + url + "': " + (contentType != null ? contentType : "null"));
            }
        }
        
        // Check if it's a scannable file (check URL extension and Content-Type)
        // Use the HttpResponse object directly if available, otherwise use Content-Type string
        boolean isScannable;
        if (httpResponse != null) {
            isScannable = urlProcessor.isJavaScriptFile(url, httpResponse);
            if (logging != null) {
                logging.logToOutput("JSMon: Scannable check for '" + url + "' using HttpResponse: " + isScannable);
            }
        } else {
            isScannable = urlProcessor.isJavaScriptFile(url, contentType);
            if (logging != null) {
                logging.logToOutput("JSMon: Scannable check for '" + url + "' using Content-Type string (" + (contentType != null ? contentType : "null") + "): " + isScannable);
            }
        }
        
        if (isScannable) {
            // Avoid processing the same URL multiple times
            if (!processedUrls.contains(url)) {
                processedUrls.add(url);
                logging.logToOutput("JSMon: Detected scannable file: " + url + (contentType != null ? " (Content-Type: " + contentType + ")" : ""));
                // Process new scannable files as they come in
                if (httpResponse != null) {
                    processJavaScriptFile(httpRequest, httpResponse);
                } else {
                    logging.logToError("JSMon: Cannot process file - HttpResponse is null for: " + url);
                }
            }
        }
        
        return ResponseReceivedAction.continueWith(response);
    }
    
    private void processJavaScriptFile(HttpRequest request, burp.api.montoya.http.message.responses.HttpResponse response) {
        // Check if automatic scanning is still enabled before processing
        if (!config.isAutomateScan()) {
            return;
        }
        
        String apiKey = config.getApiKey();
        String workspaceId = config.getWorkspaceId();
        
        if (apiKey == null || apiKey.isEmpty() || workspaceId == null || workspaceId.isEmpty()) {
            logging.logToOutput("JSMon: Skipping file - API key or workspace ID not configured");
            return;
        }
        
        String url = request.url();
        
        // Log to UI if tab is available
        if (tab != null) {
            tab.appendStatusMessage("🔄 Auto-scanning: " + url);
        }
        
        // Execute JSMon API call with headers
        boolean success = apiClient.sendToJsmon(url, workspaceId, apiKey, request);
        
        // Log result to UI
        if (tab != null) {
            if (success) {
                tab.appendStatusMessage("  ✓ Success: " + url);
                // Fetch secrets after successful scan
                tab.fetchAndDisplaySecrets();
                // Refresh user profile to update JSScan credits
                tab.fetchAndDisplayUserProfile();
            } else {
                tab.appendStatusMessage("  ✗ Failed: " + url);
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
                logging.logToOutput("JSMon: Automatic scanning disabled - stopping current scan");
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
                logging.logToOutput("JSMon: Automatic scanning enabled - scanning existing files in history...");
                
                // Pass callback to update UI in real-time
                int count = scanHttpHistory((statusMessage) -> {
                    // Check if automatic scanning was disabled during scan
                    if (!config.isAutomateScan()) {
                        if (tab != null) {
                            tab.appendStatusMessage("  ⚠ Scan stopped - automatic scanning was disabled");
                        }
                        logging.logToOutput("JSMon: Scan stopped - automatic scanning was disabled");
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
                    logging.logToOutput("JSMon: Automatic scanning initialized - " + count + " existing file(s) processed");
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
            logging.logToOutput("JSMon: Cannot scan - API key or workspace ID not configured");
            if (statusCallback != null) {
                statusCallback.accept("✗ Cannot scan - API key or workspace ID not configured");
            }
            return 0;
        }
        
        int scannedCount = 0;
        int failedCount = 0;
        Set<String> scannedUrls = ConcurrentHashMap.newKeySet();
        java.util.List<String> jsFiles = new ArrayList<>();
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
                    // Get response from proxy entry to check Content-Type
                    burp.api.montoya.http.message.responses.HttpResponse httpResponse = null;
                    String contentType = null;
                    
                    try {
                        // Try to get response from proxy entry
                        java.lang.reflect.Method responseMethod = proxyEntry.getClass().getMethod("httpResponse");
                        Object responseObj = responseMethod.invoke(proxyEntry);
                        if (responseObj instanceof burp.api.montoya.http.message.responses.HttpResponse) {
                            httpResponse = (burp.api.montoya.http.message.responses.HttpResponse) responseObj;
                            contentType = httpResponse.headerValue("Content-Type");
                        }
                    } catch (Exception e1) {
                        try {
                            // Try alternative method name
                            java.lang.reflect.Method responseMethod = proxyEntry.getClass().getMethod("response");
                            Object responseObj = responseMethod.invoke(proxyEntry);
                            if (responseObj instanceof burp.api.montoya.http.message.responses.HttpResponse) {
                                httpResponse = (burp.api.montoya.http.message.responses.HttpResponse) responseObj;
                                contentType = httpResponse.headerValue("Content-Type");
                            }
                        } catch (Exception e2) {
                            // Try to find any method that returns HttpResponse
                            try {
                                java.lang.reflect.Method[] methods = proxyEntry.getClass().getMethods();
                                for (java.lang.reflect.Method method : methods) {
                                    if (method.getParameterCount() == 0 
                                        && burp.api.montoya.http.message.responses.HttpResponse.class.isAssignableFrom(method.getReturnType())) {
                                        try {
                                            Object responseObj = method.invoke(proxyEntry);
                                            if (responseObj instanceof burp.api.montoya.http.message.responses.HttpResponse) {
                                                httpResponse = (burp.api.montoya.http.message.responses.HttpResponse) responseObj;
                                                contentType = httpResponse.headerValue("Content-Type");
                                                break;
                                            }
                                        } catch (Exception e3) {
                                            // Continue
                                        }
                                    }
                                }
                            } catch (Exception e3) {
                                // Response not available, will check extension only
                            }
                        }
                    }
                    
                    // Use urlProcessor to check BOTH extension AND Content-Type (OR logic)
                    // This ensures consistent matching with automatic scanning
                    boolean isScannableFile;
                    if (httpResponse != null) {
                        isScannableFile = urlProcessor.isJavaScriptFile(url, httpResponse);
                    } else {
                        isScannableFile = urlProcessor.isJavaScriptFile(url, contentType);
                    }
                    
                    if (logging != null && isScannableFile) {
                        logging.logToOutput("JSMon: History scan - Detected scannable file: " + url + (contentType != null ? " (Content-Type: " + contentType + ")" : ""));
                    }
                    
                    if (isScannableFile && !scannedUrls.contains(url)) {
                        scannedUrls.add(url);
                        jsFiles.add(url);
                        
                        // Store the proxy entry for later use to extract headers if possible
                        urlToProxyEntryMap.put(url, proxyEntry);
                    }
                } catch (Exception e) {
                    logging.logToError("JSMon: Error processing entry: " + e.getMessage());
                    continue;
                }
            }
            
            if (jsFiles.isEmpty()) {
                if (statusCallback != null) {
                    statusCallback.accept("✗ No scannable files found to scan");
                }
                logging.logToOutput("JSMon: No scannable files found to scan");
                return 0;
            }
            
            if (statusCallback != null) {
                statusCallback.accept("Found " + jsFiles.size() + " file(s) to scan");
            }
            logging.logToOutput("JSMon: Found " + jsFiles.size() + " file(s) to scan");
            
            // Second pass: scan each file sequentially with real-time updates
            for (int i = 0; i < jsFiles.size(); i++) {
                // Check if automatic scanning was disabled (for automatic scans) or thread was interrupted
                if (config.isAutomateScan() == false || Thread.currentThread().isInterrupted()) {
                    if (statusCallback != null) {
                        statusCallback.accept("⚠ Scan stopped by user");
                    }
                    logging.logToOutput("JSMon: Scan stopped - automatic scanning disabled or interrupted");
                    break;
                }
                
                String url = jsFiles.get(i);
                int fileNum = i + 1;
                
                // Show scanning status
                if (statusCallback != null) {
                    statusCallback.accept("[" + fileNum + "/" + jsFiles.size() + "] Scanning: " + url);
                }
                logging.logToOutput("JSMon: [" + fileNum + "/" + jsFiles.size() + "] Scanning: " + url);
                
                // Get the original request from the proxy entry (with all headers)
                burp.api.montoya.proxy.ProxyHttpRequestResponse proxyEntry = urlToProxyEntryMap.get(url);
                HttpRequest request = null;
                if (proxyEntry != null) {
                    // Try to get request from proxy entry - Montoya API might have different method names
                    try {
                        request = (HttpRequest) proxyEntry.getClass().getMethod("httpRequest").invoke(proxyEntry);
                    } catch (Exception e1) {
                        try {
                            request = (HttpRequest) proxyEntry.getClass().getMethod("request").invoke(proxyEntry);
                        } catch (Exception e2) {
                            try {
                                request = (HttpRequest) proxyEntry.getClass().getMethod("finalRequest").invoke(proxyEntry);
                            } catch (Exception e3) {
                                logging.logToError("JSMon: Could not extract request from proxy entry, using URL fallback");
                            }
                        }
                    }
                }
                // Fallback: create request from URL if proxy entry not found or request extraction failed
                if (request == null) {
                    request = HttpRequest.httpRequestFromUrl(url);
                }
                            
                // Send to JSMon API and capture result (with headers from original request)
                processedUrls.add(url);
                
                boolean success = apiClient.sendToJsmon(url, workspaceId, apiKey, request);
                
                if (success) {
                    scannedCount++;
                    if (statusCallback != null) {
                        statusCallback.accept("[" + fileNum + "/" + jsFiles.size() + "] ✓ Success: " + url);
                    }
                    logging.logToOutput("JSMon: [" + fileNum + "/" + jsFiles.size() + "] ✓ Successfully scanned: " + url);
                    // Refresh user profile to update JSScan credits after each successful scan
                    if (tab != null) {
                        tab.fetchAndDisplayUserProfile();
                    }
                } else {
                    failedCount++;
                    if (statusCallback != null) {
                        statusCallback.accept("[" + fileNum + "/" + jsFiles.size() + "] ✗ Failed: " + url);
                    }
                    logging.logToError("JSMon: [" + fileNum + "/" + jsFiles.size() + "] ✗ Failed to scan: " + url);
                }
            }
            
            // Final summary
            String summary = "Scan completed: " + scannedCount + " succeeded, " + failedCount + " failed";
            if (statusCallback != null) {
                statusCallback.accept("✓ " + summary);
            }
            logging.logToOutput("JSMon: Manual scan completed. " + summary);
            
            // Fetch secrets after scan completes if at least one scan was attempted (success or failure)
            if ((scannedCount > 0 || failedCount > 0) && tab != null) {
                tab.fetchAndDisplaySecrets();
            }
            
        } catch (Exception e) {
            logging.logToError("JSMon: Error scanning HTTP history: " + e.getMessage());
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
     * Fetch secrets from JSMon using the REST API (keysAndSecrets endpoint)
     * @param page Page number to fetch (1-based)
     */
    public String fetchSecrets(String workspaceId, String apiKey, int page) {
        return apiClient.fetchSecrets(workspaceId, apiKey, page);
    }
    
    /**
     * Fetch secrets from JSMon (backward compatibility - fetches page 1)
     */
    public String fetchSecrets(String workspaceId, String apiKey) {
        return fetchSecrets(workspaceId, apiKey, 1);
    }

    /**
     * Fetch JS URLs from JSMon intelligence API
     * @param page Page number to fetch (1-based)
     * Returns a list of JS URL entries with timestamps
     */
    public List<JsUrlEntry> fetchJsUrls(String workspaceId, String apiKey, int page) {
        return apiClient.fetchIntelligenceData(workspaceId, apiKey, "jsurls", page);
    }
    
    /**
     * Fetch JS URLs (backward compatibility - fetches page 1)
     */
    public List<JsUrlEntry> fetchJsUrls(String workspaceId, String apiKey) {
        return fetchJsUrls(workspaceId, apiKey, 1);
    }
    
    /**
     * Fetch API paths from JSMon intelligence API
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
     * Fetch URLs from JSMon intelligence API
     * @param page Page number to fetch (1-based)
     * Returns a list of URL entries with timestamps
     */
    public List<JsUrlEntry> fetchUrls(String workspaceId, String apiKey, int page) {
        return apiClient.fetchIntelligenceData(workspaceId, apiKey, "urls", page);
    }
    
    /**
     * Fetch Domains from JSMon intelligence API
     * @param page Page number to fetch (1-based)
     * Returns a list of domain entries with timestamps
     */
    public List<JsUrlEntry> fetchDomains(String workspaceId, String apiKey, int page) {
        return apiClient.fetchIntelligenceData(workspaceId, apiKey, "domains", page);
    }
    
    /**
     * Fetch IP addresses from JSMon intelligence API
     * @param page Page number to fetch (1-based)
     * Returns a list of IP address entries with timestamps
     */
    public List<JsUrlEntry> fetchIpAddresses(String workspaceId, String apiKey, int page) {
        return apiClient.fetchIntelligenceData(workspaceId, apiKey, "ipaddresses", page);
    }
    
    /**
     * Fetch Emails from JSMon intelligence API
     * @param page Page number to fetch (1-based)
     * Returns a list of email entries with timestamps
     */
    public List<JsUrlEntry> fetchEmails(String workspaceId, String apiKey, int page) {
        return apiClient.fetchIntelligenceData(workspaceId, apiKey, "emails", page);
    }
    
    /**
     * Fetch S3 buckets from JSMon intelligence API
     * @param page Page number to fetch (1-based)
     * Returns a list of S3 bucket entries with timestamps
     */
    public List<JsUrlEntry> fetchS3Buckets(String workspaceId, String apiKey, int page) {
        return apiClient.fetchIntelligenceData(workspaceId, apiKey, "s3domains", page);
    }
    
    /**
     * Fetch Invalid Node Modules from JSMon intelligence API
     * @param page Page number to fetch (1-based)
     * Returns a list of invalid node module entries with timestamps
     */
    public List<JsUrlEntry> fetchInvalidNodeModules(String workspaceId, String apiKey, int page) {
        return apiClient.fetchIntelligenceData(workspaceId, apiKey, "invalidnodemodules", page);
    }
}


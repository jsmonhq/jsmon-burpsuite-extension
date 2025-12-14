package burp.util;

import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.logging.Logging;

public class JsmonUrlProcessor {
    private final Logging logging;
    
    public JsmonUrlProcessor(Logging logging) {
        this.logging = logging;
    }
    
    /**
     * Check if URL is in scope based on scoped domain(s)
     */
    public boolean isUrlInScope(String url, String domains) {
        if (domains == null || domains.trim().isEmpty()) {
            return true; // No scope means all URLs are in scope
        }
        
        try {
            java.net.URL urlObj = new java.net.URL(url);
            String host = urlObj.getHost().toLowerCase();
            
            // Split domains by comma, newline, or space
            String[] domainList = domains.split("[,\\n\\r\\s]+");
            
            for (String domain : domainList) {
                String domainLower = domain.toLowerCase().trim();
                
                // Skip empty domains
                if (domainLower.isEmpty()) {
                    continue;
                }
                
                // Remove leading/trailing dots from domain
                domainLower = domainLower.replaceAll("^\\.+|\\.+$", "");
                
                // Check if host exactly matches domain
                if (host.equals(domainLower)) {
                    return true;
                }
                
                // Check if host is a subdomain of the domain
                if (host.endsWith("." + domainLower)) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            if (logging != null) {
                logging.logToError("Error parsing URL: " + url + " - " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * Check if response is a JavaScript file
     */
    public boolean isJavaScriptFile(String url, burp.api.montoya.http.message.responses.HttpResponse response) {
        // Check URL extension
        url = url.toLowerCase();
        if (url.endsWith(".js") || url.contains(".js?") || url.contains(".js#")) {
            return true;
        }
        
        // Check Content-Type header
        String contentType = response.headerValue("Content-Type");
        if (contentType != null && contentType.toLowerCase().contains("application/javascript")) {
            return true;
        }
        if (contentType != null && contentType.toLowerCase().contains("text/javascript")) {
            return true;
        }
        
        return false;
    }
}


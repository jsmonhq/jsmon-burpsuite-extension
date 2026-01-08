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
     * Supported file extensions for scanning
     * Note: Longer extensions should come first to avoid false matches (e.g., .jsx before .js)
     */
    private static final String[] SCANNABLE_EXTENSIONS = {
        ".jspa", ".tsx", ".jsx", ".xhtml", ".aspx", ".ashx", ".asmx",
        ".asp", ".cfm", ".svc", ".cgi", ".pl",
        ".html", ".htm", ".jsp", ".do", ".php", ".txt",
        ".xml", ".json", ".bak", ".ts", ".mjs", ".env", ".js"
    };
    
    /**
     * Content-Type strings that indicate scannable content
     */
    private static final String[] SCANNABLE_CONTENT_TYPES = {
        "javascript", "ecmascript", "html", "xhtml", "text", "plain", "json", "xml", "php", "jsp", "component"
    };
    
    /**
     * Check if URL matches any of the scannable file extensions
     * More precise matching: checks if extension is at end of path or before query/hash
     */
    private boolean hasScannableExtension(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        
        url = url.toLowerCase();
        
        // Remove query string and hash for extension checking
        String urlPath = url;
        int queryIndex = url.indexOf('?');
        int hashIndex = url.indexOf('#');
        
        if (queryIndex != -1) {
            urlPath = url.substring(0, queryIndex);
        } else if (hashIndex != -1) {
            urlPath = url.substring(0, hashIndex);
        }
        
        // Check each extension (longer ones first to avoid false matches)
        for (String ext : SCANNABLE_EXTENSIONS) {
            // Check if URL ends with the extension
            if (urlPath.endsWith(ext)) {
                return true;
            }
            // Also check if extension appears before query/hash in full URL
            int extIndex = url.indexOf(ext);
            if (extIndex != -1) {
                int afterExt = extIndex + ext.length();
                // Check if extension is followed by ? or # or end of string
                if (afterExt >= url.length() || 
                    url.charAt(afterExt) == '?' || 
                    url.charAt(afterExt) == '#') {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Check if response is a scannable file (JavaScript, HTML, JSON, or other supported extensions)
     */
    public boolean isScannableFile(String url, burp.api.montoya.http.message.responses.HttpResponse response) {
        // Check URL extension for all supported file types
        if (hasScannableExtension(url)) {
            return true;
        }
        
        // Check Content-Type header for scannable content types (if response is available)
        if (response != null) {
            String contentType = response.headerValue("Content-Type");
            return isScannableByContentType(contentType);
        }
        
        return false;
    }
    
    /**
     * Check if a file is scannable based on URL and Content-Type string
     * Overloaded method for cases where we only have Content-Type string
     */
    public boolean isScannableFile(String url, String contentType) {
        // Check URL extension for all supported file types
        boolean hasExtension = hasScannableExtension(url);
        if (logging != null) {
            logging.logToOutput("JSMon: URL extension check for '" + url + "': " + hasExtension);
        }
        
        if (hasExtension) {
            return true;
        }
        
        // If no extension match, check Content-Type header for scannable content types
        if (logging != null) {
            logging.logToOutput("JSMon: No extension match, checking Content-Type: " + (contentType != null ? contentType : "null"));
        }
        boolean contentTypeMatch = isScannableByContentType(contentType);
        if (logging != null) {
            logging.logToOutput("JSMon: Content-Type check result: " + contentTypeMatch);
        }
        return contentTypeMatch;
    }
    
    /**
     * Check if Content-Type indicates scannable content
     */
    private boolean isScannableByContentType(String contentType) {
        if (contentType != null && !contentType.isEmpty()) {
            // Remove any parameters from Content-Type (e.g., "text/x-component; charset=utf-8" -> "text/x-component")
            String contentTypeBase = contentType.split(";")[0].trim().toLowerCase();
            String contentTypeLower = contentType.toLowerCase();
            
            for (String scannableType : SCANNABLE_CONTENT_TYPES) {
                // Check both the full Content-Type and the base (without parameters)
                if (contentTypeLower.contains(scannableType) || contentTypeBase.contains(scannableType)) {
                    if (logging != null) {
                        logging.logToOutput("JSMon: Content-Type match - '" + contentType + "' contains '" + scannableType + "'");
                    }
                    return true;
                }
            }
            if (logging != null) {
                logging.logToOutput("JSMon: Content-Type '" + contentType + "' (base: '" + contentTypeBase + "') did not match any scannable types");
            }
        } else {
            if (logging != null) {
                logging.logToOutput("JSMon: Content-Type is null or empty");
            }
        }
        return false;
    }
}


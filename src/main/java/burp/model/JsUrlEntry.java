package burp.model;

public class JsUrlEntry {
    private String url;
    private String scannedAt;
    
    public JsUrlEntry(String url, String scannedAt) {
        this.url = url;
        this.scannedAt = scannedAt;
    }
    
    public String getUrl() {
        return url;
    }
    
    public String getScannedAt() {
        return scannedAt;
    }
}


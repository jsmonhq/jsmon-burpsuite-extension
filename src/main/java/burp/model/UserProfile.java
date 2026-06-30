package burp.model;

public class UserProfile {
    public String name;
    public String email;
    public String tier;
    /** @deprecated use structured limit fields */
    public String remaining;
    public String accountType;

    public int jsScanRemaining = -1;
    public int jsScanTotal = -1;
    public int addOnJsScan = 0;

    public int getAvailableJsScan() {
        if (jsScanRemaining < 0) {
            return -1;
        }
        return Math.max(0, jsScanRemaining) + Math.max(0, addOnJsScan);
    }

    public String getJsScanDisplay() {
        int available = getAvailableJsScan();
        if (available < 0) {
            return "—";
        }
        return String.valueOf(available);
    }

    public String getJsScanBadgeText() {
        int available = getAvailableJsScan();
        if (available < 0) {
            return "— scans left";
        }
        return available + " scans left";
    }
}

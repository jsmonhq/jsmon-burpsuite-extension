package burp.util;

import burp.api.montoya.logging.Logging;
import burp.model.JsUrlEntry;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class JsmonJsonParser {
    private final Logging logging;
    
    public JsmonJsonParser(Logging logging) {
        this.logging = logging;
    }
    
    /**
     * Parse workspaces from JSON response
     */
    public List<burp.model.Workspace> parseWorkspaces(String json) {
        List<burp.model.Workspace> workspaces = new ArrayList<>();

        if (json == null || json.isEmpty()) {
            return workspaces;
        }

        String arrayContent = extractJsonArrayContent(json, "\"workspaces\"");
        if (arrayContent == null) {
            arrayContent = extractJsonArrayContent(json, null);
        }
        if (arrayContent == null) {
            return workspaces;
        }

        List<String> objects = splitJsonObjects(arrayContent);
        for (String obj : objects) {
            String id = extractJsonFieldSimple(obj, "wkspId");
            String name = extractJsonFieldSimple(obj, "name");
            if (id != null && name != null) {
                workspaces.add(new burp.model.Workspace(name, id));
            }
        }
        return workspaces;
    }

    private String extractJsonArrayContent(String json, String fieldName) {
        int searchFrom = 0;
        if (fieldName != null) {
            int fieldIndex = json.indexOf(fieldName);
            if (fieldIndex == -1) {
                return null;
            }
            searchFrom = fieldIndex;
        }

        int arrayStart = json.indexOf("[", searchFrom);
        int arrayEnd = json.lastIndexOf("]");
        if (arrayStart == -1 || arrayEnd == -1 || arrayEnd <= arrayStart) {
            return null;
        }
        return json.substring(arrayStart + 1, arrayEnd);
    }
    
    /**
     * Extract JSON field value (simple string fields)
     */
    public String extractJsonFieldSimple(String json, String field) {
        if (json == null) return null;
        try {
            String pattern = "\"" + field + "\"\\s*:\\s*\"([^\"]+)\"";
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(json);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception ignored) {
        }
        return null;
    }
    
    /**
     * Structured JsScan limits parsed from viewProfile (aligned with Chrome/Firefox extensions).
     */
    public static final class JsScanLimits {
        public int remaining = -1;
        public int total = -1;
        public int addOn = 0;
    }

    /**
     * Extract JsScan credits from profile response.
     * Handles data.apiCallLimits.jsScan object/scalar plus data.addOnLimits.JsScan.
     */
    public JsScanLimits extractJsScanLimits(String json) {
        JsScanLimits limits = new JsScanLimits();
        if (json == null || json.isEmpty()) {
            return limits;
        }

        try {
            String apiBlock = extractJsonObjectBlock(json, "apiCallLimits");
            String searchIn = apiBlock != null ? apiBlock : json;

            String jsScanBlock = extractJsonObjectBlock(searchIn, "jsScan");
            if (jsScanBlock != null) {
                Integer rem = extractJsonIntField(jsScanBlock, "remaining");
                Integer tot = extractJsonIntField(jsScanBlock, "total");
                if (rem != null) {
                    limits.remaining = rem;
                }
                if (tot != null) {
                    limits.total = tot;
                }
            } else {
                Integer flatJsScan = extractJsonIntField(searchIn, "JsScan");
                if (flatJsScan == null) {
                    flatJsScan = extractJsonIntField(searchIn, "jsScan");
                }
                if (flatJsScan != null) {
                    limits.remaining = flatJsScan;
                }
            }

            String addOnBlock = extractJsonObjectBlock(json, "addOnLimits");
            if (addOnBlock != null) {
                Integer addOn = extractJsonIntField(addOnBlock, "JsScan");
                if (addOn == null) {
                    addOn = extractJsonIntField(addOnBlock, "jsScan");
                }
                if (addOn != null) {
                    limits.addOn = addOn;
                }
            }
        } catch (Exception e) {
            if (logging != null) {
                logging.logToError("Jsmon: Error parsing JsScan limits: " + e.getMessage());
            }
        }

        return limits;
    }

    /**
     * @deprecated use {@link #extractJsScanLimits(String)} for structured parsing
     */
    public String extractJsScanCredits(String json) {
        JsScanLimits limits = extractJsScanLimits(json);
        if (limits.remaining < 0) {
            return null;
        }
        return String.valueOf(Math.max(0, limits.remaining) + Math.max(0, limits.addOn));
    }

    private String extractJsonObjectBlock(String json, String field) {
        if (json == null || field == null) {
            return null;
        }
        Pattern p = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\\{");
        Matcher m = p.matcher(json);
        if (!m.find()) {
            return null;
        }
        int start = m.end() - 1;
        int brace = 0;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                brace++;
            } else if (c == '}') {
                brace--;
                if (brace == 0) {
                    return json.substring(start, i + 1);
                }
            }
        }
        return null;
    }

    private Integer extractJsonIntField(String json, String field) {
        if (json == null || field == null) {
            return null;
        }
        try {
            Pattern p = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*(\\d+)");
            Matcher m = p.matcher(json);
            if (m.find()) {
                return Integer.parseInt(m.group(1));
            }
        } catch (Exception ignored) {
        }
        return null;
    }
    
    /**
     * Extract counts from totalCountAnalysis JSON response
     */
    public void extractCountsFromJson(String json, java.util.Map<String, Integer> counts) {
        if (json == null || json.isEmpty()) {
            return;
        }
        
        try {
            Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(\\d+)");
            Matcher matcher = pattern.matcher(json);
            
            while (matcher.find()) {
                String fieldName = matcher.group(1);
                int count = Integer.parseInt(matcher.group(2));
                counts.put(fieldName, count);
            }
        } catch (Exception e) {
            if (logging != null) {
                logging.logToError("Jsmon: Error parsing counts JSON: " + e.getMessage());
            }
        }
    }
    
    /**
     * Extract data objects from secrets response
     */
    public List<String> extractDataObjects(String json) {
        List<String> objects = new ArrayList<>();
        if (json == null || json.isEmpty()) {
            return objects;
        }

        int dataIndex = json.indexOf("\"data\"");
        if (dataIndex == -1) {
            return objects;
        }
        int arrayStart = json.indexOf("[", dataIndex);
        if (arrayStart == -1) {
            return objects;
        }
        int arrayEnd = json.indexOf("]", arrayStart);
        if (arrayEnd == -1) {
            return objects;
        }

        String arrayContent = json.substring(arrayStart + 1, arrayEnd);
        objects.addAll(splitJsonObjects(arrayContent));
        return objects;
    }
    
    /**
     * Extract URLs from intelligence API response
     */
    public List<JsUrlEntry> extractJsUrlsFromResponse(String json) {
        List<JsUrlEntry> entries = new ArrayList<>();
        if (json == null || json.isEmpty()) {
            return entries;
        }
        
        try {
            int dataIndex = json.indexOf("\"data\"");
            if (dataIndex == -1) {
                return entries;
            }
            
            int arrayStart = json.indexOf("[", dataIndex);
            if (arrayStart == -1) {
                return entries;
            }
            
            int arrayEnd = json.lastIndexOf("]");
            if (arrayEnd == -1 || arrayEnd <= arrayStart) {
                return entries;
            }
            
            String arrayContent = json.substring(arrayStart + 1, arrayEnd).trim();
            
            if (arrayContent.startsWith("\"") && !arrayContent.contains("{")) {
                // Simple string array
                Pattern stringPattern = Pattern.compile("\"([^\"]+)\"");
                Matcher stringMatcher = stringPattern.matcher(arrayContent);
                LocalDateTime fetchTime = LocalDateTime.now();
                String fetchTimeStr = fetchTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                while (stringMatcher.find()) {
                    String url = stringMatcher.group(1);
                    if (url != null && !url.isEmpty()) {
                        entries.add(new JsUrlEntry(url, fetchTimeStr));
                    }
                }
            } else {
                // Array of objects
                List<String> objects = splitJsonObjects(arrayContent);
                
                if (!objects.isEmpty() && logging != null) {
                    String sample = objects.get(0);
                    logging.logToOutput("Jsmon: Sample object from intelligence API: " + sample.substring(0, Math.min(300, sample.length())));
                }
                
                for (String obj : objects) {
                    String url = extractJsonFieldSimple(obj, "value");
                    
                    if (url != null && !url.isEmpty()) {
                        String timestamp = null;
                        String[] timestampFields = {"createdAt", "scannedAt", "lastScannedOn", "scannedOn", "timestamp", "time", "date"};
                        
                        for (String field : timestampFields) {
                            timestamp = extractJsonFieldSimple(obj, field);
                            if (timestamp != null && !timestamp.isEmpty() && !timestamp.equals("null")) {
                                break;
                            }
                        }
                        
                        if (timestamp == null || timestamp.isEmpty() || timestamp.equals("null")) {
                            Pattern datePattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}[^\"]*)\"");
                            Matcher dateMatcher = datePattern.matcher(obj);
                            if (dateMatcher.find()) {
                                timestamp = dateMatcher.group(2);
                            }
                        }
                        
                        String formattedTime;
                        if (timestamp == null || timestamp.isEmpty() || timestamp.equals("null")) {
                            LocalDateTime now = LocalDateTime.now();
                            formattedTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        } else {
                            formattedTime = formatTimestamp(timestamp);
                        }
                        entries.add(new JsUrlEntry(url, formattedTime));
                    }
                }
            }
        } catch (Exception e) {
            if (logging != null) {
                logging.logToError("Jsmon: Error parsing URLs from response: " + e.getMessage());
            }
        }
        
        return entries;
    }
    
    /**
     * Format timestamp from ISO format to readable format
     */
    private String formatTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return "—";
        }
        
        try {
            if (timestamp.length() > 19) {
                timestamp = timestamp.substring(0, 19);
            }
            return timestamp.replace("T", " ");
        } catch (Exception e) {
            return timestamp;
        }
    }
    
    /**
     * Split a JSON array content into individual object strings
     */
    private List<String> splitJsonObjects(String arrayContent) {
        List<String> list = new ArrayList<>();
        if (arrayContent == null || arrayContent.isEmpty()) {
            return list;
        }

        int brace = 0;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < arrayContent.length(); i++) {
            char c = arrayContent.charAt(i);
            if (c == '{') {
                brace++;
            } else if (c == '}') {
                brace--;
            }
            current.append(c);
            if (brace == 0 && current.length() > 0 && c == '}') {
                list.add(current.toString().trim());
                current.setLength(0);
            }
        }
        return list;
    }
    
    /**
     * Escape JSON string
     */
    public String jsonEscape(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}


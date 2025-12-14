package burp.api;

import burp.api.montoya.logging.Logging;
import burp.model.JsUrlEntry;
import burp.model.UserProfile;
import burp.model.Workspace;
import burp.util.JsmonJsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JsmonApiClient {
    private static final String API_BASE_URL = "https://api.jsmon.sh/api/v2";
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Logging logging;
    private final JsmonJsonParser jsonParser;
    
    public JsmonApiClient(Logging logging) {
        this.logging = logging;
        this.jsonParser = new JsmonJsonParser(logging);
    }
    
    /**
     * Fetch workspaces from JSMon API
     */
    public List<Workspace> fetchWorkspaces(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            if (logging != null) {
                logging.logToError("API key not set");
                logging.logToOutput("JSMon: API key is null or empty");
            }
            return new ArrayList<>();
        }
        
        try {
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE_URL + "/getWorkspaces"))
                    .header("X-Jsmon-Key", apiKey.trim())
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                if (logging != null) {
                    logging.logToError("JSMon: getWorkspaces failed (HTTP " + response.statusCode() + ")");
                }
                return new ArrayList<>();
            }

            return jsonParser.parseWorkspaces(response.body());

        } catch (Exception e) {
            if (logging != null) {
                logging.logToError("Error fetching workspaces: " + e.getMessage());
            }
            return new ArrayList<>();
        }
    }
    
    /**
     * Create a new workspace
     */
    public String createWorkspace(String apiKey, String workspaceName) {
        if (apiKey == null || apiKey.isEmpty()) {
            if (logging != null) {
                logging.logToError("API key not set");
            }
            return null;
        }
        
        if (workspaceName == null || workspaceName.isEmpty()) {
            if (logging != null) {
                logging.logToError("Workspace name cannot be empty");
            }
            return null;
        }
        
        try {
            String endpoint = API_BASE_URL + "/createWorkspace";
            String payload = "{\"name\":\"" + jsonParser.jsonEscape(workspaceName) + "\"}";

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .header("X-Jsmon-Key", apiKey.trim())
                    .POST(BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();
            String body = response.body() != null ? response.body() : "";

            if (status < 200 || status >= 300) {
                if (logging != null) {
                    logging.logToError("JSMon: createWorkspace failed (HTTP " + status + ")");
                    if (!body.isEmpty()) {
                        logging.logToError("JSMon: Response body: " + body);
                    }
                }
                return null;
            }

            String id = jsonParser.extractJsonFieldSimple(body, "workspaceId");
            if (id != null) {
                if (logging != null) {
                    logging.logToOutput("Workspace created with ID: " + id);
                }
                return id;
            } else {
                if (logging != null) {
                    logging.logToError("Could not extract workspace ID from response");
                    if (!body.isEmpty()) {
                        logging.logToError("JSMon: Raw response: " + body);
                    }
                }
                return null;
            }
        } catch (Exception e) {
            if (logging != null) {
                logging.logToError("Error creating workspace: " + e.getMessage());
            }
            return null;
        }
    }
    
    /**
     * Fetch user profile
     */
    public UserProfile fetchUserProfile(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            return null;
        }

        try {
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE_URL + "/viewProfile"))
                    .header("Content-Type", "application/json")
                    .header("X-Jsmon-Key", apiKey.trim())
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            String body = response.body() != null ? response.body() : "";

            if (status < 200 || status >= 300) {
                if (logging != null) {
                    logging.logToError("JSMon: viewProfile failed (HTTP " + status + ")");
                    if (!body.isEmpty()) {
                        logging.logToError("JSMon: viewProfile response: " + body);
                    }
                }
                return null;
            }

            String name = jsonParser.extractJsonFieldSimple(body, "name");
            String email = jsonParser.extractJsonFieldSimple(body, "email");
            String remaining = jsonParser.extractJsScanCredits(body);
            String accountType = jsonParser.extractJsonFieldSimple(body, "type");

            UserProfile profile = new UserProfile();
            profile.name = name;
            profile.email = email;
            profile.remaining = remaining;
            profile.accountType = accountType;
            return profile;
        } catch (Exception e) {
            if (logging != null) {
                logging.logToError("JSMon: Error fetching user profile: " + e.getMessage());
            }
            return null;
        }
    }
    
    /**
     * Send JavaScript file URL to JSMon for scanning
     */
    public boolean sendToJsmon(String url, String workspaceId, String apiKey, burp.api.montoya.http.message.requests.HttpRequest request) {
        try {
            String endpoint = API_BASE_URL + "/uploadUrl?wkspId=" +
                    URLEncoder.encode(workspaceId, StandardCharsets.UTF_8.toString());

            String headersJson = buildHeadersJson(request);
            StringBuilder payload = new StringBuilder();
            payload.append("{\"url\":\"").append(jsonParser.jsonEscape(url)).append("\"");
            if (headersJson != null && !headersJson.isEmpty()) {
                payload.append(",\"headers\":").append(headersJson);
            }
            payload.append("}");

            java.net.http.HttpRequest httpRequest = java.net.http.HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .header("X-Jsmon-Key", apiKey.trim())
                    .POST(BodyPublishers.ofString(payload.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                return true;
            } else {
                if (logging != null) {
                    logging.logToError("JSMon: ✗ Failed to send: " + url + " (HTTP " + status + ")");
                    if (response.body() != null && !response.body().isEmpty()) {
                        logging.logToError("JSMon: Response: " + response.body());
                    }
                }
                return false;
            }
        } catch (Exception e) {
            if (logging != null) {
                logging.logToError("JSMon: ✗ Error calling JSMon API for " + url + ": " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * Build headers JSON from HTTP request
     */
    private String buildHeadersJson(burp.api.montoya.http.message.requests.HttpRequest request) {
        if (request == null) {
            return null;
        }

        List<String> headerObjects = new ArrayList<>();

        Set<String> skipHeaders = new HashSet<>(Arrays.asList(
            "Host", "Connection", "Content-Length", "Transfer-Encoding",
            "Upgrade", "TE", "Trailer", "Keep-Alive"
        ));

        try {
            for (burp.api.montoya.http.message.HttpHeader header : request.headers()) {
                String headerName = header.name();
                if (skipHeaders.contains(headerName)) {
                    continue;
                }
                String headerValue = header.value();
                if (headerValue != null && !headerValue.isEmpty()) {
                    String obj = "{\"" + jsonParser.jsonEscape(headerName) + "\":\"" + jsonParser.jsonEscape(headerValue) + "\"}";
                    headerObjects.add(obj);
                }
            }
        } catch (Exception e) {
            // Fallback: parse raw request string if headers() fails
            try {
                String requestString = request.toString();
                String[] lines = requestString.split("\r\n");
                boolean inHeaders = true;
                for (int i = 1; i < lines.length && inHeaders; i++) {
                    String line = lines[i].trim();
                    if (line.isEmpty()) {
                        inHeaders = false;
                        break;
                    }
                    int colonIndex = line.indexOf(':');
                    if (colonIndex > 0) {
                        String headerName = line.substring(0, colonIndex).trim();
                        String headerValue = line.substring(colonIndex + 1).trim();
                        if (!skipHeaders.contains(headerName) && !headerValue.isEmpty()) {
                            String obj = "{\"" + jsonParser.jsonEscape(headerName) + "\":\"" + jsonParser.jsonEscape(headerValue) + "\"}";
                            headerObjects.add(obj);
                        }
                    }
                }
            } catch (Exception e2) {
                if (logging != null) {
                    logging.logToError("JSMon: Error extracting headers: " + e2.getMessage());
                }
            }
        }

        if (headerObjects.isEmpty()) {
            return null;
        }

        return "[" + String.join(",", headerObjects) + "]";
    }
    
    /**
     * Fetch total counts for all intelligence fields
     */
    public Map<String, Integer> fetchTotalCounts(String workspaceId, String apiKey) {
        Map<String, Integer> counts = new java.util.HashMap<>();
        if (workspaceId == null || workspaceId.isEmpty() || apiKey == null || apiKey.isEmpty()) {
            return counts;
        }
        
        try {
            String endpoint = API_BASE_URL + "/totalCountAnalysis?wkspId=" +
                    URLEncoder.encode(workspaceId, StandardCharsets.UTF_8.toString()) + "&runId=";

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("X-Jsmon-Key", apiKey.trim())
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                if (logging != null) {
                    logging.logToError("JSMon: Failed to fetch counts (HTTP " + response.statusCode() + ")");
                }
                return counts;
            }

            String body = response.body();
            jsonParser.extractCountsFromJson(body, counts);
            
        } catch (Exception e) {
            if (logging != null) {
                logging.logToError("JSMon: Error fetching counts: " + e.getMessage());
            }
        }
        
        return counts;
    }
    
    /**
     * Fetch secrets from JSMon
     */
    public String fetchSecrets(String workspaceId, String apiKey, int page) {
        if (workspaceId == null || workspaceId.isEmpty() || apiKey == null || apiKey.isEmpty()) {
            return "✗ Cannot fetch secrets - workspace ID or API key not configured\n";
        }
        
        try {
            StringBuilder all = new StringBuilder();
            String endpoint = API_BASE_URL + "/keysAndSecrets?wkspId=" +
                    URLEncoder.encode(workspaceId, StandardCharsets.UTF_8.toString()) +
                    "&page=" + page + "&runId=&lastScannedOn=&formDate=&toDate=&limit=100";

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("X-Jsmon-Key", apiKey.trim())
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401) {
                return "✗ Failed to fetch secrets (unauthorized)\n";
            }
            if (response.statusCode() != 200) {
                return "✗ Failed to fetch secrets (HTTP " + response.statusCode() + ")\n";
            }

            String body = response.body();
            List<String> objects = jsonParser.extractDataObjects(body);
            for (String obj : objects) {
                all.append(obj).append("\n");
            }

            return all.toString();
        } catch (Exception e) {
            if (logging != null) {
                logging.logToError("JSMon: Error fetching secrets: " + e.getMessage());
            }
            return "✗ Error fetching secrets: " + e.getMessage() + "\n";
        }
    }
    
    /**
     * Fetch intelligence data (JS URLs, API paths, URLs, domains, etc.)
     */
    public List<JsUrlEntry> fetchIntelligenceData(String workspaceId, String apiKey, String options, int page) {
        List<JsUrlEntry> entries = new ArrayList<>();
        if (workspaceId == null || workspaceId.isEmpty() || apiKey == null || apiKey.isEmpty()) {
            if (logging != null) {
                logging.logToError("JSMon: Cannot fetch intelligence data - workspace ID or API key not configured");
            }
            return entries;
        }
        
        try {
            String endpoint = API_BASE_URL + "/intelligence?wkspId=" +
                    URLEncoder.encode(workspaceId, StandardCharsets.UTF_8.toString()) +
                    "&options=" + options + "&page=" + page + "&runId=&search=&status=";

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("X-Jsmon-Key", apiKey.trim())
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401) {
                if (logging != null) {
                    logging.logToError("JSMon: Failed to fetch intelligence data (unauthorized)");
                }
                return entries;
            }
            if (response.statusCode() != 200) {
                if (logging != null) {
                    logging.logToError("JSMon: Failed to fetch intelligence data (HTTP " + response.statusCode() + ")");
                }
                return entries;
            }

            String body = response.body();
            entries = jsonParser.extractJsUrlsFromResponse(body);

            return entries;
        } catch (Exception e) {
            if (logging != null) {
                logging.logToError("JSMon: Error fetching intelligence data: " + e.getMessage());
            }
            return entries;
        }
    }
}


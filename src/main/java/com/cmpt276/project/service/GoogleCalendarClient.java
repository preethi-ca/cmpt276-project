package com.cmpt276.project.service;

import com.cmpt276.project.model.GoogleCalendarEvent;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.*;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

// talks to Google for the calendar feature: sign in, tokens, and adding, changing and deleting events
@Component
public class GoogleCalendarClient {
    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final URI TOKEN_URL = URI.create("https://oauth2.googleapis.com/token");
    private static final URI REVOKE_URL = URI.create("https://oauth2.googleapis.com/revoke");
    private static final String EVENTS_URL = "https://www.googleapis.com/calendar/v3/calendars/primary/events";
    private static final String FORM_TYPE = "application/x-www-form-urlencoded";
    private static final String JSON_TYPE = "application/json";
    // local time with seconds and no offset, Google reads the zone from timeZone
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    // lets us add, change and delete events on calendars the customer owns
    static final String SCOPE = "https://www.googleapis.com/auth/calendar.events.owned";

    private final GoogleCalendarSettings settings;
    private final Duration requestTimeout = Duration.ofSeconds(10);
    private final HttpClient client;
    private final JsonMapper jsonMapper = new JsonMapper();

    public GoogleCalendarClient(GoogleCalendarSettings settings) {
        this.settings = settings;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    // the Google page where the customer says yes or no
    public String buildAuthorizationUrl(String state) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("client_id", settings.getClientId());
        params.put("redirect_uri", settings.getRedirectUri());
        params.put("response_type", "code");
        params.put("scope", SCOPE);
        // offline + consent so Google gives us a refresh token every time
        params.put("access_type", "offline");
        params.put("prompt", "consent");
        params.put("state", state);
        return AUTH_URL + "?" + formBody(params);
    }

    // trades the one time code for a refresh token, or null if anything goes wrong
    public String exchangeCode(String code) {
        if (!settings.isReady() || code == null || code.isBlank()) {
            return null;
        }
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("code", code);
        fields.put("client_id", settings.getClientId());
        fields.put("client_secret", settings.getClientSecret());
        fields.put("redirect_uri", settings.getRedirectUri());
        fields.put("grant_type", "authorization_code");

        HttpResponse<String> response = postForm(TOKEN_URL, formBody(fields));
        if (response == null || response.statusCode() != 200) {
            return null;
        }
        return readRefreshToken(response.body());
    }

    // tells Google to forget the token, true if Google said ok
    public boolean revokeToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("token", token);
        HttpResponse<String> response = postForm(REVOKE_URL, formBody(fields));
        return response != null && response.statusCode() == 200;
    }

    // gets a short lived access token from the saved refresh token, or null if anything goes wrong
    public String refreshAccessToken(String refreshToken) {
        if (!settings.isReady() || refreshToken == null || refreshToken.isBlank()) {
            return null;
        }
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("client_id", settings.getClientId());
        fields.put("client_secret", settings.getClientSecret());
        fields.put("refresh_token", refreshToken);
        fields.put("grant_type", "refresh_token");

        HttpResponse<String> response = postForm(TOKEN_URL, formBody(fields));
        if (response == null || response.statusCode() != 200) {
            return null;
        }
        return readAccessToken(response.body());
    }

    // adds the event to the customer's main calendar, returns Google's event id or null
    public String createEvent(String refreshToken, GoogleCalendarEvent event) {
        if (!settings.isReady() || refreshToken == null || refreshToken.isBlank()) {
            return null;
        }
        String json = eventJson(event);
        if (json == null) {
            return null;
        }
        String accessToken = refreshAccessToken(refreshToken);
        if (accessToken == null) {
            return null;
        }
        HttpResponse<String> response = send("POST", URI.create(EVENTS_URL), JSON_TYPE, json, accessToken);
        if (response == null || response.statusCode() != 200) {
            return null;
        }
        return readEventId(response.body());
    }

    // replaces the event with the new details, true if Google said ok
    public boolean updateEvent(String refreshToken, String eventId, GoogleCalendarEvent event) {
        if (!settings.isReady() || refreshToken == null || refreshToken.isBlank()
                || eventId == null || eventId.isBlank()) {
            return false;
        }
        String json = eventJson(event);
        if (json == null) {
            return false;
        }
        String accessToken = refreshAccessToken(refreshToken);
        if (accessToken == null) {
            return false;
        }
        HttpResponse<String> response = send("PUT", URI.create(eventUrl(eventId)), JSON_TYPE, json, accessToken);
        return response != null && response.statusCode() == 200;
    }

    // removes the event, true if it is gone (including when it was already deleted)
    public boolean deleteEvent(String refreshToken, String eventId) {
        if (!settings.isReady() || refreshToken == null || refreshToken.isBlank()
                || eventId == null || eventId.isBlank()) {
            return false;
        }
        String accessToken = refreshAccessToken(refreshToken);
        if (accessToken == null) {
            return false;
        }
        HttpResponse<String> response = send("DELETE", URI.create(eventUrl(eventId)), null, null, accessToken);
        if (response == null) {
            return false;
        }
        int status = response.statusCode();
        // 404 and 410 mean the customer or Google already deleted it
        return status == 200 || status == 204 || status == 404 || status == 410;
    }

    // sends a form POST and returns the response, or null if it failed or took too long
    private HttpResponse<String> postForm(URI url, String body) {
        return send("POST", url, FORM_TYPE, body, null);
    }

    // the one place that sends requests to Google, null if it failed or took too long
    private HttpResponse<String> send(String method, URI url, String contentType, String body, String accessToken) {
        HttpRequest request;
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(url)
                    .timeout(requestTimeout)
                    .header("Accept", JSON_TYPE);
            if (accessToken != null) {
                builder.header("Authorization", "Bearer " + accessToken);
            }
            if (body == null) {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            } else {
                builder.header("Content-Type", contentType);
                builder.method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            }
            request = builder.build();
        } catch (IllegalArgumentException exception) {
            return null;
        }

        CompletableFuture<HttpResponse<String>> pending = client.sendAsync(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        try {
            // limit the whole response, not just the headers
            return pending.get(requestTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            pending.cancel(true);
            return null;
        } catch (InterruptedException exception) {
            pending.cancel(true);
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException exception) {
            return null;
        }
    }

    // turns name/value pairs into a=1&b=2 with everything URL encoded
    String formBody(Map<String, String> fields) {
        StringJoiner body = new StringJoiner("&");
        for (Map.Entry<String, String> field : fields.entrySet()) {
            String value = field.getValue() == null ? "" : field.getValue();
            body.add(URLEncoder.encode(field.getKey(), StandardCharsets.UTF_8) + "="
                    + URLEncoder.encode(value, StandardCharsets.UTF_8));
        }
        return body.toString();
    }

    // the event body Google expects, built with Jackson so quotes in names are escaped
    String eventJson(GoogleCalendarEvent event) {
        if (event == null || event.getStart() == null || event.getEnd() == null) {
            return null;
        }
        ObjectNode body = jsonMapper.createObjectNode();
        body.put("summary", textOrEmpty(event.getSummary()));
        body.put("location", textOrEmpty(event.getLocation()));
        body.put("description", textOrEmpty(event.getDescription()));
        ObjectNode start = body.putObject("start");
        start.put("dateTime", event.getStart().format(DATE_TIME));
        start.put("timeZone", textOrEmpty(event.getTimeZone()));
        ObjectNode end = body.putObject("end");
        end.put("dateTime", event.getEnd().format(DATE_TIME));
        end.put("timeZone", textOrEmpty(event.getTimeZone()));
        try {
            return jsonMapper.writeValueAsString(body);
        } catch (JacksonException exception) {
            return null;
        }
    }

    // the address of one event, with the id URL encoded so it can't break the path
    String eventUrl(String eventId) {
        // URLEncoder is for forms and turns spaces into +, a path needs %20
        return EVENTS_URL + "/" + URLEncoder.encode(eventId, StandardCharsets.UTF_8).replace("+", "%20");
    }

    // pulls refresh_token out of Google's token reply, null if it isn't there
    String readRefreshToken(String json) {
        return readText(json, "refresh_token");
    }

    // pulls access_token out of Google's token reply, null if it isn't there
    String readAccessToken(String json) {
        return readText(json, "access_token");
    }

    // pulls the event id out of Google's reply, null if it isn't there
    String readEventId(String json) {
        return readText(json, "id");
    }

    // reads one text field from a JSON reply, null if it is missing, blank or the JSON is bad
    private String readText(String json, String field) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JsonNode body = jsonMapper.readTree(json);
            if (body == null) {
                return null;
            }
            JsonNode value = body.get(field);
            if (value != null && value.isString() && !value.asString().isBlank()) {
                return value.asString();
            }
        } catch (JacksonException exception) {
            return null;
        }
        return null;
    }

    private String textOrEmpty(String value) {
        return value == null ? "" : value;
    }
}

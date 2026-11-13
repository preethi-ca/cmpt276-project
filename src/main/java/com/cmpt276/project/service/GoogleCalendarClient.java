package com.cmpt276.project.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.*;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// talks to Google for the calendar feature (sign in for now, events later)
@Component
public class GoogleCalendarClient {
    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final URI TOKEN_URL = URI.create("https://oauth2.googleapis.com/token");
    private static final URI REVOKE_URL = URI.create("https://oauth2.googleapis.com/revoke");
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

    // sends a form POST and returns the response, or null if it failed or took too long
    private HttpResponse<String> postForm(URI url, String body) {
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(url)
                    .timeout(requestTimeout)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
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

    // pulls refresh_token out of Google's token reply, null if it isn't there
    String readRefreshToken(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JsonNode body = jsonMapper.readTree(json);
            if (body == null) {
                return null;
            }
            JsonNode token = body.get("refresh_token");
            if (token != null && token.isString() && !token.asString().isBlank()) {
                return token.asString();
            }
        } catch (JacksonException exception) {
            return null;
        }
        return null;
    }
}

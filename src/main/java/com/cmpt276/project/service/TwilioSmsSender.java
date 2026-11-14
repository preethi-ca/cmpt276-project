package com.cmpt276.project.service;

import com.cmpt276.project.model.BookingSmsResult;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class TwilioSmsSender {
    private static final URI TWILIO_BASE_URL = URI.create("https://api.twilio.com");

    private final BookingSmsSettings settings;
    private final URI baseUrl;
    private final Duration requestTimeout;
    private final HttpClient client;
    private final JsonMapper jsonMapper = new JsonMapper();

    @Autowired
    public TwilioSmsSender(BookingSmsSettings settings) {
        this(settings, TWILIO_BASE_URL, Duration.ofSeconds(10));
    }

    // Tests can point this at a local fake server. The application always uses Twilio's HTTPS URL.
    TwilioSmsSender(BookingSmsSettings settings, URI baseUrl, Duration requestTimeout) {
        if (!TWILIO_BASE_URL.equals(baseUrl)
                && !("http".equals(baseUrl.getScheme()) && "127.0.0.1".equals(baseUrl.getHost()))) {
            throw new IllegalArgumentException("Only Twilio or a local test server is allowed");
        }
        this.settings = settings;
        this.baseUrl = baseUrl;
        this.requestTimeout = requestTimeout;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    public BookingSmsResult send(String toPhone, String text) {
        if (!settings.isReady() || !hasText(toPhone) || !hasText(text)) {
            return BookingSmsResult.UNAVAILABLE;
        }

        HttpRequest request;
        try {
            URI endpoint = baseUrl.resolve("/2010-04-01/Accounts/" + settings.getAccountSid() + "/Messages.json");
            request = HttpRequest.newBuilder(endpoint)
                    .timeout(requestTimeout)
                    .header("Authorization", createBasicAuth())
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(createRequestBody(toPhone, text), StandardCharsets.UTF_8))
                    .build();
        } catch (IllegalArgumentException exception) {
            return BookingSmsResult.UNAVAILABLE;
        }

        CompletableFuture<HttpResponse<String>> pending = client.sendAsync(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        try {
            // Limit the entire response, including its body, rather than only waiting for headers.
            HttpResponse<String> response = pending.get(requestTimeout.toMillis(), TimeUnit.MILLISECONDS);
            return readResult(response);
        } catch (TimeoutException exception) {
            pending.cancel(true);
            return BookingSmsResult.UNCERTAIN;
        } catch (InterruptedException exception) {
            pending.cancel(true);
            Thread.currentThread().interrupt();
            return BookingSmsResult.UNCERTAIN;
        } catch (ExecutionException exception) {
            return BookingSmsResult.UNCERTAIN;
        }
    }

    // Twilio accepts an API key SID and secret as the Basic auth username and password.
    private String createBasicAuth() {
        String credentials = settings.getApiKeySid() + ":" + settings.getApiKeySecret();
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private String createRequestBody(String toPhone, String text) {
        return "To=" + encode(toPhone)
                + "&From=" + encode(settings.getFromNumber())
                + "&Body=" + encode(text);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private BookingSmsResult readResult(HttpResponse<String> response) {
        int status = response.statusCode();
        if (status >= 400 && status < 500 && status != 408) {
            return BookingSmsResult.REJECTED;
        }
        if (status == 201) {
            try {
                JsonNode body = jsonMapper.readTree(response.body());
                if (body != null) {
                    JsonNode messageSid = body.get("sid");
                    if (messageSid != null && messageSid.isString() && hasText(messageSid.asString())) {
                        return BookingSmsResult.ACCEPTED;
                    }
                }
            } catch (JacksonException exception) {
                return BookingSmsResult.UNCERTAIN;
            }
        }
        // A server error, redirect or unexpected response does not prove the text was not sent.
        return BookingSmsResult.UNCERTAIN;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}

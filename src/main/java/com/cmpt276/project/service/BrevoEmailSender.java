package com.cmpt276.project.service;

import com.cmpt276.project.model.BookingConfirmationMessage;
import com.cmpt276.project.model.BookingEmailResult;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@Service
public class BrevoEmailSender {
    private static final URI BREVO_ENDPOINT = URI.create("https://api.brevo.com/v3/smtp/email");

    private final BookingEmailSettings settings;
    private final URI endpoint;
    private final Duration requestTimeout;
    private final HttpClient client;
    private final JsonMapper jsonMapper = new JsonMapper();

    @Autowired
    public BrevoEmailSender(BookingEmailSettings settings) {
        this(settings, BREVO_ENDPOINT, Duration.ofSeconds(10));
    }

    // Tests can use a local fake server; the application always uses Brevo's HTTPS URL.
    BrevoEmailSender(BookingEmailSettings settings, URI endpoint, Duration requestTimeout) {
        if (!BREVO_ENDPOINT.equals(endpoint)
                && !("http".equals(endpoint.getScheme()) && "127.0.0.1".equals(endpoint.getHost()))) {
            throw new IllegalArgumentException("Only Brevo or a local test server is allowed");
        }
        this.settings = settings;
        this.endpoint = endpoint;
        this.requestTimeout = requestTimeout;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    public BookingEmailResult send(BookingConfirmationMessage message) {
        if (!settings.isReady() || !hasMessageDetails(message)) {
            return BookingEmailResult.UNAVAILABLE;
        }

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(endpoint)
                    .timeout(requestTimeout)
                    .header("api-key", settings.getApiKey())
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(createRequestBody(message), StandardCharsets.UTF_8))
                    .build();
        } catch (IllegalArgumentException | JacksonException exception) {
            return BookingEmailResult.UNAVAILABLE;
        }

        CompletableFuture<HttpResponse<String>> pending = client.sendAsync(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        try {
            // Limit the entire response, including its body, rather than only waiting for headers.
            HttpResponse<String> response = pending.get(requestTimeout.toMillis(), TimeUnit.MILLISECONDS);
            return readResult(response);
        } catch (TimeoutException exception) {
            pending.cancel(true);
            return BookingEmailResult.UNCERTAIN;
        } catch (InterruptedException exception) {
            pending.cancel(true);
            Thread.currentThread().interrupt();
            return BookingEmailResult.UNCERTAIN;
        } catch (ExecutionException exception) {
            return BookingEmailResult.UNCERTAIN;
        }
    }

    private String createRequestBody(BookingConfirmationMessage message) {
        ObjectNode body = jsonMapper.createObjectNode();
        ObjectNode sender = body.putObject("sender");
        sender.put("email", settings.getSenderEmail());
        sender.put("name", settings.getSenderName());
        body.putArray("to").addObject().put("email", message.getRecipient());
        body.put("subject", message.getSubject());
        body.put("textContent", message.getBody());
        return jsonMapper.writeValueAsString(body);
    }

    private BookingEmailResult readResult(HttpResponse<String> response) {
        int status = response.statusCode();
        if (status >= 400 && status < 500 && status != 408) {
            return BookingEmailResult.REJECTED;
        }
        if (status == 201) {
            try {
                JsonNode body = jsonMapper.readTree(response.body());
                if (body != null) {
                    JsonNode messageId = body.get("messageId");
                    if (messageId != null && messageId.isString() && hasText(messageId.asString())) {
                        return BookingEmailResult.ACCEPTED;
                    }
                }
            } catch (JacksonException exception) {
                return BookingEmailResult.UNCERTAIN;
            }
        }
        // A server error, redirect or unexpected response does not prove non-delivery.
        return BookingEmailResult.UNCERTAIN;
    }

    private boolean hasMessageDetails(BookingConfirmationMessage message) {
        return message != null && hasText(message.getRecipient())
                && hasText(message.getSubject()) && hasText(message.getBody());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}

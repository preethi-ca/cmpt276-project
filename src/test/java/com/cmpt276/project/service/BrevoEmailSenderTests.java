package com.cmpt276.project.service;

import com.cmpt276.project.model.BookingConfirmationMessage;
import com.cmpt276.project.model.BookingEmailResult;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

class BrevoEmailSenderTests {
    private HttpServer server;
    private FakeEmailHandler handler;
    private URI endpoint;

    @BeforeEach
    void startFakeServer() throws IOException {
        handler = new FakeEmailHandler();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v3/smtp/email", handler);
        server.start();
        endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v3/smtp/email");
    }

    @AfterEach
    void stopFakeServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sendsThePlainTextMessageAndRecognizesAcceptance() {
        BookingConfirmationMessage message = new BookingConfirmationMessage(
                "alex@example.invalid", "Booking at Alex's \"Salon\"", "Hi Alex,\nHaircut: $25\\visit — confirmed.");

        BookingEmailResult result = newSender(readySettings(), Duration.ofSeconds(2)).send(message);

        assertEquals(BookingEmailResult.ACCEPTED, result);
        assertEquals(1, handler.requestCount);
        assertEquals("POST", handler.requestMethod);
        assertEquals("fake-test-key", handler.requestHeaders.getFirst("api-key"));
        assertEquals("application/json", handler.requestHeaders.getFirst("Content-Type"));
        JsonNode body = new JsonMapper().readTree(handler.requestBody);
        assertEquals("sender@example.invalid", body.get("sender").get("email").asString());
        assertEquals("BookEasy", body.get("sender").get("name").asString());
        assertEquals(1, body.get("to").size());
        assertEquals(message.getRecipient(), body.get("to").get(0).get("email").asString());
        assertEquals(message.getSubject(), body.get("subject").asString());
        assertEquals(message.getBody(), body.get("textContent").asString());
        assertFalse(body.has("htmlContent"));
    }

    @Test
    void handlesRejectionAndAnUncertainTimeout() throws InterruptedException {
        BrevoEmailSender sender = newSender(readySettings(), Duration.ofMillis(500));
        handler.status = 401;
        handler.responseBody = "{\"code\":\"unauthorized\",\"message\":\"Invalid API key\"}";
        assertEquals(BookingEmailResult.REJECTED, sender.send(newMessage()));
        assertEquals(1, handler.requestCount);

        handler.status = 201;
        handler.responseBody = "{\"messageId\":\"fake-message-id\"}";
        handler.delayBody = true;
        long started = System.nanoTime();
        assertEquals(BookingEmailResult.UNCERTAIN, sender.send(newMessage()));
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        assertTrue(handler.bodyStarted.await(1, TimeUnit.SECONDS));
        assertTrue(elapsedMillis < 1500, "A stalled response body must not leave sending waiting");
        assertEquals(2, handler.requestCount);
    }

    @Test
    void doesNotSendWhenDisabledOrMissingSettings() {
        BookingEmailSettings disabled = new BookingEmailSettings(false, "fake-test-key", "sender@example.invalid", "BookEasy");
        BookingEmailSettings noKey = new BookingEmailSettings(true, "", "sender@example.invalid", "BookEasy");
        BookingEmailSettings noSender = new BookingEmailSettings(true, "fake-test-key", "", "BookEasy");

        assertEquals(BookingEmailResult.UNAVAILABLE, newSender(disabled, Duration.ofSeconds(2)).send(newMessage()));
        assertEquals(BookingEmailResult.UNAVAILABLE, newSender(noKey, Duration.ofSeconds(2)).send(newMessage()));
        assertEquals(BookingEmailResult.UNAVAILABLE, newSender(noSender, Duration.ofSeconds(2)).send(newMessage()));
        assertEquals(0, handler.requestCount);
    }

    private BrevoEmailSender newSender(BookingEmailSettings settings, Duration timeout) {
        return new BrevoEmailSender(settings, endpoint, timeout);
    }

    private BookingEmailSettings readySettings() {
        return new BookingEmailSettings(true, "fake-test-key", "sender@example.invalid", "BookEasy");
    }

    private BookingConfirmationMessage newMessage() {
        return new BookingConfirmationMessage("alex@example.invalid", "Your booking is confirmed", "Your appointment details.");
    }

    private static class FakeEmailHandler implements HttpHandler {
        private volatile int status = 201;
        private volatile String responseBody = "{\"messageId\":\"fake-message-id\"}";
        private volatile boolean delayBody;
        private volatile int requestCount;
        private String requestMethod;
        private Headers requestHeaders;
        private String requestBody;
        private final CountDownLatch bodyStarted = new CountDownLatch(1);

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            requestCount++;
            requestMethod = exchange.getRequestMethod();
            requestHeaders = exchange.getRequestHeaders();
            requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, response.length);
            try {
                if (delayBody) {
                    exchange.getResponseBody().write(response, 0, 1);
                    exchange.getResponseBody().flush();
                    bodyStarted.countDown();
                    Thread.sleep(2000);
                    exchange.getResponseBody().write(response, 1, response.length - 1);
                } else {
                    exchange.getResponseBody().write(response);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        }
    }
}

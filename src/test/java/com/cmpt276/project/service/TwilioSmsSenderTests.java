package com.cmpt276.project.service;

import com.cmpt276.project.model.BookingSmsResult;
import com.sun.net.httpserver.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class TwilioSmsSenderTests {
    private static final String ACCOUNT_SID = "AC11111111111111111111111111111111";
    private static final String API_KEY_SID = "SK22222222222222222222222222222222";

    private HttpServer server;
    private FakeSmsHandler handler;
    private URI baseUrl;

    @BeforeEach
    void startFakeServer() throws IOException {
        handler = new FakeSmsHandler();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/2010-04-01/Accounts/", handler);
        server.start();
        baseUrl = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterEach
    void stopFakeServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sendsTheEncodedFormAndRecognizesAcceptance() {
        String text = "Hi Alex, your haircut at Alex's Café is on Nov 20 at 14:30.\nPrice: $25 & tax = total";

        BookingSmsResult result = newSender(readySettings(), Duration.ofSeconds(2)).send("+16045550123", text);

        assertEquals(BookingSmsResult.ACCEPTED, result);
        assertEquals(1, handler.requestCount);
        assertEquals("POST", handler.requestMethod);
        assertEquals("/2010-04-01/Accounts/" + ACCOUNT_SID + "/Messages.json", handler.requestPath);
        String expectedAuth = "Basic " + Base64.getEncoder().encodeToString(
                (API_KEY_SID + ":fake-test-secret").getBytes(StandardCharsets.UTF_8));
        assertEquals(expectedAuth, handler.requestHeaders.getFirst("Authorization"));
        assertEquals("application/x-www-form-urlencoded", handler.requestHeaders.getFirst("Content-Type"));
        // An unencoded plus sign would reach Twilio as a space.
        assertTrue(handler.requestBody.contains("To=%2B16045550123"));
        Map<String, String> form = readForm(handler.requestBody);
        assertEquals(3, form.size());
        assertEquals("+16045550123", form.get("To"));
        assertEquals("+16045550100", form.get("From"));
        assertEquals(text, form.get("Body"));
    }

    @Test
    void handlesRejectionAndAnUncertainTimeout() throws InterruptedException {
        TwilioSmsSender sender = newSender(readySettings(), Duration.ofMillis(500));
        handler.status = 400;
        handler.responseBody = "{\"code\":21211,\"message\":\"Invalid 'To' Phone Number\",\"status\":400}";
        assertEquals(BookingSmsResult.REJECTED, sender.send("+16045550123", "Your booking is confirmed."));
        assertEquals(1, handler.requestCount);

        handler.status = 201;
        handler.responseBody = "{\"sid\":\"SM33333333333333333333333333333333\"}";
        handler.delayBody = true;
        long started = System.nanoTime();
        assertEquals(BookingSmsResult.UNCERTAIN, sender.send("+16045550123", "Your booking is confirmed."));
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        assertTrue(handler.bodyStarted.await(1, TimeUnit.SECONDS));
        assertTrue(elapsedMillis < 1500, "A stalled response body must not leave sending waiting");
        assertEquals(2, handler.requestCount);
    }

    @Test
    void treatsServerErrorsAndMissingSidAsUncertain() {
        TwilioSmsSender sender = newSender(readySettings(), Duration.ofSeconds(2));

        handler.status = 500;
        handler.responseBody = "{\"status\":500,\"message\":\"Twilio encountered an internal error\"}";
        assertEquals(BookingSmsResult.UNCERTAIN, sender.send("+16045550123", "Your booking is confirmed."));

        handler.status = 201;
        handler.responseBody = "{\"status\":\"queued\"}";
        assertEquals(BookingSmsResult.UNCERTAIN, sender.send("+16045550123", "Your booking is confirmed."));

        handler.responseBody = "{\"sid\":\"\"}";
        assertEquals(BookingSmsResult.UNCERTAIN, sender.send("+16045550123", "Your booking is confirmed."));

        handler.responseBody = "not json";
        assertEquals(BookingSmsResult.UNCERTAIN, sender.send("+16045550123", "Your booking is confirmed."));
        assertEquals(4, handler.requestCount);
    }

    @Test
    void doesNotSendWhenDisabledOrMissingSettings() {
        BookingSmsSettings disabled = new BookingSmsSettings(false, ACCOUNT_SID, API_KEY_SID, "fake-test-secret", "+16045550100");
        BookingSmsSettings noAccount = new BookingSmsSettings(true, "", API_KEY_SID, "fake-test-secret", "+16045550100");
        BookingSmsSettings noKey = new BookingSmsSettings(true, ACCOUNT_SID, "", "fake-test-secret", "+16045550100");
        BookingSmsSettings noSecret = new BookingSmsSettings(true, ACCOUNT_SID, API_KEY_SID, " ", "+16045550100");
        BookingSmsSettings noFrom = new BookingSmsSettings(true, ACCOUNT_SID, API_KEY_SID, "fake-test-secret", "");
        BookingSmsSettings badAccount = new BookingSmsSettings(true, "AC not valid", API_KEY_SID, "fake-test-secret", "+16045550100");

        assertEquals(BookingSmsResult.UNAVAILABLE, newSender(disabled, Duration.ofSeconds(2)).send("+16045550123", "Hi"));
        assertEquals(BookingSmsResult.UNAVAILABLE, newSender(noAccount, Duration.ofSeconds(2)).send("+16045550123", "Hi"));
        assertEquals(BookingSmsResult.UNAVAILABLE, newSender(noKey, Duration.ofSeconds(2)).send("+16045550123", "Hi"));
        assertEquals(BookingSmsResult.UNAVAILABLE, newSender(noSecret, Duration.ofSeconds(2)).send("+16045550123", "Hi"));
        assertEquals(BookingSmsResult.UNAVAILABLE, newSender(noFrom, Duration.ofSeconds(2)).send("+16045550123", "Hi"));
        assertEquals(BookingSmsResult.UNAVAILABLE, newSender(badAccount, Duration.ofSeconds(2)).send("+16045550123", "Hi"));
        TwilioSmsSender ready = newSender(readySettings(), Duration.ofSeconds(2));
        assertEquals(BookingSmsResult.UNAVAILABLE, ready.send("", "Hi"));
        assertEquals(BookingSmsResult.UNAVAILABLE, ready.send("+16045550123", null));
        assertEquals(0, handler.requestCount);
    }

    private TwilioSmsSender newSender(BookingSmsSettings settings, Duration timeout) {
        return new TwilioSmsSender(settings, baseUrl, timeout);
    }

    private BookingSmsSettings readySettings() {
        return new BookingSmsSettings(true, ACCOUNT_SID, API_KEY_SID, "fake-test-secret", "+16045550100");
    }

    private Map<String, String> readForm(String body) {
        Map<String, String> form = new HashMap<>();
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            form.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
        }
        return form;
    }

    private static class FakeSmsHandler implements HttpHandler {
        private volatile int status = 201;
        private volatile String responseBody = "{\"sid\":\"SM33333333333333333333333333333333\",\"status\":\"queued\"}";
        private volatile boolean delayBody;
        private volatile int requestCount;
        private String requestMethod;
        private String requestPath;
        private Headers requestHeaders;
        private String requestBody;
        private final CountDownLatch bodyStarted = new CountDownLatch(1);

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            requestCount++;
            requestMethod = exchange.getRequestMethod();
            requestPath = exchange.getRequestURI().getPath();
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

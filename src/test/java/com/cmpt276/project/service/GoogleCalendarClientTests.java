package com.cmpt276.project.service;

import com.cmpt276.project.model.GoogleCalendarEvent;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

class GoogleCalendarClientTests {
    private final GoogleCalendarClient client = new GoogleCalendarClient(new GoogleCalendarSettings(
            true, "fake-client-id", "fake-client-secret", "https://bookeasy.example.invalid/calendar/google/callback"));

    @Test
    void authorizationUrlAsksGoogleForOfflineCalendarAccess() {
        String url = client.buildAuthorizationUrl("fake-state-123");
        URI uri = URI.create(url);

        assertEquals("https", uri.getScheme());
        assertEquals("accounts.google.com", uri.getHost());
        assertEquals("/o/oauth2/v2/auth", uri.getPath());
        assertTrue(url.contains("client_id=fake-client-id"));
        assertTrue(url.contains(
                "redirect_uri=https%3A%2F%2Fbookeasy.example.invalid%2Fcalendar%2Fgoogle%2Fcallback"));
        assertTrue(url.contains("response_type=code"));
        assertTrue(url.contains("scope=https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fcalendar.events.owned"));
        assertTrue(url.contains("access_type=offline"));
        assertTrue(url.contains("prompt=consent"));
        assertTrue(url.contains("state=fake-state-123"));
        assertFalse(url.contains("fake-client-secret"));
    }

    @Test
    void formBodyEncodesEveryValue() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("code", "4/abc def&x=1");
        fields.put("redirect_uri", "https://bookeasy.example.invalid/cb");
        fields.put("grant_type", "authorization_code");

        assertEquals("code=4%2Fabc+def%26x%3D1"
                + "&redirect_uri=https%3A%2F%2Fbookeasy.example.invalid%2Fcb"
                + "&grant_type=authorization_code", client.formBody(fields));
    }

    @Test
    void readsRefreshTokenFromGoogleReply() {
        String json = "{\"access_token\":\"fake-access\",\"expires_in\":3599,"
                + "\"refresh_token\":\"fake-refresh-token\",\"token_type\":\"Bearer\"}";

        assertEquals("fake-refresh-token", client.readRefreshToken(json));
    }

    @Test
    void missingRefreshTokenOrBadJsonGivesNull() {
        assertNull(client.readRefreshToken("{\"access_token\":\"fake-access\"}"));
        assertNull(client.readRefreshToken("{\"refresh_token\":\"\"}"));
        assertNull(client.readRefreshToken("{\"refresh_token\":123}"));
        assertNull(client.readRefreshToken("not json at all"));
        assertNull(client.readRefreshToken(""));
        assertNull(client.readRefreshToken(null));
    }

    @Test
    void exchangeCodeGivesNullWithoutCallingGoogleWhenNotSetUp() {
        GoogleCalendarClient notReady = new GoogleCalendarClient(new GoogleCalendarSettings(false, "", "", ""));

        assertNull(notReady.exchangeCode("fake-code"));
        assertNull(client.exchangeCode(""));
        assertFalse(client.revokeToken(null));
    }

    @Test
    void eventJsonHasTheShapeGoogleExpects() {
        JsonNode body = new JsonMapper().readTree(client.eventJson(sampleEvent("Haircut at Test Salon")));

        assertEquals("Haircut at Test Salon", body.get("summary").asString());
        assertEquals("123 Main Street, Vancouver, British Columbia", body.get("location").asString());
        assertEquals("Booked with BookEasy. You can view or change it in My Appointments.",
                body.get("description").asString());
        assertEquals("2026-11-20T10:00:00", body.get("start").get("dateTime").asString());
        assertEquals("America/Vancouver", body.get("start").get("timeZone").asString());
        assertEquals("2026-11-20T11:00:00", body.get("end").get("dateTime").asString());
        assertEquals("America/Vancouver", body.get("end").get("timeZone").asString());
        assertNull(client.eventJson(null));
    }

    @Test
    void eventJsonEscapesQuotesAndSpecialCharacters() {
        String summary = "Cut \"Deluxe\" at Sam's Salon & Spa\nLine two \\ <b>caf\u00e9</b>";
        String json = client.eventJson(sampleEvent(summary));

        JsonNode body = new JsonMapper().readTree(json);
        assertEquals(summary, body.get("summary").asString());
        assertFalse(json.contains("\n"));
    }

    @Test
    void readsAccessTokenAndEventIdOrGivesNull() {
        assertEquals("fake-access", client.readAccessToken("{\"access_token\":\"fake-access\",\"expires_in\":3599}"));
        assertEquals("fake-event-1", client.readEventId("{\"kind\":\"calendar#event\",\"id\":\"fake-event-1\"}"));

        assertNull(client.readAccessToken("{\"refresh_token\":\"fake-refresh\"}"));
        assertNull(client.readAccessToken("{\"access_token\":\"\"}"));
        assertNull(client.readAccessToken("not json at all"));
        assertNull(client.readAccessToken(null));
        assertNull(client.readEventId("{\"kind\":\"calendar#event\"}"));
        assertNull(client.readEventId("{\"id\":42}"));
        assertNull(client.readEventId("{broken"));
        assertNull(client.readEventId(""));
    }

    @Test
    void eventUrlEncodesTheEventId() {
        assertEquals("https://www.googleapis.com/calendar/v3/calendars/primary/events/abc123",
                client.eventUrl("abc123"));
        assertEquals("https://www.googleapis.com/calendar/v3/calendars/primary/events/a%20b%2Fc%3Fd%3De%23f",
                client.eventUrl("a b/c?d=e#f"));
    }

    @Test
    void eventCallsGiveNullOrFalseWithoutCallingGoogleWhenNotSetUpOrMissingValues() {
        GoogleCalendarClient notReady = new GoogleCalendarClient(new GoogleCalendarSettings(false, "", "", ""));
        GoogleCalendarClient blankSettings = new GoogleCalendarClient(new GoogleCalendarSettings(true, " ", "", ""));
        GoogleCalendarEvent event = sampleEvent("Haircut at Test Salon");

        assertNull(notReady.refreshAccessToken("fake-refresh"));
        assertNull(notReady.createEvent("fake-refresh", event));
        assertFalse(notReady.updateEvent("fake-refresh", "fake-event-1", event));
        assertFalse(notReady.deleteEvent("fake-refresh", "fake-event-1"));
        assertNull(blankSettings.createEvent("fake-refresh", event));
        assertFalse(blankSettings.deleteEvent("fake-refresh", "fake-event-1"));

        assertNull(client.refreshAccessToken(" "));
        assertNull(client.createEvent("", event));
        assertNull(client.createEvent("fake-refresh", null));
        assertFalse(client.updateEvent(null, "fake-event-1", event));
        assertFalse(client.updateEvent("fake-refresh", " ", event));
        assertFalse(client.updateEvent("fake-refresh", "fake-event-1", null));
        assertFalse(client.deleteEvent("", "fake-event-1"));
        assertFalse(client.deleteEvent("fake-refresh", null));
    }

    private GoogleCalendarEvent sampleEvent(String summary) {
        return new GoogleCalendarEvent(summary, "123 Main Street, Vancouver, British Columbia",
                "Booked with BookEasy. You can view or change it in My Appointments.",
                LocalDateTime.of(2026, 11, 20, 10, 0), LocalDateTime.of(2026, 11, 20, 11, 0), "America/Vancouver");
    }
}

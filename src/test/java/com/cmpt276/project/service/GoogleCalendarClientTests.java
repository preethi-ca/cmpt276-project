package com.cmpt276.project.service;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

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
}

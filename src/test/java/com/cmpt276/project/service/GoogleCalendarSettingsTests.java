package com.cmpt276.project.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GoogleCalendarSettingsTests {
    private static final String CLIENT_ID = "fake-client-id.apps.googleusercontent.com";
    private static final String CLIENT_SECRET = "fake-client-secret";
    private static final String REDIRECT_URI = "http://localhost:8080/calendar/google/callback";

    @Test
    void isReadyWhenEnabledAndAllValuesArePresent() {
        GoogleCalendarSettings settings = new GoogleCalendarSettings(true, CLIENT_ID, CLIENT_SECRET, REDIRECT_URI);

        assertTrue(settings.isReady());
        assertEquals(CLIENT_ID, settings.getClientId());
        assertEquals(CLIENT_SECRET, settings.getClientSecret());
        assertEquals(REDIRECT_URI, settings.getRedirectUri());
    }

    @Test
    void isNotReadyWhenDisabled() {
        GoogleCalendarSettings settings = new GoogleCalendarSettings(false, CLIENT_ID, CLIENT_SECRET, REDIRECT_URI);

        assertFalse(settings.isReady());
    }

    @Test
    void isNotReadyWhenAnyValueIsBlank() {
        assertFalse(new GoogleCalendarSettings(true, "", CLIENT_SECRET, REDIRECT_URI).isReady());
        assertFalse(new GoogleCalendarSettings(true, CLIENT_ID, "   ", REDIRECT_URI).isReady());
        assertFalse(new GoogleCalendarSettings(true, CLIENT_ID, CLIENT_SECRET, "\n").isReady());
    }

    @Test
    void isNotReadyWhenAnyValueIsMissing() {
        assertFalse(new GoogleCalendarSettings(true, null, CLIENT_SECRET, REDIRECT_URI).isReady());
        assertFalse(new GoogleCalendarSettings(true, CLIENT_ID, null, REDIRECT_URI).isReady());
        assertFalse(new GoogleCalendarSettings(true, CLIENT_ID, CLIENT_SECRET, null).isReady());
    }

    @Test
    void trimsSpacesAndTrailingNewlinesFromValues() {
        GoogleCalendarSettings settings = new GoogleCalendarSettings(
                true, "  " + CLIENT_ID + " ", CLIENT_SECRET + "\n", " " + REDIRECT_URI + "\r\n");

        assertTrue(settings.isReady());
        assertEquals(CLIENT_ID, settings.getClientId());
        assertEquals(CLIENT_SECRET, settings.getClientSecret());
        assertEquals(REDIRECT_URI, settings.getRedirectUri());
    }
}

package com.cmpt276.project.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GoogleCalendarSettings {
    private final boolean enabled;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public GoogleCalendarSettings(
            @Value("${bookeasy.calendar.enabled:false}") boolean enabled,
            @Value("${bookeasy.calendar.client-id:}") String clientId,
            @Value("${bookeasy.calendar.client-secret:}") String clientSecret,
            @Value("${bookeasy.calendar.redirect-uri:}") String redirectUri) {
        this.enabled = enabled;
        // trim so a pasted value with a trailing space or newline still works
        this.clientId = clean(clientId);
        this.clientSecret = clean(clientSecret);
        this.redirectUri = clean(redirectUri);
    }

    public boolean isReady() {
        return enabled && hasText(clientId) && hasText(clientSecret) && hasText(redirectUri);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isEmpty();
    }
}

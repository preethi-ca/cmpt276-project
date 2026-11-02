package com.cmpt276.project.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BookingEmailSettings {
    private final boolean enabled;
    private final String apiKey;
    private final String senderEmail;
    private final String senderName;

    public BookingEmailSettings(
            @Value("${bookeasy.email.enabled:false}") boolean enabled,
            @Value("${bookeasy.email.api-key:}") String apiKey,
            @Value("${bookeasy.email.sender-email:}") String senderEmail,
            @Value("${bookeasy.email.sender-name:BookEasy}") String senderName) {
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
    }

    public boolean isReady() {
        return enabled && hasText(apiKey) && hasText(senderEmail) && hasText(senderName);
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public String getSenderName() {
        return senderName;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}

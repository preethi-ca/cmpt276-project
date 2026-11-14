package com.cmpt276.project.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BookingSmsSettings {
    private final boolean enabled;
    private final String accountSid;
    private final String apiKeySid;
    private final String apiKeySecret;
    private final String fromNumber;

    public BookingSmsSettings(
            @Value("${bookeasy.sms.enabled:false}") boolean enabled,
            @Value("${bookeasy.sms.account-sid:}") String accountSid,
            @Value("${bookeasy.sms.api-key-sid:}") String apiKeySid,
            @Value("${bookeasy.sms.api-key-secret:}") String apiKeySecret,
            @Value("${bookeasy.sms.from-number:}") String fromNumber) {
        this.enabled = enabled;
        this.accountSid = accountSid;
        this.apiKeySid = apiKeySid;
        this.apiKeySecret = apiKeySecret;
        this.fromNumber = fromNumber;
    }

    public boolean isReady() {
        return enabled && hasText(accountSid) && hasText(apiKeySid)
                && hasText(apiKeySecret) && hasText(fromNumber);
    }

    public String getAccountSid() {
        return accountSid;
    }

    public String getApiKeySid() {
        return apiKeySid;
    }

    public String getApiKeySecret() {
        return apiKeySecret;
    }

    public String getFromNumber() {
        return fromNumber;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}

package com.cmpt276.project.model;

// a message is null when the customer didn't pick that channel
public class AppointmentNoticeResult {
    private final String emailMessage;
    private final String smsMessage;

    public AppointmentNoticeResult(String emailMessage, String smsMessage) {
        this.emailMessage = emailMessage;
        this.smsMessage = smsMessage;
    }

    public String getEmailMessage() {
        return emailMessage;
    }

    public String getSmsMessage() {
        return smsMessage;
    }
}

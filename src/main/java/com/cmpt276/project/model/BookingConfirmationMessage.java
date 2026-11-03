package com.cmpt276.project.model;

public class BookingConfirmationMessage {
    private final String recipient;
    private final String subject;
    private final String body;

    public BookingConfirmationMessage(String recipient, String subject, String body) {
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }
}

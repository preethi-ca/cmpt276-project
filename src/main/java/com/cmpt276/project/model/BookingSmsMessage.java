package com.cmpt276.project.model;

public class BookingSmsMessage {
    private final String recipient;
    private final String text;

    public BookingSmsMessage(String recipient, String text) {
        this.recipient = recipient;
        this.text = text;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getText() {
        return text;
    }
}

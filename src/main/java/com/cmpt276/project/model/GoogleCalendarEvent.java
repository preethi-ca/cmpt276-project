package com.cmpt276.project.model;

import java.time.LocalDateTime;

// what we put in the customer's Google Calendar for one appointment (not saved in the database)
public class GoogleCalendarEvent {
    private final String summary;
    private final String location;
    private final String description;
    private final LocalDateTime start;
    private final LocalDateTime end;
    private final String timeZone;

    public GoogleCalendarEvent(String summary, String location, String description,
            LocalDateTime start, LocalDateTime end, String timeZone) {
        this.summary = summary;
        this.location = location;
        this.description = description;
        this.start = start;
        this.end = end;
        this.timeZone = timeZone;
    }

    public String getSummary() {
        return summary;
    }

    public String getLocation() {
        return location;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public String getTimeZone() {
        return timeZone;
    }
}

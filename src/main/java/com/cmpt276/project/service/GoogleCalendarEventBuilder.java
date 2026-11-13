package com.cmpt276.project.service;

import com.cmpt276.project.model.Appointment;
import com.cmpt276.project.model.GoogleCalendarEvent;
import com.cmpt276.project.owner.model.Salon;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.StringJoiner;
import org.springframework.stereotype.Component;

// turns a booking into the event we add to the customer's Google Calendar
@Component
public class GoogleCalendarEventBuilder {
    static final String TIME_ZONE = "America/Vancouver";
    static final String DESCRIPTION = "Booked with BookEasy. You can view or change it in My Appointments.";

    public GoogleCalendarEvent build(Appointment appointment, Salon salon) {
        if (appointment == null || appointment.getId() <= 0) {
            throw new IllegalArgumentException("A saved appointment is required");
        }
        if (salon == null) {
            throw new IllegalArgumentException("Salon details are required");
        }

        requireText(appointment.getSalonId(), "Appointment salon ID");
        requireText(salon.getId(), "Salon ID");
        if (!appointment.getSalonId().equals(salon.getId())) {
            throw new IllegalArgumentException("The salon does not match the appointment");
        }

        requireText(salon.getName(), "Salon name");
        requireText(appointment.getServiceName(), "Service name");
        requireText(appointment.getAppointmentTime(), "Appointment time");
        if (appointment.getAppointmentDate() == null) {
            throw new IllegalArgumentException("Appointment date is required");
        }

        LocalTime time = parseTime(appointment.getAppointmentTime());
        LocalDateTime start = appointment.getAppointmentDate().atTime(time);
        // appointments are always one hour, plusHours rolls a late slot into the next day
        LocalDateTime end = start.plusHours(1);

        String summary = appointment.getServiceName() + " at " + salon.getName();
        return new GoogleCalendarEvent(summary, addressLine(salon), DESCRIPTION, start, end, TIME_ZONE);
    }

    // address, city and province joined with commas, empty if the salon has none
    private String addressLine(Salon salon) {
        StringJoiner line = new StringJoiner(", ");
        addIfText(line, salon.getAddress());
        addIfText(line, salon.getCity());
        addIfText(line, salon.getProvince());
        return line.toString();
    }

    private void addIfText(StringJoiner line, String value) {
        if (value != null && !value.trim().isEmpty()) {
            line.add(value.trim());
        }
    }

    // times are saved like "10:00", anything else is a bad booking
    private LocalTime parseTime(String value) {
        if (!value.matches("[0-9]{2}:[0-9]{2}")) {
            throw new IllegalArgumentException("Appointment time must be HH:mm");
        }
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Appointment time must be HH:mm");
        }
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }
}

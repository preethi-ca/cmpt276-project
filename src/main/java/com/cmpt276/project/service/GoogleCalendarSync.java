package com.cmpt276.project.service;

import com.cmpt276.project.model.Appointment;
import com.cmpt276.project.model.GoogleCalendarConnection;
import com.cmpt276.project.model.GoogleCalendarEvent;
import com.cmpt276.project.model.GoogleCalendarResult;
import com.cmpt276.project.owner.model.Salon;
import com.cmpt276.project.owner.service.OwnerService;
import com.cmpt276.project.repository.AppointmentRepository;
import com.cmpt276.project.repository.GoogleCalendarConnectionRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

// keeps the customer's Google Calendar in step with their bookings if they connected one
@Service
public class GoogleCalendarSync {
    private final GoogleCalendarSettings settings;
    private final GoogleCalendarConnectionRepository connectionRepository;
    private final AppointmentRepository appointmentRepository;
    private final OwnerService ownerService;
    private final GoogleCalendarEventBuilder eventBuilder;
    private final GoogleCalendarClient client;

    public GoogleCalendarSync(GoogleCalendarSettings settings,
            GoogleCalendarConnectionRepository connectionRepository,
            AppointmentRepository appointmentRepository,
            OwnerService ownerService,
            GoogleCalendarEventBuilder eventBuilder,
            GoogleCalendarClient client) {
        this.settings = settings;
        this.connectionRepository = connectionRepository;
        this.appointmentRepository = appointmentRepository;
        this.ownerService = ownerService;
        this.eventBuilder = eventBuilder;
        this.client = client;
    }

    // the booking is already saved here, so this never throws
    public GoogleCalendarResult addBooking(Appointment appointment) {
        try {
            Optional<GoogleCalendarConnection> connection = findConnection(appointment);
            if (connection.isEmpty()) {
                return GoogleCalendarResult.NOT_CONNECTED;
            }
            // already on their calendar, adding it again would make a duplicate
            if (appointment.getGoogleEventId() != null) {
                return GoogleCalendarResult.ADDED;
            }

            Salon salon = ownerService.findPublicSalon(appointment.getSalonId());
            GoogleCalendarEvent event = eventBuilder.build(appointment, salon);
            String eventId = client.createEvent(connection.get().getRefreshToken(), event);
            if (eventId == null) {
                return GoogleCalendarResult.FAILED;
            }
            appointment.setGoogleEventId(eventId);
            appointmentRepository.save(appointment);
            return GoogleCalendarResult.ADDED;
        } catch (RuntimeException error) {
            // missing salon, bad booking data or a client problem must not undo the booking
            return GoogleCalendarResult.FAILED;
        }
    }

    // the change is already saved here, so this never throws
    public GoogleCalendarResult updateBooking(Appointment appointment) {
        try {
            Optional<GoogleCalendarConnection> connection = findConnection(appointment);
            if (connection.isEmpty()) {
                return GoogleCalendarResult.NOT_CONNECTED;
            }
            // they connected after booking, so there is no event to update yet
            if (appointment.getGoogleEventId() == null) {
                return addBooking(appointment);
            }

            Salon salon = ownerService.findPublicSalon(appointment.getSalonId());
            GoogleCalendarEvent event = eventBuilder.build(appointment, salon);
            boolean updated = client.updateEvent(connection.get().getRefreshToken(), appointment.getGoogleEventId(), event);
            if (updated) {
                return GoogleCalendarResult.UPDATED;
            }
            return GoogleCalendarResult.FAILED;
        } catch (RuntimeException error) {
            return GoogleCalendarResult.FAILED;
        }
    }

    // called before the appointment is deleted, so the event id is still there
    public GoogleCalendarResult removeBooking(Appointment appointment) {
        try {
            Optional<GoogleCalendarConnection> connection = findConnection(appointment);
            // no event on their calendar means nothing to remove
            if (connection.isEmpty() || appointment.getGoogleEventId() == null) {
                return GoogleCalendarResult.NOT_CONNECTED;
            }
            boolean removed = client.deleteEvent(connection.get().getRefreshToken(), appointment.getGoogleEventId());
            if (removed) {
                return GoogleCalendarResult.REMOVED;
            }
            return GoogleCalendarResult.FAILED;
        } catch (RuntimeException error) {
            return GoogleCalendarResult.FAILED;
        }
    }

    // message for the booking page
    public String feedback(GoogleCalendarResult result) {
        if (result == GoogleCalendarResult.ADDED) {
            return "Added to your Google Calendar.";
        }
        if (result == GoogleCalendarResult.UPDATED) {
            return "Your Google Calendar event was updated.";
        }
        if (result == GoogleCalendarResult.REMOVED) {
            return "Removed from your Google Calendar.";
        }
        if (result == GoogleCalendarResult.FAILED) {
            return "Couldn't add it to Google Calendar. Your appointment is still booked.";
        }
        // not connected, nothing to show
        return null;
    }

    // message after editing an appointment
    public String updateFeedback(GoogleCalendarResult result) {
        if (result == GoogleCalendarResult.FAILED) {
            return "Couldn't update Google Calendar. Your appointment change is still saved.";
        }
        return feedback(result);
    }

    // message after cancelling an appointment
    public String removeFeedback(GoogleCalendarResult result) {
        if (result == GoogleCalendarResult.FAILED) {
            return "Couldn't remove it from Google Calendar. Your appointment is still cancelled.";
        }
        return feedback(result);
    }

    // empty if Google Calendar is off or this customer never connected
    private Optional<GoogleCalendarConnection> findConnection(Appointment appointment) {
        if (!settings.isReady() || appointment == null || appointment.getCustomer() == null) {
            return Optional.empty();
        }
        return connectionRepository.findByCustomer(appointment.getCustomer());
    }
}

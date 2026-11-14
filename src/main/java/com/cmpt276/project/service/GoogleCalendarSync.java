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

// adds a saved booking to the customer's Google Calendar if they connected one
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
            if (!settings.isReady() || appointment == null || appointment.getCustomer() == null) {
                return GoogleCalendarResult.NOT_CONNECTED;
            }
            Optional<GoogleCalendarConnection> connection = connectionRepository.findByCustomer(appointment.getCustomer());
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

    public String feedback(GoogleCalendarResult result) {
        if (result == GoogleCalendarResult.ADDED) {
            return "Added to your Google Calendar.";
        }
        if (result == GoogleCalendarResult.FAILED) {
            return "Couldn't add it to Google Calendar. Your appointment is still booked.";
        }
        // not connected, nothing to show
        return null;
    }
}

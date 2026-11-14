package com.cmpt276.project.service;

import com.cmpt276.project.model.Account;
import com.cmpt276.project.model.Appointment;
import com.cmpt276.project.model.GoogleCalendarConnection;
import com.cmpt276.project.model.GoogleCalendarEvent;
import com.cmpt276.project.model.GoogleCalendarResult;
import com.cmpt276.project.model.RoleType;
import com.cmpt276.project.owner.model.Salon;
import com.cmpt276.project.owner.service.OwnerService;
import com.cmpt276.project.repository.AppointmentRepository;
import com.cmpt276.project.repository.GoogleCalendarConnectionRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GoogleCalendarSyncTests {
    private static final String REFRESH_TOKEN = "fake-refresh-token";

    private final GoogleCalendarSettings settings = mock(GoogleCalendarSettings.class);
    private final GoogleCalendarConnectionRepository connectionRepository = mock(GoogleCalendarConnectionRepository.class);
    private final AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
    private final OwnerService ownerService = mock(OwnerService.class);
    private final GoogleCalendarClient client = mock(GoogleCalendarClient.class);
    private final GoogleCalendarSync sync = new GoogleCalendarSync(settings, connectionRepository,
            appointmentRepository, ownerService, new GoogleCalendarEventBuilder(), client);

    private Account alex;
    private Appointment appointment;

    @BeforeEach
    void connectedCustomerWithASavedBooking() {
        alex = new Account("Alex Lee", "alex@example.invalid", "fake-password-hash", RoleType.CUSTOMER);
        appointment = new Appointment("Haircut", "service-1", "salon-1", LocalDate.of(2026, 11, 20), "10:00", alex);
        // no database here, so we set the id like a saved appointment would have
        ReflectionTestUtils.setField(appointment, "id", 7);

        Salon salon = new Salon();
        salon.setId("salon-1");
        salon.setName("Test Salon");
        salon.setAddress("123 Main Street");

        when(settings.isReady()).thenReturn(true);
        when(connectionRepository.findByCustomer(alex))
                .thenReturn(Optional.of(new GoogleCalendarConnection(alex, REFRESH_TOKEN)));
        when(ownerService.findPublicSalon("salon-1")).thenReturn(salon);
    }

    @Test
    void connectedCustomerGetsOneEventAndTheIdIsSaved() {
        when(client.createEvent(eq(REFRESH_TOKEN), any(GoogleCalendarEvent.class))).thenReturn("fake-event-id");

        assertEquals(GoogleCalendarResult.ADDED, sync.addBooking(appointment));

        verify(client, times(1)).createEvent(eq(REFRESH_TOKEN), any(GoogleCalendarEvent.class));
        assertEquals("fake-event-id", appointment.getGoogleEventId());
        verify(appointmentRepository).save(appointment);
    }

    @Test
    void customerWithoutAConnectionIsNotSentToGoogle() {
        when(connectionRepository.findByCustomer(alex)).thenReturn(Optional.empty());

        assertEquals(GoogleCalendarResult.NOT_CONNECTED, sync.addBooking(appointment));

        verifyNoInteractions(client, appointmentRepository);
        assertNull(appointment.getGoogleEventId());
    }

    @Test
    void settingsNotReadyOrMissingDetailsAreNotSentToGoogle() {
        when(settings.isReady()).thenReturn(false);
        assertEquals(GoogleCalendarResult.NOT_CONNECTED, sync.addBooking(appointment));

        when(settings.isReady()).thenReturn(true);
        assertEquals(GoogleCalendarResult.NOT_CONNECTED, sync.addBooking(null));
        Appointment noCustomer = new Appointment("Haircut", "service-1", "salon-1", LocalDate.of(2026, 11, 20), "10:00", null);
        assertEquals(GoogleCalendarResult.NOT_CONNECTED, sync.addBooking(noCustomer));

        verifyNoInteractions(client, appointmentRepository);
    }

    @Test
    void nullFromGoogleGivesFailedAndSavesNoId() {
        when(client.createEvent(anyString(), any(GoogleCalendarEvent.class))).thenReturn(null);

        assertEquals(GoogleCalendarResult.FAILED, sync.addBooking(appointment));

        assertNull(appointment.getGoogleEventId());
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void clientErrorMissingSalonOrFailedSaveGiveFailedWithoutThrowing() {
        when(client.createEvent(anyString(), any(GoogleCalendarEvent.class)))
                .thenThrow(new IllegalStateException("Test client failure"));
        assertEquals(GoogleCalendarResult.FAILED, sync.addBooking(appointment));

        when(ownerService.findPublicSalon("salon-1")).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        assertEquals(GoogleCalendarResult.FAILED, sync.addBooking(appointment));

        Appointment badTime = new Appointment("Haircut", "service-1", "salon-1", LocalDate.of(2026, 11, 20), "10am", alex);
        ReflectionTestUtils.setField(badTime, "id", 8);
        assertEquals(GoogleCalendarResult.FAILED, sync.addBooking(badTime));

        verify(appointmentRepository, never()).save(any());
        assertNull(appointment.getGoogleEventId());
    }

    @Test
    void failedSaveAfterGoogleAcceptsGivesFailedWithoutThrowing() {
        when(client.createEvent(anyString(), any(GoogleCalendarEvent.class))).thenReturn("fake-event-id");
        when(appointmentRepository.save(appointment)).thenThrow(new IllegalStateException("Test save failure"));

        assertEquals(GoogleCalendarResult.FAILED, sync.addBooking(appointment));
    }

    @Test
    void appointmentAlreadyOnTheCalendarIsNotSentAgain() {
        appointment.setGoogleEventId("existing-event-id");

        assertEquals(GoogleCalendarResult.ADDED, sync.addBooking(appointment));

        verifyNoInteractions(client, appointmentRepository);
        assertEquals("existing-event-id", appointment.getGoogleEventId());
    }

    @Test
    void feedbackTextMatchesEachResult() {
        assertEquals("Added to your Google Calendar.", sync.feedback(GoogleCalendarResult.ADDED));
        assertEquals("Couldn't add it to Google Calendar. Your appointment is still booked.",
                sync.feedback(GoogleCalendarResult.FAILED));
        assertNull(sync.feedback(GoogleCalendarResult.NOT_CONNECTED));
    }

    @Test
    void editedAppointmentUpdatesTheSameEvent() {
        appointment.setGoogleEventId("existing-event-id");
        when(client.updateEvent(eq(REFRESH_TOKEN), eq("existing-event-id"), any(GoogleCalendarEvent.class))).thenReturn(true);

        assertEquals(GoogleCalendarResult.UPDATED, sync.updateBooking(appointment));

        verify(client, times(1)).updateEvent(eq(REFRESH_TOKEN), eq("existing-event-id"), any(GoogleCalendarEvent.class));
        verify(client, never()).createEvent(anyString(), any());
        assertEquals("existing-event-id", appointment.getGoogleEventId());
    }

    @Test
    void updateRejectedByGoogleGivesFailed() {
        appointment.setGoogleEventId("existing-event-id");
        when(client.updateEvent(anyString(), anyString(), any(GoogleCalendarEvent.class))).thenReturn(false);

        assertEquals(GoogleCalendarResult.FAILED, sync.updateBooking(appointment));
    }

    @Test
    void updateWithNoEventYetAddsANewOne() {
        when(client.createEvent(eq(REFRESH_TOKEN), any(GoogleCalendarEvent.class))).thenReturn("new-event-id");

        assertEquals(GoogleCalendarResult.ADDED, sync.updateBooking(appointment));

        verify(client, never()).updateEvent(anyString(), anyString(), any());
        assertEquals("new-event-id", appointment.getGoogleEventId());
        verify(appointmentRepository).save(appointment);
    }

    @Test
    void updateForCustomerWithoutAConnectionIsNotSentToGoogle() {
        appointment.setGoogleEventId("existing-event-id");
        when(connectionRepository.findByCustomer(alex)).thenReturn(Optional.empty());

        assertEquals(GoogleCalendarResult.NOT_CONNECTED, sync.updateBooking(appointment));
        assertEquals(GoogleCalendarResult.NOT_CONNECTED, sync.updateBooking(null));

        verifyNoInteractions(client, appointmentRepository);
    }

    @Test
    void cancelledAppointmentRemovesTheEvent() {
        appointment.setGoogleEventId("existing-event-id");
        when(client.deleteEvent(REFRESH_TOKEN, "existing-event-id")).thenReturn(true);

        assertEquals(GoogleCalendarResult.REMOVED, sync.removeBooking(appointment));

        verify(client, times(1)).deleteEvent(REFRESH_TOKEN, "existing-event-id");
        verifyNoInteractions(appointmentRepository);
    }

    @Test
    void removeRejectedByGoogleGivesFailed() {
        appointment.setGoogleEventId("existing-event-id");
        when(client.deleteEvent(anyString(), anyString())).thenReturn(false);

        assertEquals(GoogleCalendarResult.FAILED, sync.removeBooking(appointment));
    }

    @Test
    void removeWithNoEventOrNoConnectionIsNotSentToGoogle() {
        assertEquals(GoogleCalendarResult.NOT_CONNECTED, sync.removeBooking(appointment));

        appointment.setGoogleEventId("existing-event-id");
        when(connectionRepository.findByCustomer(alex)).thenReturn(Optional.empty());
        assertEquals(GoogleCalendarResult.NOT_CONNECTED, sync.removeBooking(appointment));

        when(settings.isReady()).thenReturn(false);
        assertEquals(GoogleCalendarResult.NOT_CONNECTED, sync.removeBooking(appointment));
        assertEquals(GoogleCalendarResult.NOT_CONNECTED, sync.removeBooking(null));

        verifyNoInteractions(client, appointmentRepository);
    }

    @Test
    void clientErrorsOnUpdateOrRemoveGiveFailedWithoutThrowing() {
        appointment.setGoogleEventId("existing-event-id");
        when(client.updateEvent(anyString(), anyString(), any(GoogleCalendarEvent.class)))
                .thenThrow(new IllegalStateException("Test client failure"));
        when(client.deleteEvent(anyString(), anyString()))
                .thenThrow(new IllegalStateException("Test client failure"));

        assertEquals(GoogleCalendarResult.FAILED, sync.updateBooking(appointment));
        assertEquals(GoogleCalendarResult.FAILED, sync.removeBooking(appointment));

        when(ownerService.findPublicSalon("salon-1")).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        assertEquals(GoogleCalendarResult.FAILED, sync.updateBooking(appointment));
    }

    @Test
    void editAndCancelFeedbackTextMatchesEachResult() {
        assertEquals("Your Google Calendar event was updated.", sync.updateFeedback(GoogleCalendarResult.UPDATED));
        assertEquals("Added to your Google Calendar.", sync.updateFeedback(GoogleCalendarResult.ADDED));
        assertEquals("Couldn't update Google Calendar. Your appointment change is still saved.",
                sync.updateFeedback(GoogleCalendarResult.FAILED));
        assertNull(sync.updateFeedback(GoogleCalendarResult.NOT_CONNECTED));

        assertEquals("Removed from your Google Calendar.", sync.removeFeedback(GoogleCalendarResult.REMOVED));
        assertEquals("Couldn't remove it from Google Calendar. Your appointment is still cancelled.",
                sync.removeFeedback(GoogleCalendarResult.FAILED));
        assertNull(sync.removeFeedback(GoogleCalendarResult.NOT_CONNECTED));
    }
}

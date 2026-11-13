package com.cmpt276.project.service;

import com.cmpt276.project.model.Appointment;
import com.cmpt276.project.model.GoogleCalendarEvent;
import com.cmpt276.project.owner.model.Salon;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class GoogleCalendarEventBuilderTests {
    private final GoogleCalendarEventBuilder builder = new GoogleCalendarEventBuilder();

    @Test
    void buildsAOneHourEventWithTheBookingDetails() {
        Salon salon = newSalon("salon-1", "Test Salon", "123 Main Street");
        salon.setCity("Vancouver");
        salon.setProvince("British Columbia");
        Appointment appointment = savedAppointment(7, "Haircut", "salon-1", LocalDate.of(2026, 11, 20), "10:00");

        GoogleCalendarEvent event = builder.build(appointment, salon);

        assertEquals("Haircut at Test Salon", event.getSummary());
        assertEquals("123 Main Street, Vancouver, British Columbia", event.getLocation());
        assertEquals("Booked with BookEasy. You can view or change it in My Appointments.", event.getDescription());
        assertEquals(LocalDateTime.of(2026, 11, 20, 10, 0), event.getStart());
        assertEquals(LocalDateTime.of(2026, 11, 20, 11, 0), event.getEnd());
        assertEquals("America/Vancouver", event.getTimeZone());
    }

    @Test
    void daylightSavingDayStillGivesAOneHourEventAtTheBookedTime() {
        // Vancouver falls back an hour early on 2026-11-01
        Salon salon = newSalon("salon-1", "Test Salon", "123 Main Street");
        Appointment appointment = savedAppointment(7, "Haircut", "salon-1", LocalDate.of(2026, 11, 1), "10:00");

        GoogleCalendarEvent event = builder.build(appointment, salon);

        assertEquals(LocalDateTime.of(2026, 11, 1, 10, 0), event.getStart());
        assertEquals(LocalDateTime.of(2026, 11, 1, 11, 0), event.getEnd());
        assertEquals(Duration.ofHours(1), Duration.between(event.getStart(), event.getEnd()));
        assertEquals("America/Vancouver", event.getTimeZone());
    }

    @Test
    void lateSlotEndsAtMidnightTheNextDay() {
        Salon salon = newSalon("salon-1", "Test Salon", "123 Main Street");
        Appointment appointment = savedAppointment(7, "Haircut", "salon-1", LocalDate.of(2026, 12, 31), "23:00");

        GoogleCalendarEvent event = builder.build(appointment, salon);

        assertEquals(LocalDateTime.of(2026, 12, 31, 23, 0), event.getStart());
        assertEquals(LocalDateTime.of(2027, 1, 1, 0, 0), event.getEnd());
    }

    @Test
    void salonWithNoAddressGivesAnEmptyLocation() {
        Salon salon = newSalon("salon-1", "Test Salon", null);
        Appointment appointment = savedAppointment(7, "Haircut", "salon-1", LocalDate.of(2026, 11, 20), "10:00");

        assertEquals("", builder.build(appointment, salon).getLocation());
        salon.setAddress("   ");
        salon.setCity("");
        assertEquals("", builder.build(appointment, salon).getLocation());
    }

    @Test
    void refusesBadBookingDetails() {
        Salon salon = newSalon("salon-1", "Test Salon", null);
        LocalDate date = LocalDate.of(2026, 11, 20);

        Appointment unsaved = new Appointment("Haircut", "service-1", "salon-1", date, "10:00", null);
        assertCannotBuild(unsaved, salon);
        assertCannotBuild(null, salon);

        Appointment appointment = savedAppointment(7, "Haircut", "salon-1", date, "10:00");
        assertCannotBuild(appointment, null);
        assertCannotBuild(appointment, newSalon("salon-2", "North Salon", null));

        assertCannotBuild(savedAppointment(8, "", "salon-1", date, "10:00"), salon);
        assertCannotBuild(savedAppointment(9, "Haircut", "salon-1", null, "10:00"), salon);
        assertCannotBuild(savedAppointment(10, "Haircut", "salon-1", date, null), salon);
        assertCannotBuild(savedAppointment(11, "Haircut", "salon-1", date, "25:00"), salon);
        assertCannotBuild(savedAppointment(12, "Haircut", "salon-1", date, "10am"), salon);
    }

    private void assertCannotBuild(Appointment appointment, Salon salon) {
        boolean rejected = false;
        try {
            builder.build(appointment, salon);
        } catch (IllegalArgumentException exception) {
            rejected = true;
        }
        assertTrue(rejected, "Bad booking details should not make a calendar event");
    }

    // no database here, so we set the id like a saved appointment would have
    private Appointment savedAppointment(int id, String service, String salonId, LocalDate date, String time) {
        Appointment appointment = new Appointment(service, "service-1", salonId, date, time, null);
        ReflectionTestUtils.setField(appointment, "id", id);
        return appointment;
    }

    private Salon newSalon(String id, String name, String address) {
        Salon salon = new Salon();
        salon.setId(id);
        salon.setName(name);
        salon.setAddress(address);
        return salon;
    }
}

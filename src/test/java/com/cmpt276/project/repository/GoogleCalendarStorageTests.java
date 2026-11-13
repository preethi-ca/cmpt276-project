package com.cmpt276.project.repository;

import com.cmpt276.project.model.Account;
import com.cmpt276.project.model.Appointment;
import com.cmpt276.project.model.GoogleCalendarConnection;
import com.cmpt276.project.model.RoleType;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:google-calendar-storage;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "bookeasy.email.enabled=false",
        "bookeasy.calendar.enabled=false"
})
class GoogleCalendarStorageTests {
    @Autowired
    private GoogleCalendarConnectionRepository connectionRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private AccountRepository accountRepository;

    private Account alex;
    private Account sam;

    @BeforeEach
    void startWithEmptyTables() {
        connectionRepository.deleteAll();
        appointmentRepository.deleteAll();
        accountRepository.deleteAll();
        alex = accountRepository.save(new Account("Alex Lee", "alex@example.invalid", "fake-password-hash", RoleType.CUSTOMER));
        sam = accountRepository.save(new Account("Sam Chen", "sam@example.invalid", "fake-password-hash", RoleType.CUSTOMER));
    }

    @Test
    void savesAndLoadsAConnectionForItsCustomer() {
        connectionRepository.save(new GoogleCalendarConnection(alex, "fake-refresh-token"));

        GoogleCalendarConnection loaded = connectionRepository.findByCustomer(alex).orElseThrow();

        assertEquals("fake-refresh-token", loaded.getRefreshToken());
        assertEquals(alex.getId(), loaded.getCustomer().getId());
        assertNotNull(loaded.getConnectedAt());
    }

    @Test
    void customerWithoutAConnectionFindsNothing() {
        connectionRepository.save(new GoogleCalendarConnection(alex, "fake-refresh-token"));

        assertTrue(connectionRepository.findByCustomer(sam).isEmpty());
    }

    @Test
    void deletingACustomersConnectionRemovesIt() {
        connectionRepository.save(new GoogleCalendarConnection(alex, "fake-refresh-token"));
        connectionRepository.save(new GoogleCalendarConnection(sam, "other-refresh-token"));

        connectionRepository.deleteByCustomer(alex);

        assertTrue(connectionRepository.findByCustomer(alex).isEmpty());
        assertTrue(connectionRepository.findByCustomer(sam).isPresent());
    }

    @Test
    void oneCustomerCannotHaveTwoConnections() {
        connectionRepository.saveAndFlush(new GoogleCalendarConnection(alex, "fake-refresh-token"));

        assertThrows(DataIntegrityViolationException.class,
                () -> connectionRepository.saveAndFlush(new GoogleCalendarConnection(alex, "second-refresh-token")));
        assertEquals(1, connectionRepository.count());
    }

    @Test
    void appointmentStartsWithoutAnEventIdAndKeepsOneOnceSet() {
        Appointment appointment = appointmentRepository.save(
                new Appointment("Haircut", "service-1", "salon-1", LocalDate.of(2026, 12, 3), "10:00", alex));

        assertNull(appointmentRepository.findById(appointment.getId()).orElseThrow().getGoogleEventId());

        appointment.setGoogleEventId("fake-event-id");
        appointmentRepository.save(appointment);

        assertEquals("fake-event-id", appointmentRepository.findById(appointment.getId()).orElseThrow().getGoogleEventId());
    }
}

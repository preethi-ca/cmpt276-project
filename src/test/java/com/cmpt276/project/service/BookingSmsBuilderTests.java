package com.cmpt276.project.service;

import com.cmpt276.project.model.Account;
import com.cmpt276.project.model.Appointment;
import com.cmpt276.project.model.BookingSmsMessage;
import com.cmpt276.project.model.RoleType;
import com.cmpt276.project.owner.model.Salon;
import com.cmpt276.project.repository.AccountRepository;
import com.cmpt276.project.repository.AppointmentRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class BookingSmsBuilderTests {
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    private final BookingSmsBuilder builder = new BookingSmsBuilder();

    @Test
    void includesTheRightDetailsForEachCustomer() {
        Salon salon = newSalon("salon-1", "CS_Salon");
        Account customer = saveCustomer("Alex Lee", "alex@example.invalid", "604-555-1234");
        Appointment appointment = saveAppointment(customer, salon, "Haircut", LocalDate.of(2026, 11, 3), "10:00");

        BookingSmsMessage message = builder.build(appointment, salon);

        assertEquals("+16045551234", message.getRecipient());
        assertEquals("BookEasy: Your Haircut at CS_Salon is confirmed for 2026-11-03 at 10:00 (Vancouver time)."
                + " You can view it in My Appointments.", message.getText());

        Salon secondSalon = newSalon("salon-2", "North Salon");
        Account secondCustomer = saveCustomer("Sam Chen", "sam@example.invalid", "778 555 9876");
        Appointment second = saveAppointment(secondCustomer, secondSalon, "Styling", LocalDate.of(2026, 11, 7), "15:00");
        BookingSmsMessage secondMessage = builder.build(second, secondSalon);

        assertEquals("+17785559876", secondMessage.getRecipient());
        assertTrue(secondMessage.getText().contains("Styling at North Salon"));
        assertTrue(secondMessage.getText().contains("2026-11-07 at 15:00 (Vancouver time)"));
        assertFalse(secondMessage.getText().contains("Haircut"));
        assertFalse(secondMessage.getText().contains("CS_Salon"));
    }

    @Test
    void normalizesEveryPhoneFormatRegistrationAccepts() {
        Salon salon = newSalon("salon-1", "CS_Salon");
        Account customer = saveCustomer("Alex Lee", "alex@example.invalid", null);
        Appointment appointment = saveAppointment(customer, salon, "Haircut", LocalDate.of(2026, 11, 3), "10:00");
        String[] formats = {
            "(604) 555-1234", "604.555.1234", "604-555-1234", "6045551234",
            "+1 604 555 1234", "16045551234", "1-604-555-1234", "  604 555 1234  "
        };

        for (String format : formats) {
            customer.setPhoneNumber(format);
            assertEquals("+16045551234", builder.build(appointment, salon).getRecipient(), format);
        }
    }

    @Test
    void refusesAMissingOrInvalidPhone() {
        Salon salon = newSalon("salon-1", "CS_Salon");
        Account customer = saveCustomer("Alex Lee", "alex@example.invalid", null);
        Appointment appointment = saveAppointment(customer, salon, "Haircut", LocalDate.of(2026, 11, 3), "10:00");
        String[] invalidPhones = {
            null, "", "   ", "604555123", "604555123456", "26045551234",
            "604-555-CALL", "6045551234x", "+44 20 7946 0958"
        };

        for (String phone : invalidPhones) {
            customer.setPhoneNumber(phone);
            assertCannotBuild(appointment, salon);
        }
        assertTrue(appointmentRepository.findById(appointment.getId()).isPresent());

        // The same booking works once the phone is valid, so the refusals above came from the phone
        customer.setPhoneNumber("6045551234");
        assertEquals("+16045551234", builder.build(appointment, salon).getRecipient());
    }

    @Test
    void rejectsInvalidBookingDetails() {
        Salon salon = newSalon("salon-1", "CS_Salon");
        Account customer = saveCustomer("Alex Lee", "alex@example.invalid", "6045551234");
        Appointment appointment = saveAppointment(customer, salon, "Haircut", LocalDate.of(2026, 11, 3), "10:00");

        assertCannotBuild(appointment, newSalon("salon-2", "North Salon"));
        assertCannotBuild(appointment, null);

        Appointment unsaved = new Appointment("Haircut", "service-1", salon.getId(), LocalDate.of(2026, 11, 3), "10:00", customer);
        assertCannotBuild(unsaved, salon);

        assertCannotBuild(saveAppointment(customer, salon, "", LocalDate.of(2026, 11, 3), "10:00"), salon);
        assertCannotBuild(saveAppointment(customer, salon, "Haircut", null, "10:00"), salon);
        assertCannotBuild(saveAppointment(customer, salon, "Haircut", LocalDate.of(2026, 11, 3), null), salon);
    }

    @Test
    void fitsInOneTextForTypicalNames() {
        Salon salon = newSalon("salon-1", "Downtown Beauty Studio");
        Account customer = saveCustomer("Alex Lee", "alex@example.invalid", "(604) 555-1234");
        Appointment appointment = saveAppointment(customer, salon, "Women's Haircut", LocalDate.of(2026, 12, 18), "16:30");

        String text = builder.build(appointment, salon).getText();

        assertTrue(text.length() <= 160, "Length was " + text.length());
        // A curly quote or emoji would switch the text to a much shorter segment size
        assertTrue(text.matches("[A-Za-z0-9 .,:'()_-]+"), text);
    }

    private void assertCannotBuild(Appointment appointment, Salon salon) {
        boolean rejected = false;
        try {
            builder.build(appointment, salon);
        } catch (IllegalArgumentException exception) {
            rejected = true;
        }
        assertTrue(rejected, "Invalid booking details should not produce a text message");
    }

    private Account saveCustomer(String name, String email, String phoneNumber) {
        Account customer = new Account(name, email, "test-password-hash", RoleType.CUSTOMER);
        customer.setPhoneNumber(phoneNumber);
        return accountRepository.saveAndFlush(customer);
    }

    private Appointment saveAppointment(Account customer, Salon salon, String service, LocalDate date, String time) {
        return appointmentRepository.saveAndFlush(new Appointment(service, "service-1", salon.getId(), date, time, customer));
    }

    private Salon newSalon(String id, String name) {
        Salon salon = new Salon();
        salon.setId(id);
        salon.setName(name);
        return salon;
    }
}

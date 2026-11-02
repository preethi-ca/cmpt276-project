package com.cmpt276.project.service;

import com.cmpt276.project.model.Account;
import com.cmpt276.project.model.Appointment;
import com.cmpt276.project.model.BookingConfirmationMessage;
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
class BookingConfirmationBuilderTests {
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    private final BookingConfirmationBuilder builder = new BookingConfirmationBuilder();

    @Test
    void includesTheSavedCustomerAndAppointmentDetails() {
        Salon salon = newSalon("salon-1", "CS_Salon", "123 Main Street");
        Account customer = saveCustomer("Alex Lee", "alex@example.invalid");
        Appointment appointment = saveAppointment(customer, salon, "Haircut", LocalDate.of(2026, 11, 3), "10:00");

        BookingConfirmationMessage message = builder.build(appointment, salon);

        assertEquals("alex@example.invalid", message.getRecipient());
        assertEquals("Your booking at CS_Salon is confirmed", message.getSubject());
        assertEquals("Hi Alex Lee,\n\n"
                + "Your booking is confirmed.\n\n"
                + "Booking reference: " + appointment.getId() + "\n"
                + "Salon: CS_Salon\n"
                + "Address: 123 Main Street\n"
                + "Service: Haircut\n"
                + "Date: 2026-11-03\n"
                + "Time: 10:00 (Vancouver time)\n"
                + "Duration: one hour\n\n"
                + "Sign in to BookEasy and check My Appointments to view your booking.\n", message.getBody());
        assertEquals(1, appointmentRepository.findByCustomer(customer).size());
    }

    @Test
    void keepsTwoCustomersAndSalonsSeparate() {
        Salon firstSalon = newSalon("salon-1", "CS_Salon", "123 Main Street");
        Salon secondSalon = newSalon("salon-2", "North Salon", "456 North Street");
        Account firstCustomer = saveCustomer("Alex Lee", "alex@example.invalid");
        Account secondCustomer = saveCustomer("Sam Chen", "sam@example.invalid");
        Appointment first = saveAppointment(firstCustomer, firstSalon, "Haircut", LocalDate.of(2026, 11, 3), "10:00");
        Appointment second = saveAppointment(secondCustomer, secondSalon, "Styling", LocalDate.of(2026, 11, 7), "15:00");

        BookingConfirmationMessage firstMessage = builder.build(first, firstSalon);
        BookingConfirmationMessage secondMessage = builder.build(second, secondSalon);

        assertEquals("alex@example.invalid", firstMessage.getRecipient());
        assertEquals("sam@example.invalid", secondMessage.getRecipient());
        assertEquals("Your booking at North Salon is confirmed", secondMessage.getSubject());
        assertTrue(secondMessage.getBody().contains("Hi Sam Chen,"));
        assertTrue(secondMessage.getBody().contains("Booking reference: " + second.getId() + "\n"));
        assertTrue(secondMessage.getBody().contains("Service: Styling\nDate: 2026-11-07\nTime: 15:00 (Vancouver time)"));
        assertFalse(secondMessage.getBody().contains("Alex Lee"));
        assertFalse(secondMessage.getBody().contains("CS_Salon"));
    }

    @Test
    void leavesOutAMissingOrBlankAddress() {
        Salon salon = newSalon("salon-1", "CS_Salon", null);
        Account customer = saveCustomer("Alex Lee", "alex@example.invalid");
        Appointment appointment = saveAppointment(customer, salon, "Haircut", LocalDate.of(2026, 11, 3), "10:00");

        assertFalse(builder.build(appointment, salon).getBody().contains("Address:"));
        salon.setAddress("   ");
        assertFalse(builder.build(appointment, salon).getBody().contains("Address:"));
    }

    @Test
    void rejectsASalonThatDoesNotMatchTheBooking() {
        Salon salon = newSalon("salon-1", "CS_Salon", null);
        Account customer = saveCustomer("Alex Lee", "alex@example.invalid");
        Appointment appointment = saveAppointment(customer, salon, "Haircut", LocalDate.of(2026, 11, 3), "10:00");
        Salon otherSalon = newSalon("salon-2", "North Salon", null);

        assertCannotBuild(appointment, otherSalon);
        assertTrue(appointmentRepository.findById(appointment.getId()).isPresent());
    }

    @Test
    void rejectsAnAppointmentThatHasNotBeenSaved() {
        Salon salon = newSalon("salon-1", "CS_Salon", null);
        Account customer = saveCustomer("Alex Lee", "alex@example.invalid");
        Appointment appointment = new Appointment("Haircut", "service-1", salon.getId(), LocalDate.of(2026, 11, 3), "10:00", customer);

        assertCannotBuild(appointment, salon);
    }

    @Test
    void rejectsAMissingCustomerEmail() {
        Salon salon = newSalon("salon-1", "CS_Salon", null);
        Account customer = saveCustomer("Alex Lee", "   ");
        Appointment appointment = saveAppointment(customer, salon, "Haircut", LocalDate.of(2026, 11, 3), "10:00");

        assertCannotBuild(appointment, salon);
        assertTrue(appointmentRepository.findById(appointment.getId()).isPresent());
    }

    @Test
    void rejectsMissingAppointmentDetails() {
        Salon salon = newSalon("salon-1", "CS_Salon", null);
        Account customer = saveCustomer("Alex Lee", "alex@example.invalid");
        Appointment noService = saveAppointment(customer, salon, "", LocalDate.of(2026, 11, 3), "10:00");
        Appointment noDate = saveAppointment(customer, salon, "Haircut", null, "10:00");
        Appointment noTime = saveAppointment(customer, salon, "Haircut", LocalDate.of(2026, 11, 3), null);

        assertCannotBuild(noService, salon);
        assertCannotBuild(noDate, salon);
        assertCannotBuild(noTime, salon);
    }

    private void assertCannotBuild(Appointment appointment, Salon salon) {
        boolean rejected = false;
        try {
            builder.build(appointment, salon);
        } catch (IllegalArgumentException exception) {
            rejected = true;
        }
        assertTrue(rejected, "Invalid booking details should not produce an email");
    }

    private Account saveCustomer(String name, String email) {
        return accountRepository.saveAndFlush(new Account(name, email, "test-password-hash", RoleType.CUSTOMER));
    }

    private Appointment saveAppointment(Account customer, Salon salon, String service, LocalDate date, String time) {
        return appointmentRepository.saveAndFlush(new Appointment(service, "service-1", salon.getId(), date, time, customer));
    }

    private Salon newSalon(String id, String name, String address) {
        Salon salon = new Salon();
        salon.setId(id);
        salon.setName(name);
        salon.setAddress(address);
        return salon;
    }
}

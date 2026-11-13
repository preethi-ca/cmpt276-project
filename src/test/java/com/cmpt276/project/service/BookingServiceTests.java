package com.cmpt276.project.service;

import com.cmpt276.project.model.Account;
import com.cmpt276.project.model.Appointment;
import com.cmpt276.project.model.RoleType;
import com.cmpt276.project.owner.model.Salon;
import com.cmpt276.project.owner.model.SalonService;
import com.cmpt276.project.owner.service.OwnerService;
import com.cmpt276.project.repository.AccountRepository;
import com.cmpt276.project.repository.AppointmentRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.cmpt276.project.owner.model.OpeningDay;

@ExtendWith(MockitoExtension.class)
class BookingServiceTests {
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private OwnerService ownerService;
    @InjectMocks
    private BookingService bookingService;

//tests for booking for a date that has already passed
    @Test
    void shouldRejectBookingForPastDate() {
        //Create a date from yesterday
        String pastDate = LocalDate.now().minusDays(1).toString();

        Account customer = new Account(
                "Alex Lee",
                "alex@example.com",
                "password",
                RoleType.CUSTOMER
        );
        when(accountRepository.findByEmail("alex@example.com"))
                .thenReturn(Optional.of(customer));
        Salon salon = mock(Salon.class);
        SalonService service = mock(SalonService.class);

        when(ownerService.findPublicSalonBySlug("magiccuts-salon"))
                .thenReturn(salon);
        when(salon.getServices())
                .thenReturn(List.of(service));
        when(service.getId())
                .thenReturn("service1");

        //Check that the service rejects the past appointment date
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> bookingService.bookAppointment(
                        "service1",
                        "magiccuts-salon",
                        pastDate,
                        "10:00",
                        "alex@example.com"
                )
        );
        assertEquals("Sorry, this date has already passed.", exception.getMessage());
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }



//Tests for editing appointment
    //Test that a customer can successfully update an appointment
    @Test
    void shouldUpdateAppointmentDateAndTime() {
        LocalDate date = LocalDate.now().plusDays(7);

        Account customer = new Account(
                "Alex Lee",
                "alex@example.com",
                "password",
                RoleType.CUSTOMER
        );
        Appointment appointment = mock(Appointment.class);
        when(appointment.getId()).thenReturn(42);
        when(appointment.getSalonId()).thenReturn("salon1");

        Salon salon = createTestSalon(date);

        when(accountRepository.findByEmail("alex@example.com"))
                .thenReturn(Optional.of(customer));
        when(appointmentRepository.findByIdAndCustomer(42, customer))
                .thenReturn(Optional.of(appointment));
        when(ownerService.findPublicSalon("salon1")).thenReturn(salon);
        when(ownerService.findPublicSalonBySlug("magiccuts-salon"))
                .thenReturn(salon);
        when(appointmentRepository.findBySalonIdAndAppointmentDate("salon1", date))
                .thenReturn(List.of(appointment));
        when(appointmentRepository.save(appointment)).thenReturn(appointment);

        bookingService.updateAppointment(42, date.toString(), "11:00", "alex@example.com");

        verify(appointment).setAppointmentDate(date);
        verify(appointment).setAppointmentTime("11:00");
        verify(appointmentRepository).save(appointment);
    }

    //Test that an unavailable time slot doesn't work
    @Test
    void shouldRejectUnavailableAppointmentTime() {
        LocalDate date = LocalDate.now().plusDays(7);

        Account customer = new Account(
                "Alex Lee",
                "alex@example.com",
                "password",
                RoleType.CUSTOMER
        );

        Appointment appointment = mock(Appointment.class);
        when(appointment.getId()).thenReturn(42);
        when(appointment.getSalonId()).thenReturn("salon1");

        Appointment bookedAppointment = mock(Appointment.class);
        when(bookedAppointment.getId()).thenReturn(99);
        when(bookedAppointment.getAppointmentTime()).thenReturn("11:00");

        Salon salon = createTestSalon(date);

        when(accountRepository.findByEmail("alex@example.com"))
                .thenReturn(Optional.of(customer));
        when(appointmentRepository.findByIdAndCustomer(42, customer))
                .thenReturn(Optional.of(appointment));
        when(ownerService.findPublicSalon("salon1")).thenReturn(salon);
        when(ownerService.findPublicSalonBySlug("magiccuts-salon"))
                .thenReturn(salon);
        when(appointmentRepository.findBySalonIdAndAppointmentDate("salon1", date))
                .thenReturn(List.of(bookedAppointment));

        assertThrows(IllegalArgumentException.class, () ->
                bookingService.updateAppointment(42, date.toString(), "11:00", "alex@example.com")
        );
        //check that the original appointment was not changed
        verify(appointment, never()).setAppointmentDate(any());
        verify(appointment, never()).setAppointmentTime(any());
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    //Test that editing to a past date doesn't work
    @Test
    void shouldRejectUpdatingAppointmentToPastDate() {
        LocalDate pastDate = LocalDate.now().minusDays(1);

        Account customer = new Account(
                "Alex Lee",
                "alex@example.com",
                "password",
                RoleType.CUSTOMER
        );

        Appointment appointment = mock(Appointment.class);
        when(accountRepository.findByEmail("alex@example.com"))
                .thenReturn(Optional.of(customer));
        when(appointmentRepository.findByIdAndCustomer(42, customer))
                .thenReturn(Optional.of(appointment));

        assertThrows(IllegalArgumentException.class, () ->
                bookingService.updateAppointment(42, pastDate.toString(), "11:00", "alex@example.com")
        );
        verify(appointment, never()).setAppointmentDate(any());
        verify(appointment, never()).setAppointmentTime(any());
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }


    //helper method for a salon with 9-5 opening hours
    private Salon createTestSalon(LocalDate date) {
        Salon salon = new Salon();
        salon.setId("salon1");
        salon.setSlug("magiccuts-salon");

        OpeningDay openingDay = new OpeningDay();
        openingDay.setDayOfWeek(date.getDayOfWeek().getValue());
        openingDay.setOpen(true);
        openingDay.setStartTime("09:00");
        openingDay.setEndTime("17:00");

        salon.setOpeningHours(List.of(openingDay));
        return salon;
    }


// Tests for cancelling appointments
    //Test that a customer can  cancel their own appointment
    @Test
    void shouldCancelOwnAppointment() {
        Account customer = new Account(
                "Alex Lee",
                "alex@example.com",
                "password",
                RoleType.CUSTOMER
        );
        Appointment appointment = mock(Appointment.class);

        when(accountRepository.findByEmail("alex@example.com"))
                .thenReturn(Optional.of(customer));
        when(appointmentRepository.findByIdAndCustomer(42, customer))
                .thenReturn(Optional.of(appointment));

        bookingService.cancelAppointment(42, "alex@example.com");

        // Check that the appointment was deleted
        verify(appointmentRepository).delete(appointment);
    }


    //Test that a customer cannot cancel another customer's appointment
    @Test
    void shouldRejectCancellingAnotherCustomersAppointment() {
        Account customer = new Account(
                "Alex Lee",
                "alex@example.com",
                "password",
                RoleType.CUSTOMER
        );
        when(accountRepository.findByEmail("alex@example.com"))
                .thenReturn(Optional.of(customer));
        when(appointmentRepository.findByIdAndCustomer(42, customer))
                .thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () ->
                bookingService.cancelAppointment(42, "alex@example.com")
        );
        // Check that no appointment was deleted
        verify(appointmentRepository, never()).delete(any(Appointment.class));
    }
}


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
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}


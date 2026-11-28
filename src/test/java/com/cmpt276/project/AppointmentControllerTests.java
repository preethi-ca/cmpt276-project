package com.cmpt276.project;

import com.cmpt276.project.model.Account;
import com.cmpt276.project.model.Appointment;
import com.cmpt276.project.model.RoleType;
import com.cmpt276.project.owner.model.Salon;
import com.cmpt276.project.owner.service.OwnerService;
import com.cmpt276.project.repository.AccountRepository;
import com.cmpt276.project.repository.AppointmentRepository;
import com.cmpt276.project.service.BookingService;
import com.cmpt276.project.owner.model.SalonService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AppointmentControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppointmentRepository appointmentRepository;

    @MockitoBean
    private AccountRepository accountRepository;

    @MockitoBean
    private OwnerService ownerService;

    @MockitoBean
    private BookingService bookingService;

    //Test that the owner's appointments page displays unavailable counts without accessing customer bookings
    @Test
    void ownerAppointmentsReadOnlyTheSignedInSalonAndRenderRealCounts() throws Exception {
        Account owner = mock(Account.class);
        Salon salon = mock(Salon.class);
        when(owner.getId()).thenReturn(42);
        when(owner.getRoleType()).thenReturn(RoleType.SALONOWNER);
        when(accountRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
        when(ownerService.findForOwner("42")).thenReturn(salon);
        when(salon.getId()).thenReturn("owned-salon");
        when(salon.getName()).thenReturn("Owner Salon");
        when(salon.getSlug()).thenReturn("owner-salon");
        when(ownerService.findPublicSalonBySlug("owner-salon")).thenReturn(salon);
        LocalDate today = LocalDate.now(ZoneId.of("America/Vancouver"));
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        when(appointmentRepository.countBySalonIdAndAppointmentDate("owned-salon", today)).thenReturn(2L);
        when(appointmentRepository.countBySalonIdAndAppointmentDateAfter("owned-salon", today)).thenReturn(11L);
        when(appointmentRepository.countBySalonIdAndAppointmentDateBetween("owned-salon", monday, monday.plusDays(6))).thenReturn(4L);
        var customer = new Account("Customer <script>", "customer@example.com", "private-hash", RoleType.CUSTOMER);
        var booked = new Appointment("Original <Cut>", "deleted-service", "owned-salon", today, "09:00", customer);
        when(appointmentRepository.findBySalonIdAndAppointmentDateGreaterThanEqualOrderByAppointmentDateAscAppointmentTimeAscIdAsc(
                "owned-salon", today, PageRequest.of(0, 10))).thenReturn(List.of(booked));
        mockMvc.perform(get("/owner/appointments").param("ownerId", "other").param("salonId", "other")
                        .with(user("owner@test.com").roles("SALONOWNER")))
                .andExpect(status().isOk())
                .andExpect(view().name("owner/workspace"))
                .andExpect(model().attribute("section", "appointments"))
                .andExpect(model().attribute("salon", salon))
                .andExpect(model().attribute("salonUrl", "/salons/owner-salon"))
                .andExpect(model().attribute("appointmentDate", today.toString()))
                .andExpect(content().string(not(containsString("href=\"/salons/owner-salon\""))))
                .andExpect(content().string(not(containsString("/demo/salons/"))))
                .andExpect(content().string(containsString("href=\"/owner/appointments\" aria-current=\"page\"")))
                .andExpect(content().string(not(containsString("Local demo"))))
                .andExpect(model().attribute("todayAppointmentCount", 2L))
                .andExpect(model().attribute("upcomingAppointmentCount", 11L))
                .andExpect(model().attribute("weekAppointmentCount", 4L))
                .andExpect(model().attribute("totalPages", 2L))
                .andExpect(content().string(containsString("Customer &lt;script&gt;")))
                .andExpect(content().string(containsString("Original &lt;Cut&gt;")))
                .andExpect(content().string(not(containsString("private-hash"))))
                .andExpect(content().string(not(containsString("No appointments to display"))))
                .andExpect(content().string(containsString("/owner/appointments?page=2")));
        verify(appointmentRepository).countBySalonIdAndAppointmentDate("owned-salon", today);
        verify(appointmentRepository).countBySalonIdAndAppointmentDateAfter("owned-salon", today);
        verify(appointmentRepository).countBySalonIdAndAppointmentDateBetween("owned-salon", monday, monday.plusDays(6));
        verify(appointmentRepository).findBySalonIdAndAppointmentDateGreaterThanEqualOrderByAppointmentDateAscAppointmentTimeAscIdAsc(
                "owned-salon", today, PageRequest.of(0, 10));
        verifyNoMoreInteractions(appointmentRepository);
        mockMvc.perform(get("/salons/owner-salon"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/public-salon"));
    }

    //Test that an owner must have an existing salon to access the appointments page
    @Test
    void ownerAppointmentsRequireAnExistingSalon() throws Exception {
        Account owner = mock(Account.class);
        when(owner.getId()).thenReturn(43);
        when(owner.getRoleType()).thenReturn(RoleType.SALONOWNER);
        when(accountRepository.findByEmail("new-owner@test.com")).thenReturn(Optional.of(owner));
        mockMvc.perform(get("/owner/appointments").with(user("new-owner@test.com").roles("SALONOWNER")))
                .andExpect(redirectedUrl("/owner/setup"));
        verifyNoInteractions(appointmentRepository);
    }

    //Test that guests and customers cannot access the owner's appointments page
    @Test
    void ownerAppointmentsRejectGuestsAndCustomers() throws Exception {
        mockMvc.perform(get("/owner/appointments")).andExpect(redirectedUrl("/login"));
        mockMvc.perform(get("/owner/appointments").with(user("customer@test.com").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(appointmentRepository, ownerService);
    }

    //Test that a logged-in customer can view their appointments
    @Test
    void shouldShowCustomerAppointments() throws Exception {

        Account customer = mock(Account.class);
        Appointment appointment = mock(Appointment.class);
        Salon salon = mock(Salon.class);

        when(accountRepository.findByEmail("customer@test.com"))
                .thenReturn(Optional.of(customer));

        when(appointmentRepository.findByCustomer(customer))
                .thenReturn(List.of(appointment));

        when(appointment.getId())
                .thenReturn(1);

        when(appointment.getSalonId())
                .thenReturn("10");
        
        when(appointment.getAppointmentDate())
                .thenReturn(LocalDate.now(ZoneId.of("America/Vancouver")).plusDays(1));

        when(appointment.getAppointmentTime())
                .thenReturn("14:30");

        when(ownerService.findPublicSalon("10"))
                .thenReturn(salon);

        when(salon.getName())
                .thenReturn("Magic Cuts");

        mockMvc.perform(get("/customer/appointments")
                .with(user("customer@test.com").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/my-appointments"))
                .andExpect(model().attributeExists("upcomingAppointments"))
                .andExpect(model().attributeExists("pastAppointments"))
                .andExpect(model().attributeExists("salonNames"));
    }

    //Test that a customer can cancel an appointment successfully
    @Test
    void shouldCancelCustomerAppointment() throws Exception {
        mockMvc.perform(post("/customer/appointments/cancel")
                .param("id", "1")
                .with(user("customer@test.com").roles("CUSTOMER"))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customer/appointments"))
                .andExpect(flash().attribute(
                        "message", "Your appointment has been cancelled"));

        verify(bookingService).cancelAppointment(1, "customer@test.com");
    }

    //Test that a customer cannot rebook when the original salon is unavailable
    @Test
    void shouldRejectRebookingWhenSalonIsUnavailable() throws Exception {
        Account customer = mock(Account.class);
        Appointment appointment = mock(Appointment.class);

        when(accountRepository.findByEmail("customer@test.com"))
                .thenReturn(Optional.of(customer));

        when(appointmentRepository.findByIdAndCustomer(1, customer))
                .thenReturn(Optional.of(appointment));

        when(appointment.getAppointmentDate())
                .thenReturn(LocalDate.now(ZoneId.of("America/Vancouver")).minusDays(1));

        when(appointment.getAppointmentTime()).thenReturn("14:30");
        when(appointment.getSalonId()).thenReturn("10");

        //the original salon is no longer available
        when(ownerService.findPublicSalon("10")).thenReturn(null);

        mockMvc.perform(get("/customer/appointments/rebook")
                .param("id", "1")
                .with(user("customer@test.com").roles("CUSTOMER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customer/appointments"))
                .andExpect(flash().attribute(
                        "errorMessage",
                        "This salon is no longer available for booking."
                ));
    }

    //Test that customer appointments are separated and sorted correctly
    @Test
    void shouldSeparateAndSortUpcomingAndPastAppointments() throws Exception {
        Account customer = mock(Account.class);
        Salon salon = mock(Salon.class);

        LocalDate today = LocalDate.now(ZoneId.of("America/Vancouver"));

        Appointment upcomingSooner = mock(Appointment.class);
        Appointment upcomingLater = mock(Appointment.class);
        Appointment pastRecent = mock(Appointment.class);
        Appointment pastOlder = mock(Appointment.class);

        when(accountRepository.findByEmail("customer@test.com"))
                .thenReturn(Optional.of(customer));

        when(appointmentRepository.findByCustomer(customer))
                .thenReturn(List.of(pastOlder, upcomingLater, pastRecent, upcomingSooner));

        //set appointment dates and times
        when(upcomingSooner.getAppointmentDate()).thenReturn(today.plusDays(1));
        when(upcomingSooner.getAppointmentTime()).thenReturn("14:30");

        when(upcomingLater.getAppointmentDate()).thenReturn(today.plusDays(3));
        when(upcomingLater.getAppointmentTime()).thenReturn("10:00");

        when(pastRecent.getAppointmentDate()).thenReturn(today.minusDays(1));
        when(pastRecent.getAppointmentTime()).thenReturn("14:30");

        when(pastOlder.getAppointmentDate()).thenReturn(today.minusDays(3));
        when(pastOlder.getAppointmentTime()).thenReturn("10:00");

        //sse the same salon for all appointments
        for (Appointment appointment : List.of(
                upcomingSooner, upcomingLater, pastRecent, pastOlder)) {
                when(appointment.getSalonId()).thenReturn("10");
        }

        when(ownerService.findPublicSalon("10")).thenReturn(salon);
        when(salon.getName()).thenReturn("Magic Cuts");

        var result = mockMvc.perform(get("/customer/appointments")
                .with(user("customer@test.com").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/my-appointments"))
                .andExpect(model().attributeExists("upcomingAppointments"))
                .andExpect(model().attributeExists("pastAppointments"))
                .andReturn();

        //check that upcoming appointments are sorted earliest first
        List<?> upcoming = (List<?>) result.getModelAndView().getModel().get("upcomingAppointments");

        assertEquals(List.of(upcomingSooner, upcomingLater),upcoming);

        //check that past appointments are sorted most recent first
        List<?> past = (List<?>) result.getModelAndView().getModel().get("pastAppointments");

        assertEquals( List.of(pastRecent, pastOlder), past);
    }

    //Test that a customer can rebook a past appointment
    @Test
    void shouldRebookPastAppointmentSuccessfully() throws Exception {
        Account customer = mock(Account.class);
        Appointment appointment = mock(Appointment.class);
        Salon salon = mock(Salon.class);
        SalonService service = mock(SalonService.class);

        when(accountRepository.findByEmail("customer@test.com"))
                .thenReturn(Optional.of(customer));

        when(appointmentRepository.findByIdAndCustomer(1, customer))
                .thenReturn(Optional.of(appointment));

        //Set the original appointment details
        when(appointment.getAppointmentDate())
                .thenReturn(LocalDate.now(ZoneId.of("America/Vancouver")).minusDays(1));

        when(appointment.getAppointmentTime()).thenReturn("14:30");
        when(appointment.getSalonId()).thenReturn("10");
        when(appointment.getServiceId()).thenReturn("service-1");

        //Set the original salon and service
        when(ownerService.findPublicSalon("10")).thenReturn(salon);
        when(salon.getSlug()).thenReturn("magic-cuts");
        when(salon.getServices()).thenReturn(List.of(service));
        when(service.getId()).thenReturn("service-1");

        mockMvc.perform(get("/customer/appointments/rebook")
                .param("id", "1")
                .with(user("customer@test.com").roles("CUSTOMER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/customer/booking?service=service-1&salon=magic-cuts"
                ));
    }

    //Test that customer cannot rebook upcoming appointments
    @Test
    void shouldRejectRebookingAnUpcomingAppointment() throws Exception {
        Account customer = mock(Account.class);
        Appointment appointment = mock(Appointment.class);

        when(accountRepository.findByEmail("customer@test.com"))
                .thenReturn(Optional.of(customer));

        when(appointmentRepository.findByIdAndCustomer(1, customer))
                .thenReturn(Optional.of(appointment));

        when(appointment.getAppointmentDate())
                .thenReturn(LocalDate.now(ZoneId.of("America/Vancouver")).plusDays(1));

        when(appointment.getAppointmentTime()).thenReturn("14:30");

        mockMvc.perform(get("/customer/appointments/rebook")
                .param("id", "1")
                .with(user("customer@test.com").roles("CUSTOMER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customer/appointments"))
                .andExpect(flash().attribute(
                        "errorMessage",
                        "Only past appointments can be rebooked."
                ));
    }

    //Test that customers cannot rebook appointments belonging to another customer
    @Test
    void shouldRejectRebookingAnotherCustomersAppointment() throws Exception {
        Account customer = mock(Account.class);

        when(accountRepository.findByEmail("customer@test.com"))
                .thenReturn(Optional.of(customer));

        when(appointmentRepository.findByIdAndCustomer(1, customer))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/customer/appointments/rebook")
                .param("id", "1")
                .with(user("customer@test.com").roles("CUSTOMER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customer/appointments"))
                .andExpect(flash().attribute(
                        "errorMessage",
                        "Appointment not found."
                ));
    }
    //Test that customer cannot rebook an appointment that no longer exists
    @Test
    void shouldRejectRebookingANonexistentAppointment() throws Exception {
        Account customer = mock(Account.class);

        when(accountRepository.findByEmail("customer@test.com"))
                .thenReturn(Optional.of(customer));

        when(appointmentRepository.findByIdAndCustomer(999, customer))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/customer/appointments/rebook")
                .param("id", "999")
                .with(user("customer@test.com").roles("CUSTOMER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customer/appointments"))
                .andExpect(flash().attribute(
                        "errorMessage",
                        "Appointment not found."
                ));
    }

    //Test that a customer cannot rebook an appointment when the original service is unavailable
    @Test
    void shouldRejectRebookingAServiceThatIsNoLongerOffered() throws Exception {

        Account customer = mock(Account.class);
        Appointment appointment = mock(Appointment.class);
        Salon salon = mock(Salon.class);

        when(accountRepository.findByEmail("customer@test.com"))
                .thenReturn(Optional.of(customer));

        when(appointmentRepository.findByIdAndCustomer(1, customer))
                .thenReturn(Optional.of(appointment));

        when(appointment.getAppointmentDate())
                .thenReturn(LocalDate.now(ZoneId.of("America/Vancouver")).minusDays(1));

        when(appointment.getAppointmentTime()).thenReturn("14:30");
        when(appointment.getSalonId()).thenReturn("10");
        when(appointment.getServiceId()).thenReturn("deleted-service");

        when(ownerService.findPublicSalon("10")).thenReturn(salon);

        // The salon no longer offers the original service
        when(salon.getServices()).thenReturn(List.of());

        mockMvc.perform(get("/customer/appointments/rebook")
                .param("id", "1")
                .with(user("customer@test.com").roles("CUSTOMER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customer/appointments"))
                .andExpect(flash().attribute(
                        "errorMessage",
                        "This service is no longer offered by the salon."
                ));
    }

    //Test that rebooking does not modify the original appointment
    @Test
    void shouldNotModifyOriginalAppointmentWhenRebooking() throws Exception {

        Account customer = mock(Account.class);
        Appointment appointment = mock(Appointment.class);
        Salon salon = mock(Salon.class);
        SalonService service = mock(SalonService.class);

        when(accountRepository.findByEmail("customer@test.com"))
                .thenReturn(Optional.of(customer));

        when(appointmentRepository.findByIdAndCustomer(1, customer))
                .thenReturn(Optional.of(appointment));

        when(appointment.getAppointmentDate())
                .thenReturn(LocalDate.now(ZoneId.of("America/Vancouver")).minusDays(1));

        when(appointment.getAppointmentTime()).thenReturn("14:30");
        when(appointment.getSalonId()).thenReturn("10");
        when(appointment.getServiceId()).thenReturn("service-1");

        when(ownerService.findPublicSalon("10")).thenReturn(salon);
        when(salon.getSlug()).thenReturn("magic-cuts");
        when(salon.getServices()).thenReturn(List.of(service));
        when(service.getId()).thenReturn("service-1");

        mockMvc.perform(get("/customer/appointments/rebook")
                .param("id", "1")
                .with(user("customer@test.com").roles("CUSTOMER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/customer/booking?service=service-1&salon=magic-cuts"
                ));

        // Rebooking should not save or replace the original appointment
        verify(appointmentRepository, never()).save(any(Appointment.class));

        // Rebooking should not directly create or cancel an appointment
        verifyNoInteractions(bookingService);
    }

}

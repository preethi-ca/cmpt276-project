package com.cmpt276.project;

import com.cmpt276.project.model.Account;
import com.cmpt276.project.model.Appointment;
import com.cmpt276.project.model.RoleType;
import com.cmpt276.project.owner.model.Salon;
import com.cmpt276.project.owner.service.OwnerService;
import com.cmpt276.project.repository.AccountRepository;
import com.cmpt276.project.repository.AppointmentRepository;
import com.cmpt276.project.service.BookingService;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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

    @Test
    void ownerEntryAndSetupShareOneSalonLookupPerRequest() throws Exception {
        var owner = mock(Account.class);
        when(owner.getId()).thenReturn(42);
        when(owner.getRoleType()).thenReturn(RoleType.SALONOWNER);
        when(accountRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
        var signedIn = user("owner@test.com").roles("SALONOWNER");

        mockMvc.perform(get("/owner").with(signedIn)).andExpect(redirectedUrl("/owner/setup"));
        mockMvc.perform(get("/owner/setup").with(signedIn)).andExpect(status().isOk())
                .andExpect(model().attributeExists("salonForm"));

        when(ownerService.findForOwner("42")).thenReturn(new Salon());
        mockMvc.perform(get("/owner").with(signedIn)).andExpect(redirectedUrl("/owner/dashboard"));
        mockMvc.perform(get("/owner/setup").with(signedIn)).andExpect(redirectedUrl("/owner/dashboard"));
        verify(ownerService, times(4)).findForOwner("42");
    }

    @Test
    void ownerAppointmentsShowTheSignedInSalonsBookingsAndCounts() throws Exception {
        Account owner = mock(Account.class);
        Salon salon = mock(Salon.class);
        when(owner.getId()).thenReturn(42);
        when(owner.getRoleType()).thenReturn(RoleType.SALONOWNER);
        when(accountRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
        when(ownerService.findForOwner("42")).thenReturn(salon);
        when(salon.getId()).thenReturn("owned-salon");
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
                .andExpect(model().attribute("todayAppointmentCount", 2L))
                .andExpect(model().attribute("upcomingAppointmentCount", 11L))
                .andExpect(model().attribute("weekAppointmentCount", 4L))
                .andExpect(content().string(containsString("Customer &lt;script&gt;")))
                .andExpect(content().string(containsString("Original &lt;Cut&gt;")))
                .andExpect(content().string(not(containsString("private-hash"))))
                .andExpect(content().string(containsString("/owner/appointments?page=2")));
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

        when(ownerService.findPublicSalon("10"))
                .thenReturn(salon);

        when(salon.getName())
                .thenReturn("Magic Cuts");

        mockMvc.perform(get("/customer/appointments")
                .with(user("customer@test.com").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/my-appointments"))
                .andExpect(model().attributeExists("appointments"))
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
}

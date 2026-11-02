package com.cmpt276.project;

import com.cmpt276.project.model.Account;
import com.cmpt276.project.model.Appointment;
import com.cmpt276.project.owner.model.Salon;
import com.cmpt276.project.owner.service.OwnerService;
import com.cmpt276.project.repository.AccountRepository;
import com.cmpt276.project.repository.AppointmentRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
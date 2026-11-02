package com.cmpt276.project;

import com.cmpt276.project.owner.model.OpeningDay;
import com.cmpt276.project.owner.model.Salon;
import com.cmpt276.project.owner.model.SalonService;
import com.cmpt276.project.owner.service.OwnerService;
import com.cmpt276.project.service.BookingService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
class BookingControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private OwnerService ownerService;

    @Test
    void shouldShowBookingDatePage() throws Exception {

        Salon salon = mock(Salon.class);
        SalonService service = mock(SalonService.class);

        when(ownerService.findPublicSalonBySlug("magiccuts-salon"))
                .thenReturn(salon);

        when(salon.getServices())
                .thenReturn(List.of(service));

        when(service.getId())
                .thenReturn("service1");

        when(service.getName())
                .thenReturn("Haircut");

        when(salon.getOpeningHours())
                .thenReturn(List.of());

        mockMvc.perform(get("/customer/booking")
                .with(user("customer@test.com").roles("CUSTOMER"))
                .param("service", "service1")
                .param("salon", "magiccuts-salon"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/select-date"))
                .andExpect(model().attribute("service", "service1"))
                .andExpect(model().attribute("serviceName", "Haircut"))
                .andExpect(model().attribute("salon", "magiccuts-salon"))
                .andExpect(model().attributeExists("openingHours"))
                .andExpect(model().attributeExists("days"));
    }

    @Test
    void shouldReturnToDatePageWhenSalonIsClosed() throws Exception {

        Salon salon = mock(Salon.class);
        SalonService service = mock(SalonService.class);
        OpeningDay monday = mock(OpeningDay.class);

        when(ownerService.findPublicSalonBySlug("magiccuts-salon"))
                .thenReturn(salon);

        when(salon.getServices())
                .thenReturn(List.of(service));

        when(service.getId())
                .thenReturn("service1");

        when(service.getName())
                .thenReturn("Haircut");

        when(monday.getDayOfWeek())
                .thenReturn(1);

        when(monday.isOpen())
                .thenReturn(false);

        when(salon.getOpeningHours())
                .thenReturn(List.of(monday));

        mockMvc.perform(get("/customer/booking/time")
                .with(user("customer@test.com").roles("CUSTOMER"))
                .param("service", "service1")
                .param("salon", "magiccuts-salon")
                .param("appointmentDate", "2026-11-02"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/select-date"))
                .andExpect(model().attribute(
                        "error",
                        "Sorry, salon is closed on the selected date"))
                .andExpect(model().attribute("serviceName", "Haircut"))
                .andExpect(model().attributeExists("openingHours"))
                .andExpect(model().attributeExists("days"));
    }

    @Test
    void shouldShowAvailableTimesWhenSalonIsOpen() throws Exception {

        Salon salon = mock(Salon.class);
        SalonService service = mock(SalonService.class);
        OpeningDay monday = mock(OpeningDay.class);

        when(ownerService.findPublicSalonBySlug("magiccuts-salon"))
                .thenReturn(salon);

        when(salon.getServices())
                .thenReturn(List.of(service));

        when(service.getId())
                .thenReturn("service1");

        when(service.getName())
                .thenReturn("Haircut");

        when(monday.getDayOfWeek())
                .thenReturn(1);

        when(monday.isOpen())
                .thenReturn(true);

        when(salon.getOpeningHours())
                .thenReturn(List.of(monday));

        when(bookingService.getAvailableTimes(
                "magiccuts-salon",
                LocalDate.parse("2026-11-02")))
                .thenReturn(List.of("09:00", "10:00"));

        mockMvc.perform(get("/customer/booking/time")
                .with(user("customer@test.com").roles("CUSTOMER"))
                .param("service", "service1")
                .param("salon", "magiccuts-salon")
                .param("appointmentDate", "2026-11-02"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/select-time"))
                .andExpect(model().attribute("serviceName", "Haircut"))
                .andExpect(model().attribute("appointmentDate", "2026-11-02"))
                .andExpect(model().attribute(
                        "availableTimes",
                        List.of("09:00", "10:00")));
    }

    @Test
    void shouldShowBookingSummary() throws Exception {

        Salon salon = mock(Salon.class);
        SalonService service = mock(SalonService.class);

        when(ownerService.findPublicSalonBySlug("magiccuts-salon"))
                .thenReturn(salon);

        when(salon.getServices())
                .thenReturn(List.of(service));

        when(salon.getName())
                .thenReturn("Magic Cuts");

        when(service.getId())
                .thenReturn("service1");

        when(service.getName())
                .thenReturn("Haircut");

        mockMvc.perform(get("/customer/booking/confirmation")
                .with(user("customer@test.com").roles("CUSTOMER"))
                .param("service", "service1")
                .param("salon", "magiccuts-salon")
                .param("appointmentDate", "2026-11-02")
                .param("appointmentTime", "10:00"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/booking-summary"))
                .andExpect(model().attribute("salonName", "Magic Cuts"))
                .andExpect(model().attribute("serviceName", "Haircut"))
                .andExpect(model().attribute("appointmentDate", "2026-11-02"))
                .andExpect(model().attribute("appointmentTime", "10:00"))
                .andExpect(model().attribute("bookedService", service));
    }

    @Test
    void shouldConfirmBookingSuccessfully() throws Exception {

        Salon salon = mock(Salon.class);
        SalonService service = mock(SalonService.class);

        when(ownerService.findPublicSalonBySlug("magiccuts-salon"))
                .thenReturn(salon);

        when(salon.getServices())
                .thenReturn(List.of(service));

        when(salon.getName())
                .thenReturn("Magic Cuts");

        when(service.getId())
                .thenReturn("service1");

        when(service.getName())
                .thenReturn("Haircut");

        mockMvc.perform(post("/customer/booking/confirm")
                .with(user("customer@test.com").roles("CUSTOMER"))
                .with(csrf())
                .param("service", "service1")
                .param("salon", "magiccuts-salon")
                .param("appointmentDate", "2026-11-02")
                .param("appointmentTime", "10:00"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/booking-confirmation"))
                .andExpect(model().attribute("salonName", "Magic Cuts"))
                .andExpect(model().attribute("serviceName", "Haircut"))
                .andExpect(model().attribute("appointmentDate", "2026-11-02"))
                .andExpect(model().attribute("appointmentTime", "10:00"))
                .andExpect(model().attribute("bookedService", service));
    }

    @Test
    void shouldReturnToTimePageWhenBookingFails() throws Exception {

        Salon salon = mock(Salon.class);
        SalonService service = mock(SalonService.class);

        when(ownerService.findPublicSalonBySlug("magiccuts-salon"))
                .thenReturn(salon);

        when(salon.getServices())
                .thenReturn(List.of(service));

        when(service.getId())
                .thenReturn("service1");

        when(service.getName())
                .thenReturn("Haircut");

        when(bookingService.getAvailableTimes(
                "magiccuts-salon",
                LocalDate.parse("2026-11-02")))
                .thenReturn(List.of("09:00", "10:00"));

        doThrow(new IllegalArgumentException("Appointment time is no longer available"))
                .when(bookingService)
                .bookAppointment(
                        "service1",
                        "magiccuts-salon",
                        "2026-11-02",
                        "10:00",
                        "customer@test.com");

        mockMvc.perform(post("/customer/booking/confirm")
                .with(user("customer@test.com").roles("CUSTOMER"))
                .with(csrf())
                .param("service", "service1")
                .param("salon", "magiccuts-salon")
                .param("appointmentDate", "2026-11-02")
                .param("appointmentTime", "10:00"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/select-time"))
                .andExpect(model().attribute(
                        "error",
                        "Appointment time is no longer available"))
                .andExpect(model().attribute("serviceName", "Haircut"))
                .andExpect(model().attribute(
                        "availableTimes",
                        List.of("09:00", "10:00")));
    }
}
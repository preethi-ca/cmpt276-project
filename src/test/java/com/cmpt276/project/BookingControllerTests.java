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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
}
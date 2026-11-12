package com.cmpt276.project;

import com.cmpt276.project.owner.controller.PublicSalon;
import com.cmpt276.project.owner.model.Salon;
import com.cmpt276.project.owner.service.OwnerService;
import java.util.List;
import tools.jackson.databind.json.JsonMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PublicSalonControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OwnerService ownerService;

    @Test
    void customerDashboardAcceptsAnEmptySalonList() throws Exception {
        when(ownerService.findPublicSalons()).thenReturn(List.of());

        mockMvc.perform(get("/customer/dashboard").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/customer-dashboard"))
                .andExpect(model().attribute("salons", List.of()));
    }

    @Test
    void customerDashboardExposesPublicSalonDetailsWithoutOwnerIdentity() throws Exception {
        Salon salon = new Salon();
        salon.setId("salon-id");
        salon.setOwnerId("private-owner-id");
        salon.setName("Sally Salon");
        salon.setSlug("sally-salon");
        salon.setAddress("123 Main Street");
        when(ownerService.findPublicSalons()).thenReturn(List.of(salon));

        var result = mockMvc.perform(get("/customer/dashboard").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/customer-dashboard"))
                .andReturn();
        var salons = (List<?>) result.getModelAndView().getModel().get("salons");
        assertEquals(1, salons.size());
        PublicSalon item = assertInstanceOf(PublicSalon.class, salons.get(0));
        assertEquals("salon-id", item.getId());
        assertEquals("Sally Salon", item.getName());
        assertEquals("sally-salon", item.getSlug());
        assertEquals("123 Main Street", item.getAddress());
        // Guard the customer-facing projection against accidentally exposing owner account data.
        assertFalse(JsonMapper.builder().build().valueToTree(item).has("ownerId"));
    }

    @Test
    void salonListingPreservesDashboardAccessAndRoleRedirects() throws Exception {
        mockMvc.perform(get("/customer/dashboard"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mockMvc.perform(get("/customer/dashboard").with(user("owner").roles("SALONOWNER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/dashboard").with(user("owner").roles("SALONOWNER")))
                .andExpect(redirectedUrl("/owner"));
        mockMvc.perform(get("/dashboard").with(user("customer").roles("CUSTOMER")))
                .andExpect(redirectedUrl("/customer/dashboard"));
    }

    @Test
    void shouldShowPublicSalonPage() throws Exception {

        Salon salon = mock(Salon.class);

        when(ownerService.findPublicSalonBySlug("magiccuts-salon"))
                .thenReturn(salon);

        mockMvc.perform(get("/salons/magiccuts-salon"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/public-salon"))
                .andExpect(model().attributeExists("salon"))
                .andExpect(model().attributeExists("days"));
    }
}

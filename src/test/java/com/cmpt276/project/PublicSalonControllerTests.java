package com.cmpt276.project;

import com.cmpt276.project.model.Account;
import com.cmpt276.project.owner.model.Salon;
import com.cmpt276.project.owner.service.OwnerService;
import com.cmpt276.project.service.AccountService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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

    @MockitoBean
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        Account customer = mock(Account.class);
        when(customer.getName()).thenReturn("Test Customer");
        when(accountService.findByEmail("customer")).thenReturn(customer);
    }

    @Test
    void customerDashboardDoesNotOfferSalonSelection() throws Exception {
        mockMvc.perform(get("/customer/dashboard").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("customerName", "Test Customer"))
                .andExpect(model().attributeDoesNotExist("salons", "salonLabels"))
                .andExpect(content().string(containsString("<details class=\"card-body\">")))
                .andExpect(content().string(not(containsString("id=\"salon\""))))
                .andExpect(content().string(not(containsString("/customer/salons/visit"))));
        verify(ownerService, never()).findPublicSalons();
    }

    @Test
    void retiredSalonSelectionUrlReturnsNotFound() throws Exception {
        mockMvc.perform(get("/customer/salons/visit").param("slug", "sally")
                        .with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void publicSalonApiExcludesOwnerIdentity() throws Exception {
        Salon salon = new Salon();
        salon.setName("Sally Salon");
        salon.setOwnerId("private-owner-id");
        when(ownerService.findPublicSalon("salon-id")).thenReturn(salon);

        mockMvc.perform(get("/api/salons/salon-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sally Salon"))
                .andExpect(jsonPath("$.ownerId").doesNotExist());
    }

    @Test
    void dashboardEnforcesCustomerAccessAndRoleRedirects() throws Exception {
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

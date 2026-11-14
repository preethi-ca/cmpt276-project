package com.cmpt276.project;

import com.cmpt276.project.controller.DashboardController;
import com.cmpt276.project.owner.controller.PublicSalon;
import com.cmpt276.project.owner.model.Salon;
import com.cmpt276.project.owner.service.OwnerService;
import java.util.List;
import java.util.regex.Pattern;
import tools.jackson.databind.json.JsonMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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

        var result = mockMvc.perform(get("/customer/dashboard").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/customer-dashboard"))
                .andExpect(model().attribute("salons", List.of()))
                .andExpect(content().string(containsString("No salons are available yet.")))
                .andReturn();
        String html = result.getResponse().getContentAsString();
        assertTrue(Pattern.compile("<select[^>]*disabled", Pattern.DOTALL).matcher(html).find());
        assertTrue(Pattern.compile("<button[^>]*disabled[^>]*>\\s*Visit Salon", Pattern.DOTALL)
                .matcher(html).find());
    }

    @Test
    void salonDropdownRendersDistinctChoicesAndEscapesPublicText() throws Exception {
        when(ownerService.findPublicSalons()).thenReturn(List.of(
                selectionSalon("same", "Same <Salon>", "1 Main Street"),
                selectionSalon("same-2", "Same <Salon>", "2 Main Street")));

        mockMvc.perform(get("/customer/dashboard").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/customer/salons/visit\"")))
                .andExpect(content().string(containsString("name=\"slug\"")))
                .andExpect(content().string(containsString("value=\"same\"")))
                .andExpect(content().string(containsString("value=\"same-2\"")))
                .andExpect(content().string(containsString("value=\"same\">Same &lt;Salon&gt; — 1 Main Street (Burnaby)</option>")))
                .andExpect(content().string(containsString("value=\"same-2\">Same &lt;Salon&gt; — 2 Main Street (Burnaby)</option>")))
                .andExpect(content().string(containsString("aria-label=\"Choose a salon\"")))
                .andExpect(content().string(not(containsString("<label for=\"salon\""))))
                .andExpect(content().string(not(containsString("Same <Salon>"))));
    }

    @Test
    void sameNameSalonsWithoutLocationsHaveDistinctLabelsAndKeepTheirDestinations() throws Exception {
        Salon first = selectionSalon("sally-salon", "Sally Salon", "");
        first.setCity("");
        Salon second = selectionSalon("sally-salon-2", "Sally Salon", null);
        second.setCity(null);
        when(ownerService.findPublicSalons()).thenReturn(List.of(first, second));

        for (String path : List.of("/customer/dashboard", "/customer/salons/visit")) {
            mockMvc.perform(get(path).with(user("customer").roles("CUSTOMER")))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(
                            "value=\"sally-salon\">Sally Salon [sally-salon]</option>")))
                    .andExpect(content().string(containsString(
                            "value=\"sally-salon-2\">Sally Salon [sally-salon-2]</option>")));
        }
        for (Salon salon : List.of(first, second)) {
            when(ownerService.findPublicSalonBySlug(salon.getSlug())).thenReturn(salon);
            mockMvc.perform(get("/customer/salons/visit").param("slug", salon.getSlug())
                            .with(user("customer").roles("CUSTOMER")))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrl("/salons/" + salon.getSlug()));
        }
    }

    @Test
    void sameNameSalonsAtTheSameLocationHaveDistinctEscapedLabels() throws Exception {
        when(ownerService.findPublicSalons()).thenReturn(List.of(
                selectionSalon("same", "Same <Salon>", "1 Main Street"),
                selectionSalon("same-2", "Same <Salon>", "1 Main Street"),
                selectionSalon("other", "Other Salon", "1 Main Street")));

        mockMvc.perform(get("/customer/dashboard").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "value=\"same\">Same &lt;Salon&gt; — 1 Main Street (Burnaby) [same]</option>")))
                .andExpect(content().string(containsString(
                        "value=\"same-2\">Same &lt;Salon&gt; — 1 Main Street (Burnaby) [same-2]</option>")))
                .andExpect(content().string(containsString(
                        "value=\"other\">Other Salon — 1 Main Street (Burnaby)</option>")))
                .andExpect(content().string(not(containsString("Same <Salon>"))));
    }

    @Test
    void missingOrBlankSelectionShowsErrorAndReloadsChoices() throws Exception {
        when(ownerService.findPublicSalons()).thenReturn(List.of(selectionSalon("sally", "Sally", "")));
        for (String slug : List.of("", " ")) {
            mockMvc.perform(get("/customer/salons/visit").param("slug", slug)
                            .with(user("customer").roles("CUSTOMER")))
                    .andExpect(status().isOk())
                    .andExpect(view().name("customer/customer-dashboard"))
                    .andExpect(model().attribute("salonError", "Choose a salon to visit."))
                    .andExpect(content().string(containsString("aria-invalid=\"true\"")))
                    .andExpect(content().string(containsString("aria-describedby=\"salon-error\"")))
                    .andExpect(content().string(containsString("id=\"salon-error\"")))
                    .andExpect(content().string(containsString("value=\"sally\"")));
        }
        mockMvc.perform(get("/customer/salons/visit").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("salonError", "Choose a salon to visit."));
        verify(ownerService, never()).findPublicSalonBySlug(anyString());
    }

    @Test
    void validSelectionRedirectsUsingTheSavedSalonSlug() throws Exception {
        when(ownerService.findPublicSalonBySlug("selected"))
                .thenReturn(selectionSalon("saved-slug", "Selected Salon", ""));
        mockMvc.perform(get("/customer/salons/visit").param("slug", "selected")
                        .with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/salons/saved-slug"));
        verify(ownerService, never()).findPublicSalons();
    }

    @Test
    void unavailableOrUrlShapedSelectionsCannotRedirectOffSite() throws Exception {
        when(ownerService.findPublicSalons()).thenReturn(List.of(selectionSalon("remaining", "Remaining", "")));
        // Treat submitted values as salon identifiers, never as user-controlled redirect URLs.
        for (String slug : List.of("gone", "https://example.invalid", "//example.invalid", "../owner")) {
            when(ownerService.findPublicSalonBySlug(slug))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
            mockMvc.perform(get("/customer/salons/visit").param("slug", slug)
                            .with(user("customer").roles("CUSTOMER")))
                    .andExpect(status().isOk())
                    .andExpect(view().name("customer/customer-dashboard"))
                    .andExpect(model().attribute("salonError", "This salon is no longer available. Choose another salon."))
                    .andExpect(header().doesNotExist("Location"))
                    .andExpect(content().string(containsString("value=\"remaining\"")));
        }
    }

    @Test
    void unexpectedLookupFailureIsNotHiddenAsAnUnavailableSalon() {
        var failure = new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        when(ownerService.findPublicSalonBySlug("broken")).thenThrow(failure);
        assertSame(failure, assertThrows(ResponseStatusException.class,
                () -> new DashboardController(ownerService).visitSalon("broken", new ExtendedModelMap())));
    }

    @Test
    void salonVisitRequiresCustomerRole() throws Exception {
        mockMvc.perform(get("/customer/salons/visit").param("slug", "sally"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mockMvc.perform(get("/customer/salons/visit").param("slug", "sally")
                        .with(user("owner").roles("SALONOWNER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(ownerService);
    }

    @Test
    void salonVisitRedirectPreservesTheApplicationContextPath() throws Exception {
        when(ownerService.findPublicSalonBySlug("sally")).thenReturn(selectionSalon("sally", "Sally", ""));
        mockMvc.perform(get("/bookeasy/customer/salons/visit").contextPath("/bookeasy")
                        .param("slug", "sally").with(user("customer").roles("CUSTOMER")))
                .andExpect(redirectedUrl("/bookeasy/salons/sally"));
    }

    private Salon selectionSalon(String slug, String name, String address) {
        Salon salon = new Salon();
        salon.setSlug(slug);
        salon.setName(name);
        salon.setAddress(address);
        salon.setCity("Burnaby");
        return salon;
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

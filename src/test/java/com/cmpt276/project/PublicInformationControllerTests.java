package com.cmpt276.project;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:public-information;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "bookeasy.email.enabled=false",
        "bookeasy.calendar.enabled=false"
})
@AutoConfigureMockMvc
class PublicInformationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldShowAboutPageWithoutLogin() throws Exception {
        mockMvc.perform(get("/about"))
                .andExpect(status().isOk())
                .andExpect(view().name("about"))
                .andExpect(content().string(containsString("Your next salon visit, simply booked.")));
    }

    @Test
    void shouldShowPrivacyPageWithoutLogin() throws Exception {
        mockMvc.perform(get("/privacy"))
                .andExpect(status().isOk())
                .andExpect(view().name("privacy"))
                .andExpect(content().string(containsString("Privacy policy")));
    }

    @Test
    void shouldShowTermsPageWithoutLogin() throws Exception {
        mockMvc.perform(get("/terms"))
                .andExpect(status().isOk())
                .andExpect(view().name("terms"))
                .andExpect(content().string(containsString("Terms of use")));
    }

    @Test
    void privacyPageShouldLinkGooglePolicyAndExplainDisconnect() throws Exception {
        mockMvc.perform(get("/privacy"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "https://developers.google.com/terms/api-services-user-data-policy")))
                .andExpect(content().string(containsString("Google API Services User Data Policy")))
                .andExpect(content().string(containsString("Disconnect Google Calendar on your dashboard")));
    }

    @Test
    void protectedPageShouldStillNeedLogin() throws Exception {
        // the public pages should not open the customer pages
        mockMvc.perform(get("/customer/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}

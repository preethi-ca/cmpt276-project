package com.cmpt276.project;

import com.cmpt276.project.model.Account;
import com.cmpt276.project.model.GoogleCalendarConnection;
import com.cmpt276.project.model.RoleType;
import com.cmpt276.project.repository.AccountRepository;
import com.cmpt276.project.repository.GoogleCalendarConnectionRepository;
import com.cmpt276.project.service.GoogleCalendarClient;
import com.cmpt276.project.service.GoogleCalendarSettings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:google-calendar-controller;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "bookeasy.email.enabled=false",
        "bookeasy.calendar.enabled=true",
        "bookeasy.calendar.client-id=fake-client-id",
        "bookeasy.calendar.client-secret=fake-client-secret",
        "bookeasy.calendar.redirect-uri=https://bookeasy.example.invalid/calendar/google/callback"
})
@AutoConfigureMockMvc
class GoogleCalendarControllerTests {
    private static final String STATE_KEY = "googleCalendarState";
    private static final String GOOGLE_URL = "https://accounts.google.com/o/oauth2/v2/auth?state=";
    private static final String BAD_LINK = "That Google Calendar link didn't work. Please try connecting again.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private GoogleCalendarConnectionRepository connectionRepository;

    @Autowired
    private SessionRepository<? extends Session> sessionRepository;

    @MockitoBean
    private GoogleCalendarClient calendarClient;

    // real settings from the properties above, but one test can switch them off
    @MockitoSpyBean
    private GoogleCalendarSettings calendarSettings;

    private Account alex;
    private Account sam;

    @BeforeEach
    void startWithTwoCustomers() {
        connectionRepository.deleteAll();
        accountRepository.deleteAll();
        alex = accountRepository.save(new Account("Alex Lee", "alex@example.invalid", "fake-password-hash", RoleType.CUSTOMER));
        sam = accountRepository.save(new Account("Sam Chen", "sam@example.invalid", "fake-password-hash", RoleType.CUSTOMER));
        when(calendarClient.buildAuthorizationUrl(anyString()))
                .thenAnswer(call -> GOOGLE_URL + call.getArgument(0));
    }

    @Test
    void connectRedirectsToGoogleAndSavesStateInSession() throws Exception {
        MvcResult result = connectAs("alex@example.invalid");

        String state = stateFrom(result);
        // 32 random bytes in URL safe base64 is 43 characters
        assertTrue(state.matches("[A-Za-z0-9_-]{43,}"));
        assertEquals(state, savedState(sessionCookie(result)));
        verify(calendarClient).buildAuthorizationUrl(state);
    }

    @Test
    void callbackWithWrongOrMissingStateSavesNothing() throws Exception {
        MvcResult result = connectAs("alex@example.invalid");
        Cookie cookie = sessionCookie(result);

        mockMvc.perform(get("/calendar/google/callback").cookie(cookie)
                .param("state", "other-state").param("code", "fake-code")
                .with(user("alex@example.invalid").roles("CUSTOMER")))
                .andExpect(redirectedUrl("/customer/dashboard"))
                .andExpect(flash().attribute("calendarMessage", BAD_LINK));
        // a wrong try also throws the saved state away
        assertNull(savedState(cookie));

        // no session with a saved state at all
        mockMvc.perform(get("/calendar/google/callback")
                .param("state", stateFrom(result)).param("code", "fake-code")
                .with(user("alex@example.invalid").roles("CUSTOMER")))
                .andExpect(redirectedUrl("/customer/dashboard"))
                .andExpect(flash().attribute("calendarMessage", BAD_LINK));

        verify(calendarClient, never()).exchangeCode(any());
        assertEquals(0, connectionRepository.count());
    }

    @Test
    void callbackWithAccessDeniedSavesNothing() throws Exception {
        MvcResult result = connectAs("alex@example.invalid");

        mockMvc.perform(get("/calendar/google/callback").cookie(sessionCookie(result))
                .param("state", stateFrom(result)).param("error", "access_denied")
                .with(user("alex@example.invalid").roles("CUSTOMER")))
                .andExpect(redirectedUrl("/customer/dashboard"))
                .andExpect(flash().attribute("calendarMessage", "Google Calendar was not connected."));

        verify(calendarClient, never()).exchangeCode(any());
        assertEquals(0, connectionRepository.count());
    }

    @Test
    void callbackWithGoodStateSavesConnectionForThatCustomerOnly() throws Exception {
        when(calendarClient.exchangeCode("fake-code")).thenReturn("fake-refresh-token");
        MvcResult result = connectAs("alex@example.invalid");
        Cookie cookie = sessionCookie(result);
        String state = stateFrom(result);

        mockMvc.perform(get("/calendar/google/callback").cookie(cookie)
                .param("state", state).param("code", "fake-code")
                .with(user("alex@example.invalid").roles("CUSTOMER")))
                .andExpect(redirectedUrl("/customer/dashboard"))
                .andExpect(flash().attribute("calendarMessage", "Google Calendar connected."));

        assertEquals("fake-refresh-token", connectionRepository.findByCustomer(alex).orElseThrow().getRefreshToken());
        assertTrue(connectionRepository.findByCustomer(sam).isEmpty());
        assertNull(savedState(cookie));

        // the same state can't be used a second time
        mockMvc.perform(get("/calendar/google/callback").cookie(cookie)
                .param("state", state).param("code", "fake-code")
                .with(user("alex@example.invalid").roles("CUSTOMER")))
                .andExpect(flash().attribute("calendarMessage", BAD_LINK));
        verify(calendarClient, times(1)).exchangeCode(any());
    }

    @Test
    void connectingAgainReplacesTheOldToken() throws Exception {
        connectionRepository.save(new GoogleCalendarConnection(alex, "old-refresh-token"));
        when(calendarClient.exchangeCode("fake-code")).thenReturn("new-refresh-token");
        MvcResult result = connectAs("alex@example.invalid");

        mockMvc.perform(get("/calendar/google/callback").cookie(sessionCookie(result))
                .param("state", stateFrom(result)).param("code", "fake-code")
                .with(user("alex@example.invalid").roles("CUSTOMER")))
                .andExpect(flash().attribute("calendarMessage", "Google Calendar connected."));

        assertEquals(1, connectionRepository.count());
        assertEquals("new-refresh-token", connectionRepository.findByCustomer(alex).orElseThrow().getRefreshToken());
    }

    @Test
    void callbackWhenGoogleGivesNoTokenSavesNothing() throws Exception {
        when(calendarClient.exchangeCode("fake-code")).thenReturn(null);
        MvcResult result = connectAs("alex@example.invalid");

        mockMvc.perform(get("/calendar/google/callback").cookie(sessionCookie(result))
                .param("state", stateFrom(result)).param("code", "fake-code")
                .with(user("alex@example.invalid").roles("CUSTOMER")))
                .andExpect(redirectedUrl("/customer/dashboard"))
                .andExpect(flash().attribute("calendarMessage", "Couldn't connect Google Calendar. Please try again."));

        assertEquals(0, connectionRepository.count());
    }

    @Test
    void disconnectDeletesConnectionAndRevokesToken() throws Exception {
        connectionRepository.save(new GoogleCalendarConnection(alex, "fake-refresh-token"));
        connectionRepository.save(new GoogleCalendarConnection(sam, "other-refresh-token"));

        // a form without the CSRF token is rejected
        mockMvc.perform(post("/calendar/google/disconnect")
                .with(user("alex@example.invalid").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        assertTrue(connectionRepository.findByCustomer(alex).isPresent());

        mockMvc.perform(post("/calendar/google/disconnect").with(csrf())
                .with(user("alex@example.invalid").roles("CUSTOMER")))
                .andExpect(redirectedUrl("/customer/dashboard"))
                .andExpect(flash().attribute("calendarMessage", "Google Calendar disconnected."));

        verify(calendarClient).revokeToken("fake-refresh-token");
        assertTrue(connectionRepository.findByCustomer(alex).isEmpty());
        assertTrue(connectionRepository.findByCustomer(sam).isPresent());
    }

    @Test
    void salonOwnerAndLoggedOutUserCannotUseCalendarRoutes() throws Exception {
        mockMvc.perform(get("/calendar/google/connect").with(user("owner@example.invalid").roles("SALONOWNER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/calendar/google/callback").param("state", "x").param("code", "y")
                .with(user("owner@example.invalid").roles("SALONOWNER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/calendar/google/disconnect").with(csrf())
                .with(user("owner@example.invalid").roles("SALONOWNER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/calendar/google/connect")).andExpect(redirectedUrl("/login"));
        mockMvc.perform(get("/calendar/google/callback").param("state", "x").param("code", "y"))
                .andExpect(redirectedUrl("/login"));

        verifyNoInteractions(calendarClient);
    }

    @Test
    void dashboardShowsConnectOrDisconnectWithoutShowingToken() throws Exception {
        mockMvc.perform(get("/customer/dashboard").with(user("alex@example.invalid").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Connect Google Calendar")))
                .andExpect(content().string(containsString("href=\"/calendar/google/connect\"")))
                .andExpect(content().string(not(containsString("Disconnect"))));

        connectionRepository.save(new GoogleCalendarConnection(alex, "fake-refresh-token"));

        mockMvc.perform(get("/customer/dashboard").with(user("alex@example.invalid").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Connected")))
                .andExpect(content().string(containsString("Disconnect")))
                .andExpect(content().string(containsString("action=\"/calendar/google/disconnect\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(not(containsString("Connect Google Calendar"))))
                .andExpect(content().string(not(containsString("fake-refresh-token"))));
    }

    @Test
    void connectDoesNotGoToGoogleWhenNotSetUp() throws Exception {
        doReturn(false).when(calendarSettings).isReady();

        mockMvc.perform(get("/calendar/google/connect")
                .with(user("alex@example.invalid").roles("CUSTOMER")))
                .andExpect(redirectedUrl("/customer/dashboard"))
                .andExpect(flash().attribute("calendarMessage", "Google Calendar is not set up right now."));

        verifyNoInteractions(calendarClient);

        mockMvc.perform(get("/customer/dashboard").with(user("alex@example.invalid").roles("CUSTOMER")))
                .andExpect(content().string(containsString("Google Calendar isn't available right now.")))
                .andExpect(content().string(not(containsString("Connect Google Calendar"))));
    }

    // starts connecting as this customer, the mock client puts the state on the end of the URL
    private MvcResult connectAs(String email) throws Exception {
        return mockMvc.perform(get("/calendar/google/connect").with(user(email).roles("CUSTOMER")))
                .andExpect(status().is3xxRedirection())
                .andReturn();
    }

    private String stateFrom(MvcResult result) {
        String location = result.getResponse().getRedirectedUrl();
        assertNotNull(location);
        assertTrue(location.startsWith(GOOGLE_URL));
        return location.substring(GOOGLE_URL.length());
    }

    // the app keeps sessions in the database, so tests send the session cookie back like a browser
    private Cookie sessionCookie(MvcResult result) {
        Cookie cookie = result.getResponse().getCookie("SESSION");
        assertNotNull(cookie);
        return cookie;
    }

    private Object savedState(Cookie cookie) {
        String sessionId = new String(Base64.getDecoder().decode(cookie.getValue()), StandardCharsets.UTF_8);
        Session session = sessionRepository.findById(sessionId);
        assertNotNull(session);
        return session.getAttribute(STATE_KEY);
    }
}

package com.cmpt276.project;

import com.cmpt276.project.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @Test
    void shouldShowLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void shouldShowRegistrationPage() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("registrationForm"));
    }

    @Test
    void shouldRegisterAccountSuccessfully() throws Exception {
        when(accountService.register(any(), any())).thenReturn(true);

        mockMvc.perform(post("/register").with(csrf())
                .param("name", "Test User")
                .param("email", "test@example.com")
                .param("password", "password123")
                .param("roleType", "CUSTOMER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));
    }

    @Test
    void shouldReturnToRegisterPageWhenRegistrationFails() throws Exception {
        when(accountService.register(any(), any())).thenReturn(false);

        mockMvc.perform(post("/register").with(csrf())
                .param("name", "Test User")
                .param("email", "test@example.com")
                .param("password", "password123")
                .param("roleType", "CUSTOMER"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }
}
package com.cmpt276.project.service;

import com.cmpt276.project.model.Account;
import com.cmpt276.project.model.RoleType;
import com.cmpt276.project.repository.AccountRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.time.Instant;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GoogleLoginServiceTests {

    private AccountRepository accountRepository;
    private PasswordEncoder passwordEncoder;
    private GoogleLoginService googleLoginService;

    private static final String EMAIL = "customer@test.com";
    private static final String SUBJECT = "google-user-123";

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);

        googleLoginService = new GoogleLoginService(
                accountRepository, passwordEncoder);
    }

    private OidcUser createGoogleUser(
            String email, boolean verified) {

        Map<String, Object> claims = new HashMap<>();

        claims.put("sub", SUBJECT);
        claims.put("name", "Test Customer");
        claims.put("email_verified", verified);

        if (email != null) {
            claims.put("email", email);
        }

        OidcIdToken token = new OidcIdToken(
                "test-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                claims
        );

        return new DefaultOidcUser(List.of(), token);
    }

    private Account createExistingAccount(RoleType role) {
        Account account = new Account(
                "Test Customer",
                EMAIL,
                "hashed-password",
                role
        );

        account.setGoogleSubject(SUBJECT);

        return account;
    }

    @Test
    void shouldCreateNewGoogleCustomer() {
        when(accountRepository.findByGoogleSubject(SUBJECT))
                .thenReturn(Optional.empty());

        when(accountRepository.findByEmail(EMAIL))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode(anyString()))
                .thenReturn("hashed-random-password");

        when(accountRepository.saveAndFlush(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OidcUser result = googleLoginService.processGoogleUser(
                createGoogleUser(EMAIL, true));

        assertEquals(EMAIL, result.getName());

        verify(accountRepository).saveAndFlush(argThat(account ->
                EMAIL.equals(account.getEmail())
                && SUBJECT.equals(account.getGoogleSubject())
                && account.getRoleType() == RoleType.CUSTOMER
                && "hashed-random-password".equals(
                        account.getPasswordHash())
        ));

        assertTrue(result.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_CUSTOMER")));
    }

    @Test
    void shouldReuseExistingGoogleAccount() {
        Account account = createExistingAccount(RoleType.CUSTOMER);

        when(accountRepository.findByGoogleSubject(SUBJECT))
                .thenReturn(Optional.of(account));

        OidcUser result = googleLoginService.processGoogleUser(
                createGoogleUser(EMAIL, true));

        assertEquals(EMAIL, result.getName());

        verify(accountRepository, never())
                .saveAndFlush(any(Account.class));
    }

    @Test
    void shouldRejectUnverifiedEmail() {
        OAuth2AuthenticationException exception = assertThrows(
                OAuth2AuthenticationException.class,
                () -> googleLoginService.processGoogleUser(
                        createGoogleUser(EMAIL, false)));

        assertEquals("unverified_email",
                exception.getError().getErrorCode());

        verifyNoInteractions(accountRepository);
    }

    @Test
    void shouldRejectMissingEmail() {
        OAuth2AuthenticationException exception = assertThrows(
                OAuth2AuthenticationException.class,
                () -> googleLoginService.processGoogleUser(
                        createGoogleUser(null, true)));

        assertEquals("unverified_email",
                exception.getError().getErrorCode());

        verifyNoInteractions(accountRepository);
    }

    @Test
    void shouldRejectAutomaticAccountLinking() {
        Account existingAccount = new Account(
                "Existing Customer",
                EMAIL,
                "existing-password-hash",
                RoleType.CUSTOMER
        );

        when(accountRepository.findByGoogleSubject(SUBJECT))
                .thenReturn(Optional.empty());

        when(accountRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(existingAccount));

        OAuth2AuthenticationException exception = assertThrows(
                OAuth2AuthenticationException.class,
                () -> googleLoginService.processGoogleUser(
                        createGoogleUser(EMAIL, true)));

        assertEquals("account_link_required",
                exception.getError().getErrorCode());

        verify(accountRepository, never())
                .saveAndFlush(any(Account.class));
    }

    @Test
    void shouldRejectChangedGoogleEmail() {
        Account account = createExistingAccount(RoleType.CUSTOMER);

        when(accountRepository.findByGoogleSubject(SUBJECT))
                .thenReturn(Optional.of(account));

        OAuth2AuthenticationException exception = assertThrows(
                OAuth2AuthenticationException.class,
                () -> googleLoginService.processGoogleUser(
                        createGoogleUser("changed@test.com", true)));

        assertEquals("email_changed",
                exception.getError().getErrorCode());
    }

    @Test
    void shouldPreserveExistingAccountRole() {
        Account account = createExistingAccount(RoleType.SALONOWNER);

        when(accountRepository.findByGoogleSubject(SUBJECT))
                .thenReturn(Optional.of(account));

        OidcUser result = googleLoginService.processGoogleUser(
                createGoogleUser(EMAIL, true));

        assertEquals(EMAIL, result.getName());

        assertTrue(result.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_SALONOWNER")));
    }
}
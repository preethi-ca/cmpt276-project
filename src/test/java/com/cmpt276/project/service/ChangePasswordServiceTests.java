package com.cmpt276.project.service;

import com.cmpt276.project.model.Account;
import com.cmpt276.project.model.RoleType;
import com.cmpt276.project.repository.AccountRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChangePasswordServiceTests {

    private AccountRepository accountRepository;
    private PasswordEncoder passwordEncoder;
    private AccountService accountService;
    private Account account;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        accountService = new AccountService(accountRepository, passwordEncoder);

        account = new Account(
                "Test Customer",
                "customer@test.com",
                passwordEncoder.encode("OldPassword123"),
                RoleType.CUSTOMER
        );

        when(accountRepository.findByEmail("customer@test.com"))
                .thenReturn(Optional.of(account));
    }

    @Test
    void shouldChangePasswordSuccessfully() {
        boolean result = accountService.changePassword(
                "customer@test.com", "OldPassword123", "NewPassword123");

        assertTrue(result);
        assertTrue(passwordEncoder.matches(
                "NewPassword123", account.getPasswordHash()));
        assertFalse(passwordEncoder.matches(
                "OldPassword123", account.getPasswordHash()));

        verify(accountRepository).save(account);
    }

    @Test
    void shouldRejectIncorrectCurrentPassword() {
        boolean result = accountService.changePassword(
                "customer@test.com", "WrongPassword", "NewPassword123");

        assertFalse(result);
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void shouldRejectShortNewPassword() {
        assertThrows(IllegalArgumentException.class, () ->
                accountService.changePassword(
                        "customer@test.com", "OldPassword123", "short"));

        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void shouldRejectMissingCurrentPassword() {
        boolean result = accountService.changePassword(
                "customer@test.com", null, "NewPassword123");

        assertFalse(result);
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void shouldRejectUnknownAccount() {
        when(accountRepository.findByEmail("unknown@test.com"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                accountService.changePassword(
                        "unknown@test.com", "OldPassword123", "NewPassword123"));
    }

    @Test
    void shouldNormalizeEmailBeforeChangingPassword() {
        boolean result = accountService.changePassword(
                " CUSTOMER@TEST.COM ", "OldPassword123", "NewPassword123");

        assertTrue(result);
        verify(accountRepository).findByEmail("customer@test.com");
    }
}
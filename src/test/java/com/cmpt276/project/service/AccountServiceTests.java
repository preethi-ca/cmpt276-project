package com.cmpt276.project.service;

import com.cmpt276.project.controller.AuthController.RegistrationForm;
import com.cmpt276.project.model.Account;
import com.cmpt276.project.model.NotificationPreference;
import com.cmpt276.project.repository.AccountRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AccountServiceTests {

        @Autowired
        private AccountService accountService;

        @Autowired
        private AccountRepository accountRepository;

        private RegistrationForm createForm(
                        String email, String phone, String preference) {

                RegistrationForm form = new RegistrationForm();
                form.setName("Test Customer");
                form.setEmail(email);
                form.setPassword("TestPassword123!");
                form.setRoleType("CUSTOMER");
                form.setPhoneNumber(phone);
                form.setNotificationPreference(preference);

                return form;
        }

        private Errors createErrors(RegistrationForm form) {
                return new BeanPropertyBindingResult(form, "registrationForm");
        }

        @Test
        void shouldSavePhoneNumberAndSmsPreference() {
                RegistrationForm form = createForm(
                                "sms-test@example.com", "6045550123", "SMS");

                Errors errors = createErrors(form);

                boolean result = accountService.register(form, errors);

                assertTrue(result);
                assertFalse(errors.hasErrors());

                Account account = accountRepository
                                .findByEmail("sms-test@example.com")
                                .orElseThrow();

                assertEquals("6045550123", account.getPhoneNumber());
                assertEquals(NotificationPreference.SMS,
                                account.getNotificationPreference());
        }

        @Test
        void shouldRejectSmsWithoutPhoneNumber() {
                RegistrationForm form = createForm(
                                "no-phone@example.com", "", "SMS");

                Errors errors = createErrors(form);

                boolean result = accountService.register(form, errors);

                assertFalse(result);
                assertTrue(errors.hasFieldErrors("phoneNumber"));
                assertTrue(accountRepository
                                .findByEmail("no-phone@example.com")
                                .isEmpty());
        }

        @Test
        void shouldAllowEmailWithoutPhoneNumber() {
                RegistrationForm form = createForm(
                                "email-test@example.com", "", "EMAIL");

                Errors errors = createErrors(form);

                boolean result = accountService.register(form, errors);

                assertTrue(result);
                assertFalse(errors.hasErrors());

                Account account = accountRepository
                                .findByEmail("email-test@example.com")
                                .orElseThrow();

                assertNull(account.getPhoneNumber());
                assertEquals(NotificationPreference.EMAIL,
                                account.getNotificationPreference());
        }

        @Test
        void shouldRejectInvalidPhoneNumber() {
                RegistrationForm form = createForm(
                                "invalid-phone@example.com", "123", "SMS");

                Errors errors = createErrors(form);

                boolean result = accountService.register(form, errors);

                assertFalse(result);
                assertTrue(errors.hasFieldErrors("phoneNumber"));
                assertTrue(accountRepository
                                .findByEmail("invalid-phone@example.com")
                                .isEmpty());
        }

        @Test
        void shouldRejectInvalidNotificationPreference() {
                RegistrationForm form = createForm(
                                "invalid-pref@example.com", "6045550123", "INVALID");

                Errors errors = createErrors(form);

                boolean result = accountService.register(form, errors);

                assertFalse(result);
                assertTrue(errors.hasFieldErrors("notificationPreference"));
                assertTrue(accountRepository
                                .findByEmail("invalid-pref@example.com")
                                .isEmpty());
        }

        @Test
        void shouldDefaultToEmailForExistingAccounts() {
                Account account = new Account();
                account.setNotificationPreference(null);

                assertEquals(
                                NotificationPreference.EMAIL,
                                account.getNotificationPreference());
        }
}
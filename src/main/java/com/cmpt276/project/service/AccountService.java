package com.cmpt276.project.service;

import com.cmpt276.project.controller.AuthController.RegistrationForm;
import com.cmpt276.project.model.Account;
import com.cmpt276.project.model.RoleType;
import com.cmpt276.project.repository.AccountRepository;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import com.cmpt276.project.model.NotificationPreference;

/**
 * One shared account store for customers and owners, with server-side
 * registration validation.
 */
@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean register(RegistrationForm form, Errors errors) {
        String email = normalizeEmail(form.getEmail());
        String name = form.getName().trim();
        String password = form.getPassword();
        form.setEmail(email);
        form.setName(name);
        if (name.isBlank() || name.length() > 100) {
            errors.rejectValue("name", "invalid", "Enter a name of 1 to 100 characters.");
        }
        if (email.length() > 254 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            errors.rejectValue("email", "invalid", "Enter a valid email address.");
        }
        // BCrypt 的上限按 UTF-8 字节计算；拒绝超长输入，避免截断或编码异常。
        if (password.length() < 8 || password.length() > 64
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            errors.rejectValue("password", "invalid", "Use 8 to 64 characters, at most 72 UTF-8 bytes.");
        }

        if (!"CUSTOMER".equals(form.getRoleType()) && !"SALONOWNER".equals(form.getRoleType())) {
            errors.rejectValue("roleType", "invalid", "Choose Customer or Salon owner.");
        }

        // validate phone number and notification preference
        String phoneNumber = form.getPhoneNumber().trim();
        String preference = form.getNotificationPreference();

        form.setPhoneNumber(phoneNumber);

        // accept optional north american phone numbers with common formatting
        String digits = phoneNumber.replaceAll("[^0-9]", "");

        if (!phoneNumber.isBlank() &&
                (!phoneNumber.matches("[+0-9() .-]+")
                        || !(digits.length() == 10 ||
                                (digits.length() == 11 && digits.startsWith("1"))))) {
            errors.rejectValue("phoneNumber", "invalid",
                    "Enter a valid 10-digit phone number.");
        }

        if (!"EMAIL".equals(preference)
                && !"SMS".equals(preference)
                && !"BOTH".equals(preference)) {
            errors.rejectValue("notificationPreference", "invalid",
                    "Choose Email, SMS, or Both.");
        }

        if (("SMS".equals(preference) || "BOTH".equals(preference))
                && phoneNumber.isBlank()) {
            errors.rejectValue("phoneNumber", "required",
                    "A phone number is required for SMS confirmations.");
        }

        if (errors.hasErrors()) {
            return false;
        }

        if (accountRepository.findByEmail(email).isPresent()) {
            errors.rejectValue("email", "duplicate", "This email is already registered. Please sign in.");
            return false;
        }
        Account account = new Account(name, email, passwordEncoder.encode(password),
                RoleType.valueOf(form.getRoleType()));

        account.setPhoneNumber(
                phoneNumber.isBlank() ? null : phoneNumber);

        account.setNotificationPreference(
                NotificationPreference.valueOf(preference));
        try {
            // 数据库唯一约束兜底并发注册；在此刷新，才能将重复邮箱回显为表单错误。
            accountRepository.saveAndFlush(account);
        } catch (DataIntegrityViolationException exception) {
            if (accountRepository.findByEmail(email).isEmpty()) {
                throw exception;
            }
            errors.rejectValue("email", "duplicate", "This email is already registered. Please sign in.");
            return false;
        }
        return true;
    }

    public boolean changePassword(String email, String currentPassword, String newPassword) {
        String normalizedEmail = normalizeEmail(email);
    
        Account account = accountRepository.findByEmail(normalizedEmail).orElseThrow(() -> new IllegalArgumentException("Account not found"));
    
        // verify the current password
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
            return false;
        }
    
        // validate the new password
        if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 64 || newPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password must be 8 to 64 characters and at most 72 UTF-8 bytes.");
        }
    
        // hash and save the new password
        account.setPasswordHash(passwordEncoder.encode(newPassword));
        accountRepository.save(account);
    
        return true;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

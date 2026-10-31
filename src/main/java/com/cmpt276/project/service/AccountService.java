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

/** One shared account store for customers and owners, with server-side registration validation. */
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
        if (errors.hasErrors()) {
            return false;
        }
        if (accountRepository.findByEmail(email).isPresent()) {
            errors.rejectValue("email", "duplicate", "This email is already registered. Please sign in.");
            return false;
        }
        Account account = new Account(name, email, passwordEncoder.encode(password),
                RoleType.valueOf(form.getRoleType()));
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

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

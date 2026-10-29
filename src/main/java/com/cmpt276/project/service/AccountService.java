package com.cmpt276.project.service;

import com.cmpt276.project.model.Account;
import com.cmpt276.project.model.RoleType;
import com.cmpt276.project.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void register(String name, String email, String password, RoleType roleType) {
        email = email.trim().toLowerCase();

        if (accountRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("An account with this email already exists. Log in instead.");
        }

        String hashedPassword = passwordEncoder.encode(password);

        Account account = new Account(name, email, hashedPassword, roleType);

        accountRepository.save(account);
    }
}
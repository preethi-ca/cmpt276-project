package com.cmpt276.project.service;

import com.cmpt276.project.model.Account;
import com.cmpt276.project.model.RoleType;
import com.cmpt276.project.repository.AccountRepository;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class GoogleLoginService extends OidcUserService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public GoogleLoginService(
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser googleUser = super.loadUser(userRequest);
        return processGoogleUser(googleUser);
    }

    OidcUser processGoogleUser(OidcUser googleUser) {

        String subject = googleUser.getSubject();
        String email = googleUser.getEmail();
        String name = googleUser.getFullName();

        if (email == null ||
                !Boolean.TRUE.equals(googleUser.getEmailVerified())) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("unverified_email"));
        }

        email = email.trim().toLowerCase(Locale.ROOT);

        Account account = accountRepository
                .findByGoogleSubject(subject)
                .orElse(null);

        if (account == null) {

            // Do not automatically link existing password accounts.
            if (accountRepository.findByEmail(email).isPresent()) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("account_link_required"));
            }

            // Create a new customer account.
            account = new Account(
                    name == null || name.isBlank() ? email : name,
                    email,
                    passwordEncoder.encode(UUID.randomUUID().toString()),
                    RoleType.CUSTOMER
            );

            account.setGoogleSubject(subject);
            account = accountRepository.saveAndFlush(account);
        }

        // Existing BookEasy controllers identify users by email.
        if (!account.getEmail().equals(email)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_changed"));
        }

        return new DefaultOidcUser(
                List.of(new SimpleGrantedAuthority(
                        "ROLE_" + account.getRoleType().name())),
                googleUser.getIdToken(),
                googleUser.getUserInfo(),
                "email"
        );
    }
}

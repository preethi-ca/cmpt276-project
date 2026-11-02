package com.cmpt276.project.owner.auth;

import com.cmpt276.project.model.Account;
import com.cmpt276.project.model.RoleType;
import com.cmpt276.project.repository.AccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CurrentOwner {
    private final AccountRepository accounts;

    public CurrentOwner(AccountRepository accounts) {
        this.accounts = accounts;
    }

    public String getOwnerId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        if (!authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SALONOWNER"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Account account = accounts.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (account.getRoleType() != RoleType.SALONOWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        // The account uses an integer ID. Converting to a string is only compatible with the existing SalonStore,
        // and the UUids of the store and service remain unchanged.
        return String.valueOf(account.getId());
    }
}

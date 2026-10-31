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
        // 账号采用整数 ID；转换为字符串仅为兼容现有 SalonStore，店铺和服务 UUID 不变。
        return String.valueOf(account.getId());
    }
}

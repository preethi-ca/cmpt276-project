package com.cmpt276.project.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Error rendering must preserve the original 403/404 instead of redirecting to login.
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                // public pages
                .requestMatchers("/", "/register", "/login", "/css/**", "/js/**", "/images/**",
                        "/error", "/owner-assets/**", "/api/salons/**", "/demo/salons/**").permitAll()

                // role-based pages
                .requestMatchers("/customer", "/customer/**").hasRole("CUSTOMER")
                .requestMatchers("/owner", "/owner/**").hasRole("SALONOWNER")

                // all other pages require login
                .anyRequest().authenticated()
            )

            // Keep owner-page protections in the shared chain, including anonymous error pages.
            .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                "default-src 'self'; style-src 'self'; script-src 'self'; img-src 'self' data:; "
                + "object-src 'none'; base-uri 'self'; frame-ancestors 'none'; form-action 'self'")))

            // login configuration
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .permitAll()
            )

            // logout configuration
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );
        return http.build();
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
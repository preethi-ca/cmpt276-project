
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
                        // Preserve original 403/404 errors instead of redirecting to login
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()

                        // Public pages
                        .requestMatchers(
                                "/", "/about", "/privacy", "/terms",
                                "/register", "/login",
                                "/css/**", "/js/**", "/images/**",
                                "/error", "/owner-assets/**",
                                "/api/salons/**", "/demo/salons/**",
                                "/salons/**"
                        ).permitAll()

                        // Allow guests to browse services and select dates/times
                        .requestMatchers(
                                "/customer/booking",
                                "/customer/booking/time"
                        ).permitAll()

                        // Customer-only pages
                        .requestMatchers("/customer", "/customer/**")
                        .hasRole("CUSTOMER")

                        // Google Calendar integration
                        .requestMatchers("/calendar/google/**")
                        .hasRole("CUSTOMER")

                        // Salon owner pages
                        .requestMatchers("/owner", "/owner/**")
                        .hasRole("SALONOWNER")

                        // All other pages require login
                        .anyRequest().authenticated())

                // Content Security Policy
                .headers(headers -> headers.contentSecurityPolicy(csp ->
                        csp.policyDirectives(
                                "default-src 'self'; style-src 'self'; script-src 'self'; " +
                                "img-src 'self' data:; object-src 'none'; " +
                                "base-uri 'self'; frame-ancestors 'none'; form-action 'self'"
                        )))

                // Login configuration
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/dashboard", false)
                        .permitAll())

                // Logout configuration
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

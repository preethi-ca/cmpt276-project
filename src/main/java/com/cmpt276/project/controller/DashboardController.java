package com.cmpt276.project.controller;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import com.cmpt276.project.model.Account;
import com.cmpt276.project.owner.controller.PublicSalon;
import com.cmpt276.project.owner.service.OwnerService;
import com.cmpt276.project.repository.AccountRepository;
import com.cmpt276.project.repository.GoogleCalendarConnectionRepository;
import com.cmpt276.project.service.AccountService;
import com.cmpt276.project.service.GoogleCalendarSettings;

@Controller
public class DashboardController {

    private final OwnerService ownerService;
    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final GoogleCalendarConnectionRepository connectionRepository;
    private final GoogleCalendarSettings calendarSettings;

    public DashboardController(
            OwnerService ownerService,
            AccountService accountService,
            AccountRepository accountRepository,
            GoogleCalendarConnectionRepository connectionRepository,
            GoogleCalendarSettings calendarSettings) {
        this.ownerService = ownerService;
        this.accountService = accountService;
        this.accountRepository = accountRepository;
        this.connectionRepository = connectionRepository;
        this.calendarSettings = calendarSettings;
    }

    @GetMapping("/dashboard")
    public String redirectDashboard(Authentication authentication) {
        if (authentication.getAuthorities().contains(
                new SimpleGrantedAuthority("ROLE_SALONOWNER"))) {
            return "redirect:/owner";
        } else {
            return "redirect:/customer/dashboard";
        }
    }

    @GetMapping("/customer/dashboard")
    public String customerDashboard(
            Authentication authentication,
            Model model) {

        // Display the logged-in customer's name
        if (authentication != null) {
            Account account = accountService.findByEmail(
                    authentication.getName());
            model.addAttribute("customerName", account.getName());
            var recentIds = accountRepository.recentSalonIds(authentication.getName());
            model.addAttribute("recentSalons", ownerService.findRecentlyVisitedSalons(recentIds)
                    .stream().map(PublicSalon::new).toList());
        }

        // Check Google Calendar connection status
        boolean calendarConnected = false;

        if (authentication != null) {
            Optional<Account> customerOpt =
                    accountRepository.findByEmail(authentication.getName());

            if (customerOpt.isPresent()) {
                calendarConnected = connectionRepository
                        .findByCustomer(customerOpt.get())
                        .isPresent();
            }
        }

        model.addAttribute("calendarReady", calendarSettings.isReady());
        model.addAttribute("calendarConnected", calendarConnected);

        return "customer/customer-dashboard";
    }

    /**
     * Guest-readable salon data for booking integration;
     * excludes the owner's account identity.
     */
    @GetMapping("/api/salons/{id}")
    @ResponseBody
    public PublicSalon publicSalon(@PathVariable String id) {
        return new PublicSalon(ownerService.findPublicSalon(id));
    }
}
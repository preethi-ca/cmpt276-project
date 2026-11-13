package com.cmpt276.project.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cmpt276.project.model.Account;
import com.cmpt276.project.model.GoogleCalendarConnection;
import com.cmpt276.project.repository.AccountRepository;
import com.cmpt276.project.repository.GoogleCalendarConnectionRepository;
import com.cmpt276.project.service.GoogleCalendarClient;
import com.cmpt276.project.service.GoogleCalendarSettings;

@Controller
public class GoogleCalendarController {
    static final String STATE_KEY = "googleCalendarState";

    private final GoogleCalendarSettings settings;
    private final GoogleCalendarClient calendarClient;
    private final AccountRepository accountRepository;
    private final GoogleCalendarConnectionRepository connectionRepository;
    private final SecureRandom random = new SecureRandom();

    public GoogleCalendarController(GoogleCalendarSettings settings, GoogleCalendarClient calendarClient,
            AccountRepository accountRepository, GoogleCalendarConnectionRepository connectionRepository) {
        this.settings = settings;
        this.calendarClient = calendarClient;
        this.accountRepository = accountRepository;
        this.connectionRepository = connectionRepository;
    }

    // send the customer to Google to allow calendar access
    @GetMapping("/calendar/google/connect")
    public String connect(HttpSession session, RedirectAttributes redirectAttributes) {
        if (!settings.isReady()) {
            redirectAttributes.addFlashAttribute("calendarMessage", "Google Calendar is not set up right now.");
            return "redirect:/customer/dashboard";
        }
        // random state so we know the callback really started here
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        session.setAttribute(STATE_KEY, state);
        return "redirect:" + calendarClient.buildAuthorizationUrl(state);
    }

    // Google sends the customer back here after they pick allow or cancel
    @GetMapping("/calendar/google/callback")
    public String callback(@RequestParam(required = false) String state,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            Authentication authentication, HttpSession session, RedirectAttributes redirectAttributes) {
        // take the saved state out so the same link can't be used twice
        Object savedState = session.getAttribute(STATE_KEY);
        session.removeAttribute(STATE_KEY);

        if (!(savedState instanceof String) || state == null || !sameText((String) savedState, state)) {
            redirectAttributes.addFlashAttribute("calendarMessage",
                    "That Google Calendar link didn't work. Please try connecting again.");
            return "redirect:/customer/dashboard";
        }
        if (error != null) {
            redirectAttributes.addFlashAttribute("calendarMessage", "Google Calendar was not connected.");
            return "redirect:/customer/dashboard";
        }

        String refreshToken = calendarClient.exchangeCode(code);
        if (refreshToken == null) {
            redirectAttributes.addFlashAttribute("calendarMessage", "Couldn't connect Google Calendar. Please try again.");
            return "redirect:/customer/dashboard";
        }

        // one connection per customer, so replace the token if there already is one
        Account customer = findCustomer(authentication);
        Optional<GoogleCalendarConnection> existing = connectionRepository.findByCustomer(customer);
        if (existing.isPresent()) {
            GoogleCalendarConnection connection = existing.get();
            connection.setRefreshToken(refreshToken);
            connectionRepository.save(connection);
        } else {
            connectionRepository.save(new GoogleCalendarConnection(customer, refreshToken));
        }
        redirectAttributes.addFlashAttribute("calendarMessage", "Google Calendar connected.");
        return "redirect:/customer/dashboard";
    }

    // remove the saved link and tell Google to forget it
    @PostMapping("/calendar/google/disconnect")
    public String disconnect(Authentication authentication, RedirectAttributes redirectAttributes) {
        Account customer = findCustomer(authentication);
        Optional<GoogleCalendarConnection> existing = connectionRepository.findByCustomer(customer);
        if (existing.isPresent()) {
            // still delete our copy even if Google doesn't answer
            calendarClient.revokeToken(existing.get().getRefreshToken());
            connectionRepository.deleteByCustomer(customer);
            redirectAttributes.addFlashAttribute("calendarMessage", "Google Calendar disconnected.");
        }
        return "redirect:/customer/dashboard";
    }

    private Account findCustomer(Authentication authentication) {
        Optional<Account> customerOpt = accountRepository.findByEmail(authentication.getName());
        if (!customerOpt.isPresent()) {
            throw new IllegalArgumentException("Customer account not found");
        }
        return customerOpt.get();
    }

    // compare without leaking how many characters matched
    private boolean sameText(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}

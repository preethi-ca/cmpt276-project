package com.cmpt276.project.controller;

import com.cmpt276.project.model.RoleType;
import com.cmpt276.project.service.AccountService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {
    private final AccountService accountService;

    public AuthController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/register")
    public String showRegistrationPage() {
        return "register";
    }

    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    @PostMapping("/register")
    public String register(@RequestParam String name, @RequestParam String email, @RequestParam String password, @RequestParam RoleType roleType, Model model) {
        if (name.isBlank() || email.isBlank() || password.length() < 8) {
            model.addAttribute("error", "Please fill in all fields. Password must be at least 8 characters.");
            return "register";
        }

        try {
            accountService.register(name, email, password, roleType);
            return "redirect:/login?registered";

        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }
}
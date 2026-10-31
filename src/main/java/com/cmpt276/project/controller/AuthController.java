package com.cmpt276.project.controller;

import com.cmpt276.project.service.AccountService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

/** Shared authentication endpoints; Spring Security handles login and logout POSTs. */
@Controller
public class AuthController {
    private final AccountService accountService;

    public AuthController(AccountService accountService) {
        this.accountService = accountService;
    }

    @InitBinder("registrationForm")
    public void registrationFields(WebDataBinder binder) {
        // Only public registration fields may bind; never accept an account ID or encoded password.
        binder.setAllowedFields("name", "email", "password", "roleType");
    }

    @GetMapping("/register")
    public String showRegistrationPage(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());
        return "register";
    }

    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute("registrationForm") RegistrationForm form, BindingResult errors) {
        if (!accountService.register(form, errors)) {
            form.setPassword(""); // Never echo submitted passwords on validation failure.
            return "register";
        }
        return "redirect:/login?registered";
    }

    /** 注册字段与 Preethi 的 /register 一致；密码只用于校验和哈希。 */
    // public static 允许业务层复用，并让 Spring 无需 Controller 实例即可创建表单。
    public static class RegistrationForm {
        private String name = "";
        private String email = "";
        private String password = "";
        private String roleType = "";

        public String getName() { return name; }
        public void setName(String name) { this.name = name == null ? "" : name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email == null ? "" : email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password == null ? "" : password; }
        public String getRoleType() { return roleType; }
        public void setRoleType(String roleType) { this.roleType = roleType == null ? "" : roleType; }
    }
}

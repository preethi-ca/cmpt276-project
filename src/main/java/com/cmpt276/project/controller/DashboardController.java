package com.cmpt276.project.controller;

import org.springframework.stereotype.Controller;
import com.cmpt276.project.owner.controller.PublicSalon;
import com.cmpt276.project.owner.service.OwnerService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Controller
public class DashboardController {
    private final OwnerService ownerService;

    public DashboardController(OwnerService ownerService) {
        this.ownerService = ownerService;
    }

    @GetMapping("/dashboard")
    public String redirectDashboard(Authentication authentication) {
        if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SALONOWNER"))) {
            return "redirect:/owner";
        }
        else {
            return "redirect:/customer/dashboard";
        }
    }

    @GetMapping("/customer/dashboard")
    public String customerDashboard() {
        return "customer/customer-dashboard";
    }

    /** Guest-readable salon data for booking integration; excludes the owner's account identity. */
    @GetMapping("/api/salons/{id}")
    @ResponseBody
    public PublicSalon publicSalon(@PathVariable String id) {
        return new PublicSalon(ownerService.findPublicSalon(id));
    }
}
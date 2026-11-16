package com.cmpt276.project.controller;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import com.cmpt276.project.owner.controller.PublicSalon;
import com.cmpt276.project.owner.service.OwnerService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
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
    public String customerDashboard(Model model) {
        // Expose public salon fields to the customer template without the owner's account identity.
        var salons = ownerService.findPublicSalons().stream().map(PublicSalon::new).toList();
        Map<String, String> salonLabels = new HashMap<>();
        Map<String, Integer> labelCounts = new HashMap<>();
        for (PublicSalon salon : salons) {
            String label = salon.getName();
            if (salon.getAddress() != null && !salon.getAddress().isEmpty()) {
                label += " — " + salon.getAddress();
            }
            if (salon.getCity() != null && !salon.getCity().isEmpty()) {
                label += " (" + salon.getCity() + ")";
            }
            salonLabels.put(salon.getSlug(), label);
            labelCounts.merge(label, 1, Integer::sum);
        }
        salonLabels.replaceAll((slug, label) -> labelCounts.get(label) > 1
                ? label + " [" + slug + "]" : label);
        model.addAttribute("salons", salons);
        model.addAttribute("salonLabels", salonLabels);
        return "customer/customer-dashboard";
    }

    @GetMapping("/customer/salons/visit")
    public String visitSalon(@RequestParam(defaultValue = "") String slug, Model model) {
        if (slug.isBlank()) {
            model.addAttribute("salonError", "Choose a salon to visit.");
        } else {
            try {
                var salon = ownerService.findPublicSalonBySlug(slug);
                return "redirect:/salons/" + salon.getSlug();
            } catch (ResponseStatusException exception) {
                if (!exception.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                    throw exception;
                }
                model.addAttribute("salonError", "This salon is no longer available. Choose another salon.");
            }
        }
        return customerDashboard(model);
    }

    /** Guest-readable salon data for booking integration; excludes the owner's account identity. */
    @GetMapping("/api/salons/{id}")
    @ResponseBody
    public PublicSalon publicSalon(@PathVariable String id) {
        return new PublicSalon(ownerService.findPublicSalon(id));
    }
}

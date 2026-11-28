
package com.cmpt276.project.owner.controller;

import com.cmpt276.project.service.AccountService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import com.cmpt276.project.owner.service.OwnerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
public class PublicSalonController {
    private final OwnerService ownerService;
    private final AccountService accounts;

    public PublicSalonController(OwnerService ownerService, AccountService accounts) {
        this.accounts = accounts;
        this.ownerService = ownerService;
    }

    @GetMapping("/salons/{slug}")
    public String showSalon(@PathVariable String slug, Model model, Authentication authentication) {
        PublicSalon salon = new PublicSalon(ownerService.findPublicSalonBySlug(slug) );
        if (authentication != null && authentication.getAuthorities().contains(
                new SimpleGrantedAuthority("ROLE_CUSTOMER"))) {
            accounts.recordSalonVisit(authentication.getName(), salon.getId());
        }
        model.addAttribute("salon", salon);
        String[] days = {
                "Monday", "Tuesday", "Wednesday", "Thursday",
                "Friday", "Saturday", "Sunday"
        };
        model.addAttribute("days", days);

        return "customer/public-salon";
    }

    //receive and validate customer price filters
    @GetMapping("/salons/{slug}/validate-filters")
    public ResponseEntity<Map<String, Object>> validateFilters(
            @PathVariable String slug,
            @ModelAttribute SalonFilter request) {

        //check that the salon exists
        ownerService.findPublicSalonBySlug(slug);

        //validate the submitted prices
        String errorMessage = request.validate();

        if (errorMessage != null) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "message", errorMessage));
        }

        return ResponseEntity.ok(Map.of("valid", true, "message", ""));
    }
}

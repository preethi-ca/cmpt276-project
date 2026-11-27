
package com.cmpt276.project.owner.controller;

import com.cmpt276.project.service.AccountService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import com.cmpt276.project.owner.service.OwnerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

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
}

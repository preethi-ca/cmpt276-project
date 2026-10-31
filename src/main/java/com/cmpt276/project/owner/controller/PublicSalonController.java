
package com.cmpt276.project.owner.controller;

import com.cmpt276.project.owner.controller.PublicSalon;
import com.cmpt276.project.owner.service.OwnerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PublicSalonController {
    private final OwnerService ownerService;
    public PublicSalonController(OwnerService ownerService) {
        this.ownerService = ownerService;
    }

    @GetMapping("/salons/{slug}")
    public String showSalon(@PathVariable String slug, Model model) {
        PublicSalon salon = new PublicSalon(ownerService.findPublicSalonBySlug(slug) );
        model.addAttribute("salon", salon);
        String[] days = {
                "Monday", "Tuesday", "Wednesday", "Thursday",
                "Friday", "Saturday", "Sunday"
        };
        model.addAttribute("days", days);

        return "customer/public-salon";
    }
}

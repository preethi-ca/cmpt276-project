
package com.cmpt276.project.owner.controller;

import com.cmpt276.project.service.AccountService;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import com.cmpt276.project.owner.service.OwnerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
        model.addAttribute("photoIds", ownerService.getGalleryPhotoIds(salon.getId()));
        String[] days = {
                "Monday", "Tuesday", "Wednesday", "Thursday",
                "Friday", "Saturday", "Sunday"
        };
        model.addAttribute("days", days);

        return "customer/public-salon";
    }

    @GetMapping("/salons/{slug}/photos/{id}")
    public ResponseEntity<byte[]> photo(@PathVariable String slug, @PathVariable UUID id) {
        String salonId = ownerService.findPublicSalonBySlug(slug).getId();
        var photo = ownerService.getGalleryPhoto(salonId, id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(photo.getContentType())).body(photo.getContent());
    }

    //receive and validate customer price filters
    @GetMapping("/salons/{slug}/validate-filters")
    public ResponseEntity<Map<String, Object>> validateFilters(
            @PathVariable String slug,
            @ModelAttribute SalonFilter request) {

        ownerService.findPublicSalonBySlug(slug);

        String errorMessage = request.validate();

        if (errorMessage != null) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "message", errorMessage));
        }

        return ResponseEntity.ok(Map.of("valid", true, "message", ""));
    }
}

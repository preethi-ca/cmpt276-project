package com.cmpt276.project.owner.controller;

import com.cmpt276.project.owner.service.OwnerService;
import com.cmpt276.project.owner.controller.PublicSalon;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** remove it before you merge */
@Controller
@Profile("demo")
public class DemoPageController {
    private final OwnerService service;

    public DemoPageController(OwnerService service) {
        this.service = service;
    }

    @GetMapping("/")
    public String landing(Model model) {
        model.addAttribute("demo", true);
        return "owner/landing";
    }

    @GetMapping("/demo/salons/{id}")
    public String preview(@PathVariable String id, Model model) {
        model.addAttribute("salon", new PublicSalon(service.findPublicSalon(id)));
        model.addAttribute("days", new String[]{"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"});
        return "owner/preview";
    }
}

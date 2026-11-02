package com.cmpt276.project;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Profile("postgres")
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }
}

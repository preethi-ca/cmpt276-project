package com.cmpt276.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PublicInformationController {
    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/privacy")
    public String privacy() {
        return "privacy";
    }
    @GetMapping("/terms")
    public String terms() {
        return "terms";
    }
}

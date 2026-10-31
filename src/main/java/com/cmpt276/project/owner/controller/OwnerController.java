package com.cmpt276.project.owner.controller;

import com.cmpt276.project.owner.auth.CurrentOwner;
import com.cmpt276.project.owner.model.OpeningDay;
import com.cmpt276.project.owner.model.Salon;
import com.cmpt276.project.owner.service.OwnerService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/owner")
public class OwnerController {
    private final OwnerService service;
    private final CurrentOwner currentOwner;
    private final Environment environment;
    private static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

    public OwnerController(OwnerService service, CurrentOwner currentOwner, Environment environment) {
        this.service = service;
        this.currentOwner = currentOwner;
        this.environment = environment;
    }

    @InitBinder("hoursForm")
    public void hoursBinding(WebDataBinder binder) {
        binder.setAutoGrowCollectionLimit(7);
        binder.setAllowedFields("days[*].open", "days[*].startTime", "days[*].endTime");
    }

    @ModelAttribute
    public void sharedModel(Model model) {
        Salon salon = service.findForOwner(currentOwner.getOwnerId());
        model.addAttribute("salon", salon);
        model.addAttribute("days", DAYS);
        model.addAttribute("signedIn", true);
        boolean demo = environment.matchesProfiles("demo");
        model.addAttribute("demo", demo);
        if (salon != null) {
            String prefix = demo ? "/demo/salons/" : "/salons/";
            model.addAttribute("salonUrl", prefix + salon.getId());
        }
    }

    @GetMapping
    public String entry() {
        if (service.findForOwner(currentOwner.getOwnerId()) == null) {
            return "redirect:/owner/setup";
        }
        return "redirect:/owner/dashboard";
    }

    @GetMapping("/setup")
    public String setup(Model model) {
        if (service.findForOwner(currentOwner.getOwnerId()) != null) {
            return "redirect:/owner/dashboard";
        }
        model.addAttribute("salonForm", new SalonForm());
        return page(model, "setup", "Create your salon");
    }

    @PostMapping("/setup")
    public String create(@ModelAttribute("salonForm") SalonForm salonForm, BindingResult errors, Model model,
                         RedirectAttributes redirect) {
        if (!service.saveProfile(currentOwner.getOwnerId(), salonForm, true, errors)) {
            return page(model, "setup", "Create your salon");
        }
        redirect.addFlashAttribute("success", "Your salon is created. Add your services next.");
        return "redirect:/owner/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        if (model.getAttribute("salon") == null) {
            return "redirect:/owner/setup";
        }
        return page(model, "dashboard", "Dashboard");
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        Salon salon = (Salon) model.getAttribute("salon");
        if (salon == null) {
            return "redirect:/owner/setup";
        }
        SalonForm form = new SalonForm();
        form.setName(salon.getName());
        form.setDescription(salon.getDescription());
        form.setAddress(salon.getAddress());
        model.addAttribute("salonForm", form);
        return page(model, "profile", "Salon Settings");
    }

    @PostMapping("/profile")
    public String saveProfile(@ModelAttribute("salonForm") SalonForm salonForm, BindingResult errors, Model model,
                              RedirectAttributes redirect) {
        if (!service.saveProfile(currentOwner.getOwnerId(), salonForm, false, errors)) {
            return page(model, "profile", "Salon Settings");
        }
        redirect.addFlashAttribute("success", "Salon profile saved.");
        return "redirect:/owner/profile";
    }

    @GetMapping("/services")
    public String services(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "") String view, Model model) {
        if (model.getAttribute("salon") == null) {
            return "redirect:/owner/setup";
        }
        model.addAttribute("serviceForm", new ServiceForm());
        if ("add".equals(view)) {
            return page(model, "add-service", "Add Service");
        }
        serviceList(model, page);
        return page(model, "services", "Services");
    }

    @PostMapping("/services")
    public String addService(@ModelAttribute("serviceForm") ServiceForm serviceForm, BindingResult errors, Model model,
                             RedirectAttributes redirect) {
        if (!service.addService(currentOwner.getOwnerId(), serviceForm, errors)) {
            return page(model, "add-service", "Add Service");
        }
        redirect.addFlashAttribute("success", "Service added to your menu.");
        Salon salon = service.findForOwner(currentOwner.getOwnerId());
        int lastPage = (salon.getServices().size() + 9) / 10;
        return "redirect:/owner/services?page=" + lastPage;
    }

    @GetMapping("/hours")
    public String hours(Model model) {
        if (model.getAttribute("salon") == null) {
            return "redirect:/owner/setup";
        }
        model.addAttribute("hoursForm", new HoursForm());
        return page(model, "hours", "Availability");
    }

    @PostMapping("/hours")
    public String saveHours(@ModelAttribute("hoursForm") HoursForm hoursForm, BindingResult errors, Model model,
                            RedirectAttributes redirect) {
        if (!service.setOpeningHours(currentOwner.getOwnerId(), hoursForm, errors)) {
            return page(model, "hours", "Availability");
        }
        redirect.addFlashAttribute("success", "Weekly opening hours saved.");
        return "redirect:/owner/hours";
    }

    private void serviceList(Model model, int requestedPage) {
        Salon salon = (Salon) model.getAttribute("salon");
        int total = salon.getServices().size();
        int pages = Math.max(1, (total + 9) / 10);
        int current = Math.max(1, Math.min(requestedPage, pages));
        int start = (current - 1) * 10;
        model.addAttribute("servicePage", salon.getServices().subList(start, Math.min(start + 10, total)));
        model.addAttribute("currentPage", current);
        model.addAttribute("totalPages", pages);
    }

    private String page(Model model, String section, String title) {
        model.addAttribute("section", section);
        model.addAttribute("title", title);
        return "owner/workspace";
    }

    public static class SalonForm {
        private String name = "";
        private String description = "";
        private String address = "";

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
    }

    public static class ServiceForm {
        private String name = "";
        private String price = "";
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPrice() { return price; }
        public void setPrice(String price) { this.price = price; }
    }

    public static class HoursForm {
        private List<OpeningDay> days = new ArrayList<>();

        public HoursForm() {
            // Establish seven lines in advance to facilitate page binding.
            // 09:00-17:00 is just the default value of the form.
            // "open" defaults to false and has not been saved yet.
            for (int day = 1; day <= 7; day++) {
                OpeningDay entry = new OpeningDay();
                entry.setDayOfWeek(day);
                entry.setStartTime("09:00");
                entry.setEndTime("17:00");
                days.add(entry);
            }
        }

        public List<OpeningDay> getDays() { return days; }
        public void setDays(List<OpeningDay> days) { this.days = days; }
    }
}

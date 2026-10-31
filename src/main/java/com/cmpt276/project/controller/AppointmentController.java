
package com.cmpt276.project.controller;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cmpt276.project.model.Account;
import com.cmpt276.project.model.Appointment;
import com.cmpt276.project.repository.AccountRepository;
import com.cmpt276.project.repository.AppointmentRepository;
import com.cmpt276.project.owner.model.Salon;
import com.cmpt276.project.owner.model.SalonService;
import com.cmpt276.project.owner.service.OwnerService;
import java.util.HashMap; 
import java.util.List;
import java.util.Map;

@Controller
public class AppointmentController {
    private final AppointmentRepository appointmentRepository;
    private final AccountRepository accountRepository;
    private final OwnerService ownerService;
    public AppointmentController(AppointmentRepository appointmentRepository,AccountRepository accountRepository, OwnerService ownerService){
        this.appointmentRepository = appointmentRepository;
        this.accountRepository = accountRepository;
        this.ownerService = ownerService;
    }

    //show all appointments the customer has
    @GetMapping("/customer/appointments")
    public String showAppointments(Authentication authentication, Model model) {
        //find the customer's account using their email
        String customerEmail = authentication.getName();
        Optional<Account> customerOpt =accountRepository.findByEmail(customerEmail);
        if (!customerOpt.isPresent()) {
            throw new IllegalArgumentException("Customer account not found");
        }
        Account customer = customerOpt.get();
        List<Appointment> appointments = appointmentRepository.findByCustomer(customer);
        Map<Integer, String> salonNames = new HashMap<>();
        for (Appointment appointment : appointments){ 
            Salon salon = ownerService.findPublicSalon(appointment.getSalonId()); 
            salonNames.put(appointment.getId(), salon.getName());
        }
        model.addAttribute("salonNames", salonNames);
        model.addAttribute("appointments", appointmentRepository.findByCustomer(customer));
        return "customer/my-appointments";
    }

    // Show the details of one appointment
    @GetMapping("/customer/appointments/details")
    public String showAppointmentDetails(@RequestParam int id,Authentication authentication,Model model) {
        String customerEmail = authentication.getName();
        Optional<Account> customerOpt = accountRepository.findByEmail(customerEmail);
        if (!customerOpt.isPresent()) {
            throw new IllegalArgumentException("Customer account not found");
        }
        Account customer = customerOpt.get();

        Optional<Appointment> appointmentOpt =appointmentRepository.findById(id);
        if (!appointmentOpt.isPresent()) {
            throw new IllegalArgumentException("Appointment not found");
        }
        Appointment appointment = appointmentOpt.get();
        if (appointment.getCustomer().getId() != customer.getId()) {
            throw new IllegalArgumentException("You cannot view this appointment");
        }
        Salon salon = ownerService.findPublicSalon(appointment.getSalonId());
        SalonService bookedService = null;
        for (SalonService service : salon.getServices()) { 
            if (service.getId().equals(appointment.getServiceId())){ 
                bookedService = service; 
                break;
            } 
        }
        model.addAttribute("appointment", appointment);
        model.addAttribute("salon", salon);
        model.addAttribute("bookedService", bookedService);
        return "customer/appointment-details";
    }


    //Cancel appointment
    @PostMapping("/customer/appointments/cancel")
    public String cancelAppointment(@RequestParam int id,Authentication authentication, RedirectAttributes redirectAttributes) {
        String customerEmail = authentication.getName();
        Optional<Account> customerOpt = accountRepository.findByEmail(customerEmail);
        if (!customerOpt.isPresent()) {
            throw new IllegalArgumentException("Customer account not found");
        }
        Account customer = customerOpt.get();

        Optional<Appointment> appointmentOpt = appointmentRepository.findByIdAndCustomer(id, customer);
        if (!appointmentOpt.isPresent()) {
            throw new IllegalArgumentException("Appointment not found");
        }
        Appointment appointment = appointmentOpt.get();

        //delete the appointment from database
        appointmentRepository.delete(appointment);
        redirectAttributes.addFlashAttribute("message", "Your appointment has been cancelled");

        return "redirect:/customer/appointments";
    }

}
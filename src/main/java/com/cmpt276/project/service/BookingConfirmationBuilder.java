package com.cmpt276.project.service;

import com.cmpt276.project.model.Account;
import com.cmpt276.project.model.Appointment;
import com.cmpt276.project.model.BookingConfirmationMessage;
import com.cmpt276.project.owner.model.Salon;

public class BookingConfirmationBuilder {

    public BookingConfirmationMessage build(Appointment appointment, Salon salon) {
        if (appointment == null || appointment.getId() <= 0) {
            throw new IllegalArgumentException("A saved appointment is required");
        }
        if (salon == null) {
            throw new IllegalArgumentException("Salon details are required");
        }

        requireText(appointment.getSalonId(), "Appointment salon ID");
        requireText(salon.getId(), "Salon ID");
        if (!appointment.getSalonId().equals(salon.getId())) {
            throw new IllegalArgumentException("The salon does not match the appointment");
        }

        Account customer = appointment.getCustomer();
        if (customer == null) {
            throw new IllegalArgumentException("Customer details are required");
        }
        requireText(customer.getName(), "Customer name");
        requireText(customer.getEmail(), "Customer email");
        requireText(salon.getName(), "Salon name");
        requireText(appointment.getServiceName(), "Service name");
        requireText(appointment.getAppointmentTime(), "Appointment time");
        if (appointment.getAppointmentDate() == null) {
            throw new IllegalArgumentException("Appointment date is required");
        }

        String subject = "Your booking at " + salon.getName() + " is confirmed";
        String body = "Hi " + customer.getName() + ",\n\n"
                + "Your booking is confirmed.\n\n"
                + "Booking reference: " + appointment.getId() + "\n"
                + "Salon: " + salon.getName() + "\n";

        if (salon.getAddress() != null && !salon.getAddress().trim().isEmpty()) {
            body = body + "Address: " + salon.getAddress() + "\n";
        }

        body = body + "Service: " + appointment.getServiceName() + "\n"
                + "Date: " + appointment.getAppointmentDate() + "\n"
                + "Time: " + appointment.getAppointmentTime() + " (Vancouver time)\n"
                + "Duration: one hour\n\n"
                + "Sign in to BookEasy and check My Appointments to view your booking.\n";

        return new BookingConfirmationMessage(customer.getEmail(), subject, body);
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }
}

package com.cmpt276.project.service;

import com.cmpt276.project.model.Account;
import com.cmpt276.project.model.Appointment;
import com.cmpt276.project.model.BookingSmsMessage;
import com.cmpt276.project.owner.model.Salon;

public class BookingSmsBuilder {

    public BookingSmsMessage build(Appointment appointment, Salon salon) {
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
        String recipient = normalizePhone(customer.getPhoneNumber());
        if (recipient == null) {
            throw new IllegalArgumentException("A valid customer phone number is required");
        }
        requireText(salon.getName(), "Salon name");
        requireText(appointment.getServiceName(), "Service name");
        requireText(appointment.getAppointmentTime(), "Appointment time");
        if (appointment.getAppointmentDate() == null) {
            throw new IllegalArgumentException("Appointment date is required");
        }

        // Kept short so a typical booking fits in one 160 character text
        String text = "BookEasy: Your " + appointment.getServiceName()
                + " at " + salon.getName()
                + " is confirmed for " + appointment.getAppointmentDate()
                + " at " + appointment.getAppointmentTime() + " (Vancouver time)."
                + " You can view it in My Appointments.";

        return new BookingSmsMessage(recipient, text);
    }

    // Registration saves the phone the way the customer typed it, so the formatting is removed here.
    // Returns null when there is no usable +1 number.
    private String normalizePhone(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        String trimmed = phoneNumber.trim();
        if (!trimmed.matches("[+0-9() .-]+")) {
            return null;
        }

        String digits = trimmed.replaceAll("[^0-9]", "");
        if (digits.length() == 10) {
            return "+1" + digits;
        }
        if (digits.length() == 11 && digits.startsWith("1")) {
            return "+" + digits;
        }
        return null;
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }
}

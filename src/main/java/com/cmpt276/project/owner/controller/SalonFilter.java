package com.cmpt276.project.owner.controller;

import java.math.BigDecimal;

public class SalonFilter {

    private String minimumPrice;
    private String maximumPrice;

    public String getMinimumPrice() {
        return minimumPrice;
    }

    public void setMinimumPrice(String minimumPrice) {
        this.minimumPrice = minimumPrice;
    }

    public String getMaximumPrice() {
        return maximumPrice;
    }

    public void setMaximumPrice(String maximumPrice) {
        this.maximumPrice = maximumPrice;
    }

    
    //Validate the minimum and maximum prices
    public String validate() {
        String priceFormat = "[0-9]{1,5}(\\.[0-9]{1,2})?";

        if (minimumPrice != null && !minimumPrice.isBlank()
                && !minimumPrice.matches(priceFormat)) {
            return "Enter a valid minimum price with up to two decimal places";
        }

        if (maximumPrice != null && !maximumPrice.isBlank()
                && !maximumPrice.matches(priceFormat)) {
            return "Enter a valid maximum price with up to two decimal places";
        }

        BigDecimal minimum = minimumPrice == null || minimumPrice.isBlank()
                ? null : new BigDecimal(minimumPrice);

        BigDecimal maximum = maximumPrice == null || maximumPrice.isBlank()
                ? null : new BigDecimal(maximumPrice);

        if (minimum != null && minimum.compareTo(new BigDecimal("99999.99")) > 0) {
            return "Minimum price cannot exceed $99,999.99";
        }

        if (maximum != null && maximum.compareTo(new BigDecimal("99999.99")) > 0) {
            return "Maximum price cannot exceed $99,999.99";
        }

        if (minimum != null && maximum != null
                && minimum.compareTo(maximum) > 0) {
            return "Minimum price cannot be greater than maximum price";
        }

        return null;
    }
}

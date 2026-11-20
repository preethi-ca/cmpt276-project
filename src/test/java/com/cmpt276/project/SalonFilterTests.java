package com.cmpt276.project;

import com.cmpt276.project.owner.controller.SalonFilter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SalonFilterTest {

    //Both minimum and maximum prices are valid
    @Test
    void testValidPriceRange() {
        SalonFilter filter = new SalonFilter();
        filter.setMinimumPrice("25");
        filter.setMaximumPrice("100");

        assertNull(filter.validate());
    }

    //Both price fields can be empty
    @Test
    void testEmptyPriceFields() {
        SalonFilter filter = new SalonFilter();
        filter.setMinimumPrice("");
        filter.setMaximumPrice("");

        assertNull(filter.validate());
    }

    //Only minimum price is provided
    @Test
    void testMinimumPriceOnly() {
        SalonFilter filter = new SalonFilter();
        filter.setMinimumPrice("25");

        assertNull(filter.validate());
    }

    //Only maximum price is provided
    @Test
    void testMaximumPriceOnly() {
        SalonFilter filter = new SalonFilter();
        filter.setMaximumPrice("100");

        assertNull(filter.validate());
    }

    //Alphabetic characters are rejected
    @Test
    void testAlphabeticPrice() {
        SalonFilter filter = new SalonFilter();
        filter.setMinimumPrice("abc");
    
        assertNotNull(filter.validate());
    }

    //Special characters are rejected
    @Test
    void testSpecialCharacters() {
        SalonFilter filter = new SalonFilter();
        filter.setMinimumPrice("$25");

        assertNotNull(filter.validate());
    }

    //Negative prices are rejected
    @Test
    void testNegativePrice() {
        SalonFilter filter = new SalonFilter();
        filter.setMinimumPrice("-10");

        assertNotNull(filter.validate());
    }

    //Valid decimal prices are accepted
    @Test
    void testValidDecimalPrices() {
        SalonFilter filter = new SalonFilter();
        filter.setMinimumPrice("25.50");
        filter.setMaximumPrice("100.99");

        assertNull(filter.validate());
    }

    //More than two decimal places are rejected
    @Test
    void testTooManyDecimalPlaces() {
        SalonFilter filter = new SalonFilter();
        filter.setMinimumPrice("10.999");

        assertNotNull(filter.validate());
    }

    //One decimal place is accepted
    @Test
    void testOneDecimalPlace() {
        SalonFilter filter = new SalonFilter();
        filter.setMinimumPrice("25.5");

        assertNull(filter.validate());
    }

    // Prices above $99,999.99 are rejected
    @Test
    void testPriceExceedsMaximum() {
        SalonFilter filter = new SalonFilter();
        filter.setMinimumPrice("100000");

        assertNotNull(filter.validate());
    }

    //Minimum price cannot exceed maximum price
    @Test
    void testMinimumGreaterThanMaximum() {
        SalonFilter filter = new SalonFilter();
        filter.setMinimumPrice("100");
        filter.setMaximumPrice("50");

        assertNotNull(filter.validate());
    }

    //Maximum price is accepted
    @Test
    void testMaximumAllowedPrice() {
        SalonFilter filter = new SalonFilter();
        filter.setMaximumPrice("99999.99");

        assertNull(filter.validate());
    }

    //Zero is accepted as a minimum price
    @Test
    void testZeroMinimumPrice() {
        SalonFilter filter = new SalonFilter();
        filter.setMinimumPrice("0");

        assertNull(filter.validate());
    }

    //Minimum and maximum prices can be equal
    @Test
    void testEqualMinimumAndMaximumPrices() {
        SalonFilter filter = new SalonFilter();
        filter.setMinimumPrice("50");
        filter.setMaximumPrice("50");

        assertNull(filter.validate());
    }

    //Blank spaces are accepted as empty inputs
    @Test
    void testBlankPriceFields() {
        SalonFilter filter = new SalonFilter();
        filter.setMinimumPrice("   ");
        filter.setMaximumPrice("   ");

        assertNull(filter.validate());
    }

    //Prices containing spaces between numbers are rejected
    @Test
    void testPriceContainingSpaces() {
        SalonFilter filter = new SalonFilter();
        filter.setMinimumPrice("25 50");

        assertNotNull(filter.validate());
    }

    //Commas in prices are rejected
    @Test
    void testPriceContainingComma() {
        SalonFilter filter = new SalonFilter();
        filter.setMinimumPrice("1,000");

        assertNotNull(filter.validate());
    }

    //Null price fields are accepted
    @Test
    void testNullPriceFields() {
        SalonFilter filter = new SalonFilter();

        assertNull(filter.validate());
    }

    //Alphabetical sorting is accepted
    @Test
    void testAlphabeticalSorting() {
        SalonFilter filter = new SalonFilter();

        assertNull(filter.validateSort("alphabetical"));
    }

    //low to high sorting is accepted
    @Test
    void testPriceLowSorting() {
        SalonFilter filter = new SalonFilter();

        assertNull(filter.validateSort("price-low"));
    }

    //high to low sorting is accepted
    @Test
    void testPriceHighSorting() {
        SalonFilter filter = new SalonFilter();

        assertNull(filter.validateSort("price-high"));
    }

    //Default order sorting is accepted
    @Test
    void testDefaultSorting() {
        SalonFilter filter = new SalonFilter();

        assertNull(filter.validateSort("default"));
    }
    
    //Invalid sorting option is rejected
    @Test
    void testInvalidSorting() {
        SalonFilter filter = new SalonFilter();

        assertEquals("Invalid sorting option", filter.validateSort("random"));
    }
}


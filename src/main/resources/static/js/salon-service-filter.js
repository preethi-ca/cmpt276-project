document.addEventListener("DOMContentLoaded", function () {

    //Get everthing needed
    //service list
    const serviceList = document.getElementById("serviceList");
    //service cards
    const serviceCards = Array.from(document.querySelectorAll("[data-service-card]"));
    //original order of the services
    const originalOrder = [...serviceCards];
    //Apply Filters and Clear All Filters buttons
    const applyFiltersButton = document.getElementById("applyFilters");
    const clearFiltersButton = document.getElementById("clearFilters");
    //filter inputs
    const minimumPriceInput = document.getElementById("minimumPrice");
    const maximumPriceInput = document.getElementById("maximumPrice");
    //messages
    const filterError = document.getElementById("filterError");
    const noServicesMessage = document.getElementById("noServicesMessage");
    const serviceResultCount = document.getElementById("serviceResultCount");

    //Show the initial number of available services
    serviceResultCount.textContent = "Showing " + originalOrder.length + " of " + originalOrder.length + " services";

    //apply the selected filters when the button is clicked
    applyFiltersButton.addEventListener("click", async function () {
        const selectedSort = document.querySelector('input[name="serviceSort"]:checked').value;
        const selectedPriceRange = document.querySelector('input[name="priceRange"]:checked').value;

        //Get the custom minimum and maximum prices
        const minimumPriceText = minimumPriceInput.value.trim();
        const maximumPriceText = maximumPriceInput.value.trim();
        //clear any previous error message
        filterError.textContent = "";
        filterError.hidden = true;

        //make sure input is numbers and has max 2 decimal places
        const validPriceFormat = /^\d{1,5}(\.\d{1,2})?$/;

        //check the format for min price
        if (minimumPriceText !== "" && !validPriceFormat.test(minimumPriceText)) {
            filterError.textContent =
                "Minimum price must be a positive number with no more than 2 decimal places and cannot exceed $99,999.99";
            filterError.hidden = false;
            return;
        }

        //check the format for max price
        if (maximumPriceText !== "" && !validPriceFormat.test(maximumPriceText)) {
            filterError.textContent =
                "Maximum price must be a positive number with no more than 2 decimal places and cannot exceed $99,999.99";
            filterError.hidden = false;
            return;
        }

        //Convert valid inputs into numbers
        const minimumPrice = minimumPriceText === "" ? null : Number(minimumPriceText);
        const maximumPrice = maximumPriceText === "" ? null : Number(maximumPriceText);

        // Check that prices do not exceed the maximum allowed amount
        if (
            (minimumPrice !== null && minimumPrice > 99999.99) ||
            (maximumPrice !== null && maximumPrice > 99999.99)
        ) {
            filterError.textContent = "Prices cannot be greater than $99,999.99";
            filterError.hidden = false;
            return;
        }

        //Check if the custom price range is valid
        if (
            (minimumPrice !== null && minimumPrice < 0) ||
            (maximumPrice !== null && maximumPrice < 0) ||
            (minimumPrice !== null && maximumPrice !== null && minimumPrice > maximumPrice)
        ) {
            filterError.textContent = "Please enter a valid price range";
            filterError.hidden = false;
            return;
        }

        //Send prices to the Java backend for validation
        try {
            const parameters = new URLSearchParams({minimumPrice: minimumPriceText,maximumPrice: maximumPriceText });
            const response = await fetch(window.location.pathname + "/validate-filters?" + parameters);
            const result = await response.json();

            if (!response.ok) {
                filterError.textContent = result.message;
                filterError.hidden = false;
                return;
            }

        } catch (error) {
            filterError.textContent = "Unable to validate prices. Please try again.";
            filterError.hidden = false;
            return;
        }

        //start with all the original service cards
        let filteredServices = [...originalOrder];
        //use the filter selected
        if (selectedPriceRange === "under-25") {
            filteredServices = filteredServices.filter(card => Number(card.dataset.price) < 25);
        } else if (selectedPriceRange === "under-50") {
            filteredServices = filteredServices.filter(card => Number(card.dataset.price) < 50);
        } else if (selectedPriceRange === "under-100") {
            filteredServices = filteredServices.filter(card => Number(card.dataset.price) < 100);
        } else if (selectedPriceRange === "above-100") {
            filteredServices = filteredServices.filter(card => Number(card.dataset.price) > 100);
        }

        //use the chosen minimum price the customer input
        if (minimumPrice !== null) {
            filteredServices = filteredServices.filter(
                card => Number(card.dataset.price) >= minimumPrice
            );
        }

        //use the chosen maximum price the customer input
        if (maximumPrice !== null) {
            filteredServices = filteredServices.filter(
                card => Number(card.dataset.price) <= maximumPrice
            );
        }

        //Sort the filtered services by price
        if (selectedSort === "price-low") {
            filteredServices.sort((a, b) => Number(a.dataset.price) - Number(b.dataset.price));
        } else if (selectedSort === "price-high") {
            filteredServices.sort((a, b) => Number(b.dataset.price) - Number(a.dataset.price));
        } else if (selectedSort === "alphabetical") {
            filteredServices.sort((a, b) => {
                const nameA = a.querySelector("h3").textContent.trim();
                const nameB = b.querySelector("h3").textContent.trim();
                return nameA.localeCompare(nameB);
            });
        }

        //Only display the services that match the filters
        serviceCards.forEach(function (card) {card.hidden = !filteredServices.includes(card); });

        //Put the matching services in the selected order
        filteredServices.forEach(function (card) {serviceList.appendChild(card);});

        //update the number of matching services
        serviceResultCount.textContent = "Showing " + filteredServices.length + " of " + originalOrder.length + " services";

        //if no services match the filters show a message
        noServicesMessage.hidden = filteredServices.length !== 0;
    });


    //clear all filters when the Clear All button is clicked
    clearFiltersButton.addEventListener("click", function () {
        //Reset sorting to default
        document.querySelector('input[name="serviceSort"][value="default"]').checked = true;

        //Reset price range to all prices
        document.querySelector('input[name="priceRange"][value="all"]').checked = true;

        //clear custom price inputs
        minimumPriceInput.value = "";
        maximumPriceInput.value = "";

        //clear any error messages
        filterError.textContent = "";
        filterError.hidden = true;

        //show all services in their original order
        originalOrder.forEach(function (card) {
            card.hidden = false;
            serviceList.appendChild(card);
        });

        //update the service count
        serviceResultCount.textContent = "Showing " + originalOrder.length + " of " + originalOrder.length + " services";

        //hide the no services message
        noServicesMessage.hidden = true;
    });
});

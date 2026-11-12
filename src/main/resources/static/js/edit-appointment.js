const form = document.getElementById("editAppointmentForm");
const dateInput = document.getElementById("appointmentDate");
const timeSelect = document.getElementById("appointmentTime");
const availabilityMessage = document.getElementById("availabilityMessage");
const appointmentId = form.dataset.appointmentId;

//refresh available times when the selected date changes
dateInput.addEventListener("change", function () {
    const selectedDate = dateInput.value;
    if (!selectedDate) {
        return;
    }
    timeSelect.disabled = true;
    availabilityMessage.textContent = "Loading available times...";

    fetch(`/customer/appointments/available-times?id=${appointmentId}&date=${selectedDate}`)
        .then(response => {
            if (!response.ok) {
                throw new Error("Unable to load available times");
            }
            return response.json();
        })
        .then(times => {
            timeSelect.innerHTML = "";

            //Handle dates with no available times
            if (times.length === 0) {
                const option = document.createElement("option");
                option.value = "";
                option.textContent = "No available times for this date";
                timeSelect.appendChild(option);
                availabilityMessage.textContent = "Please select another date";
                return;
            }

            //show the available time options
            times.forEach(time => {
                const option = document.createElement("option");
                option.value = time;
                option.textContent = time;
                timeSelect.appendChild(option);
            });
            availabilityMessage.textContent = "Available times have been updated";
        })
        .catch(error => {
            availabilityMessage.textContent = error.message;
        })
        .finally(() => {
            timeSelect.disabled = false;
        });
});

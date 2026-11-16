document.addEventListener("DOMContentLoaded", function () {
    //appointment cancellation forms
    const cancelForms = document.querySelectorAll('form[action*="/customer/appointments/cancel"]');

    //confirmation message
    cancelForms.forEach(function (form) {
        form.addEventListener("submit", function (event) {
            const confirmed = window.confirm("Are you sure you want to cancel this appointment?");
            if (!confirmed) {
                event.preventDefault();
            }
        });
    });
});
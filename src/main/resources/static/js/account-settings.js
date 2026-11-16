document.addEventListener("DOMContentLoaded", function () {
    const togglePassword = document.getElementById("togglePassword");
    const passwordFields = document.getElementById("passwordFields");

    if (!togglePassword || !passwordFields) {
        return;
    }

    togglePassword.addEventListener("click", function () {
        const isOpening = passwordFields.hidden;

        passwordFields.hidden = !isOpening;

        togglePassword.textContent = isOpening
            ? "Cancel"
            : "Change Password";

        togglePassword.setAttribute("aria-expanded", String(isOpening));

        if (!isOpening) {
            passwordFields.querySelector("form").reset();
        }
    });
});
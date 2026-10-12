(() => {
    const form = document.querySelector("[data-circulation-due-form]");
    if (!form) return;

    const dueAt = form.elements.dueAt;
    const error = document.getElementById("dueAt-error");

    const validate = () => {
        const value = dueAt.value;
        let message = "";
        if (!value) {
            message = "Due date and time is required.";
        } else if (dueAt.min && value < dueAt.min) {
            message = "Due date must be after the issue date and time.";
        }

        dueAt.setCustomValidity(message);
        dueAt.classList.toggle("due-date-input-error", Boolean(message));
        dueAt.setAttribute("aria-invalid", String(Boolean(message)));
        error.textContent = message;
        error.classList.toggle("visible", Boolean(message));
        return !message;
    };

    dueAt.addEventListener("input", validate);
    dueAt.addEventListener("change", validate);
    dueAt.addEventListener("blur", validate);
    form.addEventListener("submit", event => {
        if (!validate()) {
            event.preventDefault();
            dueAt.focus();
        }
    });
})();

(() => {
    const form = document.querySelector("[data-circulation-profile]");
    if (!form) return;

    const firstName = form.elements.firstName;
    const lastName = form.elements.lastName;
    const email = form.elements.email;
    const phone = form.elements.phone;
    const staffNumberSuffix = form.elements.staffNumberSuffix;
    const password = form.elements.password;
    const confirmPassword = form.elements.confirmPassword;

    const namePattern = /^[\p{L}][\p{L} .'-]*$/u;
    const emailPattern = /^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$/;
    const phoneCharacters = /^\+?[\d ]+$/;
    const sriLankanPhone = /^(?:\+94|94|0)\d{9}$/;

    const showResult = (input, message) => {
        const error = document.getElementById(input.getAttribute("aria-describedby"));
        input.setCustomValidity(message);
        input.classList.toggle("circulation-input-error", Boolean(message));
        input.setAttribute("aria-invalid", String(Boolean(message)));
        if (error) {
            error.textContent = message;
            error.classList.toggle("visible", Boolean(message));
        }
        return !message;
    };

    const validateName = (input, label) => {
        const value = input.value.trim();
        let message = "";
        if (!value) message = `${label} is required.`;
        else if (value.length < 2) message = `${label} must contain at least 2 characters.`;
        else if (!namePattern.test(value)) message = `${label} cannot contain numbers or unsupported special characters.`;
        return showResult(input, message);
    };

    const validateEmail = () => {
        const value = email.value.trim();
        const message = !value ? "Login email is required."
            : !emailPattern.test(value) ? "Enter a valid email address." : "";
        return showResult(email, message);
    };

    const validatePhone = () => {
        const value = phone.value.trim();
        let message = "";
        if (value && !phoneCharacters.test(value)) {
            message = "Phone number can contain digits, spaces and a leading + only.";
        } else if (value && !sriLankanPhone.test(value.replaceAll(" ", ""))) {
            message = "Enter a valid Sri Lankan phone number, for example 0771234567.";
        }
        return showResult(phone, message);
    };

    const validateStaffNumber = () => {
        const value = staffNumberSuffix.value.trim();
        const message = /^\d{3}$/.test(value) ? "" : "Staff number must contain exactly 3 digits.";
        return showResult(staffNumberSuffix, message);
    };

    const validatePassword = () => {
        const value = password.value;
        let message = "";
        if (value && (value.length < 8 || value.length > 72)) {
            message = "New password must contain 8 to 72 characters.";
        } else if (/\s/.test(value)) {
            message = "New password cannot contain spaces.";
        } else if (value && (!/\p{Lu}/u.test(value) || !/\p{Ll}/u.test(value)
                || !/\d/.test(value) || !/[^\p{L}\d\s]/u.test(value))) {
            message = "Use an uppercase letter, lowercase letter, number and symbol.";
        }
        const valid = showResult(password, message);
        if (confirmPassword.value) validateConfirmation();
        return valid;
    };

    const validateConfirmation = () => {
        let message = "";
        if (!password.value && confirmPassword.value) message = "Enter a new password first.";
        else if (password.value && confirmPassword.value !== password.value) message = "Passwords do not match.";
        return showResult(confirmPassword, message);
    };

    const validators = [
        [firstName, () => validateName(firstName, "First name")],
        [lastName, () => validateName(lastName, "Last name")],
        [email, validateEmail],
        [phone, validatePhone],
        [staffNumberSuffix, validateStaffNumber],
        [password, validatePassword],
        [confirmPassword, validateConfirmation]
    ];

    validators.forEach(([input, validate]) => {
        input.addEventListener("input", validate);
        input.addEventListener("blur", validate);
    });

    form.addEventListener("submit", event => {
        let firstInvalid = null;
        validators.forEach(([input, validate]) => {
            if (!validate() && !firstInvalid) firstInvalid = input;
        });
        if (firstInvalid) {
            event.preventDefault();
            firstInvalid.focus();
        }
    });
})();

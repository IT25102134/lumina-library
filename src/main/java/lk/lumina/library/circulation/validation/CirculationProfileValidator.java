package lk.lumina.library.circulation.validation;

import java.util.regex.Pattern;

/** Validation rules owned by the Circulation Desk profile flow only. */
final class CirculationProfileValidator {
    private static final int NAME_MAX_LENGTH = 80;
    private static final int EMAIL_MAX_LENGTH = 180;
    private static final int PASSWORD_MIN_LENGTH = 8;
    private static final int PASSWORD_MAX_LENGTH = 72;

    private static final Pattern NAME = Pattern.compile("[\\p{L}][\\p{L} .'-]*");
    private static final Pattern EMAIL = Pattern.compile(
            "[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+");
    private static final Pattern SRI_LANKAN_PHONE = Pattern.compile("(?:\\+94|94|0)\\d{9}");
    private static final Pattern PHONE_CHARACTERS = Pattern.compile("\\+?[\\d ]+");
    private static final Pattern UPPERCASE = Pattern.compile(".*\\p{Lu}.*");
    private static final Pattern LOWERCASE = Pattern.compile(".*\\p{Ll}.*");
    private static final Pattern DIGIT = Pattern.compile(".*\\d.*");
    private static final Pattern SYMBOL = Pattern.compile(".*[^\\p{L}\\d\\s].*");

    private CirculationProfileValidator() {}

    static String validate(String firstName, String lastName, String email, String phone,
            String password, String confirmPassword) {
        String error = validateName("First name", firstName);
        if (error != null) return error;

        error = validateName("Last name", lastName);
        if (error != null) return error;

        String cleanEmail = trim(email);
        if (cleanEmail.isEmpty()) return "Login email is required.";
        if (cleanEmail.length() > EMAIL_MAX_LENGTH || !EMAIL.matcher(cleanEmail).matches()) {
            return "Enter a valid email address.";
        }

        String cleanPhone = trim(phone);
        if (!cleanPhone.isEmpty()) {
            if (!PHONE_CHARACTERS.matcher(cleanPhone).matches()) {
                return "Phone number may contain digits, spaces and a leading + only.";
            }
            String compactPhone = cleanPhone.replace(" ", "");
            if (!SRI_LANKAN_PHONE.matcher(compactPhone).matches()) {
                return "Enter a valid Sri Lankan phone number, for example 0771234567 or +94112345678.";
            }
        }

        String newPassword = password == null ? "" : password;
        String confirmation = confirmPassword == null ? "" : confirmPassword;
        if (newPassword.isBlank()) {
            return confirmation.isBlank() ? null : "Enter a new password before confirming it.";
        }
        if (newPassword.length() < PASSWORD_MIN_LENGTH || newPassword.length() > PASSWORD_MAX_LENGTH) {
            return "New password must contain 8 to 72 characters.";
        }
        if (newPassword.chars().anyMatch(Character::isWhitespace)) {
            return "New password cannot contain spaces.";
        }
        if (!UPPERCASE.matcher(newPassword).matches()
                || !LOWERCASE.matcher(newPassword).matches()
                || !DIGIT.matcher(newPassword).matches()
                || !SYMBOL.matcher(newPassword).matches()) {
            return "New password must include an uppercase letter, lowercase letter, number and symbol.";
        }
        if (!newPassword.equals(confirmation)) return "New password and confirmation do not match.";
        return null;
    }

    static String validateStaffNumberSuffix(String suffix) {
        String clean = trim(suffix);
        return clean.matches("\\d{3}") ? null : "Staff number must contain exactly 3 digits.";
    }

    private static String validateName(String label, String value) {
        String clean = trim(value);
        if (clean.isEmpty()) return label + " is required.";
        if (clean.length() < 2 || clean.length() > NAME_MAX_LENGTH) {
            return label + " must contain 2 to 80 characters.";
        }
        if (!NAME.matcher(clean).matches()) {
            return label + " may contain letters, spaces, apostrophes, hyphens and full stops only.";
        }
        return null;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}

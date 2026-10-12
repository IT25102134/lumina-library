package lk.lumina.library.circulation.validation;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CirculationProfileValidatorTest {
    @Test
    void acceptsValidDetailsAndStrongOptionalPassword() {
        assertNull(CirculationProfileValidator.validate(
                "Nimal-Anura", "Fernando", "circulation@lumina.lk",
                "+94 77 123 4567", "Library@123", "Library@123"));
        assertNull(CirculationProfileValidator.validate(
                "Nimal", "Fernando", "circulation@lumina.lk",
                "", "", ""));
    }

    @Test
    void rejectsNumbersInNamesAndLettersInPhone() {
        assertTrue(CirculationProfileValidator.validate(
                "Nimal435436", "Fernando", "circulation@lumina.lk",
                "0771234567", "", "").contains("First name"));
        assertTrue(CirculationProfileValidator.validate(
                "Nimal", "Fernando", "circulation@lumina.lk",
                "+94 11 234 567efg", "", "").contains("Phone number"));
        assertTrue(CirculationProfileValidator.validate(
                "Nimal", "Fernando", "circulation@lumina.lk",
                "077-123-4567", "", "").contains("Phone number"));
    }

    @Test
    void rejectsInvalidEmailAndPasswords() {
        assertEquals("Enter a valid email address.", CirculationProfileValidator.validate(
                "Nimal", "Fernando", "not-an-email", "", "", ""));
        assertTrue(CirculationProfileValidator.validate(
                "Nimal", "Fernando", "circulation@lumina.lk", "",
                "password", "password").contains("uppercase"));
        assertTrue(CirculationProfileValidator.validate(
                "Nimal", "Fernando", "circulation@lumina.lk", "",
                "Library@123", "Different@123").contains("do not match"));
    }

    @Test
    void staffNumberSuffixRequiresExactlyThreeDigits() {
        assertNull(CirculationProfileValidator.validateStaffNumberSuffix("002"));
        assertNotNull(CirculationProfileValidator.validateStaffNumberSuffix("02"));
        assertNotNull(CirculationProfileValidator.validateStaffNumberSuffix("2A2"));
        assertNotNull(CirculationProfileValidator.validateStaffNumberSuffix("1000"));
    }
}

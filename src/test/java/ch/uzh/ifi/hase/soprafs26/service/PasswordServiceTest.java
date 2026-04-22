package ch.uzh.ifi.hase.soprafs26.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PasswordServiceTest {

    private final PasswordService passwordService = new PasswordService();

    @Test
    public void hashPassword_success() {
        String rawPassword = "mySecretPassword";
        String hashedPassword = passwordService.hashPassword(rawPassword);

        assertNotNull(hashedPassword);
        assertNotEquals(rawPassword, hashedPassword);
    }

    @Test
    public void matches_success() {
        String rawPassword = "mySecretPassword";
        String hashedPassword = passwordService.hashPassword(rawPassword);

        assertTrue(passwordService.matches(rawPassword, hashedPassword));
    }

    @Test
    public void matches_fail_incorrectPassword() {
        String rawPassword = "mySecretPassword";
        String wrongPassword = "wrongPassword";
        String hashedPassword = passwordService.hashPassword(rawPassword);

        assertFalse(passwordService.matches(wrongPassword, hashedPassword));
    }
}
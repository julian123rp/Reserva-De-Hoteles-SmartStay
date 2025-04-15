package com.example.SmartStay.user;

import org.junit.Test;
import static org.junit.Assert.*;

import java.security.NoSuchAlgorithmException;

public class PasswordTest {

    @Test
    public void testCorrectPassword() throws NoSuchAlgorithmException {
        String password = "safePassword4321";
        byte[] hash = Password.hashPassword(password);

        assertTrue(Password.verifyPassword("safePassword4321", hash));
    }

    @Test
    public void testIncorrectPassword() throws NoSuchAlgorithmException {
        String password = "safePassword4321";
        byte[] hash = Password.hashPassword(password);

        assertFalse(Password.verifyPassword("incorrectPassword4321", hash));
    }
}
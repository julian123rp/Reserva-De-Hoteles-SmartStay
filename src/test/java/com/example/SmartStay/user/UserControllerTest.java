package com.example.SmartStay.user;

import org.junit.Test;
import static org.junit.Assert.*;

public class UserControllerTest {

    @Test
    public void testValidEmail() {
        assertFalse(UserController.isValidEmail("test"));
        assertFalse(UserController.isValidEmail("test@"));
        assertTrue(UserController.isValidEmail("test@test.com"));

        assertFalse(UserController.isValidName(""));
        assertTrue(UserController.isValidName("test test"));

        assertFalse(UserController.isValidPassword("12345"));
        assertTrue(UserController.isValidPassword("12345678"));
    }
}
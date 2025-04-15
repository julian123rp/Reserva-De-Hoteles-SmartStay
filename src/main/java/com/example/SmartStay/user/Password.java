package com.example.SmartStay.user;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Password {

    public static byte[] hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest(password.getBytes());
    }

    public static boolean verifyPassword(String password, byte[] storedHash) throws NoSuchAlgorithmException {
        byte[] newHash = hashPassword(password);
        return Arrays.equals(newHash, storedHash);
    }
}
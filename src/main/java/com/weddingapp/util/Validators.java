package com.weddingapp.util;

import java.util.regex.Pattern;

/**
 * Utility class for input validation
 */
public final class Validators {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^(\\+84|0)[0-9]{9,10}$"
    );
    
    private Validators() {
    }
    
    /**
     * Validate email format
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return true; // Email is optional
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }
    
    /**
     * Validate Vietnamese phone number
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return true; // Phone is optional
        }
        String cleaned = phone.trim().replaceAll("[\\s-]", "");
        return PHONE_PATTERN.matcher(cleaned).matches();
    }
    
    /**
     * Validate required field
     */
    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }
    
    /**
     * Validate positive number
     */
    public static boolean isPositive(double value) {
        return value > 0;
    }
    
    /**
     * Validate positive integer
     */
    public static boolean isPositive(int value) {
        return value > 0;
    }
}


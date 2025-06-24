package com.ecommerce.user.utils;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class EmailValidator {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    public static boolean isValidEmail(String email) {
        if (email == null) {
            throw new NullPointerException("Email cannot be null");
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }
}

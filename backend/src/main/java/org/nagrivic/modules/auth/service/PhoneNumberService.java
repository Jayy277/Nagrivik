package org.nagrivic.modules.auth.service;

import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PhoneNumberService {

    // Matches optional +91 or 91 or leading 0, followed by a valid 10-digit Indian mobile starting with 6, 7, 8, or 9
    private static final Pattern INDIA_PHONE_PATTERN = Pattern.compile("^(?:\\+91|91|0)?([6-9]\\d{9})$");

    public String normalize(String rawPhone) {
        if (rawPhone == null || rawPhone.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be blank");
        }

        String cleaned = rawPhone.replaceAll("[\\s\\-\\(\\)]", "").trim();
        Matcher matcher = INDIA_PHONE_PATTERN.matcher(cleaned);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid Indian mobile phone number format: " + rawPhone);
        }

        String tenDigitNumber = matcher.group(1);
        return "+91" + tenDigitNumber;
    }

    public boolean isValid(String rawPhone) {
        if (rawPhone == null || rawPhone.trim().isEmpty()) {
            return false;
        }
        String cleaned = rawPhone.replaceAll("[\\s\\-\\(\\)]", "").trim();
        return INDIA_PHONE_PATTERN.matcher(cleaned).matches();
    }
}

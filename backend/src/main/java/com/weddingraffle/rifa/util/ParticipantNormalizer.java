package com.weddingraffle.rifa.util;

import org.springframework.util.StringUtils;

public final class ParticipantNormalizer {

    private ParticipantNormalizer() {}

    public static String normalizeName(String name) {
        return name.trim();
    }

    public static String normalizeEmail(String email) {
        return StringUtils.hasText(email) ? email.trim().toLowerCase() : null;
    }

    public static String normalizePhone(String phone) {
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() != 10 && digits.length() != 11) {
            throw new IllegalArgumentException("Phone must have 10 or 11 digits.");
        }
        return digits;
    }
}

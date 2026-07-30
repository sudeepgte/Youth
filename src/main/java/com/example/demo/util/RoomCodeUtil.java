package com.example.demo.util;

import java.util.Random;

public class RoomCodeUtil {

    // Unambiguous character set (excludes O, 0, I, 1, L)
    private static final String UNAMBIGUOUS_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final Random RANDOM = new Random();

    /**
     * Generates a clean 6-character room code without visually confusing characters.
     */
    public static String generateRoomCode() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(UNAMBIGUOUS_CHARS.charAt(RANDOM.nextInt(UNAMBIGUOUS_CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * Normalizes a room code to a canonical representation:
     * - Trims leading/trailing whitespace
     * - Converts to uppercase
     * - Maps 'O' to '0'
     * - Maps 'I' and 'L' to '1'
     */
    public static String normalize(String rawCode) {
        if (rawCode == null) return "";
        String trimmed = rawCode.trim().toUpperCase();
        StringBuilder sb = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            if (ch == 'O') {
                sb.append('0');
            } else if (ch == 'I' || ch == 'L') {
                sb.append('1');
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    /**
     * Checks if two room codes are equivalent after canonical normalization.
     */
    public static boolean isMatch(String code1, String code2) {
        return normalize(code1).equals(normalize(code2));
    }
}

package com.museum.reservation.common;

public final class MaskUtil {

    private MaskUtil() {
    }

    public static String idCard(String value) {
        if (value == null || value.length() < 8) {
            return value;
        }
        return value.substring(0, 4) + "**********" + value.substring(value.length() - 4);
    }

    public static String phone(String value) {
        if (value == null || value.length() < 7) {
            return value;
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }

    public static String name(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return value.charAt(0) + "*".repeat(Math.max(1, value.length() - 1));
    }
}

package com.shiwei.seckill.common.security;

public final class DesensitizeUtil {
    private DesensitizeUtil() {
    }

    public static String mobile(String value) {
        if (value == null || value.length() < 7) {
            return value;
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }

    public static String address(String value) {
        if (value == null || value.length() <= 6) {
            return value;
        }
        return value.substring(0, 6) + "****";
    }
}


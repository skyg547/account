package com.ho.account.shared.util;

import org.springframework.stereotype.Service;

@Service
public class DataMaskingService {

    public String mask(String value, String pattern) {
        if (value == null || value.isEmpty())
            return value;

        switch (pattern.toUpperCase()) {
            case "ACCOUNT":
                return maskAccount(value);
            case "REG_NO":
                return maskRegNo(value);
            case "EMAIL":
                return maskEmail(value);
            default:
                return maskDefault(value);
        }
    }

    private String maskAccount(String value) {
        // 123-456-7890 -> 123-***-7890
        if (value.length() < 7)
            return "****";
        return value.substring(0, 4) + "****" + value.substring(value.length() - 3);
    }

    private String maskRegNo(String value) {
        // 800101-1234567 -> 800101-1******
        if (value.length() < 8)
            return "****";
        return value.substring(0, 8) + "******";
    }

    private String maskEmail(String value) {
        // abc@example.com -> a**@example.com
        int atIndex = value.indexOf("@");
        if (atIndex <= 1)
            return "****";
        return value.substring(0, 1) + "***" + value.substring(atIndex);
    }

    private String maskDefault(String value) {
        if (value.length() <= 2)
            return "**";
        return value.substring(0, 1) + "****" + value.substring(value.length() - 1);
    }
}

package com.saas.libms.common;

import java.security.SecureRandom;

public final class PublicIdGenerator {

    private static final String CHARACTERS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private PublicIdGenerator() {}

    public static String generate(String prefix) {
        return prefix +"-"+randomString(6);
    }

    public static String generateVerificationCode() {
        return randomString(6);
    }

    private static String randomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

}


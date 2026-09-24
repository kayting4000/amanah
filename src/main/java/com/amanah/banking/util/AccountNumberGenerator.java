package com.amanah.banking.util;

import java.security.SecureRandom;

public class AccountNumberGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PREFIX = "AMN";

    private AccountNumberGenerator() {}

    /**
     * Generates a unique-looking account number: AMN + 9 digits = 12 chars total.
     * Uniqueness is verified by the caller against the database.
     */
    public static String generate() {
        long number = (long) (RANDOM.nextDouble() * 1_000_000_000L);
        return PREFIX + String.format("%09d", number);
    }
}

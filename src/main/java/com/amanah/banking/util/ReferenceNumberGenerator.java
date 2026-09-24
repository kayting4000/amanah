package com.amanah.banking.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.security.SecureRandom;

public class ReferenceNumberGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    /** 12 digits: yyMMddHHmmss */
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyMMddHHmmss");
    private static final int RANDOM_BOUND = 100_000;

    private ReferenceNumberGenerator() {}

    /**
     * Generates a reference: TXN + timestamp (yyMMddHHmmss) + 5 random digits.
     * <p>
     * The result is exactly 20 characters (3 + 12 + 5) so that it always fits the
     * {@code transactions.reference_number VARCHAR(20)} column.
     */
    public static String generate() {
        String ts = LocalDateTime.now().format(FMT);
        int rand = RANDOM.nextInt(RANDOM_BOUND);
        return "TXN" + ts + String.format("%05d", rand);
    }
}

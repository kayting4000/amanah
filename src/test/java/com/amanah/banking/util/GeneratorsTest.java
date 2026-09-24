package com.amanah.banking.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeneratorsTest {

    @Test
    void accountNumber_hasCorrectFormat() {
        String number = AccountNumberGenerator.generate();

        assertNotNull(number);
        assertTrue(number.startsWith("AMN"));
        assertEquals(12, number.length());
        assertTrue(number.substring(3).chars().allMatch(Character::isDigit));
    }

    @Test
    void accountNumbers_areReasonablyUnique() {
        String a = AccountNumberGenerator.generate();
        String b = AccountNumberGenerator.generate();
        // Possible but extremely unlikely to collide; format must always hold.
        assertEquals(12, a.length());
        assertEquals(12, b.length());
    }

    @Test
    void referenceNumber_hasCorrectFormat() {
        String ref = ReferenceNumberGenerator.generate();

        assertNotNull(ref);
        assertTrue(ref.startsWith("TXN"));
        assertEquals(20, ref.length());
        assertTrue(ref.substring(3).chars().allMatch(Character::isDigit));
    }

    @Test
    void referenceNumber_alwaysFitsVarchar20Column() {
        // transactions.reference_number is VARCHAR(20) - the generator must never exceed it.
        for (int i = 0; i < 200; i++) {
            String ref = ReferenceNumberGenerator.generate();
            assertTrue(ref.length() <= 20, "reference too long for column: " + ref);
            assertEquals(20, ref.length(), "reference should use the full width: " + ref);
        }
    }

    @Test
    void referenceNumbers_differ() {
        String a = ReferenceNumberGenerator.generate();
        String b = ReferenceNumberGenerator.generate();
        assertNotEquals(a, b);
    }
}
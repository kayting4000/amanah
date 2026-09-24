package com.amanah.banking.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static final String SECRET =
        "amanah_jwt_secret_replace_in_production_min_32_chars_ok";

    @Test
    void generateAndParse_roundTrip() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 86400000L);

        String token = jwtUtil.generateToken("ali", "CUSTOMER");

        assertNotNull(token);
        assertTrue(jwtUtil.isValid(token));
        assertEquals("ali", jwtUtil.extractUsername(token));
    }

    @Test
    void invalidToken_isNotValid() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 86400000L);

        assertFalse(jwtUtil.isValid("not.a.jwt"));
        assertFalse(jwtUtil.isValid(""));
    }

    @Test
    void expiredToken_isNotValid() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, -1000L);
        String token = jwtUtil.generateToken("ali", "CUSTOMER");

        assertFalse(jwtUtil.isValid(token));
    }
}
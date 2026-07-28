package com.example.distribution_backernd.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "404E635266556A586E3272357538782F413F4428472B4B6250655368566D5971");
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", 3600000L); // 1 hour
    }

    @Test
    void shouldGenerateValidTokenAndExtractUsername() {
        String username = "boss";
        String token = jwtUtil.generateToken(username);

        assertNotNull(token);
        assertEquals(username, jwtUtil.extractUsername(token));
        assertTrue(jwtUtil.isTokenValid(token));
    }

    @Test
    void shouldDetectExpiredToken() {
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", -1000L);
        String token = jwtUtil.generateToken("boss");

        assertThrows(Exception.class, () -> jwtUtil.extractUsername(token));
    }
}
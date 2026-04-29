package com.banking.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        // Base64 encoded secret — minimum 256 bits for HS256
        ReflectionTestUtils.setField(jwtService, "secret",
                "dGVzdC1zZWNyZXQta2V5LW11c3QtYmUtYXQtbGVhc3QtMjU2LWJpdHMtbG9uZw==");
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L);

        // Use Spring's built-in User — no UserEntity needed
        userDetails = User.withUsername("test@bank.com")
                .password("encoded-password")
                .authorities(Collections.emptyList())
                .build();
    }

    @Test
    void shouldGenerateNonNullToken() {
        String token = jwtService.generateToken(userDetails);
        assertThat(token).isNotNull();
    }

    @Test
    void shouldExtractUsernameFromToken() {
        String token = jwtService.generateToken(userDetails);
        String username = jwtService.extractUsername(token);
        assertThat(username).isEqualTo("test@bank.com");
    }

    @Test
    void shouldReturnTrueForValidToken() {
        String token = jwtService.generateToken(userDetails);
        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void shouldReturnFalseForTokenBelongingToDifferentUser() {
        String token = jwtService.generateToken(userDetails);

        UserDetails anotherUser = User.withUsername("other@bank.com")
                .password("pass")
                .authorities(Collections.emptyList())
                .build();

        assertThat(jwtService.isTokenValid(token, anotherUser)).isFalse();
    }

    @Test
    void shouldReturnFalseForExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "expiration", -1000L);
        String token = jwtService.generateToken(userDetails);
        assertThat(jwtService.isTokenValid(token, userDetails)).isFalse();
    }
}
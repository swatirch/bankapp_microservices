package com.banking.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtService jwtService;
    private JwtFilter jwtFilter;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret",
                "dGVzdC1zZWNyZXQta2V5LW11c3QtYmUtYXQtbGVhc3QtMjU2LWJpdHMtbG9uZw==");
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L);

        jwtFilter = new JwtFilter(jwtService, userDetailsService);

        userDetails = User.withUsername("test@bank.com")
                .password("pass")
                .authorities(Collections.emptyList())
                .build();

        // clear security context before each test
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldContinueFilterChain_whenNoAuthorizationHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldContinueFilterChain_whenAuthHeaderDoesNotStartWithBearer() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic sometoken");

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldAuthenticateUser_whenValidTokenProvided() throws Exception {
        String token = jwtService.generateToken(userDetails);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(userDetailsService.loadUserByUsername("test@bank.com"))
                .thenReturn(userDetails);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext()
                .getAuthentication()).isNotNull();
    }

    @Test
    void shouldNotAuthenticate_whenTokenIsMalformed() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer malformed.token.here");

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldNotAuthenticate_whenTokenBelongsToDifferentUser() throws Exception {
        String token = jwtService.generateToken(userDetails);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        UserDetails wrongUser = User.withUsername("test@bank.com")
                .password("pass")
                .authorities(Collections.emptyList())
                .build();

        when(userDetailsService.loadUserByUsername("test@bank.com"))
                .thenReturn(wrongUser);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
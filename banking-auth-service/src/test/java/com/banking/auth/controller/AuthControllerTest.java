package com.banking.auth.controller;

import com.banking.auth.config.SecurityConfig;
import com.banking.auth.dto.request.LoginRequest;
import com.banking.auth.dto.request.RegisterRequest;
import com.banking.auth.service.AuthService;
import com.banking.common.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private AuthService authService;

        @MockitoBean
        private JwtService jwtService;

        @MockitoBean
        private UserDetailsService userDetailsService;

        @MockitoBean
        private AuthenticationManager authenticationManager;

        @Test
        void register_shouldReturn201_whenValidRequest() throws Exception {
                RegisterRequest request = new RegisterRequest(
                                "John", "john@bank.com", "password123");

                when(authService.register(any())).thenReturn("jwt-token");

                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.token").value("jwt-token"));
        }

        @Test
        void register_shouldReturn400_whenEmailAlreadyExists() throws Exception {
                RegisterRequest request = new RegisterRequest(
                                "John", "john@bank.com", "password123");

                when(authService.register(any()))
                                .thenThrow(new RuntimeException("Email already exists"));

                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void register_shouldReturn400_whenRequestIsInvalid() throws Exception {
                RegisterRequest request = new RegisterRequest(
                                "", "", ""); // blank fields

                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void login_shouldReturn200_whenValidCredentials() throws Exception {
                LoginRequest request = new LoginRequest("john@bank.com", "password123");

                when(authService.login(any())).thenReturn("jwt-token");

                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.token").value("jwt-token"));
        }

        @Test
        void login_shouldReturn401_whenInvalidCredentials() throws Exception {
                LoginRequest request = new LoginRequest("john@bank.com", "wrong-password");

                when(authService.login(any()))
                                .thenThrow(new BadCredentialsException("Bad credentials"));

                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isUnauthorized());
        }
}

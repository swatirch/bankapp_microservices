package com.banking.account.controller;

import com.banking.account.config.SecurityConfig;
import com.banking.account.domain.Account;
import com.banking.account.domain.AccountType;
import com.banking.account.security.AccountUserDetailsService;
import com.banking.account.service.AccountService;
import com.banking.common.exception.BankingException;
import com.banking.common.exception.ErrorCode;
import com.banking.common.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, AccountUserDetailsService.class, JwtService.class})
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AccountService accountService;

    private Account stubAccount(AccountType type, BigDecimal balance) {
        return new Account("John", type, balance);
    }

    private void authenticateAs(String userId, String role) {
        UserDetails user = User.withUsername(userId)
                .password("")
                .authorities(new SimpleGrantedAuthority("ROLE_" + role))
                .build();
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    class CreateAccount {

        @Test
        void shouldReturn201WhenAccountCreated() throws Exception {
            authenticateAs("user-123", "CUSTOMER");
            Account account = stubAccount(AccountType.SAVINGS, new BigDecimal("1000.00"));
            when(accountService.createAccount(any(), any(), any(), any()))
                    .thenReturn(account);

            String body = objectMapper.writeValueAsString(
                    Map.of("ownerName", "John",
                            "accountType", "SAVINGS",
                            "initialDeposit", "1000.00"));

            mockMvc.perform(post("/api/accounts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.ownerName").value("John"))
                    .andExpect(jsonPath("$.accountStatus").value("ACTIVE"));
        }

        @Test
        void shouldReturn400WhenOwnerNameIsBlank() throws Exception {
            authenticateAs("user-123", "CUSTOMER");
            String body = objectMapper.writeValueAsString(
                    Map.of("ownerName", "",
                            "accountType", "SAVINGS",
                            "initialDeposit", "1000.00"));

            mockMvc.perform(post("/api/accounts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
        }

        @Test
        void shouldReturn400WhenInitialDepositIsZero() throws Exception {
            authenticateAs("user-123", "CUSTOMER");
            String body = objectMapper.writeValueAsString(
                    Map.of("ownerName", "John",
                            "accountType", "SAVINGS",
                            "initialDeposit", "0.00"));

            mockMvc.perform(post("/api/accounts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
        }

        @Test
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            String body = objectMapper.writeValueAsString(
                    Map.of("ownerName", "John",
                            "accountType", "SAVINGS",
                            "initialDeposit", "1000.00"));

            mockMvc.perform(post("/api/accounts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class GetAccount {

        @Test
        void shouldReturn200WhenAccountFound() throws Exception {
            authenticateAs("user-123", "ADMIN");
            Account account = stubAccount(AccountType.SAVINGS, new BigDecimal("1000.00"));
            when(accountService.getAccount(account.getAccountId())).thenReturn(account);

            mockMvc.perform(get("/api/accounts/" + account.getAccountId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ownerName").value("John"));
        }

        @Test
        void shouldReturn400WhenAccountNotFound() throws Exception {
            authenticateAs("user-123", "ADMIN");
            when(accountService.getAccount("bad-id"))
                    .thenThrow(new BankingException(
                            ErrorCode.ACCOUNT_NOT_FOUND, "Account not found"));

            mockMvc.perform(get("/api/accounts/bad-id"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class Deposit {

        @Test
        void shouldReturn200AfterDeposit() throws Exception {
            authenticateAs("user-123", "ADMIN");
            Account account = stubAccount(AccountType.SAVINGS, new BigDecimal("1000.00"));
            account.deposit(new BigDecimal("500.00"));
            when(accountService.getAccount(anyString())).thenReturn(account);
            when(accountService.deposit(anyString(), any())).thenReturn(account);

            String body = objectMapper.writeValueAsString(Map.of("amount", "500.00"));

            mockMvc.perform(post("/api/accounts/some-id/deposit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.balance").value(1500.00));
        }

        @Test
        void shouldReturn400WhenDepositAmountIsZero() throws Exception {
            authenticateAs("user-123", "ADMIN");
            String body = objectMapper.writeValueAsString(Map.of("amount", "0.00"));

            mockMvc.perform(post("/api/accounts/some-id/deposit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
        }
    }

    @Nested
    class Withdraw {

        @Test
        void shouldReturn200AfterWithdrawal() throws Exception {
            authenticateAs("user-123", "ADMIN");
            Account account = stubAccount(AccountType.SAVINGS, new BigDecimal("1000.00"));
            account.withdraw(new BigDecimal("200.00"));
            when(accountService.getAccount(anyString())).thenReturn(account);
            when(accountService.withdraw(anyString(), any())).thenReturn(account);

            String body = objectMapper.writeValueAsString(Map.of("amount", "200.00"));

            mockMvc.perform(post("/api/accounts/some-id/withdraw")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.balance").value(800.00));
        }

        @Test
        void shouldReturn400WhenInsufficientBalance() throws Exception {
            authenticateAs("user-123", "ADMIN");
            Account account = stubAccount(AccountType.SAVINGS, new BigDecimal("1000.00"));
            when(accountService.getAccount(anyString())).thenReturn(account);
            when(accountService.withdraw(anyString(), any()))
                    .thenThrow(new BankingException(
                            ErrorCode.INSUFFICIENT_BALANCE, "Insufficient balance"));

            String body = objectMapper.writeValueAsString(Map.of("amount", "5000.00"));

            mockMvc.perform(post("/api/accounts/some-id/withdraw")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("INSUFFICIENT_BALANCE"));
        }

        @Test
        void shouldReturn400WhenWithdrawAmountIsNegative() throws Exception {
            authenticateAs("user-123", "ADMIN");
            String body = objectMapper.writeValueAsString(Map.of("amount", "-100.00"));

            mockMvc.perform(post("/api/accounts/some-id/withdraw")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
        }
    }

    @Nested
    class Transfer {

        @Test
        void shouldReturn200AfterTransfer() throws Exception {
            authenticateAs("user-123", "ADMIN");
            Account from = stubAccount(AccountType.SAVINGS, new BigDecimal("800.00"));
            Account to = stubAccount(AccountType.SAVINGS, new BigDecimal("700.00"));
            when(accountService.getAccount(anyString())).thenReturn(from);
            when(accountService.transfer(anyString(), anyString(), any()))
                    .thenReturn(List.of(from, to));

            String body = objectMapper.writeValueAsString(
                    Map.of("toAccountId", "target-id", "amount", "200.00"));

            mockMvc.perform(post("/api/accounts/some-id/transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fromAccount").exists())
                    .andExpect(jsonPath("$.toAccount").exists());
        }

        @Test
        void shouldReturn400WhenAmountIsZero() throws Exception {
            authenticateAs("user-123", "ADMIN");
            String body = objectMapper.writeValueAsString(
                    Map.of("toAccountId", "target-id", "amount", "0.00"));

            mockMvc.perform(post("/api/accounts/some-id/transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
        }

        @Test
        void shouldReturn400WhenSameAccount() throws Exception {
            authenticateAs("user-123", "ADMIN");
            Account account = stubAccount(AccountType.SAVINGS, new BigDecimal("1000.00"));
            when(accountService.getAccount(anyString())).thenReturn(account);
            when(accountService.transfer(anyString(), anyString(), any()))
                    .thenThrow(new BankingException(
                            ErrorCode.SAME_ACCOUNT_TRANSFER,
                            "Cannot transfer to the same account"));

            String body = objectMapper.writeValueAsString(
                    Map.of("toAccountId", "some-id", "amount", "200.00"));

            mockMvc.perform(post("/api/accounts/some-id/transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("SAME_ACCOUNT_TRANSFER"));
        }
    }
}
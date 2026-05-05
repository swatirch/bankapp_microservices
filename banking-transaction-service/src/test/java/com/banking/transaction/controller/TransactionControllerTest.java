package com.banking.transaction.controller;

import com.banking.transaction.config.SecurityConfig;
import com.banking.transaction.domain.TransactionType;
import com.banking.transaction.entity.TransactionEntity;
import com.banking.transaction.repository.TransactionRepository;
import com.banking.transaction.security.TransactionUserDetailsService;
import com.banking.common.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@Import({ SecurityConfig.class, TransactionUserDetailsService.class, JwtService.class })
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionRepository transactionRepository;

    private void authenticateAs(String userId, String role) {
        UserDetails user = User.withUsername(userId)
                .password("")
                .authorities(new SimpleGrantedAuthority("ROLE_" + role))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private TransactionEntity buildEntity(String accountId, com.banking.transaction.domain.TransactionType type,
            BigDecimal amount, BigDecimal balanceAfter) {
        TransactionEntity entity = new TransactionEntity();
        entity.setTransactionId("txn-" + System.nanoTime());
        entity.setAccountId(accountId);
        entity.setType(type);
        entity.setAmount(amount);
        entity.setBalanceAfter(balanceAfter);
        entity.setDescription(type.name() + " of " + amount);
        entity.setTimestamp(LocalDateTime.now());
        return entity;
    }

    @Test
    void shouldReturn200WithTransactionHistory() throws Exception {
        authenticateAs("user-123", "CUSTOMER");

        TransactionEntity t1 = buildEntity("acc-001",
                TransactionType.DEPOSIT,
                new BigDecimal("500.00"),
                new BigDecimal("1500.00"));
        TransactionEntity t2 = buildEntity("acc-001",
                TransactionType.WITHDRAWAL,
                new BigDecimal("200.00"),
                new BigDecimal("1300.00"));

        when(transactionRepository.findByAccountIdOrderByTimestampDesc(
                eq("acc-001"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(t1, t2)));

        mockMvc.perform(get("/api/transactions/acc-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$.content[1].type").value("WITHDRAWAL"));
    }

    @Test
    void shouldReturn200WithEmptyListWhenNoTransactions() throws Exception {
        authenticateAs("user-123", "CUSTOMER");

        when(transactionRepository.findByAccountIdOrderByTimestampDesc(
                eq("acc-999"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/transactions/acc-999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/transactions/acc-001"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnCorrectFieldsInResponse() throws Exception {
        authenticateAs("user-123", "CUSTOMER");

        TransactionEntity entity = buildEntity("acc-001",
                TransactionType.DEPOSIT,
                new BigDecimal("500.00"),
                new BigDecimal("1500.00"));
        entity.setDescription("Deposit of 500.00");

        when(transactionRepository.findByAccountIdOrderByTimestampDesc(
                eq("acc-001"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));

        mockMvc.perform(get("/api/transactions/acc-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].transactionId").exists())
                .andExpect(jsonPath("$.content[0].accountId").value("acc-001"))
                .andExpect(jsonPath("$.content[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$.content[0].amount").value(500.00))
                .andExpect(jsonPath("$.content[0].balanceAfter").value(1500.00))
                .andExpect(jsonPath("$.content[0].description").value("Deposit of 500.00"))
                .andExpect(jsonPath("$.content[0].timestamp").exists());
    }

    @Test
    void shouldSupportPagination() throws Exception {
        authenticateAs("user-123", "CUSTOMER");

        when(transactionRepository.findByAccountIdOrderByTimestampDesc(
                eq("acc-001"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/transactions/acc-001")
                .param("page", "0")
                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
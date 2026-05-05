package com.banking.transaction.controller;

import com.banking.transaction.dto.response.TransactionResponse;
import com.banking.transaction.repository.TransactionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Transactions", description = "Transaction history per account")
public class TransactionController {

    private final TransactionRepository transactionRepository;

    public TransactionController(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/{accountId}")
    @Operation(summary = "Get transaction history for an account")
    public Page<TransactionResponse> getTransactions(
            @PathVariable String accountId,
            @PageableDefault(size = 10) Pageable pageable) {
        return transactionRepository
                .findByAccountIdOrderByTimestampDesc(accountId, pageable)
                .map(TransactionResponse::from);
    }
}
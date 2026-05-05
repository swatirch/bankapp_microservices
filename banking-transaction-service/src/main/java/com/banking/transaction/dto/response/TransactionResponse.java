package com.banking.transaction.dto.response;

import com.banking.transaction.domain.TransactionType;
import com.banking.transaction.entity.TransactionEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        String transactionId,
        String accountId,
        TransactionType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String description,
        LocalDateTime timestamp) {
    public static TransactionResponse from(TransactionEntity entity) {
        return new TransactionResponse(
                entity.getTransactionId(),
                entity.getAccountId(),
                entity.getType(),
                entity.getAmount(),
                entity.getBalanceAfter(),
                entity.getDescription(),
                entity.getTimestamp());
    }
}
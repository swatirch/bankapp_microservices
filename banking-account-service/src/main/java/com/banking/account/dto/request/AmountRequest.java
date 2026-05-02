package com.banking.account.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AmountRequest(

        @NotNull(message = "Amount must not be null")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount

) {}
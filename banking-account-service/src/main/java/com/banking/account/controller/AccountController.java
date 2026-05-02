package com.banking.account.controller;

import com.banking.account.domain.Account;
import com.banking.account.dto.request.AmountRequest;
import com.banking.account.dto.request.CreateAccountRequest;
import com.banking.account.dto.request.TransferRequest;
import com.banking.account.dto.response.AccountResponse;
import com.banking.account.dto.response.TransferResponse;
import com.banking.account.service.AccountService;
import com.banking.common.exception.BankingException;
import com.banking.common.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Account Management", description = "APIs for managing bank accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new bank account")
    public AccountResponse createAccount(
            @Valid @RequestBody CreateAccountRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        return AccountResponse.from(
                accountService.createAccount(
                        request.ownerName(),
                        request.accountType(),
                        request.initialDeposit(),
                        currentUser.getUsername()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get account by ID")
    public AccountResponse getAccount(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails currentUser) {
        Account account = accountService.getAccount(id);
        verifyOwnership(account, currentUser);
        return AccountResponse.from(account);
    }

    @PostMapping("/{id}/deposit")
    @Operation(summary = "Deposit money into account")
    public AccountResponse deposit(
            @PathVariable String id,
            @Valid @RequestBody AmountRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        Account account = accountService.getAccount(id);
        verifyOwnership(account, currentUser);
        return AccountResponse.from(accountService.deposit(id, request.amount()));
    }

    @PostMapping("/{id}/withdraw")
    @Operation(summary = "Withdraw money from account")
    public AccountResponse withdraw(
            @PathVariable String id,
            @Valid @RequestBody AmountRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        Account account = accountService.getAccount(id);
        verifyOwnership(account, currentUser);
        return AccountResponse.from(accountService.withdraw(id, request.amount()));
    }

    @PostMapping("/{id}/transfer")
    @Operation(summary = "Transfer money to another account")
    public TransferResponse transfer(
            @PathVariable String id,
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        Account account = accountService.getAccount(id);
        verifyOwnership(account, currentUser);
        List<Account> result = accountService.transfer(
                id, request.toAccountId(), request.amount());
        return TransferResponse.of(
                AccountResponse.from(result.get(0)),
                AccountResponse.from(result.get(1)));
    }

    private void verifyOwnership(Account account, UserDetails currentUser) {
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) return;
        if (!account.getOwnerId().equals(currentUser.getUsername())) {
            throw new BankingException(ErrorCode.INVALID_ACCOUNT,
                    "Access denied: you do not own this account");
        }
    }
}
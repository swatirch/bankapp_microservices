package com.banking.account.domain;

import com.banking.common.exception.BankingException;
import com.banking.common.exception.ErrorCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Account implements Serializable {
  private static final long serialVersionUID = 1L;

  private String accountId;
  private String accountNumber;
  private String ownerName;
  private AccountType accountType;
  private BigDecimal balance;
  private AccountStatus accountStatus;
  private LocalDateTime createdAt;
  private List<Transaction> transactions;
  private String ownerId;

  public Account(String ownerName, AccountType accountType, BigDecimal initialDeposit) {
    validate(ownerName);
    validateInitialDeposit(accountType, initialDeposit);
    this.accountId = UUID.randomUUID().toString();
    this.accountNumber = generateAccountNumber();
    this.ownerName = ownerName.trim();
    this.accountType = accountType;
    this.createdAt = LocalDateTime.now();
    this.transactions = new ArrayList<>();
    this.balance = initialDeposit;
    this.accountStatus = AccountStatus.ACTIVE;
    this.ownerId = null;
    this.transactions.add(new Transaction(
        initialDeposit,
        TransactionType.DEPOSIT,
        this.balance,
        "Initial deposit"));
  }

  private Account() {
  }

  public static Account reconstitute(
      String accountId,
      String accountNumber,
      String ownerName,
      AccountType accountType,
      BigDecimal balance,
      AccountStatus accountStatus,
      LocalDateTime createdAt,
      List<Transaction> transactions,
      String ownerId) {
    Account account = new Account();
    account.accountId = accountId;
    account.accountNumber = accountNumber;
    account.ownerName = ownerName;
    account.accountType = accountType;
    account.balance = balance;
    account.accountStatus = accountStatus;
    account.createdAt = createdAt;
    account.transactions = new ArrayList<>(transactions);
    account.ownerId = ownerId;
    return account;
  }

  public void deposit(BigDecimal amount) {
    validateAccountIsActive();
    validateAmount(amount);
    this.balance = this.balance.add(amount);
    this.transactions.add(new Transaction(
        amount, TransactionType.DEPOSIT, this.balance, "Deposit"));
  }

  public void withdraw(BigDecimal amount) {
    validateAccountIsActive();
    validateNotFixedDeposit();
    validateAmount(amount);
    if (amount.compareTo(this.balance) > 0) {
      throw new BankingException(ErrorCode.INSUFFICIENT_BALANCE,
          "Insufficient balance. Available: " + this.balance);
    }
    this.balance = this.balance.subtract(amount);
    this.transactions.add(new Transaction(
        amount, TransactionType.WITHDRAWAL, this.balance, "Withdrawal"));
  }

  public void block() {
    this.accountStatus = AccountStatus.BLOCKED;
  }

  public void activate() {
    this.accountStatus = AccountStatus.ACTIVE;
  }

  public void deactivate() {
    this.accountStatus = AccountStatus.INACTIVE;
  }

  public void transferOut(BigDecimal amount) {
    validateAccountIsActive();
    validateNotFixedDeposit();
    validateAmount(amount);
    if (amount.compareTo(this.balance) > 0) {
      throw new BankingException(ErrorCode.INSUFFICIENT_BALANCE,
          "Insufficient balance for transfer. Available: " + this.balance);
    }
    this.balance = this.balance.subtract(amount);
    this.transactions.add(new Transaction(
        amount, TransactionType.TRANSFER_OUT, this.balance, "Transfer out"));
  }

  public void transferIn(BigDecimal amount) {
    validateAccountIsActive();
    validateNotFixedDeposit();
    validateAmount(amount);
    this.balance = this.balance.add(amount);
    this.transactions.add(new Transaction(
        amount, TransactionType.TRANSFER_IN, this.balance, "Transfer in"));
  }

  private void validate(String ownerName) {
    if (ownerName == null || ownerName.trim().isEmpty()) {
      throw new BankingException(ErrorCode.INVALID_ACCOUNT,
          "Owner name cannot be null or blank");
    }
  }

  private void validateAmount(BigDecimal amount) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new BankingException(ErrorCode.INVALID_AMOUNT,
          "Amount must be greater than zero");
    }
  }

  private void validateAccountIsActive() {
    if (this.accountStatus != AccountStatus.ACTIVE) {
      throw new BankingException(ErrorCode.INVALID_ACCOUNT_STATUS,
          "Cannot perform transaction on a " + this.accountStatus + " account");
    }
  }

  private void validateNotFixedDeposit() {
    if (this.accountType == AccountType.FIXED_DEPOSIT) {
      throw new BankingException(ErrorCode.INVALID_ACCOUNT_STATUS,
          "Withdrawals not allowed on Fixed Deposit accounts before maturity");
    }
  }

  private void validateInitialDeposit(AccountType accountType, BigDecimal amount) {
    validateAmount(amount);
    BigDecimal minimumDeposit = getMinimumDeposit(accountType);
    if (amount.compareTo(minimumDeposit) < 0) {
      throw new BankingException(ErrorCode.INVALID_AMOUNT,
          "Minimum initial deposit for " + accountType +
              " account is " + minimumDeposit);
    }
  }

  private BigDecimal getMinimumDeposit(AccountType accountType) {
    return switch (accountType) {
      case SAVINGS -> new BigDecimal("1.00");
      case CURRENT -> new BigDecimal("10000.00");
      case FIXED_DEPOSIT -> new BigDecimal("1000.00");
    };
  }

  private String generateAccountNumber() {
    return "ACC" + UUID.randomUUID().toString().substring(0, 13).toUpperCase();
  }

  public String getAccountId() {
    return accountId;
  }

  public String getAccountNumber() {
    return accountNumber;
  }

  public String getOwnerName() {
    return ownerName;
  }

  public AccountType getAccountType() {
    return accountType;
  }

  public BigDecimal getBalance() {
    return balance;
  }

  public AccountStatus getAccountStatus() {
    return accountStatus;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public List<Transaction> getTransactions() {
    return Collections.unmodifiableList(transactions);
  }

  public String getOwnerId() {
    return ownerId;
  }
}
package com.banking.account.service;

import com.banking.account.domain.Account;
import com.banking.account.domain.AccountType;
import com.banking.account.dto.response.AccountResponse;
import com.banking.account.entity.AccountEntity;
import com.banking.account.kafka.TransactionEvent;
import com.banking.account.kafka.TransactionEventProducer;
import com.banking.account.mapper.AccountMapper;
import com.banking.account.repository.AccountRepository;
import com.banking.common.exception.BankingException;
import com.banking.common.exception.ErrorCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final NotificationService notificationService;
    private final TransactionEventProducer eventProducer;
    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);

    public AccountService(AccountRepository accountRepository,
            NotificationService notificationService,
            TransactionEventProducer eventProducer) {
        this.accountRepository = accountRepository;
        this.notificationService = notificationService;
        this.eventProducer = eventProducer;
    }

    @Transactional
    public Account createAccount(String ownerName, AccountType accountType,
            BigDecimal initialDeposit, String ownerId) {
        Account account = new Account(ownerName, accountType, initialDeposit);
        AccountEntity entity = AccountMapper.toEntity(account);
        entity.setOwnerId(ownerId);
        AccountEntity saved = accountRepository.save(entity);
        return AccountMapper.toDomain(saved);
    }

    @Transactional
    @CacheEvict(value = "accounts", key = "#accountId")
    public Account deposit(String accountId, BigDecimal amount) {
        AccountEntity entity = accountRepository.findById(accountId)
                .orElseThrow(() -> new BankingException(
                        ErrorCode.ACCOUNT_NOT_FOUND, "Account not found: " + accountId));

        Account account = AccountMapper.toDomain(entity);
        account.deposit(amount);

        entity.setBalance(account.getBalance());
        entity.setAccountStatus(account.getAccountStatus());
        AccountEntity saved = accountRepository.save(entity);
        Account result = AccountMapper.toDomain(saved);

        try {
            notificationService.sendDepositNotification(accountId, amount);
        } catch (Exception e) {
            logger.warn("Failed to send deposit notification for account {}: {}",
                    accountId, e.getMessage());
        }

        eventProducer.publish(new TransactionEvent(
                UUID.randomUUID().toString(),
                accountId,
                TransactionEvent.EventType.DEPOSIT,
                amount,
                result.getBalance(),
                "Deposit of " + amount));

        return result;
    }

    @Transactional
    @CacheEvict(value = "accounts", key = "#accountId")
    public Account withdraw(String accountId, BigDecimal amount) {
        AccountEntity entity = accountRepository.findById(accountId)
                .orElseThrow(() -> new BankingException(
                        ErrorCode.ACCOUNT_NOT_FOUND, "Account not found: " + accountId));

        Account account = AccountMapper.toDomain(entity);
        account.withdraw(amount);

        entity.setBalance(account.getBalance());
        entity.setAccountStatus(account.getAccountStatus());
        AccountEntity saved = accountRepository.save(entity);
        Account result = AccountMapper.toDomain(saved);

        try {
            notificationService.sendWithdrawalNotification(accountId, amount);
        } catch (Exception e) {
            logger.warn("Failed to send withdrawal notification for account {}: {}",
                    accountId, e.getMessage());
        }

        eventProducer.publish(new TransactionEvent(
                UUID.randomUUID().toString(),
                accountId,
                TransactionEvent.EventType.WITHDRAWAL,
                amount,
                result.getBalance(),
                "Withdrawal of " + amount));

        return result;
    }

    @Transactional
    @CacheEvict(value = "accounts", allEntries = true)
    public List<Account> transfer(String fromAccountId, String toAccountId, BigDecimal amount) {
        if (fromAccountId.equals(toAccountId)) {
            throw new BankingException(ErrorCode.INVALID_OPERATION, "Cannot transfer to the same account");
        }

        AccountEntity fromEntity = accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new BankingException(
                        ErrorCode.ACCOUNT_NOT_FOUND, "Account not found: " + fromAccountId));
        AccountEntity toEntity = accountRepository.findById(toAccountId)
                .orElseThrow(() -> new BankingException(
                        ErrorCode.ACCOUNT_NOT_FOUND, "Account not found: " + toAccountId));

        Account fromAccount = AccountMapper.toDomain(fromEntity);
        Account toAccount = AccountMapper.toDomain(toEntity);

        fromAccount.transferOut(amount);
        toAccount.transferIn(amount);

        fromEntity.setBalance(fromAccount.getBalance());
        fromEntity.setAccountStatus(fromAccount.getAccountStatus());

        toEntity.setBalance(toAccount.getBalance());
        toEntity.setAccountStatus(toAccount.getAccountStatus());

        AccountEntity savedFrom = accountRepository.save(fromEntity);
        AccountEntity savedTo = accountRepository.save(toEntity);

        Account resultFrom = AccountMapper.toDomain(savedFrom);
        Account resultTo = AccountMapper.toDomain(savedTo);

        try {
            notificationService.sendTransferNotification(fromAccountId, toAccountId, amount);
        } catch (Exception e) {
            logger.warn("Failed to send transfer notification: {}", e.getMessage());
        }

        eventProducer.publish(new TransactionEvent(
                UUID.randomUUID().toString(), fromAccountId,
                TransactionEvent.EventType.TRANSFER_OUT, amount,
                resultFrom.getBalance(), "Transfer to " + toAccountId));

        eventProducer.publish(new TransactionEvent(
                UUID.randomUUID().toString(), toAccountId,
                TransactionEvent.EventType.TRANSFER_IN, amount,
                resultTo.getBalance(), "Transfer from " + fromAccountId));

        return List.of(resultFrom, resultTo);
    }

    @Cacheable(value = "accounts", key = "#accountId")
    @Transactional(readOnly = true)
    public Account getAccount(String accountId) {
        return findAccountById(accountId);
    }

    @Transactional(readOnly = true)
    public Page<AccountResponse> getAllAccounts(Pageable pageable) {
        return accountRepository.findAll(pageable)
                .map(AccountMapper::toDomain)
                .map(AccountResponse::from);
    }

    @Transactional
    @CacheEvict(value = "accounts", key = "#accountId")
    public void blockAccount(String accountId) {
        AccountEntity entity = accountRepository.findById(accountId)
                .orElseThrow(() -> new BankingException(
                        ErrorCode.ACCOUNT_NOT_FOUND, "Account not found: " + accountId));
        Account account = AccountMapper.toDomain(entity);
        account.block();
        entity.setAccountStatus(account.getAccountStatus());
        accountRepository.save(entity);
    }

    @Transactional
    @CacheEvict(value = "accounts", key = "#accountId")
    public void unblockAccount(String accountId) {
        AccountEntity entity = accountRepository.findById(accountId)
                .orElseThrow(() -> new BankingException(
                        ErrorCode.ACCOUNT_NOT_FOUND, "Account not found: " + accountId));
        Account account = AccountMapper.toDomain(entity);
        account.activate();
        entity.setAccountStatus(account.getAccountStatus());
        accountRepository.save(entity);
    }

    private Account findAccountById(String accountId) {
        return accountRepository.findById(accountId)
                .map(AccountMapper::toDomain)
                .orElseThrow(() -> new BankingException(
                        ErrorCode.ACCOUNT_NOT_FOUND,
                        "Account not found: " + accountId));
    }
}

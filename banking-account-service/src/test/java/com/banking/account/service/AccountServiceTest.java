package com.banking.account.service;

import com.banking.account.domain.Account;
import com.banking.account.domain.AccountStatus;
import com.banking.account.domain.AccountType;
import com.banking.account.entity.AccountEntity;
import com.banking.account.kafka.TransactionEventProducer;
import com.banking.account.repository.AccountRepository;
import com.banking.common.exception.BankingException;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private TransactionEventProducer eventProducer;

    @InjectMocks
    private AccountService accountService;

    private AccountEntity buildEntity(String owner, BigDecimal balance) {
        AccountEntity entity = new AccountEntity();
        entity.setAccountId(UUID.randomUUID().toString());
        entity.setAccountNumber("ACC" + UUID.randomUUID().toString()
                .substring(0, 8).toUpperCase());
        entity.setOwnerName(owner);
        entity.setAccountType(AccountType.SAVINGS);
        entity.setBalance(balance);
        entity.setAccountStatus(AccountStatus.ACTIVE);
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }

    @Test
    void shouldCreateAccountAndSaveIt() {
        AccountEntity entity = buildEntity("John", new BigDecimal("1000.00"));
        when(accountRepository.save(any(AccountEntity.class))).thenReturn(entity);

        Account created = accountService.createAccount(
                "John", AccountType.SAVINGS, new BigDecimal("1000.00"), "user-123");

        assertNotNull(created);
        assertEquals("John", created.getOwnerName());
        verify(accountRepository, times(1)).save(any(AccountEntity.class));
    }

    @Test
    void shouldDepositMoneyInAccount() {
        AccountEntity entity = buildEntity("John", new BigDecimal("1000.00"));
        when(accountRepository.findById(entity.getAccountId()))
                .thenReturn(Optional.of(entity));
        when(accountRepository.save(any(AccountEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Account result = accountService.deposit(
                entity.getAccountId(), new BigDecimal("500.00"));

        assertEquals(0, result.getBalance().compareTo(new BigDecimal("1500.00")));
        verify(accountRepository, times(1)).save(any(AccountEntity.class));
        verify(eventProducer, times(1)).publish(any());
    }

    @Test
    void shouldWithdrawMoneyFromAccount() {
        AccountEntity entity = buildEntity("John", new BigDecimal("1000.00"));
        when(accountRepository.findById(entity.getAccountId()))
                .thenReturn(Optional.of(entity));
        when(accountRepository.save(any(AccountEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Account result = accountService.withdraw(
                entity.getAccountId(), new BigDecimal("200.00"));

        assertEquals(0, result.getBalance().compareTo(new BigDecimal("800.00")));
        verify(accountRepository, times(1)).save(any(AccountEntity.class));
        verify(eventProducer, times(1)).publish(any());
    }

    @Test
    void shouldReturnAccountById() {
        AccountEntity entity = buildEntity("John", new BigDecimal("1000.00"));
        when(accountRepository.findById(entity.getAccountId()))
                .thenReturn(Optional.of(entity));

        Account found = accountService.getAccount(entity.getAccountId());

        assertEquals("John", found.getOwnerName());
        verify(accountRepository, times(1)).findById(entity.getAccountId());
    }

    @Test
    void shouldThrowBankingExceptionWhenAccountNotFound() {
        when(accountRepository.findById("bad-id")).thenReturn(Optional.empty());

        assertThrows(BankingException.class,
                () -> accountService.deposit("bad-id", new BigDecimal("500.00")));
    }

    @Test
    void shouldThrowBankingExceptionWhenInsufficientBalance() {
        AccountEntity entity = buildEntity("John", new BigDecimal("1000.00"));
        when(accountRepository.findById(entity.getAccountId()))
                .thenReturn(Optional.of(entity));

        assertThrows(BankingException.class,
                () -> accountService.withdraw(
                        entity.getAccountId(), new BigDecimal("1500.00")));
    }

    @Test
    void shouldPublishKafkaEventAfterDeposit() {
        AccountEntity entity = buildEntity("John", new BigDecimal("1000.00"));
        when(accountRepository.findById(entity.getAccountId()))
                .thenReturn(Optional.of(entity));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        accountService.deposit(entity.getAccountId(), new BigDecimal("500.00"));

        verify(eventProducer, times(1)).publish(any());
    }

    @Test
    void shouldPublishKafkaEventAfterWithdrawal() {
        AccountEntity entity = buildEntity("John", new BigDecimal("1000.00"));
        when(accountRepository.findById(entity.getAccountId()))
                .thenReturn(Optional.of(entity));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        accountService.withdraw(entity.getAccountId(), new BigDecimal("200.00"));

        verify(eventProducer, times(1)).publish(any());
    }

    @Nested
    class TransferTest {

        private AccountEntity buildEntity(String owner, BigDecimal balance) {
            AccountEntity e = new AccountEntity();
            e.setAccountId(UUID.randomUUID().toString());
            e.setAccountNumber("ACC" + UUID.randomUUID().toString()
                    .substring(0, 8).toUpperCase());
            e.setOwnerName(owner);
            e.setAccountType(AccountType.SAVINGS);
            e.setBalance(balance);
            e.setAccountStatus(AccountStatus.ACTIVE);
            e.setCreatedAt(LocalDateTime.now());
            return e;
        }

        @Test
        void shouldTransferBetweenTwoAccounts() {
            AccountEntity from = buildEntity("Alice", new BigDecimal("1000.00"));
            AccountEntity to = buildEntity("Bob", new BigDecimal("500.00"));

            when(accountRepository.findById(from.getAccountId()))
                    .thenReturn(Optional.of(from));
            when(accountRepository.findById(to.getAccountId()))
                    .thenReturn(Optional.of(to));
            when(accountRepository.save(any()))
                    .thenAnswer(inv -> inv.getArgument(0));

            var result = accountService.transfer(
                    from.getAccountId(), to.getAccountId(), new BigDecimal("200.00"));

            assertEquals(2, result.size());
            assertEquals(0, new BigDecimal("800.00").compareTo(result.get(0).getBalance()));
            assertEquals(0, new BigDecimal("700.00").compareTo(result.get(1).getBalance()));
            verify(eventProducer, times(2)).publish(any());
        }

        @Test
        void shouldThrowWhenTransferringToSameAccount() {
            assertThrows(BankingException.class,
                    () -> accountService.transfer(
                            "same-id", "same-id", new BigDecimal("100.00")));
        }

        @Test
        void shouldThrowWhenFromAccountNotFound() {
            when(accountRepository.findById("bad-id")).thenReturn(Optional.empty());

            assertThrows(BankingException.class,
                    () -> accountService.transfer(
                            "bad-id", "other-id", new BigDecimal("100.00")));
        }

        @Test
        void shouldThrowWhenToAccountNotFound() {
            AccountEntity from = buildEntity("Alice", new BigDecimal("1000.00"));
            when(accountRepository.findById(from.getAccountId()))
                    .thenReturn(Optional.of(from));
            when(accountRepository.findById("bad-id")).thenReturn(Optional.empty());

            assertThrows(BankingException.class,
                    () -> accountService.transfer(
                            from.getAccountId(), "bad-id", new BigDecimal("100.00")));
        }
    }
}
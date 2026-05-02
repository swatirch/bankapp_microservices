package com.banking.account.service;

import com.banking.account.domain.AccountStatus;
import com.banking.account.domain.AccountType;
import com.banking.account.entity.AccountEntity;
import com.banking.account.kafka.TransactionEventProducer;
import com.banking.account.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private TransactionEventProducer eventProducer; // ← required: prevents NPE on publish()

    @InjectMocks
    private AccountService accountService;

    private AccountEntity stubEntity(String accountId) {
        AccountEntity entity = new AccountEntity();
        entity.setAccountId(accountId);
        entity.setAccountNumber("ACC-001");
        entity.setOwnerName("Test User");
        entity.setAccountType(AccountType.SAVINGS);
        entity.setBalance(new BigDecimal("1000.00"));
        entity.setAccountStatus(AccountStatus.ACTIVE);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setOwnerId("user-123");
        return entity;
    }

    @Test
    void deposit_shouldTriggerDepositNotification() {
        String accountId = "acc-123";
        BigDecimal amount = new BigDecimal("500.00");
        AccountEntity entity = stubEntity(accountId);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(entity));
        when(accountRepository.save(any())).thenReturn(entity);

        accountService.deposit(accountId, amount);

        verify(notificationService, times(1)).sendDepositNotification(accountId, amount);
    }

    @Test
    void withdraw_shouldTriggerWithdrawalNotification() {
        String accountId = "acc-123";
        BigDecimal amount = new BigDecimal("100.00");
        AccountEntity entity = stubEntity(accountId);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(entity));
        when(accountRepository.save(any())).thenReturn(entity);

        accountService.withdraw(accountId, amount);

        verify(notificationService, times(1)).sendWithdrawalNotification(accountId, amount);
    }

    @Test
    void deposit_whenNotificationFails_shouldStillCompleteDeposit() {
        String accountId = "acc-123";
        BigDecimal amount = new BigDecimal("500.00");
        AccountEntity entity = stubEntity(accountId);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(entity));
        when(accountRepository.save(any())).thenReturn(entity);

        // notification throws — simulates SMS gateway down
        doThrow(new RuntimeException("SMS gateway down"))
                .when(notificationService)
                .sendDepositNotification(any(), any());

        // deposit must NOT throw — notification failure is non-fatal
        assertThatCode(() -> accountService.deposit(accountId, amount))
                .doesNotThrowAnyException();
    }
}
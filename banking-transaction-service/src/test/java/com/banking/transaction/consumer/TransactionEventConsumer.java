package com.banking.transaction.consumer;

import com.banking.transaction.domain.TransactionType;
import com.banking.transaction.entity.TransactionEntity;
import com.banking.transaction.kafka.TransactionEvent;
import com.banking.transaction.kafka.TransactionEventConsumer;
import com.banking.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionEventConsumerTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionEventConsumer consumer;

    private TransactionEvent buildEvent(String eventId, String accountId,
            TransactionEvent.EventType type, BigDecimal amount, BigDecimal balanceAfter) {
        TransactionEvent event = new TransactionEvent();
        event.setEventId(eventId);
        event.setAccountId(accountId);
        event.setEventType(type);
        event.setAmount(amount);
        event.setBalanceAfter(balanceAfter);
        event.setDescription(type.name() + " of " + amount);
        event.setTimestamp(LocalDateTime.now());
        return event;
    }

    @Test
    void shouldSaveTransactionWhenDepositEventReceived() {
        TransactionEvent event = buildEvent(
                "evt-001", "acc-123",
                TransactionEvent.EventType.DEPOSIT,
                new BigDecimal("500.00"),
                new BigDecimal("1500.00"));

        when(transactionRepository.existsById("evt-001")).thenReturn(false);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        consumer.consume(event);

        ArgumentCaptor<TransactionEntity> captor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(transactionRepository, times(1)).save(captor.capture());

        TransactionEntity saved = captor.getValue();
        assertEquals("evt-001", saved.getTransactionId());
        assertEquals("acc-123", saved.getAccountId());
        assertEquals(TransactionType.DEPOSIT, saved.getType());
        assertEquals(0, new BigDecimal("500.00").compareTo(saved.getAmount()));
        assertEquals(0, new BigDecimal("1500.00").compareTo(saved.getBalanceAfter()));
    }

    @Test
    void shouldSaveTransactionWhenWithdrawalEventReceived() {
        TransactionEvent event = buildEvent(
                "evt-002", "acc-123",
                TransactionEvent.EventType.WITHDRAWAL,
                new BigDecimal("200.00"),
                new BigDecimal("800.00"));

        when(transactionRepository.existsById("evt-002")).thenReturn(false);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        consumer.consume(event);

        ArgumentCaptor<TransactionEntity> captor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(transactionRepository, times(1)).save(captor.capture());

        TransactionEntity saved = captor.getValue();
        assertEquals(TransactionType.WITHDRAWAL, saved.getType());
        assertEquals(0, new BigDecimal("200.00").compareTo(saved.getAmount()));
    }

    @Test
    void shouldSaveTransactionWhenTransferOutEventReceived() {
        TransactionEvent event = buildEvent(
                "evt-003", "acc-123",
                TransactionEvent.EventType.TRANSFER_OUT,
                new BigDecimal("300.00"),
                new BigDecimal("700.00"));

        when(transactionRepository.existsById("evt-003")).thenReturn(false);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        consumer.consume(event);

        ArgumentCaptor<TransactionEntity> captor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(transactionRepository, times(1)).save(captor.capture());

        assertEquals(TransactionType.TRANSFER_OUT, captor.getValue().getType());
    }

    @Test
    void shouldSaveTransactionWhenTransferInEventReceived() {
        TransactionEvent event = buildEvent(
                "evt-004", "acc-456",
                TransactionEvent.EventType.TRANSFER_IN,
                new BigDecimal("300.00"),
                new BigDecimal("800.00"));

        when(transactionRepository.existsById("evt-004")).thenReturn(false);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        consumer.consume(event);

        ArgumentCaptor<TransactionEntity> captor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(transactionRepository, times(1)).save(captor.capture());

        assertEquals(TransactionType.TRANSFER_IN, captor.getValue().getType());
        assertEquals("acc-456", captor.getValue().getAccountId());
    }

    @Test
    void shouldSkipDuplicateEvent() {
        TransactionEvent event = buildEvent(
                "evt-001", "acc-123",
                TransactionEvent.EventType.DEPOSIT,
                new BigDecimal("500.00"),
                new BigDecimal("1500.00"));

        // already exists in DB
        when(transactionRepository.existsById("evt-001")).thenReturn(true);

        consumer.consume(event);

        // must NOT save again
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void shouldUseCurrentTimestampWhenEventTimestampIsNull() {
        TransactionEvent event = buildEvent(
                "evt-005", "acc-123",
                TransactionEvent.EventType.DEPOSIT,
                new BigDecimal("100.00"),
                new BigDecimal("1100.00"));
        event.setTimestamp(null); // simulate missing timestamp

        when(transactionRepository.existsById("evt-005")).thenReturn(false);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        consumer.consume(event);

        ArgumentCaptor<TransactionEntity> captor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(transactionRepository).save(captor.capture());

        assertNotNull(captor.getValue().getTimestamp());
    }

    @Test
    void shouldSaveCorrectDescriptionFromEvent() {
        TransactionEvent event = buildEvent(
                "evt-006", "acc-123",
                TransactionEvent.EventType.DEPOSIT,
                new BigDecimal("500.00"),
                new BigDecimal("1500.00"));
        event.setDescription("Deposit of 500.00");

        when(transactionRepository.existsById("evt-006")).thenReturn(false);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        consumer.consume(event);

        ArgumentCaptor<TransactionEntity> captor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(transactionRepository).save(captor.capture());

        assertEquals("Deposit of 500.00", captor.getValue().getDescription());
    }
}
package com.banking.notification.consumer;

import com.banking.notification.kafka.NotificationEventConsumer;
import com.banking.notification.kafka.TransactionEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
class NotificationEventConsumerTest {

    @InjectMocks
    private NotificationEventConsumer consumer;

    private TransactionEvent buildEvent(String accountId,
            TransactionEvent.EventType type, BigDecimal amount) {
        TransactionEvent event = new TransactionEvent();
        event.setEventId("evt-" + System.nanoTime());
        event.setAccountId(accountId);
        event.setEventType(type);
        event.setAmount(amount);
        event.setBalanceAfter(new BigDecimal("1500.00"));
        event.setDescription(type.name() + " of " + amount);
        event.setTimestamp(LocalDateTime.now());
        return event;
    }

    @Test
    void shouldHandleDepositEventWithoutThrowing() {
        TransactionEvent event = buildEvent("acc-123",
                TransactionEvent.EventType.DEPOSIT,
                new BigDecimal("500.00"));

        assertThatCode(() -> consumer.consume(event))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldHandleWithdrawalEventWithoutThrowing() {
        TransactionEvent event = buildEvent("acc-123",
                TransactionEvent.EventType.WITHDRAWAL,
                new BigDecimal("200.00"));

        assertThatCode(() -> consumer.consume(event))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldHandleTransferInEventWithoutThrowing() {
        TransactionEvent event = buildEvent("acc-456",
                TransactionEvent.EventType.TRANSFER_IN,
                new BigDecimal("300.00"));

        assertThatCode(() -> consumer.consume(event))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldHandleTransferOutEventWithoutThrowing() {
        TransactionEvent event = buildEvent("acc-123",
                TransactionEvent.EventType.TRANSFER_OUT,
                new BigDecimal("300.00"));

        assertThatCode(() -> consumer.consume(event))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldHandleNullTimestampWithoutThrowing() {
        TransactionEvent event = buildEvent("acc-123",
                TransactionEvent.EventType.DEPOSIT,
                new BigDecimal("500.00"));
        event.setTimestamp(null);

        assertThatCode(() -> consumer.consume(event))
                .doesNotThrowAnyException();
    }
}
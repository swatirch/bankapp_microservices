package com.banking.notification.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class NotificationEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(NotificationEventConsumer.class);

    @KafkaListener(topics = "banking.transactions", groupId = "notification-service-group", containerFactory = "kafkaListenerContainerFactory")
    public void consume(TransactionEvent event) {
        logger.info("Received {} event for account {} — amount: {} balance: {}",
                event.getEventType(),
                event.getAccountId(),
                event.getAmount(),
                event.getBalanceAfter());

        switch (event.getEventType()) {
            case DEPOSIT -> sendDepositNotification(event);
            case WITHDRAWAL -> sendWithdrawalNotification(event);
            case TRANSFER_IN -> sendTransferInNotification(event);
            case TRANSFER_OUT -> sendTransferOutNotification(event);
            default -> logger.warn("Unknown event type: {}", event.getEventType());
        }
    }

    private void sendDepositNotification(TransactionEvent event) {
        logger.info("[SMS] Account {}: ₹{} deposited. Available balance: ₹{}",
                event.getAccountId(), event.getAmount(), event.getBalanceAfter());
    }

    private void sendWithdrawalNotification(TransactionEvent event) {
        logger.info("[SMS] Account {}: ₹{} withdrawn. Available balance: ₹{}",
                event.getAccountId(), event.getAmount(), event.getBalanceAfter());
    }

    private void sendTransferInNotification(TransactionEvent event) {
        logger.info("[SMS] Account {}: ₹{} received. Available balance: ₹{}",
                event.getAccountId(), event.getAmount(), event.getBalanceAfter());
    }

    private void sendTransferOutNotification(TransactionEvent event) {
        logger.info("[SMS] Account {}: ₹{} transferred out. Available balance: ₹{}",
                event.getAccountId(), event.getAmount(), event.getBalanceAfter());
    }
}
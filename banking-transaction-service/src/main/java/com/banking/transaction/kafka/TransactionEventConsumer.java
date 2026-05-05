package com.banking.transaction.kafka;

import com.banking.transaction.domain.TransactionType;
import com.banking.transaction.entity.TransactionEntity;
import com.banking.transaction.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TransactionEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(TransactionEventConsumer.class);

    private final TransactionRepository transactionRepository;

    public TransactionEventConsumer(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @KafkaListener(topics = "banking.transactions", groupId = "transaction-service-group", containerFactory = "kafkaListenerContainerFactory")
    public void consume(TransactionEvent event) {
        logger.info("Received {} event for account {} amount {}",
                event.getEventType(), event.getAccountId(), event.getAmount());

        // idempotency check — don't save duplicates
        if (transactionRepository.existsById(event.getEventId())) {
            logger.warn("Duplicate event {} — skipping", event.getEventId());
            return;
        }

        TransactionEntity entity = new TransactionEntity();
        entity.setTransactionId(event.getEventId());
        entity.setAccountId(event.getAccountId());
        entity.setType(TransactionType.valueOf(event.getEventType().name()));
        entity.setAmount(event.getAmount());
        entity.setBalanceAfter(event.getBalanceAfter());
        entity.setDescription(event.getDescription());
        entity.setTimestamp(event.getTimestamp() != null
                ? event.getTimestamp()
                : LocalDateTime.now());

        transactionRepository.save(entity);
        logger.info("Saved transaction {} for account {}",
                entity.getTransactionId(), entity.getAccountId());
    }
}
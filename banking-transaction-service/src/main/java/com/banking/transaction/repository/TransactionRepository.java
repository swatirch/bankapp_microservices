package com.banking.transaction.repository;

import com.banking.transaction.entity.TransactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<TransactionEntity, String> {

    Page<TransactionEntity> findByAccountIdOrderByTimestampDesc(
            String accountId, Pageable pageable);
}
package com.banking.account.repository;

import com.banking.account.domain.AccountStatus;
import com.banking.account.domain.AccountType;
import com.banking.account.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<AccountEntity, String> {
    Optional<AccountEntity> findByAccountNumber(String accountNumber);

    // keep the List version for InterestCalculationService (if any)
    List<AccountEntity> findByAccountTypeAndAccountStatus(
            AccountType accountType, AccountStatus status);

    // add this Pageable version for Spring Batch reader
    Page<AccountEntity> findByAccountTypeAndAccountStatus(
            AccountType accountType, AccountStatus status, Pageable pageable);
}
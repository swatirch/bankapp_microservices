package com.banking.account.mapper;

import com.banking.account.domain.Account;
import com.banking.account.entity.AccountEntity;

public class AccountMapper {

    // Domain → Entity (before saving to DB)
    public static AccountEntity toEntity(Account account) {
        AccountEntity entity = new AccountEntity();
        entity.setAccountId(account.getAccountId());
        entity.setAccountNumber(account.getAccountNumber());
        entity.setOwnerName(account.getOwnerName());
        entity.setAccountType(account.getAccountType());
        entity.setBalance(account.getBalance());
        entity.setAccountStatus(account.getAccountStatus());
        entity.setCreatedAt(account.getCreatedAt());
        entity.setOwnerId(account.getOwnerId());
        return entity;
    }

    // Entity → Domain (after loading from DB)
    public static Account toDomain(AccountEntity entity) {
        return Account.reconstitute(
                entity.getAccountId(),
                entity.getAccountNumber(),
                entity.getOwnerName(),
                entity.getAccountType(),
                entity.getBalance(),
                entity.getAccountStatus(),
                entity.getCreatedAt(),
                java.util.List.of(), // ← no transactions here, Kafka handles that
                entity.getOwnerId()
        );
    }

    private AccountMapper() {
    }
}
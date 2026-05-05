package com.banking.account.domain;

import com.banking.common.exception.BankingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    @Nested
    class SavingsAccountTest {

        private Account account;

        @BeforeEach
        void setUp() {
            account = new Account("John", AccountType.SAVINGS,
                    new BigDecimal("1000.00"));
        }

        @Test
        void shouldCreateSavingsAccountWithCorrectDetails() {
            assertEquals("John", account.getOwnerName());
            assertEquals(AccountType.SAVINGS, account.getAccountType());
            assertEquals(AccountStatus.ACTIVE, account.getAccountStatus());
            assertEquals(new BigDecimal("1000.00"), account.getBalance());
            assertNotNull(account.getAccountId());
            assertNotNull(account.getAccountNumber());
            assertNotNull(account.getCreatedAt());
        }

        @Test
        void shouldAllowOpeningSavingsAccountWithMinimumOneRupee() {
            Account minAccount = new Account("John", AccountType.SAVINGS,
                    new BigDecimal("1.00"));
            assertEquals(new BigDecimal("1.00"), minAccount.getBalance());
        }

        @Test
        void shouldNotAllowOpeningSavingsAccountWithZeroDeposit() {
            assertThrows(BankingException.class, () -> new Account("John", AccountType.SAVINGS, BigDecimal.ZERO));
        }

        @Test
        void shouldDepositMoney() {
            account.deposit(new BigDecimal("500.00"));
            assertEquals(new BigDecimal("1500.00"), account.getBalance());
        }

        @Test
        void shouldWithdrawMoney() {
            account.withdraw(new BigDecimal("200.00"));
            assertEquals(new BigDecimal("800.00"), account.getBalance());
        }

        @Test
        void shouldThrowExceptionWhenInsufficientBalance() {
            assertThrows(BankingException.class, () -> account.withdraw(new BigDecimal("2000.00")));
        }

        @Test
        void shouldAllowWithdrawingExactBalance() {
            account.withdraw(new BigDecimal("1000.00"));
            assertEquals(new BigDecimal("0.00"), account.getBalance());
        }

        @Test
        void shouldThrowExceptionWhenTransactingOnBlockedAccount() {
            account.block();
            assertThrows(BankingException.class, () -> account.deposit(new BigDecimal("500.00")));
        }

        @Test
        void shouldThrowExceptionWhenTransactingOnInactiveAccount() {
            account.deactivate();
            assertThrows(BankingException.class, () -> account.deposit(new BigDecimal("500.00")));
        }

        @Test
        void shouldRecordInitialDepositAsFirstTransaction() {
            assertEquals(1, account.getTransactions().size());
            assertEquals(TransactionType.DEPOSIT,
                    account.getTransactions().get(0).getType());
            assertEquals("Initial deposit",
                    account.getTransactions().get(0).getDescription());
        }

        @Test
        void shouldRecordTransactionAfterDeposit() {
            account.deposit(new BigDecimal("500.00"));
            assertEquals(2, account.getTransactions().size());
            assertEquals(TransactionType.DEPOSIT,
                    account.getTransactions().get(1).getType());
        }

        @Test
        void shouldRecordTransactionAfterWithdrawal() {
            account.withdraw(new BigDecimal("200.00"));
            assertEquals(2, account.getTransactions().size());
            assertEquals(TransactionType.WITHDRAWAL,
                    account.getTransactions().get(1).getType());
        }
    }

    @Nested
    class CurrentAccountTest {

        @Test
        void shouldCreateCurrentAccountWithMinimumDeposit() {
            Account account = new Account("Business Ltd",
                    AccountType.CURRENT, new BigDecimal("10000.00"));
            assertEquals(new BigDecimal("10000.00"), account.getBalance());
        }

        @Test
        void shouldNotAllowOpeningCurrentAccountBelowMinimum() {
            assertThrows(BankingException.class, () -> new Account("Business Ltd", AccountType.CURRENT,
                    new BigDecimal("5000.00")));
        }
    }

    @Nested
    class FixedDepositAccountTest {

        @Test
        void shouldCreateFixedDepositAccountWithMinimumDeposit() {
            Account account = new Account("John",
                    AccountType.FIXED_DEPOSIT, new BigDecimal("1000.00"));
            assertEquals(new BigDecimal("1000.00"), account.getBalance());
        }

        @Test
        void shouldNotAllowOpeningFixedDepositAccountBelowMinimum() {
            assertThrows(BankingException.class, () -> new Account("John", AccountType.FIXED_DEPOSIT,
                    new BigDecimal("500.00")));
        }

        @Test
        void shouldNotAllowWithdrawalFromFixedDepositAccount() {
            Account fdAccount = new Account("John",
                    AccountType.FIXED_DEPOSIT, new BigDecimal("1000.00"));
            assertThrows(BankingException.class, () -> fdAccount.withdraw(new BigDecimal("500.00")));
        }
    }

    @Nested
    class AccountValidationTest {

        @Test
        void shouldThrowExceptionWhenNameIsNull() {
            assertThrows(BankingException.class, () -> new Account(null, AccountType.SAVINGS,
                    new BigDecimal("1000.00")));
        }

        @Test
        void shouldThrowExceptionWhenNameIsBlank() {
            assertThrows(BankingException.class, () -> new Account("   ", AccountType.SAVINGS,
                    new BigDecimal("1000.00")));
        }

        @Test
        void shouldThrowExceptionWhenDepositingZero() {
            Account account = new Account("John", AccountType.SAVINGS,
                    new BigDecimal("1000.00"));
            assertThrows(BankingException.class, () -> account.deposit(BigDecimal.ZERO));
        }

        @Test
        void shouldThrowExceptionWhenDepositingNegativeAmount() {
            Account account = new Account("John", AccountType.SAVINGS,
                    new BigDecimal("1000.00"));
            assertThrows(BankingException.class, () -> account.deposit(new BigDecimal("-100.00")));
        }
    }

    @Nested
    class TransferTest {

        private Account sender;
        private Account receiver;

        @BeforeEach
        void setUp() {
            sender = new Account("Alice", AccountType.SAVINGS,
                    new BigDecimal("1000.00"));
            receiver = new Account("Bob", AccountType.SAVINGS,
                    new BigDecimal("500.00"));
        }

        @Test
        void shouldDeductBalanceFromSender() {
            sender.transferOut(new BigDecimal("200.00"));
            assertEquals(0, new BigDecimal("800.00").compareTo(sender.getBalance()));
        }

        @Test
        void shouldAddBalanceToReceiver() {
            receiver.transferIn(new BigDecimal("300.00"));
            assertEquals(0, new BigDecimal("800.00").compareTo(receiver.getBalance()));
        }

        @Test
        void shouldRecordTransferOutTransaction() {
            sender.transferOut(new BigDecimal("200.00"));
            assertEquals(TransactionType.TRANSFER_OUT,
                    sender.getTransactions().get(1).getType());
        }

        @Test
        void shouldRecordTransferInTransaction() {
            receiver.transferIn(new BigDecimal("300.00"));
            assertEquals(TransactionType.TRANSFER_IN,
                    receiver.getTransactions().get(1).getType());
        }

        @Test
        void shouldThrowWhenInsufficientBalanceForTransfer() {
            assertThrows(BankingException.class,
                    () -> sender.transferOut(new BigDecimal("2000.00")));
        }

        @Test
        void shouldThrowWhenSenderIsBlocked() {
            sender.block();
            assertThrows(BankingException.class,
                    () -> sender.transferOut(new BigDecimal("200.00")));
        }

        @Test
        void shouldThrowWhenReceiverIsBlocked() {
            receiver.block();
            assertThrows(BankingException.class,
                    () -> receiver.transferIn(new BigDecimal("200.00")));
        }

        @Test
        void shouldThrowWhenSenderIsFixedDeposit() {
            Account fd = new Account("Alice", AccountType.FIXED_DEPOSIT,
                    new BigDecimal("1000.00"));
            assertThrows(BankingException.class,
                    () -> fd.transferOut(new BigDecimal("200.00")));
        }

        @Test
        void shouldThrowWhenReceiverIsFixedDeposit() {
            Account fd = new Account("Bob", AccountType.FIXED_DEPOSIT,
                    new BigDecimal("1000.00"));
            assertThrows(BankingException.class,
                    () -> fd.transferIn(new BigDecimal("200.00")));
        }
    }
}
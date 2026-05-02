package com.banking.common.exception;

public class SameAccountTransferException extends BankingException {
    public SameAccountTransferException(String message) {
        super(ErrorCode.SAME_ACCOUNT_TRANSFER, message);
    }
}
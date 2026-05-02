package com.banking.common.exception;

public class InvalidAmountException extends BankingException{

    public InvalidAmountException(String message){
        super(ErrorCode.INVALID_AMOUNT,message);
    }
}

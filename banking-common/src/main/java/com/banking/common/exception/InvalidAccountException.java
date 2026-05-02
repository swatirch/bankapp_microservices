package com.banking.common.exception;

public class InvalidAccountException extends BankingException{

    public InvalidAccountException(String message){
        super(ErrorCode.INVALID_ACCOUNT,message);
    }
}

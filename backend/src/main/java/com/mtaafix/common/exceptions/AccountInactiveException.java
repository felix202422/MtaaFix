package com.mtaafix.common.exceptions;

public class AccountInactiveException extends RuntimeException {

    public AccountInactiveException() {
        super("Account is inactive.");
    }
}

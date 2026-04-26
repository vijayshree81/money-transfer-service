package com.mybank.moneytransfer.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public final class AccountNotFoundException extends BusinessException {

	public AccountNotFoundException(String message) {
		super(message, HttpStatus.NOT_FOUND);
	}

	public AccountNotFoundException(String message, HttpStatus httpStatus) {
		super(message, httpStatus);
	}
}

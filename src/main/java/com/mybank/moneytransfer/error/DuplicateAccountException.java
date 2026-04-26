package com.mybank.moneytransfer.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public final class DuplicateAccountException extends BusinessException {

	public DuplicateAccountException(String message) {
		super(message, HttpStatus.CONFLICT);
	}

	public DuplicateAccountException(String message, HttpStatus httpStatus) {
		super(message, httpStatus);
	}
}

package com.mybank.moneytransfer.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public final class OverDraftException extends BusinessException {

	public OverDraftException(String message) {
		super(message);
	}

	public OverDraftException(String message, HttpStatus httpStatus) {
		super(message, httpStatus);
	}
}

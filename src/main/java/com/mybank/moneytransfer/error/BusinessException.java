package com.mybank.moneytransfer.error;

import org.springframework.http.HttpStatus;

public sealed class BusinessException extends RuntimeException
		permits AccountNotFoundException, OverDraftException {

	private final HttpStatus httpStatus;

	public BusinessException(String message) {
		super(message);
		this.httpStatus = HttpStatus.BAD_REQUEST;
	}

	public BusinessException(String message, HttpStatus httpStatus) {
		super(message);
		this.httpStatus = httpStatus;
	}

	public HttpStatus getHttpStatus() {
		return httpStatus;
	}
}

package com.mybank.moneytransfer.error;

import java.util.Date;

public record ExceptionResponse(
		Date timestamp,
		String message,
		String details,
		String httpCodeMessage
) {
}

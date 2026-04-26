package com.mybank.moneytransfer.error;

import java.util.Date;
import java.util.stream.Collectors;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class AppExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public final ResponseEntity<ExceptionResponse> handleBusinessException(final BusinessException ex,
			final WebRequest request) {
		var status = ex.getHttpStatus();
		var exceptionResponse = new ExceptionResponse(new Date(), ex.getMessage(),
				request.getDescription(false), status.getReasonPhrase());
		return new ResponseEntity<>(exceptionResponse, status);
	}

	@ExceptionHandler(ApplicationException.class)
	public final ResponseEntity<ExceptionResponse> handleApplicationException(final ApplicationException ex,
			final WebRequest request) {
		var exceptionResponse = new ExceptionResponse(new Date(), ex.getMessage(),
				request.getDescription(false), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
		return new ResponseEntity<>(exceptionResponse, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		var errors = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.collect(Collectors.joining(", "));
		var exceptionResponse = new ExceptionResponse(new Date(), errors,
				request.getDescription(false), HttpStatus.BAD_REQUEST.getReasonPhrase());
		return new ResponseEntity<>(exceptionResponse, HttpStatus.BAD_REQUEST);
	}
}

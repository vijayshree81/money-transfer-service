package com.mybank.moneytransfer.controller;

import java.math.BigDecimal;
import java.util.Date;
import java.util.NoSuchElementException;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.mybank.moneytransfer.dto.BalanceResult;
import com.mybank.moneytransfer.error.AccountNotFoundException;
import com.mybank.moneytransfer.error.ExceptionResponse;
import com.mybank.moneytransfer.entity.Account;
import com.mybank.moneytransfer.service.AccountManagementService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/mybank/v1/account")
@Tag(name = "AccountManagement", description = "AccountManagement APIs")
public class AccountManagementController {

	private final AccountManagementService accountManagementService;

	public AccountManagementController(AccountManagementService accountManagementService) {
		this.accountManagementService = accountManagementService;
	}

	@GetMapping("/info")
	@Operation(summary = "API to Get Account Info")
	public Account getAccount(@RequestHeader("accountId") final String accountId) {
		try {
			return this.accountManagementService.findByAccountId(accountId);
		} catch (final NoSuchElementException e) {
			throw new AccountNotFoundException("Unable to find Account for id:" + accountId);
		}
	}

	@PostMapping
	@Operation(summary = "API to Create Account")
	public ResponseEntity<?> createAccount(@RequestBody final Account account) {
		if (account.getAccountId() == null || account.getAccountId().length() != 9) {
			var error = new ExceptionResponse(new Date(), "Invalid account ID:" + account.getAccountId(),
					"uri=/mybank/v1/account", HttpStatus.BAD_REQUEST.getReasonPhrase());
			return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
		}
		if (account.getBalance() == null) {
			account.setBalance(new BigDecimal("0"));
		}
		if (account.getBalance().compareTo(BigDecimal.ZERO) < 0) {
			var error = new ExceptionResponse(new Date(), "Account balance can not be negative, value:" + account.getBalance(),
					"uri=/mybank/v1/account", HttpStatus.BAD_REQUEST.getReasonPhrase());
			return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
		}
		this.accountManagementService.createAccount(account);
		return new ResponseEntity<>(account, HttpStatus.CREATED);
	}

	@GetMapping("/balance")
	@Operation(summary = "API to get Account Balance")
	public ResponseEntity<?> checkBalance(@RequestHeader("accountId") String accountId) {
		if (accountId == null || accountId.length() != 9) {
			var error = new ExceptionResponse(new Date(), "Invalid account ID:" + accountId,
					"uri=/mybank/v1/account/" + accountId + "/balance", HttpStatus.BAD_REQUEST.getReasonPhrase());
			return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
		}
		var account = accountManagementService.findByAccountId(accountId);
		var finalBalance = account != null ? account.getBalance() : null;
		var balance = new BalanceResult(accountId, null, finalBalance);
		return ResponseEntity.ok(balance);
	}
}

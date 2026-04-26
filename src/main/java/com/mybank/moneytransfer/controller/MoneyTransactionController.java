package com.mybank.moneytransfer.controller;

import java.util.UUID;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mybank.moneytransfer.dto.TransactionStatus;
import com.mybank.moneytransfer.dto.TransferResult;
import com.mybank.moneytransfer.error.AccountNotFoundException;
import com.mybank.moneytransfer.error.ApplicationException;
import com.mybank.moneytransfer.error.OverDraftException;
import com.mybank.moneytransfer.entity.FundTransferLog;
import com.mybank.moneytransfer.dto.TransferRequest;
import com.mybank.moneytransfer.service.FundTransferLogService;
import com.mybank.moneytransfer.service.MoneyTransactionService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/mybank/v1/transaction")
@Tag(name = "MoneyTransaction", description = "MoneyTransaction APIs")
public class MoneyTransactionController {

	private static final Logger log = LoggerFactory.getLogger(MoneyTransactionController.class);

	private final MoneyTransactionService moneyTxnService;
	private final FundTransferLogService fundTransferLogService;

	public MoneyTransactionController(MoneyTransactionService moneyTxnService,
			FundTransferLogService fundTransferLogService) {
		this.moneyTxnService = moneyTxnService;
		this.fundTransferLogService = fundTransferLogService;
	}

	@PostMapping(consumes = { "application/json" })
	@Operation(summary = "API to transfer money")
	public ResponseEntity<?> transferMoney(@RequestBody @Valid TransferRequest request) throws Exception {
		var transactionId = UUID.randomUUID().toString();
		try {
			var fundTxnLog = moneyTxnService.transferMoney(request, transactionId);
			var balanceAfterTransfer = request.includeBalance()
					? moneyTxnService.checkBalance(request.accountFromId())
					: null;
			var result = new TransferResult(
					fundTxnLog.getTransactionId(),
					request.accountFromId(),
					request.amount(),
					balanceAfterTransfer,
					TransactionStatus.SUCCESS
			);
			return new ResponseEntity<>(result, HttpStatus.ACCEPTED);
		} catch (AccountNotFoundException e) {
			saveFailureLog(request, transactionId, "Account not found: " + e.getMessage());
			log.error("Fail to transfer money", e);
			throw e;
		} catch (OverDraftException e) {
			saveFailureLog(request, transactionId, "Insufficient funds: " + e.getMessage());
			log.error("Fail to transfer money", e);
			throw e;
		} catch (ApplicationException ae) {
			saveFailureLog(request, transactionId, "Internal processing error");
			log.error("Fail to transfer money", ae);
			throw ae;
		}
	}

	private void saveFailureLog(TransferRequest request, String transactionId, String comment) {
		try {
			var fundTxnLog = new FundTransferLog(request.requestId(), transactionId,
					request.accountFromId(), request.accountToId(), request.amount(), TransactionStatus.FAILED);
			fundTxnLog.setComment(comment);
			fundTransferLogService.saveInNewTransaction(fundTxnLog);
		} catch (Exception ex) {
			log.error("Failed to save failure audit log for transaction {}", transactionId, ex);
		}
	}
}

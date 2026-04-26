package com.mybank.moneytransfer.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mybank.moneytransfer.entity.AccountAuditLog;
import com.mybank.moneytransfer.entity.FundTransferLog;
import com.mybank.moneytransfer.service.AccountAuditLogService;
import com.mybank.moneytransfer.service.FundTransferLogService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/mybank/v1/audit")
@Tag(name = "Audit", description = "Audit APIs for account and transaction logs")
public class AuditController {

	private final AccountAuditLogService accountAuditLogService;
	private final FundTransferLogService fundTransferLogService;

	public AuditController(AccountAuditLogService accountAuditLogService,
			FundTransferLogService fundTransferLogService) {
		this.accountAuditLogService = accountAuditLogService;
		this.fundTransferLogService = fundTransferLogService;
	}

	@GetMapping("/account/logs")
	@Operation(summary = "API to return account audit logs")
	public List<AccountAuditLog> getAccountAuditLogs() {
		return accountAuditLogService.findAll();
	}

	@GetMapping("/account/{accountId}/logs")
	@Operation(summary = "API to return account audit logs by account ID")
	public ResponseEntity<?> getAccountAuditLogsByAccountId(@PathVariable("accountId") String accountId) {
		if (accountId == null || accountId.length() != 9) {
			return ResponseEntity.badRequest().body("Invalid account ID:" + accountId);
		}
		return ResponseEntity.ok(accountAuditLogService.findByAccountId(accountId));
	}

	@GetMapping("/transaction/logs")
	@Operation(summary = "API to return fund transfer logs")
	public List<FundTransferLog> getFundTransferLogs() {
		return fundTransferLogService.findAll();
	}
}

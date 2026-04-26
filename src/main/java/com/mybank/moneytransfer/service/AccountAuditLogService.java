package com.mybank.moneytransfer.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.mybank.moneytransfer.entity.AccountAuditLog;
import com.mybank.moneytransfer.repository.AccountAuditLogRepository;

@Service
public class AccountAuditLogService {

	private final AccountAuditLogRepository accountAuditLogRepository;

	public AccountAuditLogService(AccountAuditLogRepository accountAuditLogRepository) {
		this.accountAuditLogRepository = accountAuditLogRepository;
	}

	public List<AccountAuditLog> findAll() {
		return accountAuditLogRepository.findAll();
	}

	public List<AccountAuditLog> findByAccountId(String accountId) {
		return accountAuditLogRepository.findByAccountId(accountId);
	}

	public void save(AccountAuditLog auditLog) {
		accountAuditLogRepository.save(auditLog);
	}
}

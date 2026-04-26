package com.mybank.moneytransfer.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mybank.moneytransfer.error.AccountNotFoundException;
import com.mybank.moneytransfer.entity.Account;
import com.mybank.moneytransfer.entity.AccountAuditLog;
import com.mybank.moneytransfer.repository.AccountRepository;

@Service
public class AccountManagementService {

	private final AccountRepository accountRepository;
	private final AccountAuditLogService accountAuditLogService;

	public AccountManagementService(AccountRepository accountRepository,
			AccountAuditLogService accountAuditLogService) {
		this.accountRepository = accountRepository;
		this.accountAuditLogService = accountAuditLogService;
	}

	public Account findByAccountId(final String accountId) throws AccountNotFoundException {
		return accountRepository.findByAccountId(accountId)
				.orElseThrow(() -> new AccountNotFoundException("Account with id:" + accountId + " does not exist."));
	}

	public Account getAccountForUpdate(final String accountId) throws AccountNotFoundException {
		return accountRepository.getAccountForUpdate(accountId)
				.orElseThrow(() -> new AccountNotFoundException("Account with id:" + accountId + " does not exist."));
	}

	public void save(final Account account) {
		this.accountRepository.save(account);
	}

	@Transactional
	public Account createAccount(final Account account) {
		this.accountRepository.save(account);
		accountAuditLogService.save(new AccountAuditLog(
				account.getAccountId(), "CREATE", "SUCCESS",
				"Account created with balance: " + account.getBalance()));
		return account;
	}
}

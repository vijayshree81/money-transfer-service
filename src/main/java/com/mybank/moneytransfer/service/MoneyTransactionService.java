package com.mybank.moneytransfer.service;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.mybank.moneytransfer.dto.BalanceResult;
import com.mybank.moneytransfer.dto.TransactionStatus;
import com.mybank.moneytransfer.error.AccountNotFoundException;
import com.mybank.moneytransfer.error.ApplicationException;
import com.mybank.moneytransfer.error.OverDraftException;
import com.mybank.moneytransfer.entity.Account;
import com.mybank.moneytransfer.entity.FundTransferLog;
import com.mybank.moneytransfer.dto.TransferRequest;
import com.mybank.moneytransfer.repository.AccountRepository;

@Service
public class MoneyTransactionService {

	private static final Logger log = LoggerFactory.getLogger(MoneyTransactionService.class);

	private final AccountRepository accountRepository;
	private final FundTransferLogService fundTransferLogService;
	private final RestTemplate restTemplate;

	@Value("${endpoint.accountBalance}")
	private String retrieveAccountBalanceUrl;

	public MoneyTransactionService(AccountRepository accountRepository,
			FundTransferLogService fundTransferLogService, RestTemplate restTemplate) {
		this.accountRepository = accountRepository;
		this.fundTransferLogService = fundTransferLogService;
		this.restTemplate = restTemplate;
	}

	@Transactional
	public FundTransferLog transferMoney(TransferRequest transfer, String transactionId)
			throws OverDraftException, AccountNotFoundException {
		// Prevent same-account transfer
		if (transfer.accountFromId().equals(transfer.accountToId())) {
			throw new OverDraftException("Source and destination accounts must be different");
		}

		// Lock accounts in sorted order to prevent deadlock
		String firstId, secondId;
		if (transfer.accountFromId().compareTo(transfer.accountToId()) < 0) {
			firstId = transfer.accountFromId();
			secondId = transfer.accountToId();
		} else {
			firstId = transfer.accountToId();
			secondId = transfer.accountFromId();
		}

		var first = accountRepository.getAccountForUpdate(firstId)
				.orElseThrow(() -> new AccountNotFoundException(
						"Account with id:" + firstId + " does not exist."));
		var second = accountRepository.getAccountForUpdate(secondId)
				.orElseThrow(() -> new AccountNotFoundException(
						"Account with id:" + secondId + " does not exist."));

		// Map back to from/to regardless of lock order
		Account accountFrom, accountTo;
		if (first.getAccountId().equals(transfer.accountFromId())) {
			accountFrom = first;
			accountTo = second;
		} else {
			accountFrom = second;
			accountTo = first;
		}

		if (accountFrom.getBalance().compareTo(transfer.amount()) < 0) {
			throw new OverDraftException(
					"Account with id:" + accountFrom.getAccountId() + " does not have enough balance to transfer.");
		}
		accountFrom.setBalance(accountFrom.getBalance().subtract(transfer.amount()));
		accountTo.setBalance(accountTo.getBalance().add(transfer.amount()));

		// Save success log inside the same transaction — commits or rolls back together
		var fundTxnLog = new FundTransferLog(transfer.requestId(), transactionId,
				transfer.accountFromId(), transfer.accountToId(), transfer.amount(), TransactionStatus.SUCCESS);
		fundTransferLogService.save(fundTxnLog);
		return fundTxnLog;
	}

	public BigDecimal checkBalance(String accountId) throws ApplicationException {
		try {
			var url = retrieveAccountBalanceUrl.replace("{accountId}", accountId);
			log.info("checking balance from {}", url);
			var balanceCheckResult = restTemplate.getForEntity(url, BalanceResult.class);
			if (balanceCheckResult.getStatusCode().is2xxSuccessful() && balanceCheckResult.hasBody()) {
				return balanceCheckResult.getBody().finalBalance();
			}
		} catch (ResourceAccessException ex) {
			if (ex.getCause() instanceof SocketTimeoutException) {
				throw new ApplicationException("Encounter timeout error, please check with system administrator.");
			}
		}
		throw new ApplicationException("Encounter internal server error, please check with system administrator");
	}
}

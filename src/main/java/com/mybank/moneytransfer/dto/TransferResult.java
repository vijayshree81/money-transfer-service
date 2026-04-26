package com.mybank.moneytransfer.dto;

import java.math.BigDecimal;

public record TransferResult(
		String transactionId,
		String accountFromId,
		BigDecimal amountTransfer,
		BigDecimal balanceAfterTransfer,
		TransactionStatus status
) {
}

package com.mybank.moneytransfer.dto;

import java.math.BigDecimal;

public record BalanceResult(
		String accountId,
		BigDecimal prevBalance,
		BigDecimal finalBalance
) {
}

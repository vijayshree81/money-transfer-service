package com.mybank.moneytransfer.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

public record TransferRequest(
		@NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
		String requestId,

		@NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
		String accountFromId,

		@NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
		String accountToId,

		@NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
		@DecimalMin(value = "0.01", message = "Transfer amount must be greater than zero")
		BigDecimal amount,

		@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
		boolean getBalance
) {
	public TransferRequest(String requestId, String accountFromId, String accountToId, BigDecimal amount) {
		this(requestId, accountFromId, accountToId, amount, false);
	}
}

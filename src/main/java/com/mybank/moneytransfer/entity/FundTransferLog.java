package com.mybank.moneytransfer.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.hibernate.annotations.CreationTimestamp;

import com.mybank.moneytransfer.dto.TransactionStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class FundTransferLog implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column
	private String requestId;
	@Column
	private String transactionId;
	@Column
	private String accountFromId;
	@Column
	private String accountToId;
	@Column
	private BigDecimal amountTransfer;
	@Column
	@Enumerated(EnumType.STRING)
	private TransactionStatus status;
	@CreationTimestamp
	@Column
	private Timestamp transactionDateTime;
	@Column
	private String comment;

	public FundTransferLog(String requestId, String transactionId, String accountFromId, String accountToId,
			BigDecimal amountTransfer, TransactionStatus status) {
		this.requestId = requestId;
		this.transactionId = transactionId;
		this.accountFromId = accountFromId;
		this.accountToId = accountToId;
		this.amountTransfer = amountTransfer;
		this.status = status;
	}

	@Override
	public int hashCode() {
		return Objects.hash(requestId, transactionId, accountFromId, accountToId,
				amountTransfer, status, transactionDateTime);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		return obj instanceof FundTransferLog other
				&& Objects.equals(requestId, other.requestId)
				&& Objects.equals(transactionId, other.transactionId)
				&& Objects.equals(accountFromId, other.accountFromId)
				&& Objects.equals(accountToId, other.accountToId)
				&& Objects.equals(amountTransfer, other.amountTransfer)
				&& status == other.status
				&& Objects.equals(transactionDateTime, other.transactionDateTime);
	}
}

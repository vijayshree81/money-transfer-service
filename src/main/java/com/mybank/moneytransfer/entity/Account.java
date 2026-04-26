package com.mybank.moneytransfer.entity;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ACCOUNT")
@Getter
@Setter
@NoArgsConstructor
public class Account {
	@Column
	@Id
	private String accountId;
	@Column
	private String type;
	@Column
	private String status;
	@CreationTimestamp
	@Column
	private Timestamp createdDateTime;
	@Column
	private BigDecimal balance;

	public Account(String accountId, String type, String status, Timestamp createdDate, BigDecimal balance) {
		this.accountId = accountId;
		this.type = type;
		this.status = status;
		this.createdDateTime = createdDate;
		this.balance = balance;
	}

	@Override
	public int hashCode() {
		return Objects.hash(accountId, type);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		return obj instanceof Account other
				&& Objects.equals(accountId, other.accountId)
				&& Objects.equals(type, other.type);
	}
}

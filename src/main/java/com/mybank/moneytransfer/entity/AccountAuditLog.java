package com.mybank.moneytransfer.entity;

import java.sql.Timestamp;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.hibernate.annotations.CreationTimestamp;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class AccountAuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column
	private String accountId;

	@Column
	private String action;

	@Column
	private String status;

	@Column
	private String details;

	@CreationTimestamp
	@Column
	private Timestamp createdDateTime;

	public AccountAuditLog(String accountId, String action, String status, String details) {
		this.accountId = accountId;
		this.action = action;
		this.status = status;
		this.details = details;
	}

	@Override
	public int hashCode() {
		return Objects.hash(accountId, action, status);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		return obj instanceof AccountAuditLog other
				&& Objects.equals(accountId, other.accountId)
				&& Objects.equals(action, other.action)
				&& Objects.equals(status, other.status);
	}
}

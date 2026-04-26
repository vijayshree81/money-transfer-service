package com.mybank.moneytransfer.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mybank.moneytransfer.entity.AccountAuditLog;

@Repository
public interface AccountAuditLogRepository extends JpaRepository<AccountAuditLog, Long> {

	List<AccountAuditLog> findByAccountId(String accountId);
}

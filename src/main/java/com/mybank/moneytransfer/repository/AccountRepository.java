package com.mybank.moneytransfer.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.mybank.moneytransfer.entity.Account;

@Repository
public interface AccountRepository extends JpaRepository<Account, Integer> {

	@Query("FROM Account where accountId=:accountId")
	Optional<Account> findByAccountId(String accountId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Transactional
	@Query("FROM Account where accountId=:accountId")
	Optional<Account> getAccountForUpdate(String accountId);
}

package com.mybank.moneytransfer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mybank.moneytransfer.entity.FundTransferLog;

@Repository
public interface FundTransferLogRepository extends JpaRepository<FundTransferLog, Integer> {

}

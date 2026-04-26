package com.mybank.moneytransfer.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.mybank.moneytransfer.entity.FundTransferLog;
import com.mybank.moneytransfer.repository.FundTransferLogRepository;

@Service
public class FundTransferLogService {

	private final FundTransferLogRepository fundTransferRepository;

	public FundTransferLogService(FundTransferLogRepository fundTransferRepository) {
		this.fundTransferRepository = fundTransferRepository;
	}

	public List<FundTransferLog> findAll() {
		return this.fundTransferRepository.findAll();
	}

	public void save(final FundTransferLog fundTransferLog) {
		this.fundTransferRepository.save(fundTransferLog);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void saveInNewTransaction(final FundTransferLog fundTransferLog) {
		this.fundTransferRepository.save(fundTransferLog);
	}
}

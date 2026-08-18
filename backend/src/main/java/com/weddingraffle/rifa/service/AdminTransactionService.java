package com.weddingraffle.rifa.service;

import com.weddingraffle.rifa.dto.AdminTransactionResponse;
import com.weddingraffle.rifa.dto.CashTransactionCreateRequest;
import com.weddingraffle.rifa.dto.CashTransactionCreateResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminTransactionService {

    Page<AdminTransactionResponse> list(String email, Pageable pageable);

    CashTransactionCreateResponse createCashTransaction(CashTransactionCreateRequest request);

    void deleteCashTransaction(String externalReference);
}

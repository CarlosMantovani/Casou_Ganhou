package com.weddingraffle.rifa.service;

import com.weddingraffle.rifa.dto.AdminTransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminTransactionService {

    Page<AdminTransactionResponse> list(String email, Pageable pageable);
}

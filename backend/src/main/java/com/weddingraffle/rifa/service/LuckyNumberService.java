package com.weddingraffle.rifa.service;

import com.weddingraffle.rifa.entity.LuckyNumber;
import com.weddingraffle.rifa.entity.Transaction;
import java.util.List;

public interface LuckyNumberService {

    List<LuckyNumber> generateFor(Transaction transaction);

    List<String> findNumbers(String externalReference);

    List<String> findApprovedNumbersByPhone(String phone);

    List<String> findPreviousApprovedNumbers(String phone, String externalReference);
}

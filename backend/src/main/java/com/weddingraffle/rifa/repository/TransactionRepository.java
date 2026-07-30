package com.weddingraffle.rifa.repository;

import com.weddingraffle.rifa.entity.Transaction;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByExternalReference(String externalReference);

    Page<Transaction> findByEmailContainingIgnoreCase(String email, Pageable pageable);
}

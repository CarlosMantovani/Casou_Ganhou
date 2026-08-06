package com.weddingraffle.rifa.repository;

import com.weddingraffle.rifa.entity.Transaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByExternalReference(String externalReference);

    Page<Transaction> findByEmailContainingIgnoreCase(String email, Pageable pageable);

    Page<Transaction> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String name, String email, Pageable pageable);

    @Query(
            value =
                    """
                    SELECT t.phone AS phone, SUM(t.quantity) AS quantity
                    FROM transaction t
                    WHERE t.status = 'APPROVED'
                    GROUP BY t.phone
                    ORDER BY SUM(t.quantity) DESC, t.phone ASC
                    LIMIT 5
                    """,
            nativeQuery = true)
    List<TopBuyerProjection> findTopApprovedBuyers();
}

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

    Optional<Transaction> findFirstByPhoneOrderByCreatedAtAsc(String phone);

    Page<Transaction> findByEmailContainingIgnoreCase(String email, Pageable pageable);

    Page<Transaction> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String name, String email, Pageable pageable);

    @Query(
            """
            select
                t.participantFlagCode as code,
                t.participantFlagName as name,
                t.participantFlagEmoji as emoji,
                sum(t.quantity) as totalNumbers
            from RaffleTransaction t
            where t.status = com.weddingraffle.rifa.entity.PaymentStatus.APPROVED
            group by t.participantFlagCode, t.participantFlagName, t.participantFlagEmoji
            order by sum(t.quantity) desc, t.participantFlagName asc
            """)
    List<FlagRankingProjection> findApprovedFlagRanking(Pageable pageable);
}

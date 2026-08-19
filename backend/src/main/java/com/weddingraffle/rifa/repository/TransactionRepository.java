package com.weddingraffle.rifa.repository;

import com.weddingraffle.rifa.entity.Transaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query(
            value =
                    """
                    select
                        cast(count(id) as bigint) as "totalTransactions",
                        cast(coalesce(sum(case when status = 'APPROVED' then quantity else 0 end), 0) as bigint)
                            as "approvedLuckyNumbers",
                        coalesce(sum(case when status = 'APPROVED' then total_amount else 0 end), 0)
                            as "approvedRevenue"
                    from transaction
                    """,
            nativeQuery = true)
    AdminTransactionSummaryProjection getAdminSummary();

    Optional<Transaction> findByExternalReference(String externalReference);

    Optional<Transaction> findFirstByPhoneOrderByCreatedAtAsc(String phone);

    @Query("select distinct raffleTransaction.participantFlagCode from RaffleTransaction raffleTransaction")
    List<String> findDistinctParticipantFlagCodes();

    Page<Transaction> findByEmailContainingIgnoreCase(String email, Pageable pageable);

    Page<Transaction> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String name, String email, Pageable pageable);

    @Query(
            value =
                    """
                    select
                        participant_flag_code as code,
                        participant_flag_name as name,
                        participant_flag_emoji as emoji,
                        cast(sum(quantity) as bigint) as "totalNumbers"
                    from transaction
                    where status = 'APPROVED'
                    group by participant_flag_code, participant_flag_name, participant_flag_emoji
                    order by sum(quantity) desc, participant_flag_name asc
                    """,
            nativeQuery = true)
    List<FlagRankingProjection> findApprovedFlagRanking(Pageable pageable);
}

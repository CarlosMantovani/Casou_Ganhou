package com.weddingraffle.rifa.repository;

import com.weddingraffle.rifa.entity.RaffleDraw;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RaffleDrawRepository extends JpaRepository<RaffleDraw, Long> {

    Optional<RaffleDraw> findFirstByOrderByIdAsc();

    Optional<RaffleDraw> findFirstByOrderByIdDesc();
}

package com.ho.account.ecl.core.infrastructure.adapter.persistence.jpa;

import com.ho.account.ecl.core.domain.model.TransitionMatrix;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface JpaTransitionMatrixRepository extends JpaRepository<TransitionMatrix, Long> {
    List<TransitionMatrix> findByBaseDate(LocalDate baseDate);
    List<TransitionMatrix> findByBaseDateAndFromRating(LocalDate baseDate, String fromRating);
}

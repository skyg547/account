package com.ho.account.ecl.core.infrastructure.adapter.persistence;

import com.ho.account.ecl.core.application.port.out.TransitionMatrixRepository;
import com.ho.account.ecl.core.domain.model.TransitionMatrix;
import com.ho.account.ecl.core.infrastructure.adapter.persistence.jpa.JpaTransitionMatrixRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class TransitionMatrixPersistenceAdapter implements TransitionMatrixRepository {
    private final JpaTransitionMatrixRepository jpaRepository;

    @Override public List<TransitionMatrix> findByBaseDate(LocalDate baseDate) { return jpaRepository.findByBaseDate(baseDate); }
    @Override public List<TransitionMatrix> findByBaseDateAndFromRating(LocalDate baseDate, String fromRating) {
        return jpaRepository.findByBaseDateAndFromRating(baseDate, fromRating);
    }
}

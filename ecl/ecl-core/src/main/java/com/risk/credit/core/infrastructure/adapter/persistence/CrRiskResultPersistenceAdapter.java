package com.risk.credit.core.infrastructure.adapter.persistence;

import com.risk.credit.core.application.port.out.CrRiskResultRepository;
import com.risk.credit.core.domain.result.CrRiskResult;
import com.risk.credit.core.infrastructure.adapter.persistence.jpa.JpaCrRiskResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * [Adapter] CrRiskResultRepository 포트의 JPA 구현체.
 */
@Repository
@RequiredArgsConstructor
public class CrRiskResultPersistenceAdapter implements CrRiskResultRepository {

    private final JpaCrRiskResultRepository jpaRepository;

    @Override
    public List<CrRiskResult> findAllByBaseDate(LocalDate baseDate) {
        return jpaRepository.findAllByBaseDate(baseDate);
    }

    @Override
    public Page<CrRiskResult> findByBaseDate(LocalDate baseDate, Pageable pageable) {
        return jpaRepository.findByBaseDate(baseDate, pageable);
    }

    @Override
    public Optional<CrRiskResult> findByBaseDateAndAccountId(LocalDate baseDate, Long accountId) {
        return jpaRepository.findByBaseDateAndAccountId(baseDate, accountId);
    }

    @Override
    public CrRiskResult save(CrRiskResult result) {
        return jpaRepository.save(result);
    }

    @Override
    public void saveAll(Iterable<CrRiskResult> results) {
        jpaRepository.saveAll(results);
    }

    @Override
    public void deleteAllByBaseDate(LocalDate baseDate) {
        jpaRepository.deleteAllByBaseDate(baseDate);
    }
}

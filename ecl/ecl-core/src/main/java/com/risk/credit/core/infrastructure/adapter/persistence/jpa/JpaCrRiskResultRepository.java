package com.risk.credit.core.infrastructure.adapter.persistence.jpa;

import com.risk.credit.core.domain.result.CrRiskResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.Optional;

/**
 * [Infrastructure] JPA를 사용한 리스크 결과 데이터 접근 인터페이스.
 */
public interface JpaCrRiskResultRepository extends JpaRepository<CrRiskResult, Long> {
    java.util.List<CrRiskResult> findAllByBaseDate(LocalDate baseDate);

    Page<CrRiskResult> findByBaseDate(LocalDate baseDate, Pageable pageable);

    Optional<CrRiskResult> findByBaseDateAndAccountId(LocalDate baseDate, Long accountId);

    void deleteAllByBaseDate(LocalDate baseDate);
}

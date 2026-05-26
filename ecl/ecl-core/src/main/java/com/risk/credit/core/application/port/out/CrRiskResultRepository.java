package com.risk.credit.core.application.port.out;

import com.risk.credit.core.domain.result.CrRiskResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * [Port] 신용리스크 산출 결과(Result) 데이터소스 인터페이스.
 */
public interface CrRiskResultRepository {
    List<CrRiskResult> findAllByBaseDate(LocalDate baseDate);

    Page<CrRiskResult> findByBaseDate(LocalDate baseDate, Pageable pageable);

    Optional<CrRiskResult> findByBaseDateAndAccountId(LocalDate baseDate, Long accountId);

    CrRiskResult save(CrRiskResult result);

    void saveAll(Iterable<CrRiskResult> results);

    void deleteAllByBaseDate(LocalDate baseDate);
}

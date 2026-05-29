package com.ho.account.ecl.core.application.port.out;

import com.ho.account.ecl.core.domain.result.AllowanceEclResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * [Port] 대손충당금(IFRS9) 산출 결과(Result) 데이터소스 인터페이스.
 */
public interface AllowanceEclResultRepository {
    List<AllowanceEclResult> findAllByBaseDate(LocalDate baseDate);

    Page<AllowanceEclResult> findByBaseDate(LocalDate baseDate, Pageable pageable);

    Optional<AllowanceEclResult> findByBaseDateAndAccountId(LocalDate baseDate, Long accountId);

    AllowanceEclResult save(AllowanceEclResult result);

    void saveAll(Iterable<AllowanceEclResult> results);

    void deleteAllByBaseDate(LocalDate baseDate);
}


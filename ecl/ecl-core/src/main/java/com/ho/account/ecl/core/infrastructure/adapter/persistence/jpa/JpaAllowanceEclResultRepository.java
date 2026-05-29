package com.ho.account.ecl.core.infrastructure.adapter.persistence.jpa;

import com.ho.account.ecl.core.domain.result.AllowanceEclResult;
import com.ho.account.ecl.core.domain.result.AllowanceEclResultId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.Optional;

/**
 * [Infrastructure] JPA를 사용한 대손충당금 결과 데이터 접근 인터페이스.
 */
public interface JpaAllowanceEclResultRepository extends JpaRepository<AllowanceEclResult, AllowanceEclResultId> {
    java.util.List<AllowanceEclResult> findAllByBaseDate(LocalDate baseDate);

    Page<AllowanceEclResult> findByBaseDate(LocalDate baseDate, Pageable pageable);

    Optional<AllowanceEclResult> findByBaseDateAndAccountId(LocalDate baseDate, Long accountId);

    void deleteAllByBaseDate(LocalDate baseDate);
}



package com.ho.account.ecl.core.infrastructure.adapter.persistence;

import com.ho.account.ecl.core.application.port.out.AllowanceEclResultRepository;
import com.ho.account.ecl.core.domain.result.AllowanceEclResult;
import com.ho.account.ecl.core.infrastructure.adapter.persistence.jpa.JpaAllowanceEclResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * [Adapter] AllowanceEclResultRepository 포트의 JPA 구현체.
 */
@Repository
@RequiredArgsConstructor
public class AllowanceEclResultPersistenceAdapter implements AllowanceEclResultRepository {

    private final JpaAllowanceEclResultRepository jpaRepository;

    @Override
    public List<AllowanceEclResult> findAllByBaseDate(LocalDate baseDate) {
        return jpaRepository.findAllByBaseDate(baseDate);
    }

    @Override
    public Page<AllowanceEclResult> findByBaseDate(LocalDate baseDate, Pageable pageable) {
        return jpaRepository.findByBaseDate(baseDate, pageable);
    }

    @Override
    public Optional<AllowanceEclResult> findByBaseDateAndAccountId(LocalDate baseDate, Long accountId) {
        return jpaRepository.findByBaseDateAndAccountId(baseDate, accountId);
    }

    @Override
    public AllowanceEclResult save(AllowanceEclResult result) {
        return jpaRepository.save(result);
    }

    @Override
    public void saveAll(Iterable<AllowanceEclResult> results) {
        jpaRepository.saveAll(results);
        jpaRepository.flush();
    }

    @Override
    public void deleteAllByBaseDate(LocalDate baseDate) {
        jpaRepository.deleteAllByBaseDate(baseDate);
    }
}


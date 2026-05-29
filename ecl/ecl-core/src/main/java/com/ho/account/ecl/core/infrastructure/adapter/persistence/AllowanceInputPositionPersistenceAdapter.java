package com.ho.account.ecl.core.infrastructure.adapter.persistence;

import com.ho.account.shared.finance.entity.AllowanceInputPosition;
import com.ho.account.ecl.core.application.port.out.AllowanceInputPositionRepository;
import com.ho.account.ecl.core.infrastructure.adapter.persistence.jpa.JpaAllowanceInputPositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * [Adapter] AllowanceInputPositionRepository 포트의 JPA 구현체.
 */
@Repository
@RequiredArgsConstructor
public class AllowanceInputPositionPersistenceAdapter implements AllowanceInputPositionRepository {

    private final JpaAllowanceInputPositionRepository jpaRepository;

    @Override
    public List<AllowanceInputPosition> findByBaseDt(LocalDate baseDt) {
        return jpaRepository.findByBaseDt(baseDt);
    }
}


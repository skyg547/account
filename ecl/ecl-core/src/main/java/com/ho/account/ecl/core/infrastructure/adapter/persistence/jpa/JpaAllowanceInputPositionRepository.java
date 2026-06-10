package com.ho.account.ecl.core.infrastructure.adapter.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

/**
 * [Infrastructure] JPA를 사용한 통합 대손충당금 입력 포지션 데이터 접근 인터페이스.
 */
public interface JpaAllowanceInputPositionRepository
        extends JpaRepository<AllowanceInputPositionJpaEntity, AllowanceInputPositionJpaId> {
    List<AllowanceInputPositionJpaEntity> findByBaseDt(LocalDate baseDt);
}



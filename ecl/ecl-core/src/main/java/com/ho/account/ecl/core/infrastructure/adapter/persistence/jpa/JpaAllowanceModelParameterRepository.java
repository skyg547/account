package com.ho.account.ecl.core.infrastructure.adapter.persistence.jpa;

import com.ho.account.ecl.core.domain.model.AllowanceModelParameter;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 모델 파라미터 테이블에 접근하는 Spring Data JPA 저장소입니다.
 */
public interface JpaAllowanceModelParameterRepository extends JpaRepository<AllowanceModelParameter, String> {
}

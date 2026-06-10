package com.ho.account.ecl.core.application.port.out;

import com.ho.account.ecl.core.domain.model.AllowanceModelParameter;

import java.util.List;

/**
 * 대손충당금 모델 파라미터 영속성 출력 포트.
 *
 * <p>애플리케이션과 API는 모델 파라미터의 조회·저장 의도에만 의존하며,
 * 실제 Spring Data JPA 사용은 infrastructure 어댑터에 격리합니다.</p>
 */
public interface AllowanceModelParameterRepository {

    List<AllowanceModelParameter> findAll();

    AllowanceModelParameter save(AllowanceModelParameter parameter);
}

package com.ho.account.ecl.core.infrastructure.adapter.persistence;

import com.ho.account.ecl.core.application.port.out.AllowanceModelParameterRepository;
import com.ho.account.ecl.core.domain.model.AllowanceModelParameter;
import com.ho.account.ecl.core.infrastructure.adapter.persistence.jpa.JpaAllowanceModelParameterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 모델 파라미터 출력 포트를 JPA 저장소에 연결하는 영속성 어댑터입니다.
 */
@Repository
@RequiredArgsConstructor
public class AllowanceModelParameterPersistenceAdapter implements AllowanceModelParameterRepository {

    private final JpaAllowanceModelParameterRepository jpaRepository;

    @Override
    public List<AllowanceModelParameter> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public AllowanceModelParameter save(AllowanceModelParameter parameter) {
        return jpaRepository.save(parameter);
    }
}

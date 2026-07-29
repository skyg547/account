package com.ho.account.auth.core.infrastructure.persistence;

import com.ho.account.auth.core.application.port.out.DepartmentValidationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 로컬/단독(Standalone) 구동 환경용 부서 검증 어댑터.
 * 외부 master-data 모듈이 꺼져 있는 환경에서 부서 코드 검증을 무조건 승인 처리하여 로컬 개발 편의성을 높입니다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "auth.master-data.enabled", havingValue = "false", matchIfMissing = true)
public class LocalDepartmentValidationAdapter implements DepartmentValidationPort {

    @Override
    public boolean existsDepartmentCode(String departmentCode) {
        if (departmentCode == null || departmentCode.isBlank()) {
            return false;
        }
        log.info("Standalone/Local mode: Auto-approving departmentCode validation for [{}]", departmentCode);
        return true;
    }
}

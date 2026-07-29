package com.ho.account.shared.infrastructure.security.service;

import com.ho.account.shared.infrastructure.security.application.port.out.AuditLogPersistencePort;
import com.ho.account.shared.infrastructure.security.domain.AuditLog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TracingService {

    private final AuditLogPersistencePort auditLogPersistencePort;

    /**
     * 특정 엔티티와 관련된 모든 감사 로그를 수집합니다 (추적성 확보).
     * 
     * @param targetEntity 엔티티 명 (예: "JOURNAL_ENTRY", "LOAN_CONTRACT")
     * @param targetId     엔티티 식별자
     * @return 추적된 로그 리스트
     */
    public List<AuditLog> extractTraceabilityPackage(String targetEntity, String targetId) {
        return auditLogPersistencePort.findByTargetEntityAndTargetId(targetEntity, targetId);
    }
}

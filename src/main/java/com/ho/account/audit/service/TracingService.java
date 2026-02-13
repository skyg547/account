package com.ho.account.audit.service;

import com.ho.account.audit.domain.AuditLog;
import com.ho.account.audit.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TracingService {

    private final AuditLogRepository auditLogRepository;

    @Autowired
    public TracingService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * 특정 엔티티와 관련된 모든 감사 로그를 수집합니다 (추적성 확보).
     * 
     * @param targetEntity 엔티티 명 (예: "JOURNAL_ENTRY", "LOAN_CONTRACT")
     * @param targetId     엔티티 식별자
     * @return 추적된 로그 리스트
     */
    public List<AuditLog> extractTraceabilityPackage(String targetEntity, String targetId) {
        List<AuditLog> packageLogs = new ArrayList<>();

        // 1. 대상 엔티티의 초기 생성/수정 로그
        packageLogs.addAll(auditLogRepository.findByTargetEntityAndTargetId(targetEntity, targetId));

        // 2. 관련된 하위 작업 또는 연계된 프로세스 로그 (예: 전표 전기 시 관련 GL 로그 등)
        // 이 부분은 비즈니스 로직에 따라 확장 가능

        return packageLogs;
    }
}

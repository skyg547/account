package com.ho.account.mart.core.domain.ods.audit.service;

import com.ho.account.mart.core.application.port.out.OdsDqAuditRepository;
import com.ho.account.mart.core.domain.ods.audit.OdsDqAudit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * [Legacy Support] 데이터 품질 서비스 (Old Version Support)
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DataQualityService {

    private final OdsDqAuditRepository dqAuditRepository;

    /**
     * 단순 DQ 오류 기록 (Legacy 로직 지원)
     */
    public void logDqError(LocalDate baseDate, String tableName, String accountNo, 
                          String errorType, String errorMessage) {
        
        OdsDqAudit audit = OdsDqAudit.builder()
                .baseDate(baseDate)
                .tableName(tableName)
                .accountNo(accountNo)
                .auditType(errorType)
                .auditMessage(errorMessage)
                .severity("MEDIUM")
                .auditTimestamp(LocalDateTime.now())
                .build();
                
        dqAuditRepository.save(audit);
    }
}

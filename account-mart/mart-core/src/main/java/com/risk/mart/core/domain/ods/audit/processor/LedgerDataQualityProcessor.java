package com.risk.mart.core.domain.ods.audit.processor;

import com.risk.mart.core.domain.ods.audit.OdsDqAudit;
import com.risk.mart.core.domain.ods.loan.OdsAccountLedger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * [배치 프로세서] 원장 데이터 품질 검사 프로세서
 */
@Slf4j
@Component
public class LedgerDataQualityProcessor implements ItemProcessor<OdsAccountLedger, OdsDqAudit> {

    @Override
    public OdsDqAudit process(OdsAccountLedger ledger) {
        String accountNo = ledger.getAccountNo();
        
        // 잔액이 음수인 경우 품질 오류로 기록
        if (ledger.getOutstandingAmount() != null && ledger.getOutstandingAmount().doubleValue() < 0) {
            log.warn("🚨 [DQ] 음수 잔액 발견: {}", accountNo);
            
            return OdsDqAudit.builder()
                .baseDate(LocalDate.now()) // 배치는 JobParameter 사용 권장
                .tableName("ods_acc_ledger")
                .accountNo(accountNo)
                .auditType("NEGATIVE_BALANCE")
                .auditMessage("대출 잔액이 음수입니다.")
                .severity("HIGH")
                .auditTimestamp(LocalDateTime.now())
                .build();
        }
        
        return null;
    }
}

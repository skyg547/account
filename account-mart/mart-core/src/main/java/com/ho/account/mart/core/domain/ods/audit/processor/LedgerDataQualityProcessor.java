package com.ho.account.mart.core.domain.ods.audit.processor;

import com.ho.account.mart.core.domain.ods.audit.OdsDqAudit;
import com.ho.account.mart.core.domain.ods.loan.OdsAccountLedger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * [DQ 규칙] 원장 데이터 품질 검사 규칙.
 *
 * <p>초보자 설명: Batch가 데이터를 몇 건씩 읽는지는 mart-batch가 정하고,
 * 이 core 클래스는 "이 원장 한 건이 업무적으로 정상인지"만 판단한다.</p>
 */
@Slf4j
@Component
public class LedgerDataQualityProcessor {

    public OdsDqAudit inspect(OdsAccountLedger ledger, LocalDate baseDate) {
        String accountNo = ledger.getAccountNo();

        // 잔액이 음수인 경우 품질 오류로 기록
        if (ledger.getOutstandingAmount() != null && ledger.getOutstandingAmount().signum() < 0) {
            log.warn("🚨 [DQ] 음수 잔액 발견: {}", accountNo);

            return OdsDqAudit.builder()
                    .baseDate(baseDate)
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

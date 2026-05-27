package com.ho.account.ecl.core.application.pipeline;

import com.ho.account.ecl.core.application.port.out.CrAccountBulkPort;
import com.ho.account.ecl.core.domain.exposure.CrAccount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * [CRM] 대손충당금(IFRS9) 데이터 품질(DQ) 검증 서비스
 * 대손충당금(IFRS9) 산출 전 부적격 데이터를 필터링합니다.
 *
 * [초보자를 위한 개념 설명]
 * DQ (Data Quality, 데이터 품질)는 공장의 '안전 필터'와 같습니다.
 * '나쁜 데이터'가 들어가면(Garbage In), '나쁜 결과'가 나옵니다(Garbage Out).
 * 이 서비스는 차주 유형, 등급, 금액 등 대손충당금(IFRS9) 산출에 필수적인 정보가 올바르게 채워져 있는지 확인합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskDataQualityService {

    private final CrAccountBulkPort accountBulkPort;

    /**
     * [Enterprise-Scale] 전사 대손충당금(IFRS9) 데이터 품질(DQ) 검증을 수행합니다.
     *
     * 💡 [비즈니스 가이드]
     * 수억 건의 데이터를 하나씩 검사하는 것은 불가능합니다.
     * 본 로직은 인프라 레이어의 SQL 집합 처리를 통해 단 몇 번의 쿼리로 전체 데이터의 결함 여부를 식별합니다.
     * 결함이 발견된 데이터는 '산출 제외' 상태로 마킹되어 정합성이 보장된 데이터만 산출 라인에 투입됩니다.
     */
    @Transactional
    public void runFullDataQualityCheck(LocalDate baseDate) {
        log.info("🚀 [DQ] {} 기준 데이터 품질 검증(Data Quality Check) 시작", baseDate);

        accountBulkPort.clearErrorMessages();

        log.info("  -> [CR001] 필수 식별정보(계좌번호/차준ID) 누락 검사 중...");
        int cr001Count = accountBulkPort.markInvalidIdentityAccounts();
        if (cr001Count > 0) {
            log.warn("     [경고] {}건의 필수 정보 누락 데이터 식별 및 제외 처리", cr001Count);
        }

        log.info("  -> [CR002] 대손충당금(IFRS9) 산출 필수값(등급/차주유형) 누락 검사 중...");
        int cr002Count = accountBulkPort.markInvalidRiskParamAccounts();
        if (cr002Count > 0) {
            log.warn("     [경고] {}건의 리스크 파라미터 누락 데이터 식별 및 제외 처리", cr002Count);
        }

        log.info("  -> [CR003] 비정상 가액(마이너스 잔액 등) 검사 중...");
        int cr003Count = accountBulkPort.markInvalidAmountAccounts();
        if (cr003Count > 0) {
            log.warn("     [경고] {}건의 비정상 가액 데이터 식별 및 제외 처리", cr003Count);
        }

        log.info("✅ [DQ] 데이터 품질 검증 완료. (총 {}건 부적격 식별)", cr001Count + cr002Count + cr003Count);
    }

    /**
     * 계산 단계에서 단건 계좌의 최소 정합성을 재검증합니다.
     */
    public boolean validate(CrAccount account) {
        if (account == null || Boolean.FALSE.equals(account.getIsActive())) {
            return false;
        }
        if (account.getAccountNo() == null || account.getAccountNo().isBlank()) {
            return false;
        }
        if (account.getCustomer() == null || account.getCustomer().getCustomerType() == null) {
            return false;
        }
        if (account.getCustomer().getInternalRating() == null
                && (account.getInternalRating() == null || account.getInternalRating().isBlank())) {
            return false;
        }
        if (account.getOutstandingAmount() == null || account.getOutstandingAmount().signum() < 0) {
            return false;
        }
        return account.getNotionalAmount() != null && account.getNotionalAmount().signum() >= 0;
    }
}

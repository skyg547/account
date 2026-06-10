package com.ho.account.ecl.core.application.service.integration;

import com.ho.account.ecl.core.application.port.out.AllowanceInputPositionSnapshot;
import com.ho.account.ecl.core.application.port.out.CrAccountBulkPort;
import com.ho.account.ecl.core.application.port.out.CrCustomerBulkPort;
import com.ho.account.ecl.core.application.port.out.AllowanceInputPositionRepository;
import com.ho.account.ecl.core.domain.exposure.CrAccount;
import com.ho.account.ecl.core.domain.exposure.CrCustomer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * [금융 아키텍처] CDM(통합 마트) -> 신용결산 대손 엔진 데이터 동기화 서비스 (v6.0 고도화)
 * 
 * 💡 [초보자를 위한 개념 설명]
 * 이 서비스는 '대량 수송 열차'와 같습니다. 
 * 데이터 마트라는 거대한 창고에서 수만 명의 고객과 수십만 개의 대출 정보를 
 * 하나씩 옮기는 대신, 벌크(Bulk)라는 큰 컨테이너에 담아 한꺼번에 신용결산 대손 엔진으로 옮겨줍니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CdmSyncService {

    private final AllowanceInputPositionRepository cdmRepository;
    private final CrCustomerBulkPort customerBulkPort;
    private final CrAccountBulkPort accountBulkPort;

    /**
     * 특정 기준일의 통합 마트 데이터를 신용결산 대손 엔진으로 고속 동기화합니다.
     */
    @Transactional
    public void syncFromCdm(LocalDate baseDate) {
        log.info("🔄 [CDM Bulk Sync] 대손충당금(IFRS9) 데이터 동기화 시작 (기준일: {})", baseDate);

        // 1. 통합 마트에서 해당 날짜의 모든 포지션 조회
        List<AllowanceInputPositionSnapshot> cdmPositions = cdmRepository.findByBaseDt(baseDate);
        log.info("📥 [CDM Bulk Sync] 마트 데이터 로드 완료: {} 건", cdmPositions.size());

        if (cdmPositions.isEmpty()) {
            log.warn("⚠️ [CDM Bulk Sync] 해당 날짜에 동기화할 데이터가 없습니다.");
            return;
        }

        // 2. 차주(Customer) 정보 고속 Upsert
        syncCustomersBulk(cdmPositions);

        // 3. 계좌(Account) 정보 고속 Upsert
        syncAccountsBulk(cdmPositions);

        log.info("✅ [CDM Bulk Sync] 대손충당금(IFRS9) 데이터 동기화 완료.");
    }

    private void syncCustomersBulk(List<AllowanceInputPositionSnapshot> positions) {
        // 중복 제거된 고객 정보 생성
        List<CrCustomer> customers = positions.stream()
                .collect(Collectors.toMap(
                        AllowanceInputPositionSnapshot::customerCode,
                        p -> CrCustomer.builder()
                                .customerCode(p.customerCode())
                                .customerName(p.customerName() != null ? p.customerName() : "Unknown-" + p.customerCode())
                                .customerType(p.customerType())
                                .internalRating(p.internalRating())
                                .industryCode(p.industryCode())
                                .countryCode(p.countryCode())
                                .isSme(p.isSme() != null ? p.isSme() : false)
                                .warningLevel(p.warningLevel() != null ? p.warningLevel() : "NORMAL")
                                .build(),
                        (existing, replacement) -> existing
                ))
                .values().stream().toList();

        customerBulkPort.bulkUpsert(customers);
        log.info("👤 [CDM Bulk Sync] 차주 정보 Upsert 완료: {} 건", customers.size());
    }

    private void syncAccountsBulk(List<AllowanceInputPositionSnapshot> positions) {
        List<CrAccount> accounts = positions.stream()
                .map(p -> CrAccount.builder()
                        .accountNo(p.accountNo())
                        .productCode(p.productCode())
                        .currency(p.currency().name())
                        .notionalAmount(p.limitAmount())
                        .outstandingAmount(p.outstandingAmount())
                        .productCategory(p.productCategory() != null ? p.productCategory().name() : null)
                        .interestRate(p.interestRate())
                        .repaymentMethod(p.repaymentMethod())
                        .gracePeriod(p.gracePeriod())
                        .repaymentFreq(p.repaymentFrequency())
                        .branchCode(p.branchCode())
                        .bizUnitCode(p.businessUnitCode())
                        .staging(p.staging())
                        .delinquentDays(p.delinquentDays())
                        .openDate(p.openDate())
                        .maturityDate(p.maturityDate())
                        .internalRating(p.internalRating())
                        .isDebtRestructured(p.isDebtRestructured() != null ? p.isDebtRestructured() : false)
                        .build())
                .toList();

        accountBulkPort.bulkUpsert(accounts);
        log.info("💳 [CDM Bulk Sync] 계좌 정보 Upsert 완료: {} 건", accounts.size());
    }
}


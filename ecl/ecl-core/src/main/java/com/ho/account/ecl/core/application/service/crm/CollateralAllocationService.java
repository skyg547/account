package com.ho.account.ecl.core.application.service.crm;

import com.ho.account.ecl.core.application.port.out.CrAccountCollateralRepository;
import com.ho.account.ecl.core.application.port.out.CrBulkOperationPort;
import com.ho.account.ecl.core.application.port.out.CrAccountRepository;
import com.ho.account.ecl.core.application.port.out.CrCollateralRepository;
import com.ho.account.ecl.core.application.port.out.CrCustomerRepository;
import com.ho.account.ecl.core.domain.calculator.CollateralAllocationCalculator;
import com.ho.account.ecl.core.domain.calculator.CollateralAllocationCalculator.AllocationResult;
import com.ho.account.ecl.core.domain.collateral.CrAccountCollateral;
import com.ho.account.ecl.core.domain.collateral.CrCollateral;
import com.ho.account.ecl.core.domain.exposure.CrAccount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * [애플리케이션 서비스] [CRM] 담보 배분 오케스트레이션 서비스 (Collateral Allocation Application Service)
 * 
 * 💡 [DDD & Hexagonal Architecture 설계 원칙]
 * 1. Application Service의 역할 (Orchestration):
 *    본 클래스는 포트(Repository, BulkOperationPort)를 사용하여 영속성 데이터를 조율하고,
 *    트랜잭션 범위를 설정하며, 순수 도메인 연산자({@link CollateralAllocationCalculator})를 호출하는 역할만 전담합니다.
 * 
 * 2. Domain Calculator로의 핵심 연산 이관 (Encapsulation):
 *    수학적 최적화(선형계획법 LP Simplex Solver), 대출 상품별 손실 절감 우선순위 계산, 폭포수 순차 배분 산식은
 *    모두 도메인 계층의 Pure Domain Calculator 로 이관되었습니다.
 *    이를 통해 도메인 로직의 재사용성, 단위 테스트 용이성, 그리고 Anemic Domain Model 탈피를 달성합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollateralAllocationService {

    private final CrCustomerRepository customerRepository;
    private final CrAccountRepository accountRepository;
    private final CrCollateralRepository collateralRepository;
    private final CrAccountCollateralRepository accountCollateralRepository;
    private final CrBulkOperationPort bulkOperationPort;
    private final ApartmentCollateralService apartmentCollateralService;
    private final CollateralAllocationCalculator collateralAllocationCalculator;

    /**
     * [애플리케이션 서비스 오케스트레이션] 모든 활성 고객에 대해 담보 배분을 수행한다.
     * 배치 작업 등에서 호출되며 고객별 담보 배분 프로세스를 조율합니다.
     */
    @Transactional
    public void allocateAllCollaterals() {
        log.info("🚀 [CRM 전체 최적화] 전사 고객 대상 담보 배분 프로세스를 시작합니다.");
        accountRepository.findByIsActiveTrue().stream()
                .map(CrAccount::getCustomer)
                .filter(java.util.Objects::nonNull)
                .map(customer -> customer.getId())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .forEach(customerId -> {
            try {
                allocateCollateralsForCustomer(customerId);
            } catch (Exception e) {
                log.error("❌ [CRM 최적화 실패] 고객(ID: {}) 배분 중 오류 발생: {}", customerId, e.getMessage());
            }
        });
        log.info("✅ [CRM 전체 최적화] 전사 담보 배분 프로세스가 완료되었습니다.");
    }

    /**
     * [애플리케이션 서비스 오케스트레이션] 고객의 계좌 및 담보를 조회하고,
     * Pure Domain Calculator를 기동하여 최적 배분 결과를 영속화합니다.
     */
    @Transactional
    public void allocateCollateralsForCustomer(Long customerId) {
        log.info("⚖️ [CRM 최적화] 고객(ID: {})의 수학적 담보 배분을 시작합니다.", customerId);

        List<CrAccount> accounts = accountRepository.findByCustomer_IdAndIsActiveTrue(customerId);
        List<CrCollateral> collaterals = collateralRepository.findByCustomer_IdAndIsActiveTrue(customerId);

        if (accounts.isEmpty() || collaterals.isEmpty()) {
            log.info("  - 배분할 계좌 또는 담보가 없어 작업을 종료합니다.");
            return;
        }

        // 1. 기존 배분 정보 초기화 (Infrastructure IO)
        for (CrAccount account : accounts) {
            bulkOperationPort.deleteAllocationByAccountId(account.getId());
        }

        try {
            // 2. Pure Domain Calculator 호출 (LP 최적화 계산)
            log.info("  - Domain Calculator LP 최적화 연산 호출 중...");
            List<AllocationResult> results = collateralAllocationCalculator.calculateLpOptimization(accounts, collaterals);
            
            // 3. 연산 결과 저장 (Infrastructure IO)
            saveAllocationResults(results);
            log.info("✅ [CRM 최적화] 고객(ID: {})의 최적 배분 결과를 저장했습니다.", customerId);
        } catch (Exception e) {
            log.error("❌ [CRM 최적화] 최적화 엔진 실행 중 오류 발생. 단순 Waterfall 방식으로 폴백합니다.", e);
            runSimpleWaterfallAllocation(accounts, collaterals);
        }
    }

    /**
     * [Fallback Logic - Application Orchestration] Waterfall 방식 폴백 처리
     */
    private void runSimpleWaterfallAllocation(List<CrAccount> accounts, List<CrCollateral> collaterals) {
        Map<Long, BigDecimal> existingAllocations = new HashMap<>();
        for (CrAccount account : accounts) {
            BigDecimal sum = bulkOperationPort.sumAllocationByAccountId(account.getId());
            existingAllocations.put(account.getId(), sum != null ? sum : BigDecimal.ZERO);
        }

        List<AllocationResult> results = collateralAllocationCalculator.calculateWaterfallAllocation(
                accounts, collaterals, existingAllocations);

        saveAllocationResults(results);
    }

    /**
     * 계산 결과를 DB 엔티티로 매핑하여 저장하는 영속성 조율 메서드
     */
    private void saveAllocationResults(List<AllocationResult> results) {
        for (AllocationResult result : results) {
            CrAccountCollateral mapping = CrAccountCollateral.builder()
                    .account(result.getAccount())
                    .collateral(result.getCollateral())
                    .allocationAmount(result.getAllocatedAmount())
                    .build();
            accountCollateralRepository.save(java.util.Objects.requireNonNull(mapping));
        }
    }
}


package com.ho.account.ecl.core.application.service.crm;

import com.ho.account.ecl.core.application.port.out.CrAccountCollateralRepository;
import com.ho.account.ecl.core.application.port.out.CrBulkOperationPort;
import com.ho.account.ecl.core.application.port.out.CrAccountRepository;
import com.ho.account.ecl.core.application.port.out.CrCollateralRepository;
import com.ho.account.ecl.core.application.port.out.CrCustomerRepository;
import com.ho.account.ecl.core.domain.collateral.CrAccountCollateral;
import com.ho.account.ecl.core.domain.collateral.CrCollateral;
import com.ho.account.ecl.core.domain.exposure.CrAccount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * [Service] [CRM] 아파트/부동산 담보 특화 배분 서비스 (Apartment Collateral Service)
 *
 * 💡 [초보자를 위한 개념 설명]
 * 아파트나 부동산 담보는 일반 담보보다 평가 방식이 조금 더 복잡합니다.
 * KB 시세 같은 시장 가격과 LTV 한도, 선순위 채권 금액 등을 반영하여 실제 인정 가능한 담보 가치를 계산합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApartmentCollateralService {

    private final CrCustomerRepository customerRepository;
    private final CrAccountRepository accountRepository;
    private final CrCollateralRepository collateralRepository;
    private final CrAccountCollateralRepository accountCollateralRepository;
    private final CrBulkOperationPort bulkOperationPort;

    /**
     * [고도화] 특정 고객의 부동산/아파트 담보에 대해 특화 배분 로직을 수행한다.
     * 
     * 💡 [비즈니스 가이드]
     * 일반 담보와 달리 부동산은 실시간 시세가 변하며 LTV(담보인정비율) 규제를 받습니다.
     * 이 메서드는 특정 고객이 가진 부동산 담보의 '진짜 가치'를 계산하고, 
     * 해당 고객의 대출 계좌에 우선적으로 배분합니다.
     */
    @Transactional
    public void processApartmentCollateralsForCustomer(Long customerId) {
        log.info("🏠 [부동산 담보 특화] 고객(ID: {})의 담보 가치 산출 및 배분을 시작합니다.", customerId);

        List<CrAccount> customerAccounts = accountRepository.findByCustomer_IdAndIsActiveTrue(customerId);

        collateralRepository.findByCustomer_IdAndIsActiveTrue(customerId).stream()
                .filter(collateral -> "REAL_ESTATE".equals(collateral.getCollateralType())
                        || "APARTMENT".equals(collateral.getCollateralType()))
                .forEach(collateral -> {
                    // 1. 이미 다른 로직에서 배분된 금액이 있는지 확인 (중복 배분 방지)
                    BigDecimal allocated = bulkOperationPort.sumAllocationByCollateralId(collateral.getId());

                    // 2. 미배분 상태이거나 배분액이 0인 경우에만 특화 배분 수행
                    if (allocated == null || allocated.signum() == 0) {
                        allocateApartmentCollateral(collateral, customerAccounts);
                    }
                });
    }

    /**
     * 아파트/부동산 담보의 유효 가치(Available Collateral Value)를 산출한다.
     * 
     * 💡 [산식 설명]
     * 1. 시장가: KB 시세가 있으면 우선 적용, 없으면 감정평가액 사용.
     * 2. 담보한도: (시장가 * LTV 비율) -> 은행이 법적으로 빌려줄 수 있는 최대치.
     * 3. 선순위 차감: 해당 담보에 이미 다른 빚(선순위 채권)이 있다면 그만큼 뺍니다.
     * 최종 유효 가치 = (시장가 * LTV) - 선순위 채권액
     */
    public BigDecimal calculateEffectiveValue(CrCollateral collateral) {
        log.debug("📊 [가치 산정] 담보코드: {} 의 유효 가액 계산 중...", collateral.getCollateralCode());

        // 1. 시장가 결정 (KB 시세 > 감정가)
        BigDecimal marketValue = (collateral.getKbMarketPrice() != null) ? collateral.getKbMarketPrice()
                : collateral.getAppraisalAmount();

        // 2. LTV 한도 적용 (기본값 70%)
        BigDecimal ltvLimit = (collateral.getLtvLimit() != null) ? collateral.getLtvLimit() : new BigDecimal("0.70");

        // 3. 선순위 채권액 반영
        BigDecimal priorLien = (collateral.getPriorLienAmount() != null) ? collateral.getPriorLienAmount()
                : BigDecimal.ZERO;

        // 4. 공식 적용
        BigDecimal effectiveValue = marketValue.multiply(ltvLimit).subtract(priorLien);
        
        // 결과가 0보다 작으면 0원으로 처리 (빚이 담보가치보다 큰 경우)
        return effectiveValue.max(BigDecimal.ZERO);
    }

    /**
     * 부동산 담보의 유효 가치를 대출 계좌별로 차례대로 나눠줍니다.
     * 
     * 💡 [폭포수 방식 배분]
     * 부동산 담보는 규모가 크기 때문에 한 계좌가 다 쓰지 못하면 다음 계좌로 잔액을 넘겨줍니다.
     * 1. 한도 계산 -> 2. 계좌A 배분 -> 3. 남은 금액 계산 -> 4. 계좌B 배분...
     */
    @Transactional
    public void allocateApartmentCollateral(CrCollateral collateral, List<CrAccount> targetAccounts) {
        // [Safety Check] 부동산/아파트 타입이 아니면 중단
        if (!"REAL_ESTATE".equals(collateral.getCollateralType())
                && !"APARTMENT".equals(collateral.getCollateralType())) {
            return;
        }

        // 1. 위에서 정의한 공식으로 유효 배분 가액 산출
        BigDecimal effectiveCollateralAmt = calculateEffectiveValue(collateral);

        log.info("   -> [{}] 유효 담보가액 산출 완료: {}", collateral.getCollateralCode(), effectiveCollateralAmt);

        // 2. 고객의 여러 대출 계좌들에 순차적으로 '한땀 한땀' 배분합니다.
        BigDecimal remainingCollateralAmt = effectiveCollateralAmt;
        
        for (CrAccount account : targetAccounts) {
            // 더 이상 나눠줄 담보가 없으면 루프 탈출
            if (remainingCollateralAmt.compareTo(BigDecimal.ZERO) <= 0) break;

            // 이미 배분된 금액을 뺀 '진짜 부족한 금액(Uncollateralized)'을 찾습니다.
            BigDecimal alreadyCoveredAmt = bulkOperationPort.sumAllocationByAccountId(account.getId());
            if (alreadyCoveredAmt == null) alreadyCoveredAmt = BigDecimal.ZERO;

            BigDecimal debtToCover = account.getOutstandingAmount().subtract(alreadyCoveredAmt);

            // 보호해야 할 대출 잔액이 남아있을 경우에만 배분 진행
            if (debtToCover.compareTo(BigDecimal.ZERO) > 0) {
                // 남은 담보액과 대출 잔액 중 더 작은 값을 배분액으로 결정
                BigDecimal allocationAmt = remainingCollateralAmt.min(debtToCover);

                // 매핑 데이터 생성 및 저장 (DB 반영)
                CrAccountCollateral allocationRecord = CrAccountCollateral.builder()
                        .account(account)
                        .collateral(collateral)
                        .allocationAmount(allocationAmt)
                        .build();

                accountCollateralRepository.save(java.util.Objects.requireNonNull(allocationRecord));
                
                // 사용한 만큼 담보 잔액 차감
                remainingCollateralAmt = remainingCollateralAmt.subtract(allocationAmt);

                log.debug("    - 계좌 번호 [{}] 에 {} 원 배분 (남은 담보: {})", 
                        account.getAccountNo(), allocationAmt, remainingCollateralAmt);
            }
        }
    }
}

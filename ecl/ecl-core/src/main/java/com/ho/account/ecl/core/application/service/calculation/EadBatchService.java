package com.ho.account.ecl.core.application.service.calculation;

import com.ho.account.shared.finance.enums.CalculationStatus;
import com.ho.account.shared.finance.enums.CrStaging;
import com.ho.account.ecl.core.application.port.out.CrAccountCollateralRepository;
import com.ho.account.ecl.core.application.port.out.CrAccountRepository;
import com.ho.account.ecl.core.application.port.out.CrProductMasterRepository;
import com.ho.account.ecl.core.application.port.out.CrRiskResultRepository;
import com.ho.account.ecl.core.domain.calculator.EadCalculator;
import com.ho.account.ecl.core.domain.calculator.IrbRegulatoryParams;
import com.ho.account.ecl.core.domain.collateral.CrAccountCollateral;
import com.ho.account.ecl.core.domain.exposure.CrAccount;
import com.ho.account.ecl.core.domain.model.CrProductMaster;
import com.ho.account.ecl.core.domain.result.CrRiskResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * [백엔드] 부도 시 익스포저(EAD) 및 CRM(신용위험완화) 산출 배치 서비스.
 * 활성 계좌를 조회하며 보정 EAD와 담보 공제 내역을 산출 결과로 생성합니다.
 *
 * 💡 [초보자를 위한 개념 설명]
 * 이 서비스는 매일 밤 은행의 모든 대출 계좌를 훑으며
 * "만약 내일 고객이 부도가 난다면, 우리가 최종적으로 떼일 돈이 얼마인가?"를 계산합니다.
 * 단순 잔액뿐만 아니라 아직 안 쓴 한도(마이너스 통장 등)와 고객이 맡긴 담보의 가치를
 * 모두 고려하여 '실질적인 위험 금액'을 뽑아냅니다.
 *
 * 🔧 [v2.0 고도화 내역]
 * - 기존: CCF(0.75), 헤어컷(0.20), 담보금액(ZERO) 하드코딩
 * - 변경: 상품 마스터(CrProductMaster), 담보 배분(CrAccountCollateral)에서 실제 값 조회
 * - 추가: IrbRegulatoryParams를 통한 LGD 파라미터 주입
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EadBatchService {

    private final CrRiskResultRepository resultRepository;
    private final CrAccountRepository accountRepository;
    private final CrProductMasterRepository productMasterRepository;
    private final CrAccountCollateralRepository accountCollateralRepository;
    private final EadCalculator calculator;
    private final IrbParameterService irbParameterService;

    /**
     * 전사 EAD 배치 산출을 실행합니다.
     *
     * 💡 [v2.0 변경점]
     * 기존에는 모든 계좌에 동일한 CCF(0.75)와 헤어컷(0.20)을 적용했지만,
     * 이제는 각 계좌의 상품 코드를 통해 마스터 테이블에서 실제 CCF를 조회하고,
     * 배분된 담보의 실제 헤어컷과 가액을 사용합니다.
     *
     * @param baseDate 산출 기준 일자
     * @return 산출 결과 목록
     */
    @Transactional
    public List<CrRiskResult> executeBatch(LocalDate baseDate) {
        log.info("🚀 [EAD 배치] 고도화 EAD/CRM 배치 산출 시작 - 기준일: {}", baseDate);

        // 규제 파라미터 로드 (배치 전체에서 한 번만 조회)
        IrbRegulatoryParams params = irbParameterService.getParameters();
        BigDecimal securedLgd = params.getSecuredLgdFloor();
        BigDecimal unsecuredLgd = params.getUnsecuredLgdFloor();

        List<CrAccount> accounts = accountRepository.findByIsActiveTrue();
        List<CrRiskResult> results = new ArrayList<>();

        int successCount = 0;
        int failCount = 0;

        for (CrAccount account : accounts) {
            try {
                // ── 1단계: 상품 마스터에서 CCF 조회 ──
                // 💡 각 상품(신용대출, 한도대출 등)마다 CCF가 다르므로 마스터 테이블에서 실제 값을 가져옵니다.
                BigDecimal ccfRate = productMasterRepository
                        .findByProductCode(account.getProductCode())
                        .map(CrProductMaster::getCcfRate)
                        .orElseGet(() -> {
                            log.warn("⚠️ [EAD 배치] 상품코드 '{}'의 CCF를 찾을 수 없습니다. 기본값 0.75 적용",
                                    account.getProductCode());
                            return new BigDecimal("0.75"); // 보수적 기본값
                        });

                // ── 2단계: 배분된 담보 정보 조회 ──
                // 💡 CollateralAllocationService가 미리 배분해 놓은 담보 내역을 가져옵니다.
                List<CrAccountCollateral> collaterals = accountCollateralRepository.findByAccount(account);

                BigDecimal totalCollateralAmt = BigDecimal.ZERO;
                BigDecimal weightedHaircutSum = BigDecimal.ZERO;

                for (CrAccountCollateral ac : collaterals) {
                    BigDecimal amt = ac.getAllocationAmount();
                    BigDecimal hc = ac.getCollateral().getBaseHaircut();
                    totalCollateralAmt = totalCollateralAmt.add(amt);
                    weightedHaircutSum = weightedHaircutSum.add(amt.multiply(hc));
                }

                // 가중평균 헤어컷 산출
                BigDecimal effectiveHaircut = totalCollateralAmt.compareTo(BigDecimal.ZERO) > 0
                        ? weightedHaircutSum.divide(totalCollateralAmt, 4, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

                // ── 3단계: 고도화 EAD 산출 ──
                Object[] advResult = calculator.calculateAdvancedEAD(
                        account.getOutstandingAmount(),
                        account.getNotionalAmount(),
                        ccfRate,
                        totalCollateralAmt,
                        effectiveHaircut,
                        securedLgd,    // DB에서 로드한 담보부 LGD
                        unsecuredLgd   // DB에서 로드한 무담보부 LGD
                );

                BigDecimal eadStar = (BigDecimal) advResult[0];
                BigDecimal appliedCcf = (BigDecimal) advResult[1];

                CrRiskResult result = CrRiskResult.builder()
                        .baseDate(baseDate)
                        .account(account)
                        .staging(account.getStaging() != null ? account.getStaging() : CrStaging.STAGE1)
                        .ead(eadStar)
                        .appliedCcf(appliedCcf)
                        .status(CalculationStatus.COMPLETED)
                        .calculationCompletedAt(LocalDateTime.now())
                        .build();

                results.add(result);
                successCount++;

            } catch (Exception e) {
                log.error("❌ [EAD 배치] 계좌 '{}' 산출 중 오류 발생: {}", account.getAccountNo(), e.getMessage());
                results.add(CrRiskResult.builder()
                        .baseDate(baseDate)
                        .account(account)
                        .staging(CrStaging.STAGE1)
                        .status(CalculationStatus.FAILED)
                        .errorMessage(e.getMessage())
                        .build());
                failCount++;
            }
        }

        log.info("✅ [EAD 배치] 산출 완료 - 성공: {}건, 실패: {}건, 총: {}건",
                successCount, failCount, accounts.size());

        resultRepository.saveAll(results);
        return results;
    }

    /**
     * 특정 기준일의 산출 결과를 페이징 조회합니다.
     * 검증 및 모니터링 용도입니다.
     */
    public Page<CrRiskResult> getResultsForVerification(LocalDate baseDate, Pageable pageable) {
        return resultRepository.findByBaseDate(baseDate, pageable);
    }
}

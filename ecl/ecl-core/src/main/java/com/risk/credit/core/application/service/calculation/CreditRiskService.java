package com.risk.credit.core.application.service.calculation;

import com.risk.credit.core.application.pipeline.RiskDataQualityService;
import com.risk.credit.core.application.port.out.CrAccountRepository;
import com.risk.credit.core.application.port.out.CrCustomerRepository;
import com.risk.credit.core.application.port.out.CrProductMasterRepository;
import com.risk.credit.core.application.port.out.CrAccountCollateralRepository;
import com.risk.credit.core.application.port.out.CrBulkOperationPort;
import com.risk.credit.core.application.port.out.CrRiskResultRepository;
import com.risk.credit.core.application.port.out.CrGradeMasterRepository;
import com.risk.credit.core.application.port.out.CrLgdSegmentMasterRepository;
import com.risk.credit.core.application.port.out.CrRegulatoryParameterRepository;
import com.risk.credit.core.application.service.crm.ApartmentCollateralService;
import com.risk.credit.core.application.service.crm.CollateralAllocationService;
import com.risk.credit.core.domain.calculator.CreditRiskCalculator;
import com.risk.credit.core.domain.calculator.EadCalculator;
import com.risk.credit.core.domain.calculator.IrbRegulatoryParams;
import com.risk.credit.core.domain.collateral.CrAccountCollateral;
import com.risk.credit.core.domain.exposure.CrAccount;
import com.risk.credit.core.domain.exposure.CrCustomer;
import com.risk.credit.core.domain.model.CrGradeMaster;
import com.risk.credit.core.domain.model.CrLgdSegmentMaster;
import com.risk.credit.core.domain.model.CrProductMaster;
import com.risk.credit.core.domain.model.CrRegulatoryParameter;
import com.risk.credit.core.domain.model.SaRwMapper;
import com.risk.credit.core.domain.result.CrRiskResult;
import com.risk.common.enums.CalculationStatus;
import com.risk.common.enums.CrStaging;
import com.risk.common.enums.CustomerType;
import com.risk.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * [Service] 고도화 신용 리스크 분석 서비스 (Advanced Credit Risk Analysis Service)
 * IFRS 9 스테이징, Basel III/IV EAD, CRM 및 RWA (표준방법/내부등급법) 산출을 통합합니다.
 *
 * 💡 [초보자를 위한 개념 설명]
 * 이 서비스는 신용 리스크 시스템의 '두뇌' 역할을 합니다.
 * 여러 데이터(계좌, 고객, 담보 등)를 한데 모으고, 약속된 '엔진'(계산기)을 돌려
 * 은행이 현재 지고 있는 손실과 대비해 얼마만큼의 자본을 보유해야 하는지 결정합니다.
 *
 * 🔧 [v2.0 고도화 내역]
 * - IrbParameterService를 통해 DB에서 규제 파라미터를 로드하여 산출에 사용
 * - CreditRiskCalculator의 고도화 메서드를 호출하여 차주 유형별 IRB 산식 분기
 * - EadCalculator의 고도화 메서드를 호출하여 DB 기반 LGD 적용
 * - LifetimePdService의 전이행렬 기반 Lifetime PD 곡선 생성
 * - PD 미존재 시 기본값 하드코딩 제거 → IrbRegulatoryParams의 PD Floor 사용
 *
 * 💡 [초보자를 위한 가이드 - 산출 파이프라인]
 * 리스크 산출은 거대한 컨베이어 벨트와 같습니다.
 * 1. 데이터 검증: 재료가 싱싱한지 확인 (DQ)
 * 2. 상태 분류: 환자가 얼마나 아픈지 진단 (Staging)
 * 3. 부도 확률: 망할 확률 계산 (PD)
 * 4. 손실 예측: 망했을 때 얼마를 떼일지 계산 (LGD)
 * 5. 노출 금액: 그때 빌려준 돈이 정확히 얼마일지 계산 (EAD)
 * 6. 최종 Risk: 위 값들을 곱해서 '위험 가중치'와 '충당금' 산출 (RWA, ECL)
 *
 * 📊 [산출 파이프라인 요약]
 * ① DQ(데이터 품질) 검증
 * ② IFRS 9 스테이징 판정
 * ③ PD 조회 (등급 마스터 → PD Floor 적용)
 * ④ Lifetime PD 곡선 생성 (전이행렬 기반)
 * ⑤ 담보 집계 및 LGD 결정
 * ⑥ EAD* 산출 (CCF + CRM)
 * ⑦ ECL(기대손실) 산출
 * ⑧ RWA 산출 (SA + IRB)
 * ⑨ 결과 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CreditRiskService {

        private final CrAccountRepository accountRepository;
        private final CrCustomerRepository customerRepository;
        private final CrProductMasterRepository productMasterRepository;
        private final CrRiskResultRepository riskResultRepository;
        private final CollateralAllocationService collateralAllocationService;
        private final ApartmentCollateralService apartmentCollateralService;

        private final StagingService stagingService;
        private final CreditRiskCalculator riskCalculator;
        private final SaRwMapper saRwMapper;
        private final RiskDataQualityService dqService;
        private final LifetimePdService lifetimePdService;
        private final ForwardLookingEclService forwardLookingEclService;
        private final CrBulkOperationPort bulkOperationPort;

        /** [v2.5] 도메인 특화 산출 서비스 연동 */
        private final PdCalculationService pdCalculationService;
        private final LgdCalculationService lgdCalculationService;
        private final CcfCalculationService ccfCalculationService;
        private final EadCrmCalculationService eadCrmCalculationService;

        /** [v2.0] IRB 규제 파라미터 로딩 서비스 */
        private final IrbParameterService irbParameterService;

        /**
         * [고도화] 기준 일자의 기존 산출 결과를 삭제하여 데이터 중복을 방지한다. (Idempotency 보장)
         * 💡 산출을 처음부터 다시 시작할 때 호출합니다.
         */
        @Transactional
        public void clearPreviousResults(LocalDate baseDate) {
                bulkOperationPort.clearBatchResults(baseDate);
        }

        /**
         * [고도화] 모든 리스크 산출용 기저 데이터 캐시를 최신화한다.
         * 💡 병렬 처리를 수행하는 Worker 스레드들이 DB 조회 없이 메모리에서 즉시 값을 참조할 수 있도록 합니다.
         */
        @Transactional(readOnly = true)
        public void refreshAllCaches(LocalDate baseDate) {
                log.info("📦 [캐시 워밍업] 모든 도메인 기저 데이터의 캐시를 로드합니다.");
                
                stagingService.refreshRankCache();      // 등급 순서(Notch Order) 캐시 갱신
                pdCalculationService.refreshCache();    // 등급별 PD 값 캐시 갱신
                ccfCalculationService.refreshCache();   // 상품별 CCF 비율 캐시 갱신
                irbParameterService.refreshCache();     // IRB 규제 파라미터 캐시 갱신
                lifetimePdService.refreshCache(baseDate); // [v2.0] 전이행렬 캐시 갱신
                forwardLookingEclService.refreshCache();
                
                log.info("✅ [캐시 워밍업] 모든 기저 데이터 캐시 로드가 완료되었습니다.");
        }

        /**
         * 특정 계좌에 대해 신용 리스크 산출을 실행한다.
         *
         * 💡 [산출 흐름 단계별 안내]
         * 이 메서드는 한 건의 계좌에 대해 아래 9단계를 순서대로 수행합니다.
         * 각 단계가 어떤 역할을 하는지 주석으로 상세히 설명합니다.
         *
         * @param accountId 산출 대상 계좌 ID
         * @param baseDate  산출 기준 일자 (리스크 시점)
         * @return 산출 결과 엔티티 (DB에 저장된 상태)
         */
        @Transactional
        @NonNull
        @SuppressWarnings("null")
        public CrRiskResult calculateAccountRisk(@NonNull Long accountId, @NonNull LocalDate baseDate) {
                CrAccount account = accountRepository.findById(accountId)
                                .orElseThrow(() -> new ResourceNotFoundException("CrAccount", accountId));

                // ──────────────────────────────────────────
                // 0단계: 데이터 품질(DQ) 검증
                // 💡 원천 데이터의 무결성을 먼저 확인합니다.
                //    필수 필드 누락, 비정상 값 등이 있으면 산출을 중단합니다.
                // ──────────────────────────────────────────
                if (!dqService.validate(account)) {
                        CrRiskResult failedResult = CrRiskResult.builder()
                                        .baseDate(baseDate)
                                        .account(account)
                                        .staging(CrStaging.STAGE1)
                                        .status(CalculationStatus.FAILED)
                                        .errorMessage("DQ 검증 실패")
                                        .build();
                        riskResultRepository.save(failedResult);
                        return failedResult;
                }

                CrCustomer customer = account.getCustomer();
                CustomerType customerType = customer.getCustomerType();

                // ──────────────────────────────────────────
                // [v2.0] IRB 규제 파라미터 로드
                // 💡 DB의 cr_regulatory_parameters 테이블에서 PD Floor, 상관계수,
                //    만기조정 상수 등 모든 규제 상수를 한 번에 로드합니다.
                // ──────────────────────────────────────────
                IrbRegulatoryParams irbParams = irbParameterService.getParameters();

                // ──────────────────────────────────────────
                // 1단계: IFRS 9 스테이징 판정
                // 💡 연체 일수, 조기경보, 채권재조정 여부 등을 종합하여
                //    이 계좌가 Stage 1(정상) / 2(주의) / 3(손상) 중 어디에 해당하는지 결정합니다.
                // ──────────────────────────────────────────
                CrStaging stage = stagingService.determineStage(
                                account,
                                customer.getWarningLevel(),
                                Boolean.TRUE.equals(account.getIsDebtRestructured()));

                // ──────────────────────────────────────────
                // 잔여 만기 계산
                // 💡 만기까지 남은 기간을 년 단위로 계산합니다.
                //    바젤 규정상 최소 1년을 적용합니다.
                //    만기 정보가 없으면 기본 2.5년을 사용합니다.
                // ──────────────────────────────────────────
                double maturityYears = 2.5; // 기본 잔여만기 2.5년
                if (account.getMaturityDate() != null) {
                        long days = ChronoUnit.DAYS.between(baseDate, account.getMaturityDate());
                        maturityYears = Math.max(days / 365.0, 1.0);
                }

                // ──────────────────────────────────────────
                // ──────────────────────────────────────────
                // 2단계: PD(부도율) 조회 및 동적 보정 (고도화 반영)
                // 💡 [v2.4] 계좌 레벨 등급 우선 순위 적용 및 연체 할증 도입
                // ──────────────────────────────────────────
                CrProductMaster product = productMasterRepository.findByProductCode(account.getProductCode())
                                .orElse(CrProductMaster.builder().ccfRate(new BigDecimal("0.75")).build());

                // ──────────────────────────────────────────
                // 2단계: PD(부도율) 조회 및 동적 보정 (PdCalculationService 연동)
                // ──────────────────────────────────────────
                BigDecimal finalPd = pdCalculationService.calculatePd(account, irbParams);

                // ──────────────────────────────────────────
                // 3단계: Lifetime PD 곡선 생성
                // 💡 [v2.0] 전이행렬 기반 고도화 산출
                // ──────────────────────────────────────────
                List<BigDecimal> marginalPds = lifetimePdService.generateMarginalPdCurve(
                                finalPd, maturityYears,
                                customer.getInternalRating(), baseDate);

                // ──────────────────────────────────────────
                // 4단계: CCF 산출 (CcfCalculationService 연동)
                // ──────────────────────────────────────────
                BigDecimal finalCcf = ccfCalculationService.calculateCcf(account.getProductCode());

                // ──────────────────────────────────────────
                // 5단계: EAD* 및 CRM 산출 (EadCrmCalculationService 연동)
                // ──────────────────────────────────────────
                EadCrmCalculationService.EadCrmResult eadCrm = eadCrmCalculationService.calculateEadCrm(account, finalCcf, irbParams);
                
                BigDecimal eadStar = eadCrm.getEadStar();
                BigDecimal appliedCcf = eadCrm.getAppliedCcf();
                BigDecimal eadRaw = eadCrm.getEadRaw();
                BigDecimal crmDeduction = eadCrm.getCrmDeduction();

                // ──────────────────────────────────────────
                // 6단계: LGD 결정 (LgdCalculationService 연동)
                // ──────────────────────────────────────────
                BigDecimal finalLgd = lgdCalculationService.calculateLgd(
                        customerType.name(), 
                        eadCrm.getMajorCollateralType(), 
                        eadCrm.getTotalCollateralAmt().compareTo(BigDecimal.ZERO) > 0, 
                        irbParams
                );

                // ──────────────────────────────────────────
                // 6단계: IFRS 9 ECL(기대손실) 산출 (Forward-Looking 시나리오 반영)
                // 💡 [v2.0] 거시경제 시나리오(호황, 평균, 침체)별 가중평균 ECL 산출
                // ──────────────────────────────────────────
                ForwardLookingEclService.FlEclResult flEcl = forwardLookingEclService.calculateWeightedEcl(
                                stage, marginalPds, finalLgd, eadStar,
                                irbParams.getDefaultDiscountRate(), // [v2.0] 파라미터화된 할인율 사용
                                baseDate.getYear());

                // ──────────────────────────────────────────
                // 7단계: RWA(위험가중자산) 산출
                // ──────────────────────────────────────────
                // 7-1. 표준방법(SA) RWA
                BigDecimal appliedRw = saRwMapper.getStandardRw(customerType,
                                customer.getInternalRating());
                BigDecimal rwaSa = riskCalculator.calculateRwaSa(eadStar, appliedRw);

                // 7-2. [v2.0] 내부등급법(IRB) RWA - 차주 유형별 분기 + 규제 파라미터 적용
                CreditRiskCalculator.IrbResult irbResult = riskCalculator.calculateRwaIrb(
                                finalPd, finalLgd, eadStar,
                                maturityYears,
                                irbParams,       // [v2.0] DB에서 로드한 규제 파라미터
                                customerType,    // [v2.0] 차주 유형별 IRB 산식 분기
                                customer.getAnnualSales(), // [고도화] 실제 차주 매출액 반영
                                customer.getFinancialSectorCode()); // [v4.1 고도화] 업권 코드 추가 반영

                // ──────────────────────────────────────────
                // 8단계: 결과 저장
                // 💡 모든 산출 결과와 중간값(K, R, 만기조정 등)을 저장하여
                //    감사 추적(Audit Trail)이 가능하도록 합니다.
                // ──────────────────────────────────────────
                CrRiskResult result = CrRiskResult.builder()
                                .id(account.getId()) // [v3.0] 계좌 ID를 결과 ID로 사용 (단계별 연동 안정성 확보)
                                .baseDate(baseDate).account(account).staging(stage)
                                .ead(eadRaw).eadStar(eadStar).appliedCcf(appliedCcf).crmDeduction(crmDeduction)
                                .pd(finalPd).lgd(finalLgd)
                                .expectedLoss(flEcl.getWeightedEcl()) // 가중평균 ECL 저장
                                .unexpectedLoss(riskCalculator.calculateUnexpectedLoss(finalPd, finalLgd, eadStar))
                                .eclBoom(flEcl.getEclBoom())
                                .eclBase(flEcl.getEclBase())
                                .eclRecession(flEcl.getEclRecession())
                                .weightedEcl(flEcl.getWeightedEcl())
                                .appliedRw(appliedRw).rwaSa(rwaSa).rwaIrb(irbResult.getRwa())
                                .kValue(irbResult.getKValue()).rValue(irbResult.getRValue())
                                .maturityAdj(irbResult.getMaturityAdj())
                                .status(CalculationStatus.COMPLETED).calculationCompletedAt(LocalDateTime.now())
                                .build();

                riskResultRepository.save(result);
                return result;
        }

        /**
         * [고도화] 전사 신용리스크 산출 요약 지표를 조회합니다.
         * 
         * 💡 [초보자를 위한 가이드]
         * 개별 계좌 수백만 건의 산출 결과를 다 더해서 "우리 은행 전체의 신용 리스크가 얼마인가"를 
         * 요약해주는 '정산서' 같은 역할을 합니다.
         */
        public Object getResultsSummary(LocalDate baseDate) {
                log.info("📊 [요약 조회] {} 기준 전사 신용리스크 요약 지표 집계 시작", baseDate);
                
                List<CrRiskResult> results = riskResultRepository.findAllByBaseDate(baseDate);
                
                if (results.isEmpty()) {
                        return java.util.Map.of("message", "산출 결과가 없습니다.");
                }

                // 💡 [Java Stream 설명] 
                // 전체 목록에서 필요한 수치들을 뽑아내어 합계(sum)나 평균(average)을 냅니다.
                BigDecimal totalEad = results.stream()
                                .map(r -> r.getEadStar() != null ? r.getEadStar() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalRwaIrb = results.stream()
                                .map(r -> r.getRwaIrb() != null ? r.getRwaIrb() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalEcl = results.stream()
                                .map(r -> r.getExpectedLoss() != null ? r.getExpectedLoss() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                double avgPd = results.stream()
                                .mapToDouble(r -> r.getPd() != null ? r.getPd().doubleValue() : 0.0)
                                .average().orElse(0.0);

                // 스테이징별(정상/주의/손상) 분포 집계
                java.util.Map<String, Long> stagingSummary = results.stream()
                                .collect(Collectors.groupingBy(r -> r.getStaging().name(), Collectors.counting()));

                return java.util.Map.of(
                        "baseDate", baseDate,
                        "totalEad", totalEad,
                        "totalRwaIrb", totalRwaIrb,
                        "totalEcl", totalEcl,
                        "avgPd", avgPd,
                        "stagingSummary", stagingSummary
                );
        }
}

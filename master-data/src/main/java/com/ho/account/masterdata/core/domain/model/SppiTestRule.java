package com.ho.account.masterdata.core.domain.model;

/**
 * IFRS 9 회계 기준에 따른 SPPI (Solely Payments of Principal and Interest) 테스트 규칙을 정의하는 도메인 모델입니다.
 * 
 * <p><b>초보자 가이드(Beginner's Guide):</b></p>
 * <p>
 * SPPI 테스트는 금융자산을 장부에 어떤 방식(분류)으로 기록할지 결정하는 가장 중요한 관문 중 하나입니다.
 * 어떤 대출이나 채권이 "순수하게 원금과 이자로만" 구성되어 있다면 SPPI 테스트를 통과(Pass)한 것으로 봅니다.
 * 통과하면 보통 AC(상각후원가)로 회계처리하여 이자만 인식하면 되지만, 
 * 만약 원금/이자가 아닌 주식 전환권 같은 복잡한 파생 옵션이 섞여 있어 실패(Fail)하면 FVPL(당기손익-공정가치) 
 * 등으로 분류하여 매번 시장가치(시가)로 재평가해야 합니다.
 * </p>
 */
public class SppiTestRule {

    private final String productCode;
    private final boolean hasEquityConversionOption;
    private final boolean hasLeverageFeature;
    private final boolean hasNonRecourseFeature;

    public SppiTestRule(String productCode, boolean hasEquityConversionOption, 
                        boolean hasLeverageFeature, boolean hasNonRecourseFeature) {
        this.productCode = productCode;
        this.hasEquityConversionOption = hasEquityConversionOption;
        this.hasLeverageFeature = hasLeverageFeature;
        this.hasNonRecourseFeature = nonRecourseFeatureCheck(hasNonRecourseFeature);
    }

    private boolean nonRecourseFeatureCheck(boolean feature) {
        // 소구권 없는 대출(Non-Recourse Loan)은 기초자산의 성과에 따라 원리금이 달라질 위험이 커서
        // SPPI 요건을 깐깐하게 봅니다.
        return feature;
    }

    /**
     * SPPI 테스트를 수행합니다.
     * 
     * @return SPPI 요건 충족 여부 (true: 통과, false: 실패)
     */
    public boolean evaluate() {
        /*
         * [SPPI 실패 조건]
         * 1. 주식 전환권(Equity Conversion Option)이 있는 경우 (원리금 외 수익 변동)
         * 2. 레버리지 특징이 있는 경우
         * (※ 실무에서는 훨씬 복잡하지만, 핵심 개념인 '원금과 이자 외의 수익/손실 요인'이 있으면 Fail)
         */
        if (hasEquityConversionOption) {
            return false;
        }
        if (hasLeverageFeature) {
            return false;
        }

        // 특정 비소구권 특징이 있더라도 기초자산이 원리금을 충분히 커버하는지 등의 추가 로직이 필요하나,
        // 여기서는 단순화하여 옵션이 없으면 통과로 간주.
        return true;
    }

    public String getProductCode() {
        return productCode;
    }
}

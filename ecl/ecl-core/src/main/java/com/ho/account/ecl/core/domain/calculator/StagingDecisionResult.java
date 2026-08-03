package com.ho.account.ecl.core.domain.calculator;

import com.ho.account.shared.finance.enums.CrStaging;
import lombok.Builder;
import lombok.Getter;

/**
 * [도메인 VO] IFRS 9 스테이징 판정 결과.
 * 
 * 💡 [초보자를 위한 개념 설명]
 * 이 객체는 대출 자산이 Stage 1(정상), Stage 2(주의), Stage 3(손상) 중 어디로 결정되었는지와
 * 그렇게 판정된 '이유(Primary Trigger)' 및 세부 지표(연체일수, 등급하락폭 등)를 보존하는 불변 객체입니다.
 */
@Getter
@Builder
public class StagingDecisionResult {

    /** 최종 결정된 IFRS 9 단계 (STAGE1, STAGE2, STAGE3) */
    private final CrStaging stage;

    /** 스테이징 결정의 주된 사유 (예: "DELINQUENCY_90_DAYS", "NOTCH_DOWNGRADE_GE_3", "NORMAL") */
    private final String primaryTrigger;

    /** 평가 시점 연체 일수 */
    private final int delinquentDays;

    /** 등급 하락 노치 차이 (현재 등급 랭크 - 최초 실행 등급 랭크) */
    private final int notchDowngrade;

    /** 조기경보 등급 ("NORMAL", "WARNING", "CRITICAL") */
    private final String warningLevel;

    /** 채권재조정 여부 */
    private final boolean isDebtRestructured;
}

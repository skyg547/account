package com.ho.account.closing.core.domain.model;

/**
 * 은행 시스템의 일일 마감(End of Day) 및 개시(Beginning of Day) 상태를 정의하는 Enum 클래스입니다.
 * 
 * <p><b>초보자 가이드(Beginner's Guide):</b></p>
 * <p>
 * 은행 회계 시스템에서는 하루의 영업이 끝난 후(자정 부근)에 '일일 마감(EOD, End of Day)' 배치를 돌립니다.
 * 이 배치가 도는 동안에는 원장(Ledger)에 데이터가 들어오거나 빠져나가는 트랜잭션이 발생하면 안 됩니다.
 * 따라서 시스템 전체의 상태를 통제(Control)하기 위해 이 상태 머신(State Machine)을 사용합니다.
 * </p>
 * 
 * <ul>
 * <li><b>OPEN</b>: 주간 영업 상태. 모든 입출금 트랜잭션이 허용됩니다.</li>
 * <li><b>PRE_CLOSING</b>: 마감 준비. 더 이상의 신규 외부 거래는 막고, 내부 정산만 허용합니다.</li>
 * <li><b>CLOSING_IN_PROGRESS</b>: 마감 배치 수행 중 (Accrual, GL 집계 등). 모든 트랜잭션 차단.</li>
 * <li><b>CLOSED</b>: 마감 완료. 다음 영업일로 넘어갈 준비가 된 상태.</li>
 * <li><b>BOD_IN_PROGRESS</b>: 영업 개시(Beginning of Day) 준비 중. 시스템 초기화 및 날짜 변경 처리.</li>
 * </ul>
 */
public enum EodState {

    /**
     * 주간 영업 상태
     * - 모든 정상적인 입/출금, 대출 실행 등의 트랜잭션 허용.
     */
    OPEN("영업 중", true),

    /**
     * 마감 준비 상태
     * - 외부망(대외계 등)으로부터의 유입을 차단하고, 내부 계류 중인 거래만 정리함.
     */
    PRE_CLOSING("마감 준비", false),

    /**
     * EOD 배치 수행 상태
     * - Accrual(이자 발생), 외화 평가(FX Revaluation), 대손충당금(ECL) 등 핵심 무거운 배치 수행.
     * - 원장 변동 절대 금지.
     */
    CLOSING_IN_PROGRESS("EOD 마감 진행 중", false),

    /**
     * EOD 마감 완료 상태
     * - 하루의 모든 장부가 닫힘.
     */
    CLOSED("마감 완료", false),

    /**
     * BOD (Beginning of Day) 처리 상태
     * - 영업일자를 다음 날짜로 갱신하고, 시스템 리소스를 초기화함.
     * - 처리가 끝나면 다시 OPEN 상태로 전이됨.
     */
    BOD_IN_PROGRESS("영업 개시 준비 중", false);

    private final String description;
    private final boolean transactionAllowed;

    EodState(String description, boolean transactionAllowed) {
        this.description = description;
        this.transactionAllowed = transactionAllowed;
    }

    /**
     * 해당 상태일 때 트랜잭션 처리가 허용되는지 여부를 반환합니다.
     * 
     * @return 거래 허용 여부 (true면 허용, false면 차단)
     */
    public boolean isTransactionAllowed() {
        return transactionAllowed;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 다음 논리적 상태로 전이할 수 있는지 검사하는 메서드.
     * 
     * <p><b>아키텍처 팁:</b> 무분별한 상태 변경을 막기 위해 상태 전이 제약조건(Transition Rules)을
     * 도메인 내부에 캡슐화(Encapsulation)하여 비즈니스 정합성을 보장합니다.</p>
     */
    public boolean canTransitionTo(EodState nextState) {
        switch (this) {
            case OPEN:
                return nextState == PRE_CLOSING;
            case PRE_CLOSING:
                return nextState == CLOSING_IN_PROGRESS || nextState == OPEN; // 롤백 가능
            case CLOSING_IN_PROGRESS:
                return nextState == CLOSED;
            case CLOSED:
                return nextState == BOD_IN_PROGRESS;
            case BOD_IN_PROGRESS:
                return nextState == OPEN;
            default:
                return false;
        }
    }
}

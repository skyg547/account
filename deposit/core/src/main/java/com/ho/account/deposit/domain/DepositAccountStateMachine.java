package com.ho.account.deposit.domain;

/**
 * [순수 도메인 객체 (Pure Domain Model)] 수신(Deposit) 계좌 상태 전이 검증기.
 *
 * 💡 [DDD 원칙 - 도메인 계층의 프레임워크 독립성 (Framework Decoupling)]
 * 도메인 계층은 특정 프레임워크(Spring Framework 등)에 의존하지 않는 순수 자바 객체(POJO)여야 합니다.
 * 1. **프레임워크 독립성**: `@Component` 등 프레임워크 어노테이션을 제거하여 순수한 비즈니스 규칙만 포함합니다.
 * 2. **테스트 용이성**: Spring ApplicationContext 실행 없이 매우 빠른 단위 테스트(POJO Unit Test) 작성이 가능합니다.
 * 3. **헥사고날 아키텍처 코어**: 도메인 모델은 애플리케이션의 핵심(Core) 영역으로 외부 프레임워크, DB, WEB 인프라 변경으로부터 완전히 보호됩니다.
 *
 * 💡 [초보자를 위한 상태 머신 설명]
 * 수신 계좌는 생성된 순간부터 해지될 때까지 특정 법칙에 따라서만 상태가 바뀔 수 있습니다.
 * 예를 들어, 이미 해지된(`CLOSED`) 계좌를 다시 활성화(`ACTIVE`)할 수 없으며,
 * 정지된(`SUSPENDED`) 계좌는 해지 또는 상태 해제만 가능합니다.
 *
 * 📌 [허용 상태 전이 테이블]
 * - PENDING_OPEN -> ACTIVE, CLOSED
 * - ACTIVE -> MATURED, EARLY_TERMINATED, CLOSED, DORMANT, SUSPENDED
 * - MATURED -> CLOSED
 * - EARLY_TERMINATED -> CLOSED
 * - SUSPENDED -> ACTIVE, CLOSED
 * - DORMANT -> ACTIVE, CLOSED
 * - CLOSED -> (최종 상태, 전이 불가)
 */
public class DepositAccountStateMachine {

    public void validateStateTransition(DepositStatus currentStatus, DepositStatus targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            throw new IllegalArgumentException("currentStatus and targetStatus are required.");
        }
        if (currentStatus == targetStatus) {
            return; // 동일 상태 유지 허용
        }

        boolean isValid = switch (currentStatus) {
            case ACTIVE -> targetStatus == DepositStatus.CLOSED || targetStatus == DepositStatus.DORMANT || targetStatus == DepositStatus.SUSPENDED;
            case DORMANT, SUSPENDED -> targetStatus == DepositStatus.ACTIVE || targetStatus == DepositStatus.CLOSED;
            case CLOSED -> false; // 이미 해지된 계좌는 상태 변경 불가
        };

        if (!isValid) {
            throw new IllegalStateException(
                    String.format("Invalid deposit account state transition from %s to %s", currentStatus, targetStatus));
        }
    }
}

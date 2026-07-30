package com.ho.account.budget.application.port.out;

/**
 * 멱등성 키 조회 전에 직렬화 지점을 획득하는 출력 Port입니다.
 *
 * <p>구현체는 사전 생성된 고정 개수 shard 행 중 안정적인 hash로 선택한 행을
 * pessimistic write lock 해야 합니다. 따라서 아직 결과 행이 없는 첫 동시 요청도
 * 동일 shard에서 순서대로 실행됩니다.</p>
 */
public interface BudgetIdempotencyLockPort {

    void lockTransferRequestKey(String requestKey);

    void lockExecutionSourceKey(String sourceType, String sourceId, String sourceLineId);
}

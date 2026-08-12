package com.ho.account.ecl.api.port;

/**
 * 이미 완료된 이벤트/배치 실행 요청 시 발생하는 예외.
 * Kafka Consumer 등의 멱등성(Idempotency) 처리에 활용됩니다.
 */
public class BatchAlreadyCompletedException extends RuntimeException {
    public BatchAlreadyCompletedException(String message) {
        super(message);
    }
}

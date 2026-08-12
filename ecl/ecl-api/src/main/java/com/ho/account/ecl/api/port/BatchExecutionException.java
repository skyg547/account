package com.ho.account.ecl.api.port;

/**
 * 배치 트리거 및 실행 과정에서 오류 발생 시 반환되는 전용 예외.
 */
public class BatchExecutionException extends RuntimeException {
    public BatchExecutionException(String message) {
        super(message);
    }

    public BatchExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}

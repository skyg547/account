package com.ho.account.masterdata.core.domain.exception;

/**
 * 하나의 외부 승인 식별자가 서로 다른 기준정보 변경에 재사용됐음을 나타냅니다.
 */
public class MasterDataIdempotencyConflictException extends IllegalStateException {

    public MasterDataIdempotencyConflictException(String sourceReference) {
        super("Master-data source reference conflicts with an existing request: " + sourceReference);
    }
}

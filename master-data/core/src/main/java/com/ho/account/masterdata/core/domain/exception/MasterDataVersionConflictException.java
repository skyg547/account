package com.ho.account.masterdata.core.domain.exception;

/**
 * 요청이 기준으로 삼은 SCD2 순번과 현재 저장 이력이 달라졌음을 나타냅니다.
 */
public class MasterDataVersionConflictException extends IllegalStateException {

    public MasterDataVersionConflictException(String message) {
        super(message);
    }
}
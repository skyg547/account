package com.ho.account.masterdata.core.domain.exception;

/**
 * 요청한 SCD2 순번이 현재 이력과 충돌하거나 CREATE 업무 키의 이력이 이미 존재함을 나타냅니다.
 */
public class MasterDataVersionConflictException extends IllegalStateException {

    public MasterDataVersionConflictException(String message) {
        super(message);
    }
}

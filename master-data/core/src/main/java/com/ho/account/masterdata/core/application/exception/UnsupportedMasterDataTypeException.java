package com.ho.account.masterdata.core.application.exception;

import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;

/**
 * 아직 실제 도메인 유즈케이스와 영속성 어댑터가 연결되지 않은 기준정보 유형입니다.
 */
public class UnsupportedMasterDataTypeException extends IllegalArgumentException {

    public UnsupportedMasterDataTypeException(MasterDataType targetType) {
        super("No MasterDataChangeApplier supports targetType: " + targetType);
    }
}
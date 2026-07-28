package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;

/**
 * 승인된 기준정보 변경 요청을 targetType별 실제 업무 유즈케이스에 연결하는 전략입니다.
 *
 * <p>지원하지 않는 유형은 변경 요청 서비스에서 fail-closed로 중단되므로, 실제 SCD2 반영 없이
 * 상태만 APPLIED로 바뀌지 않습니다.</p>
 */
public interface MasterDataChangeApplier {

    // @todo CURRENCY, EXCHANGE_RATE, FISCAL_PERIOD도 도메인 서비스와 영속성 포트를 완성한 뒤
    // typed applier와 버전 조회 어댑터를 함께 연결해야 한다. 구현 전에는 요청 접수부터 fail-closed한다.
    MasterDataChangeRequest.MasterDataType targetType();

    void apply(MasterDataChangeRequest request);
}
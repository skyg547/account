package com.ho.account.masterdata.core.application.port.out;

import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;

/**
 * 변경 요청의 직렬화된 payload를 애플리케이션 command로 복원하는 출력 포트입니다.
 *
 * <p>core의 applier는 JSON/Jackson 기술을 직접 알지 않고 이 계약만 사용합니다.</p>
 */
public interface MasterDataChangePayloadDecoder {

    <T> T decode(MasterDataChangeRequest request, Class<T> payloadType);
}
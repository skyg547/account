package com.ho.account.masterdata.core.application.port.out;

import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;

/**
 * 업무 키별로 지금까지 저장된 SCD2 이력 개수를 조회하는 출력 포트입니다.
 *
 * <p>애플리케이션 서비스는 테이블이나 JPA Repository를 모르고도 변경 요청의
 * {@code requestedVersion}이 현재 이력 다음 순서인지 검증할 수 있습니다.</p>
 */
public interface MasterDataVersionQueryPort {

    long countPersistedVersions(MasterDataType targetType, String targetKey);
}
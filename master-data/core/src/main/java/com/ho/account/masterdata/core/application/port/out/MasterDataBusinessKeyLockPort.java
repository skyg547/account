package com.ho.account.masterdata.core.application.port.out;

import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;

/**
 * 같은 기준정보 업무 키의 변경을 현재 트랜잭션이 끝날 때까지 직렬화하는 출력 포트입니다.
 *
 * <p>아직 SCD2 행이 없는 CREATE도 잠금 대상입니다. 같은 트랜잭션에서 같은 키를 다시
 * 잠글 수 있어야 하며, 서로 다른 키는 독립적으로 진행합니다. 호출자는 잠금 후 현재 이력과
 * 버전을 다시 확인하고 실제 변경 및 승인 상태 저장까지 같은 트랜잭션에 포함해야 합니다.</p>
 */
public interface MasterDataBusinessKeyLockPort {

    void lock(MasterDataType targetType, String targetKey);
}

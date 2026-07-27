package com.ho.account.masterdata.core.application.port.out;

import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 마스터 데이터 변경 요청 영속성 포트
 */
public interface MasterDataChangeRequestPersistencePort {

    Optional<MasterDataChangeRequest> findById(Long id);

    /**
     * 승인, 반려, 반영처럼 상태를 바꾸는 흐름에서 동일 요청을 직렬화합니다.
     */
    Optional<MasterDataChangeRequest> findByIdForUpdate(Long id);

    Optional<MasterDataChangeRequest> findBySourceReference(String sourceReference);

    List<MasterDataChangeRequest> findAll();

    List<MasterDataChangeRequest> findByStatus(ChangeStatus status);

    List<MasterDataChangeRequest> findReadyToApply(LocalDate effectiveDate, int limit);

    MasterDataChangeRequest save(MasterDataChangeRequest request);
}

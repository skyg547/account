package com.ho.account.masterdata.core.application.port.out;

import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeStatus;
import com.ho.account.masterdata.core.application.query.PageSlice;
import com.ho.account.masterdata.core.application.query.PageWindow;
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

    PageSlice<MasterDataChangeRequest> findByStatus(ChangeStatus status, PageWindow page);

    List<MasterDataChangeRequest> findReadyToApply(LocalDate effectiveDate, int limit);

    /**
     * sourceReference의 최초 저장 또는 기존 행 조회를 같은 트랜잭션에서 원자적으로 수행합니다.
     * created가 true인 경우 호출자의 후속 검증이 실패하면 최초 저장도 함께 롤백되어야 합니다.
     */
    CreateOrLoadResult createOrLoad(MasterDataChangeRequest request);

    record CreateOrLoadResult(MasterDataChangeRequest request, boolean created) { }

    MasterDataChangeRequest save(MasterDataChangeRequest request);
}

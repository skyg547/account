package com.ho.account.masterdata.core.port.out;

import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeStatus;
import java.util.List;
import java.util.Optional;

/**
 * 마스터 변경요청 저장소 출력 포트입니다.
 */
public interface MasterDataChangeRequestPersistencePort {

    Optional<MasterDataChangeRequest> findById(Long id);

    List<MasterDataChangeRequest> findByStatus(ChangeStatus status);

    MasterDataChangeRequest save(MasterDataChangeRequest request);
}

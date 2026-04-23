package com.ho.account.masterdata.core.port.out;

import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeStatus;
import java.util.List;
import java.util.Optional;

/**
 * 留덉뒪??蹂寃쎌슂泥???μ냼 異쒕젰 ?ы듃?낅땲??
 */
public interface MasterDataChangeRequestPersistencePort {

    Optional<MasterDataChangeRequest> findById(Long id);

    List<MasterDataChangeRequest> findByStatus(ChangeStatus status);

    MasterDataChangeRequest save(MasterDataChangeRequest request);
}

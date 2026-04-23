package com.ho.account.masterdata.core.application.port.out;

import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeStatus;
import java.util.List;
import java.util.Optional;

/**
 * ë‰???‚ÂƒìŒ?‚ï???????°ì’•???????…ë•²??
 */
public interface MasterDataChangeRequestPersistencePort {

    Optional<MasterDataChangeRequest> findById(Long id);

    List<MasterDataChangeRequest> findByStatus(ChangeStatus status);

    MasterDataChangeRequest save(MasterDataChangeRequest request);
}


package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;

/**
 * ????ë‰???‚ÂƒìŒ?‚ï?????¼ì £ ë‰????ë’ª?³Â??ë’ª???¸ìŠœ??ë’— application port??…ë•²??
 */
public interface MasterDataChangeApplier {

    void apply(MasterDataChangeRequest request);
}

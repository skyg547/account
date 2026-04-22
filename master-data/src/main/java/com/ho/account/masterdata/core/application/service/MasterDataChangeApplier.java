package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;

/**
 * 승인된 마스터 변경요청을 실제 마스터 유스케이스에 적용하는 application port입니다.
 */
public interface MasterDataChangeApplier {

    void apply(MasterDataChangeRequest request);
}

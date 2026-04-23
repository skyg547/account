package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;

/**
 * ?뱀씤??留덉뒪??蹂寃쎌슂泥?쓣 ?ㅼ젣 留덉뒪???좎뒪耳?댁뒪???곸슜?섎뒗 application port?낅땲??
 */
public interface MasterDataChangeApplier {

    void apply(MasterDataChangeRequest request);
}

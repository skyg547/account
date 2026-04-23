package com.ho.account.masterdata.core.application.port.in;

import com.ho.account.masterdata.core.domain.model.MasterDataChangeRequest;
import java.util.List;

/**
 * 마스터 데이터 변경 요청 유즈케이스
 */
public interface MasterDataChangeRequestUseCase {
    List<MasterDataChangeRequest> getAllChangeRequests();
}

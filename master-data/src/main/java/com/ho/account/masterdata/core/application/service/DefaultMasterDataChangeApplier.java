package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 마스터 데이터 변경 승인 사항을 실제 도메인에 반영하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultMasterDataChangeApplier {

    public void apply(MasterDataChangeRequest request) {
        log.info("Applying change request: {} for target: {}", request.getId(), request.getTargetKey());
        // 실제 도메인 반영 로직 (AccountSubject, Department 등 분기 처리 필요)
    }
}

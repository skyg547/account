package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.application.port.out.MasterDataChangePayloadDecoder;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.exception.MasterDataVersionConflictException;
import com.ho.account.masterdata.core.domain.policy.MasterDataValidityPolicy;
import java.time.LocalDate;

/**
 * typed applier들이 공통으로 사용하는 payload와 업무 식별자 검증입니다.
 */
final class MasterDataChangeApplierSupport {

    private MasterDataChangeApplierSupport() {
    }

    static <T> T decode(
            MasterDataChangeRequest request,
            MasterDataChangePayloadDecoder decoder,
            Class<T> payloadType) {
        return decoder.decode(request, payloadType);
    }

    static String targetKey(MasterDataChangeRequest request, String payloadKey) {
        String targetKey = request.getTargetKey();
        if (payloadKey == null || payloadKey.isBlank()) {
            return targetKey;
        }
        if (!targetKey.equals(payloadKey)) {
            throw new IllegalArgumentException(
                    "Payload key " + payloadKey + " does not match change-request targetKey " + targetKey);
        }
        return targetKey;
    }

    static Long requirePersistentId(Long id, MasterDataChangeRequest request) {
        if (id == null) {
            throw new IllegalStateException(
                    "Active " + request.getTargetType() + " has no persistent ID for targetKey "
                            + request.getTargetKey());
        }
        return id;
    }

    static void requireCurrentUpdateWindow(
            LocalDate currentValidTo, LocalDate requestedValidFrom, LocalDate requestedValidTo) {
        LocalDate newValidTo = requestedValidTo != null ? requestedValidTo : LocalDate.of(9999, 12, 31);
        // 입력 자체의 역전된 날짜는 400입니다. 유효한 승인 구간이 현재 구간을 벗어나는 경우만
        // 이력 수가 그대로인 DEACTIVATE 이후의 상태 충돌로 분류합니다. 직접 수정 규칙은 유지합니다.
        MasterDataValidityPolicy.requireValidityWindow(requestedValidFrom, newValidTo);
        if (newValidTo.isAfter(currentValidTo)) {
            throw new MasterDataVersionConflictException(
                    "Approved update extends the current SCD2 validity window.");
        }
    }
}

package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.application.port.out.MasterDataChangePayloadDecoder;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;

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
}
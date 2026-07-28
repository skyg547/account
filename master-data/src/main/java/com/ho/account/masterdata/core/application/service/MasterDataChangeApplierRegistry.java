package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.application.exception.UnsupportedMasterDataTypeException;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 기준정보 유형과 실제 반영 전략을 한 번만 연결하는 불변 레지스트리입니다.
 *
 * <p>매 요청마다 전략 목록을 순회하지 않고, 애플리케이션 시작 시 중복 담당 유형을
 * 즉시 실패시켜 어떤 전략이 업무 변경을 처리하는지 명확하게 유지합니다.</p>
 */
final class MasterDataChangeApplierRegistry {

    private final Map<MasterDataType, MasterDataChangeApplier> appliers;

    MasterDataChangeApplierRegistry(List<MasterDataChangeApplier> candidates) {
        Objects.requireNonNull(candidates, "appliers are required");
        EnumMap<MasterDataType, MasterDataChangeApplier> registered = new EnumMap<>(MasterDataType.class);
        for (MasterDataChangeApplier candidate : candidates) {
            MasterDataChangeApplier applier = Objects.requireNonNull(candidate, "applier is required");
            MasterDataType targetType = Objects.requireNonNull(applier.targetType(), "applier targetType is required");
            MasterDataChangeApplier duplicate = registered.putIfAbsent(targetType, applier);
            if (duplicate != null) {
                throw new IllegalStateException("Multiple MasterDataChangeAppliers support targetType: " + targetType);
            }
        }
        this.appliers = Map.copyOf(registered);
    }

    MasterDataChangeApplier require(MasterDataType targetType) {
        MasterDataChangeApplier applier = appliers.get(targetType);
        if (applier == null) {
            throw new UnsupportedMasterDataTypeException(targetType);
        }
        return applier;
    }
}
package com.ho.account.masterdata.core.infrastructure.persistence.mapper;

import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.infrastructure.persistence.entity.MasterDataChangeRequestEntity;
import org.springframework.stereotype.Component;

/**
 * MasterDataChangeRequest Domain POJO <-> MasterDataChangeRequestEntity 양방향 변환 Mapper.
 * 
 * 🐣 [Data Mapper 패턴 교육적 주석]
 * MasterDataChangeRequest 애그리게이트와 영속성 엔티티(MasterDataChangeRequestEntity) 간 양방향 매핑을 수행합니다.
 */
@Component
public class MasterDataChangeRequestMapper {

    public MasterDataChangeRequest toDomain(MasterDataChangeRequestEntity entity) {
        if (entity == null) return null;
        return MasterDataChangeRequest.reconstitute(
                entity.getId(),
                entity.getTargetType(),
                entity.getTargetKey(),
                entity.getChangeType(),
                entity.getStatus(),
                entity.getLockVersion(),
                entity.getEffectiveDate(),
                entity.getRequestedVersion(),
                entity.getRequestedBy(),
                entity.getApprovedBy(),
                entity.getRequestedAt(),
                entity.getApprovedAt(),
                entity.getReason(),
                entity.getPayloadJson(),
                entity.getSourceReference(),
                entity.getAppliedAt());
    }

    public MasterDataChangeRequestEntity toEntity(MasterDataChangeRequest domain) {
        if (domain == null) return null;
        MasterDataChangeRequestEntity entity = new MasterDataChangeRequestEntity();
        entity.setId(domain.getId());
        entity.setTargetType(domain.getTargetType());
        entity.setTargetKey(domain.getTargetKey());
        entity.setChangeType(domain.getChangeType());
        entity.setStatus(domain.getStatus());
        entity.setLockVersion(domain.getLockVersion());
        entity.setEffectiveDate(domain.getEffectiveDate());
        entity.setRequestedVersion(domain.getRequestedVersion());
        entity.setRequestedBy(domain.getRequestedBy());
        entity.setApprovedBy(domain.getApprovedBy());
        entity.setRequestedAt(domain.getRequestedAt());
        entity.setApprovedAt(domain.getApprovedAt());
        entity.setReason(domain.getReason());
        entity.setPayloadJson(domain.getPayloadJson());
        entity.setSourceReference(domain.getSourceReference());
        entity.setAppliedAt(domain.getAppliedAt());
        return entity;
    }
}

package com.ho.account.masterdata.core.infrastructure.persistence.mapper;

import com.ho.account.masterdata.core.domain.model.Department;
import com.ho.account.masterdata.core.infrastructure.persistence.entity.DepartmentEntity;
import org.springframework.stereotype.Component;

/**
 * Department Domain POJO <-> DepartmentEntity 양방향 변환 Mapper.
 * 
 * 🐣 [Data Mapper 패턴 교육적 주석]
 * Department 도메인 모델과 DepartmentEntity 간의 변환을 담당합니다.
 */
@Component
public class DepartmentMapper {

    public Department toDomain(DepartmentEntity entity) {
        if (entity == null) return null;
        Department domain = new Department();
        domain.setId(entity.getId());
        domain.setCode(entity.getCode());
        domain.setName(entity.getName());
        domain.setType(entity.getType());
        domain.setValidFrom(entity.getValidFrom());
        domain.setValidTo(entity.getValidTo());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        domain.setAuditUser(entity.getAuditUser());

        if (entity.getParent() != null) {
            domain.setParent(toDomain(entity.getParent()));
        }
        return domain;
    }

    public DepartmentEntity toEntity(Department domain) {
        if (domain == null) return null;
        DepartmentEntity entity = new DepartmentEntity();
        entity.setId(domain.getId());
        entity.setCode(domain.getCode());
        entity.setName(domain.getName());
        entity.setType(domain.getType());
        entity.setValidFrom(domain.getValidFrom());
        entity.setValidTo(domain.getValidTo());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setAuditUser(domain.getAuditUser());

        if (domain.getParent() != null) {
            entity.setParent(toEntity(domain.getParent()));
        }
        return entity;
    }
}

package com.ho.account.masterdata.core.application.command;

import com.ho.account.masterdata.core.domain.model.Department;
import java.time.LocalDate;

public record DepartmentCommand(
        String code,
        String name,
        String parentCode,
        Department.DepartmentType type,
        LocalDate validFrom,
        LocalDate validTo) {

    public boolean hasParentCode() {
        return parentCode != null && !parentCode.isBlank();
    }

    public Department toEntity() {
        Department entity = new Department();
        entity.setCode(code);
        entity.setName(name);
        entity.setType(type);
        entity.setValidFrom(validFrom);
        entity.setValidTo(validTo);
        return entity;
    }
}

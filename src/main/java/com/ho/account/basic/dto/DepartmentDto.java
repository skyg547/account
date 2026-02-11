package com.ho.account.basic.dto;

import com.ho.account.basic.domain.Department;
import java.time.LocalDate;

public class DepartmentDto {

    private final String code;
    private final String name;
    private final String parentCode;
    private final Department.DepartmentType type;
    private final LocalDate validFrom;
    private final LocalDate validTo;

    public DepartmentDto(String code, String name, String parentCode, Department.DepartmentType type, LocalDate validFrom, LocalDate validTo) {
        this.code = code;
        this.name = name;
        this.parentCode = parentCode;
        this.type = type;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    public static DepartmentDto fromEntity(Department entity) {
        if (entity == null) {
            return null;
        }

        String parentCode = (entity.getParent() != null) ? entity.getParent().getCode() : null;

        return new DepartmentDto(
                entity.getCode(),
                entity.getName(),
                parentCode,
                entity.getType(),
                entity.getValidFrom(),
                entity.getValidTo()
        );
    }

    // Getters
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getParentCode() { return parentCode; }
    public Department.DepartmentType getType() { return type; }
    public LocalDate getValidFrom() { return validFrom; }
    public LocalDate getValidTo() { return validTo; }
}

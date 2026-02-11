package com.ho.account.basic.dto;

import com.ho.account.basic.domain.Department;
import java.time.LocalDate;

public class DepartmentRequestDto {
    private String code;
    private String name;
    private String parentCode;
    private Department.DepartmentType type;
    private LocalDate validFrom;
    private LocalDate validTo;
    
    // Default constructor for JSON deserialization
    public DepartmentRequestDto() {
    }

    public Department toEntity() {
        Department entity = new Department();
        entity.setCode(this.code);
        entity.setName(this.name);
        // parent will be set in the service
        entity.setType(this.type);
        entity.setValidFrom(this.validFrom);
        entity.setValidTo(this.validTo);
        return entity;
    }

    // Getters and Setters
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getParentCode() { return parentCode; }
    public void setParentCode(String parentCode) { this.parentCode = parentCode; }

    public Department.DepartmentType getType() { return type; }
    public void setType(Department.DepartmentType type) { this.type = type; }
    
    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }

    public LocalDate getValidTo() { return validTo; }
    public void setValidTo(LocalDate validTo) { this.validTo = validTo; }
}

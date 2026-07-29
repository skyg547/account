package com.ho.account.masterdata.api.dto;

import com.ho.account.masterdata.core.domain.model.Department;
import com.ho.account.masterdata.core.application.command.DepartmentCommand;
import java.time.LocalDate;

public class DepartmentRequestDto {
    private String code;
    private String name;
    private String parentCode;
    private Department.DepartmentType type;
    private LocalDate validFrom;
    private LocalDate validTo;
    
    public DepartmentRequestDto() {
    }

    public DepartmentCommand toCommand() {
        return new DepartmentCommand(code, name, parentCode, type, validFrom, validTo);
    }

    // Getter ?Setter
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

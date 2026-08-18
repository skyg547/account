package com.ho.account.masterdata.api.dto;

import com.ho.account.masterdata.core.domain.model.Department;
import java.time.LocalDate;
import java.util.Objects;

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

    /**
     * 조회·생성 결과를 응답 DTO로 옮긴다.
     *
     * <p>{@code entity}는 필수다. 이전에는 null이면 null을 돌려줬으나, 호출부가 그 결과를
     * 그대로 {@code ResponseEntity.ok(...)}에 넣기 때문에 그 경로가 실제로 도달했다면
     * <strong>200 OK에 빈 본문</strong>이 나갔을 것이다. 현재 호출부는 모두 use case가
     * 만든 엔티티라 null이 올 수 없으므로, 계약을 명시하고 프로그래밍 오류를 즉시
     * 드러낸다.</p>
     */
    public static DepartmentDto fromEntity(Department entity) {
        Objects.requireNonNull(entity, "Department must not be null");

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

    // Getter
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getParentCode() { return parentCode; }
    public Department.DepartmentType getType() { return type; }
    public LocalDate getValidFrom() { return validFrom; }
    public LocalDate getValidTo() { return validTo; }
}

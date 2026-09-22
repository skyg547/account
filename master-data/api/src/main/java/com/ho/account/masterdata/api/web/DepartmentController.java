package com.ho.account.masterdata.api.web;

import com.ho.account.masterdata.core.application.port.in.DepartmentUseCase;
import com.ho.account.masterdata.core.domain.model.Department;
import com.ho.account.masterdata.api.dto.DepartmentDto;
import com.ho.account.masterdata.api.dto.DepartmentRequestDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 부서 관리 컨트롤러
 */
@RestController
@RequestMapping("/api/basic/departments")
public class DepartmentController {

    private final DepartmentUseCase departmentUseCase;

    public DepartmentController(DepartmentUseCase departmentUseCase) {
        this.departmentUseCase = departmentUseCase;
    }

    /**
     * 모든 부서 조회
     */
    @GetMapping
    public List<DepartmentDto> getAllDepartments() {
        return departmentUseCase.getAllDepartments().stream()
                .map(DepartmentDto::fromEntity)
                .toList();
    }

    /**
     * 활성 부서 조회
     */
    @GetMapping("/active")
    public List<DepartmentDto> getActiveDepartments() {
        return departmentUseCase.getActiveDepartments().stream()
                .map(DepartmentDto::fromEntity)
                .toList();
    }

    /**
     * 부서 상세 조회
     */
    @GetMapping("/{code}")
    public ResponseEntity<DepartmentDto> getDepartmentByCode(@PathVariable String code) {
        return departmentUseCase.getDepartmentByCode(code)
                .map(DepartmentDto::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 부서 등록
     */
    @PostMapping
    public ResponseEntity<DepartmentDto> createDepartment(
            @RequestHeader(value = "X-Auth-Roles", required = false) String authenticatedRoles,
            @RequestBody DepartmentRequestDto requestDto) {
        MasterDataDirectWritePolicy.requireAdminRole(authenticatedRoles);
        Department created = departmentUseCase.createDepartment(requestDto.toCommand());
        return ResponseEntity.ok(DepartmentDto.fromEntity(created));
    }
}

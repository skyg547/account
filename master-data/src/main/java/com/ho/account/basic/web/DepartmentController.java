package com.ho.account.masterdata.api.web;

import com.ho.account.basic.domain.Department;
import com.ho.account.masterdata.api.dto.DepartmentRequestDto;
import com.ho.account.masterdata.core.application.usecase.DepartmentUseCase;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/basic/departments")
public class DepartmentController {

    private final DepartmentUseCase departmentUseCase;

    public DepartmentController(DepartmentUseCase departmentUseCase) {
        this.departmentUseCase = departmentUseCase;
    }

    // 부서 생성
    @PostMapping
    public ResponseEntity<Department> createDepartment(@RequestBody DepartmentRequestDto requestDto) {
        Department createdDepartment = departmentUseCase.createDepartment(requestDto.toCommand());
        return ResponseEntity.ok(createdDepartment);
    }

    // 전체 부서 조회
    @GetMapping
    public List<Department> getAllDepartments() {
        return departmentUseCase.getAllDepartments();
    }

    // 사용 중인 부서만 조회
    @GetMapping("/active")
    public List<Department> getActiveDepartments() {
        return departmentUseCase.findAllActiveDepartments();
    }

    // 부서 상세 조회
    @GetMapping("/{deptCode}")
    public ResponseEntity<Department> getDepartmentByCode(@PathVariable String deptCode) {
        return departmentUseCase.getDepartmentByCode(deptCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 부서 정보 수정
    @PutMapping("/{deptCode}")
    public ResponseEntity<Department> updateDepartment(@PathVariable String deptCode,
            @RequestBody DepartmentRequestDto departmentDetails) {
        try {
            Department updatedDepartment = departmentUseCase.updateDepartment(deptCode, departmentDetails.toCommand());
            return ResponseEntity.ok(updatedDepartment);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 부서 삭제 (논리적 삭제)
    @DeleteMapping("/{deptCode}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable String deptCode) {
        try {
            departmentUseCase.deactivateDepartment(deptCode);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

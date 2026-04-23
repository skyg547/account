package com.ho.account.masterdata.api.web;

import com.ho.account.masterdata.core.domain.model.Department;
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

    // 遺???앹꽦
    @PostMapping
    public ResponseEntity<Department> createDepartment(@RequestBody DepartmentRequestDto requestDto) {
        Department createdDepartment = departmentUseCase.createDepartment(requestDto.toCommand());
        return ResponseEntity.ok(createdDepartment);
    }

    // ?꾩껜 遺??議고쉶
    @GetMapping
    public List<Department> getAllDepartments() {
        return departmentUseCase.getAllDepartments();
    }

    // ?ъ슜 以묒씤 遺?쒕쭔 議고쉶
    @GetMapping("/active")
    public List<Department> getActiveDepartments() {
        return departmentUseCase.findAllActiveDepartments();
    }

    // 遺???곸꽭 議고쉶
    @GetMapping("/{deptCode}")
    public ResponseEntity<Department> getDepartmentByCode(@PathVariable String deptCode) {
        return departmentUseCase.getDepartmentByCode(deptCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 遺???뺣낫 ?섏젙
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

    // 遺????젣 (?쇰━????젣)
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

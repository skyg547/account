package com.ho.account.basic.service;

import com.ho.account.basic.domain.Department;
import com.ho.account.basic.repository.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    @Autowired
    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    // 부서 생성
    public Department createDepartment(Department department) {
        if (departmentRepository.existsByDeptCode(department.getDeptCode())) {
            throw new IllegalArgumentException("이미 존재하는 부서 코드입니다: " + department.getDeptCode());
        }
        return departmentRepository.save(department);
    }

    // 전체 부서 조회
    @Transactional(readOnly = true)
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    // 사용 중인 부서만 조회
    @Transactional(readOnly = true)
    public List<Department> getActiveDepartments() {
        return departmentRepository.findByUseYnTrue();
    }

    // 부서 상세 조회
    @Transactional(readOnly = true)
    public Optional<Department> getDepartmentByCode(String deptCode) {
        return departmentRepository.findByDeptCode(deptCode);
    }

    // 부서 정보 수정
    public Department updateDepartment(Long id, Department departmentDetails) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("부서를 찾을 수 없습니다. ID: " + id));

        department.setDeptName(departmentDetails.getDeptName());
        department.setParentDeptCode(departmentDetails.getParentDeptCode());
        department.setUseYn(departmentDetails.getUseYn());
        
        return departmentRepository.save(department);
    }

    // 부서 삭제 (논리적 삭제)
    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("부서를 찾을 수 없습니다. ID: " + id));
        department.setUseYn(false);
        departmentRepository.save(department);
    }
}

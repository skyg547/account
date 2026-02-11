package com.ho.account.basic.service;

import com.ho.account.basic.domain.Department;
import com.ho.account.basic.dto.DepartmentRequestDto;
import com.ho.account.basic.repository.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    @Autowired
    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    public Department createDepartment(DepartmentRequestDto requestDto) {
        if (departmentRepository.existsById(requestDto.getCode())) {
            throw new IllegalArgumentException("이미 존재하는 부서 코드입니다: " + requestDto.getCode());
        }

        Department department = requestDto.toEntity();

        if (requestDto.getParentCode() != null && !requestDto.getParentCode().isEmpty()) {
            Department parent = departmentRepository.findById(requestDto.getParentCode())
                    .orElseThrow(() -> new IllegalArgumentException("상위 부서를 찾을 수 없습니다. 코드: " + requestDto.getParentCode()));
            department.setParent(parent);
        }

        if (department.getValidFrom() == null) {
            department.setValidFrom(LocalDate.now());
        }
        if (department.getValidTo() == null) {
            department.setValidTo(LocalDate.of(9999, 12, 31));
        }
        return departmentRepository.save(department);
    }

    @Transactional(readOnly = true)
    public Optional<Department> findDepartmentByCode(String code) {
        return departmentRepository.findById(code);
    }

    @Transactional(readOnly = true)
    public List<Department> findAllActiveDepartments() {
        LocalDate today = LocalDate.now();
        return departmentRepository.findAll().stream()
                .filter(dept -> !today.isBefore(dept.getValidFrom()) && !today.isAfter(dept.getValidTo()))
                .collect(Collectors.toList());
    }

    public Department updateDepartment(String code, DepartmentRequestDto requestDto) {
        Department department = departmentRepository.findById(code)
                .orElseThrow(() -> new IllegalArgumentException("부서를 찾을 수 없습니다. 코드: " + code));

        if (requestDto.getParentCode() != null && !requestDto.getParentCode().isEmpty()) {
            Department parent = departmentRepository.findById(requestDto.getParentCode())
                    .orElseThrow(() -> new IllegalArgumentException("상위 부서를 찾을 수 없습니다. 코드: " + requestDto.getParentCode()));
            department.setParent(parent);
        } else {
            department.setParent(null);
        }

        department.setName(requestDto.getName());
        department.setType(requestDto.getType());
        
        return departmentRepository.save(department);
    }

    public void deactivateDepartment(String code) {
        Department department = departmentRepository.findById(code)
                .orElseThrow(() -> new IllegalArgumentException("부서를 찾을 수 없습니다. 코드: " + code));
        
        if (department.getValidTo().isAfter(LocalDate.now())) {
            department.setValidTo(LocalDate.now());
            departmentRepository.save(department);
        }
    }
}


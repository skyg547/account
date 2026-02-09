package com.accounting.system.basic.repository;

import com.accounting.system.basic.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByDeptCode(String deptCode);
    List<Department> findByUseYnTrue();
    boolean existsByDeptCode(String deptCode);
}

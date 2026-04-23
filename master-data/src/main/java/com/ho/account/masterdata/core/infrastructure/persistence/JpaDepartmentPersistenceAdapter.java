package com.ho.account.masterdata.core.infrastructure.persistence;

import com.ho.account.masterdata.core.domain.model.Department;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.DepartmentRepository;
import com.ho.account.masterdata.core.port.out.DepartmentPersistencePort;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaDepartmentPersistenceAdapter implements DepartmentPersistencePort {

    private final DepartmentRepository departmentRepository;

    public JpaDepartmentPersistenceAdapter(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Override
    public boolean existsByCode(String code) {
        return departmentRepository.existsById(code);
    }

    @Override
    public Optional<Department> findByCode(String code) {
        return departmentRepository.findByCode(code);
    }

    @Override
    public List<Department> findAll() {
        return departmentRepository.findAll();
    }

    @Override
    public Department save(Department department) {
        return departmentRepository.save(department);
    }
}

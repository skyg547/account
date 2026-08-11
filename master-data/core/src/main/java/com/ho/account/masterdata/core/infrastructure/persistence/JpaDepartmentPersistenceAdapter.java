package com.ho.account.masterdata.core.infrastructure.persistence;

import com.ho.account.masterdata.core.domain.model.Department;
import com.ho.account.masterdata.core.infrastructure.persistence.entity.DepartmentEntity;
import com.ho.account.masterdata.core.infrastructure.persistence.mapper.DepartmentMapper;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.DepartmentRepository;
import com.ho.account.masterdata.core.application.port.out.DepartmentPersistencePort;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaDepartmentPersistenceAdapter implements DepartmentPersistencePort {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    public JpaDepartmentPersistenceAdapter(
            DepartmentRepository departmentRepository,
            DepartmentMapper departmentMapper) {
        this.departmentRepository = departmentRepository;
        this.departmentMapper = departmentMapper;
    }

    @Override
    public boolean existsByCode(String code) {
        return departmentRepository.findCurrentByCode(code).isPresent();
    }

    @Override
    public Optional<Department> findById(Long id) {
        return departmentRepository.findById(id).map(departmentMapper::toDomain);
    }

    @Override
    public Optional<Department> findActiveByCode(String code) {
        return departmentRepository.findCurrentByCode(code).map(departmentMapper::toDomain);
    }

    @Override
    public Optional<Department> findActiveByCodeAt(String code, java.time.LocalDate asOfDate) {
        return departmentRepository.findActiveByCode(code, asOfDate).map(departmentMapper::toDomain);
    }


    @Override
    public List<Department> findAll() {
        return departmentRepository.findAll().stream()
                .map(departmentMapper::toDomain)
                .toList();
    }

    @Override
    public List<Department> findAllActive() {
        return departmentRepository.findActiveVersions(java.time.LocalDate.now()).stream()
                .map(departmentMapper::toDomain)
                .toList();
    }

    @Override
    public Department save(Department department) {
        DepartmentEntity entity = departmentMapper.toEntity(department);
        DepartmentEntity saved = departmentRepository.save(entity);
        return departmentMapper.toDomain(saved);
    }
}


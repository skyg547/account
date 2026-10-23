package com.ho.account.masterdata.core.infrastructure.persistence;

import com.ho.account.masterdata.core.domain.model.Department;
import com.ho.account.masterdata.core.infrastructure.persistence.entity.DepartmentEntity;
import com.ho.account.masterdata.core.infrastructure.persistence.mapper.DepartmentMapper;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.DepartmentRepository;
import com.ho.account.masterdata.core.application.port.out.DepartmentPersistencePort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaDepartmentPersistenceAdapter implements DepartmentPersistencePort {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    @PersistenceContext
    private EntityManager entityManager;

    public JpaDepartmentPersistenceAdapter(
            DepartmentRepository departmentRepository,
            DepartmentMapper departmentMapper) {
        this.departmentRepository = departmentRepository;
        this.departmentMapper = departmentMapper;
    }

    @Override
    public boolean existsByCode(String code) {
        return departmentRepository.existsByCode(code);
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
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Department> findActiveByCodeForUpdate(String code) {
        return departmentRepository.findCurrentByCode(code).map(entity -> {
            // 상위 트랜잭션이 미리 읽은 객체도 키 잠금 대기 후 DB의 최신 구간으로 다시 검증합니다.
            entityManager.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
            return departmentMapper.toDomain(entity);
        });
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
        // 기존 구간 종료를 먼저 flush해야 새 IDENTITY 행의 즉시 INSERT가 중복 기간으로 거부되지 않습니다.
        DepartmentEntity saved = entity.getId() == null
                ? departmentRepository.save(entity)
                : departmentRepository.saveAndFlush(entity);
        return departmentMapper.toDomain(saved);
    }
}

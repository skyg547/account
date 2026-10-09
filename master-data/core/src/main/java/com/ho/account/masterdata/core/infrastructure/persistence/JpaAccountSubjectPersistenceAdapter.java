package com.ho.account.masterdata.core.infrastructure.persistence;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.infrastructure.persistence.entity.AccountSubjectEntity;
import com.ho.account.masterdata.core.infrastructure.persistence.mapper.AccountSubjectMapper;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.AccountSubjectRepository;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaAccountSubjectPersistenceAdapter implements AccountSubjectPersistencePort {

    private final AccountSubjectRepository accountSubjectRepository;
    private final AccountSubjectMapper accountSubjectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    public JpaAccountSubjectPersistenceAdapter(
            AccountSubjectRepository accountSubjectRepository,
            AccountSubjectMapper accountSubjectMapper) {
        this.accountSubjectRepository = accountSubjectRepository;
        this.accountSubjectMapper = accountSubjectMapper;
    }

    @Override
    public boolean existsByCode(String code) {
        return accountSubjectRepository.existsByCode(code);
    }

    @Override
    public Optional<AccountSubject> findByCode(String code) {
        return accountSubjectRepository.findByCode(code)
                .map(accountSubjectMapper::toDomain);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<AccountSubject> findByCodeForUpdate(String code) {
        return accountSubjectRepository.findByCode(code).map(entity -> {
            // 상위 트랜잭션이 미리 읽은 객체도 키 잠금 대기 후 DB의 최신 구간으로 다시 검증합니다.
            entityManager.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
            return accountSubjectMapper.toDomain(entity);
        });
    }

    @Override
    public Optional<AccountSubject> findByCodeAt(String code, LocalDate asOfDate) {
        return accountSubjectRepository.findActiveByCode(code, asOfDate)
                .map(accountSubjectMapper::toDomain);
    }

    @Override
    public List<AccountSubject> findAll() {
        return accountSubjectRepository.findAll().stream()
                .map(accountSubjectMapper::toDomain)
                .toList();
    }

    @Override
    public List<AccountSubject> findAllActive(LocalDate asOfDate) {
        return accountSubjectRepository.findActiveVersions(asOfDate).stream()
                .map(accountSubjectMapper::toDomain)
                .toList();
    }

    @Override
    public AccountSubject save(AccountSubject accountSubject) {
        AccountSubjectEntity entity = accountSubjectMapper.toEntity(accountSubject);
        // 기존 구간 종료를 먼저 flush해야 새 IDENTITY 행의 즉시 INSERT가 중복 기간으로 거부되지 않습니다.
        AccountSubjectEntity saved = entity.getId() == null
                ? accountSubjectRepository.save(entity)
                : accountSubjectRepository.saveAndFlush(entity);
        return accountSubjectMapper.toDomain(saved);
    }
}

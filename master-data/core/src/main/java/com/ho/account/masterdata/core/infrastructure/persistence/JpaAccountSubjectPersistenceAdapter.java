package com.ho.account.masterdata.core.infrastructure.persistence;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.infrastructure.persistence.entity.AccountSubjectEntity;
import com.ho.account.masterdata.core.infrastructure.persistence.mapper.AccountSubjectMapper;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.AccountSubjectRepository;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaAccountSubjectPersistenceAdapter implements AccountSubjectPersistencePort {

    private final AccountSubjectRepository accountSubjectRepository;
    private final AccountSubjectMapper accountSubjectMapper;

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
        AccountSubjectEntity saved = accountSubjectRepository.save(entity);
        return accountSubjectMapper.toDomain(saved);
    }
}



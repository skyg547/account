package com.ho.account.masterdata.core.infrastructure.persistence;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.AccountSubjectRepository;
import com.ho.account.masterdata.core.port.out.AccountSubjectPersistencePort;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaAccountSubjectPersistenceAdapter implements AccountSubjectPersistencePort {

    private final AccountSubjectRepository accountSubjectRepository;

    public JpaAccountSubjectPersistenceAdapter(AccountSubjectRepository accountSubjectRepository) {
        this.accountSubjectRepository = accountSubjectRepository;
    }

    @Override
    public boolean existsByCode(String code) {
        return accountSubjectRepository.existsByCode(code);
    }

    @Override
    public Optional<AccountSubject> findByCode(String code) {
        return accountSubjectRepository.findByCode(code);
    }

    @Override
    public List<AccountSubject> findAll() {
        return accountSubjectRepository.findAll();
    }

    @Override
    public AccountSubject save(AccountSubject accountSubject) {
        return accountSubjectRepository.save(accountSubject);
    }
}

package com.ho.account.masterdata.core.application.port.out;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Output port for account-subject persistence.
 *
 * <p>Application services depend on this technology-independent contract.
 * JPA repository details are hidden behind the infrastructure adapter.</p>
 */
public interface AccountSubjectPersistencePort {

    boolean existsByCode(String code);

    Optional<AccountSubject> findByCode(String code);

    /** 업무 키 잠금 후, 기존 영속성 캐시 대신 최신 현재 버전을 잠가 읽습니다. */
    Optional<AccountSubject> findByCodeForUpdate(String code);

    Optional<AccountSubject> findByCodeAt(String code, LocalDate asOfDate);

    List<AccountSubject> findAll();

    List<AccountSubject> findAllActive(LocalDate asOfDate);

    AccountSubject save(AccountSubject accountSubject);
}

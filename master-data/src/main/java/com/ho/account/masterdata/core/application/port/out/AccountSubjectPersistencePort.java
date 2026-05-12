package com.ho.account.masterdata.core.application.port.out;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
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

    List<AccountSubject> findAll();

    AccountSubject save(AccountSubject accountSubject);
}

package com.ho.account.masterdata.core.application.port.in;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.application.command.AccountSubjectCommand;
import java.util.List;
import java.util.Optional;

public interface AccountSubjectUseCase {

    AccountSubject createAccountSubject(AccountSubjectCommand command);

    Optional<AccountSubject> findAccountSubjectByCode(String code);

    List<AccountSubject> findAllActiveAccountSubjects();

    AccountSubject updateAccountSubject(String code, AccountSubjectCommand command);

    void deactivateAccountSubject(String code);

    List<AccountSubject> getAllAccountSubjects();
}


package com.ho.account.masterdata.core.application.usecase;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.masterdata.core.application.command.AccountSubjectCommand;
import java.util.List;
import java.util.Optional;

public interface AccountSubjectUseCase {

    AccountSubject createAccountSubject(AccountSubjectCommand command);

    Optional<AccountSubject> findAccountSubjectByCode(String code);

    List<AccountSubject> findAllActiveAccountSubjects();

    AccountSubject updateAccountSubject(String code, AccountSubjectCommand command);

    void deactivateAccountSubject(String code);
}

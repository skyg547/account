package com.ho.account.masterdata.core.application.port.in;

import com.ho.account.masterdata.core.application.command.AccountSubjectCommand;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AccountSubjectUseCase {

    AccountSubject createAccountSubject(AccountSubjectCommand command);

    Optional<AccountSubject> findAccountSubjectByCode(String code);

    List<AccountSubject> findAllActiveAccountSubjects();

    AccountSubject updateAccountSubject(String code, AccountSubjectCommand command);

    void deactivateAccountSubject(String code);

    /**
     * 승인 워크플로에서 정한 종료일로 현재 SCD2 버전을 비활성화합니다.
     */
    void deactivateAccountSubject(String code, LocalDate effectiveDate);

    List<AccountSubject> getAllAccountSubjects();
}
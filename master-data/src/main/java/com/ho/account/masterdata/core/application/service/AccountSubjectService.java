package com.ho.account.masterdata.core.application.service;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.masterdata.core.application.command.AccountSubjectCommand;
import com.ho.account.masterdata.core.application.usecase.AccountSubjectUseCase;
import com.ho.account.masterdata.core.domain.policy.MasterDataValidityPolicy;
import com.ho.account.masterdata.core.port.out.AccountSubjectPersistencePort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 계정과목 마스터 데이터에 대한 비즈니스 로직을 처리하는 서비스 클래스입니다.
 */
@Service
@Transactional
public class AccountSubjectService implements AccountSubjectUseCase {

    private final AccountSubjectPersistencePort accountSubjectPersistencePort;

    public AccountSubjectService(AccountSubjectPersistencePort accountSubjectPersistencePort) {
        this.accountSubjectPersistencePort = accountSubjectPersistencePort;
    }

    /**
     * 새로운 계정과목을 생성합니다.
     * @param requestDto 생성할 계정과목 정보가 담긴 DTO
     * @return 저장된 계정과목 엔티티
     */
    public AccountSubject createAccountSubject(AccountSubjectCommand command) {
        if (accountSubjectPersistencePort.existsByCode(command.code())) {
            throw new IllegalArgumentException("이미 존재하는 계정 코드입니다: " + command.code());
        }

        AccountSubject accountSubject = command.toEntity();

        if (command.hasParentCode()) {
            AccountSubject parent = accountSubjectPersistencePort.findByCode(command.parentCode())
                    .orElseThrow(() -> new IllegalArgumentException("상위 계정을 찾을 수 없습니다. 코드: " + command.parentCode()));
            accountSubject.setParent(parent);
        }

        MasterDataValidityPolicy.applyDefaultWindow(accountSubject::getValidFrom, accountSubject::setValidFrom,
                accountSubject::getValidTo, accountSubject::setValidTo);
        return accountSubjectPersistencePort.save(accountSubject);
    }

    /**
     * 코드로 특정 계정과목을 조회합니다.
     * @param code 조회할 계정 코드
     * @return Optional<AccountSubject>
     */
    @Transactional(readOnly = true)
    public Optional<AccountSubject> findAccountSubjectByCode(String code) {
        return accountSubjectPersistencePort.findByCode(code);
    }

    /**
     * 현재 시점(today)에 유효한 모든 계정과목을 조회합니다.
     * @return 유효한 계정과목 리스트
     */
    @Transactional(readOnly = true)
    public List<AccountSubject> findAllActiveAccountSubjects() {
        return accountSubjectPersistencePort.findAll().stream()
                .filter(MasterDataValidityPolicy.isActiveNow(AccountSubject::getValidFrom, AccountSubject::getValidTo))
                .toList();
    }

    /**
     * 계정과목 정보를 수정합니다.
     * @param code 수정할 계정 코드
     * @param requestDto 수정할 내용이 담긴 DTO
     * @return 수정된 계정과목 엔티티
     */
    public AccountSubject updateAccountSubject(String code, AccountSubjectCommand command) {
        AccountSubject account = accountSubjectPersistencePort.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("계정과목을 찾을 수 없습니다. 코드: " + code));

        if (command.hasParentCode()) {
            AccountSubject parent = accountSubjectPersistencePort.findByCode(command.parentCode())
                    .orElseThrow(() -> new IllegalArgumentException("상위 계정을 찾을 수 없습니다. 코드: " + command.parentCode()));
            account.setParent(parent);
        } else {
            account.setParent(null);
        }

        account.setName(command.name());
        account.setCategory(command.category());
        account.setBalanceType(command.balanceType());
        account.setReportLine(command.reportLine());
        account.setUnsettled(command.unsettled());
        account.setFixedAsset(command.fixedAsset());

        return accountSubjectPersistencePort.save(account);
    }

    /**
     * 특정 계정과목을 비활성화합니다. (논리적 삭제)
     * @param code 비활성화할 계정 코드
     */
    public void deactivateAccountSubject(String code) {
        AccountSubject account = accountSubjectPersistencePort.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("계정과목을 찾을 수 없습니다. 코드: " + code));

        MasterDataValidityPolicy.closeIfActive(account::getValidTo, account::setValidTo);
        accountSubjectPersistencePort.save(account);
    }
}


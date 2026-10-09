package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.application.command.AccountSubjectCommand;
import com.ho.account.masterdata.core.application.port.in.AccountSubjectUseCase;
import com.ho.account.masterdata.core.domain.policy.MasterDataValidityPolicy;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.domain.exception.MasterDataVersionConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * [애플리케이션 서비스] AccountSubjectService
 * 계정과목(Account Subject) 마스터 데이터를 관리하는 헥사고날 아키텍처의 핵심 유즈케이스(UseCase) 구현체입니다.
 * SCD2(Slowly Changing Dimension Type 2) 원칙을 적용하여 데이터 변경 이력을 관리합니다.
 * 
 * 🐣 [초보자를 위한 설명]
 * 이 서비스는 회사의 '계정과목 관리자' 역할을 합니다.
 * 새로운 계정과목표를 발급(createAccountSubject)하거나, 
 * 기존 계정과목표의 내용이 바뀌면 옛날 표는 마감(terminate) 처리하고 새로운 표를 발급(updateAccountSubject)합니다.
 * 이 과정을 통해 과거 장부에 적힌 계정과목과 현재 장부에 적히는 계정과목이 섞이지 않도록 엄격하게 관리합니다.
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
     * @param command 생성할 계정과목 정보
     * @return 생성된 계정과목 객체
     */
    @Override
    public AccountSubject createAccountSubject(AccountSubjectCommand command) {
        // CREATE는 신규 업무 키 전용입니다. 미래 예약 및 종료된 SCD2 이력도 키 재사용을 막습니다.
        if (accountSubjectPersistencePort.existsByCode(command.code())) {
            throw new MasterDataVersionConflictException("이미 이력이 존재하는 계정과목 코드입니다: " + command.code());
        }

        AccountSubject accountSubject = command.toEntity();

        if (command.hasParentCode()) {
            AccountSubject parent = accountSubjectPersistencePort.findByCode(command.parentCode())
                    .orElseThrow(() -> new IllegalArgumentException("상위 계정과목을 찾을 수 없습니다. 코드: " + command.parentCode()));
            accountSubject.setParent(parent);
        }

        MasterDataValidityPolicy.applyDefaultWindow(accountSubject::getValidFrom, accountSubject::setValidFrom,
                accountSubject::getValidTo, accountSubject::setValidTo);
        return accountSubjectPersistencePort.save(accountSubject);
    }

    /**
     * 코드로 현재 활성화된 계정과목을 조회합니다.
     * @param code 조회할 계정코드
     * @return Optional<AccountSubject>
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<AccountSubject> findAccountSubjectByCode(String code) {
        return accountSubjectPersistencePort.findByCode(code);
    }

    /**
     * 현재 시점(today)에 유효한 모든 계정과목을 조회합니다.
     * @return 유효한 계정과목 리스트
     */
    @Override
    @Transactional(readOnly = true)
    public List<AccountSubject> findAllActiveAccountSubjects() {
        return accountSubjectPersistencePort.findAllActive(LocalDate.now());
    }

    /**
     * 계정과목 정보를 수정합니다. (SCD2 적용)
     * 기존 이력을 종료하고 새로운 버전의 행을 생성합니다.
     * @param code 수정할 계정코드
     * @param command 수정할 내용
     * @return 새롭게 생성된 계정과목 버전 객체
     */
    @Override
    public AccountSubject updateAccountSubject(String code, AccountSubjectCommand command) {
        AccountSubject currentActive = accountSubjectPersistencePort.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("활성화된 계정과목을 찾을 수 없습니다. 코드: " + code));

        // SCD2: 기존 활성 버전 종료
        LocalDate newValidFrom = command.validFrom() != null ? command.validFrom() : LocalDate.now();
        LocalDate newValidTo = command.validTo() != null
                ? command.validTo()
                : LocalDate.of(9999, 12, 31);
        MasterDataValidityPolicy.requireVersionSplit(
                currentActive.getValidFrom(), currentActive.getValidTo(), newValidFrom, newValidTo);
        LocalDate oldValidTo = newValidFrom.minusDays(1);
        
        // 참조 검증과 신규 버전 조립을 먼저 끝내야 잘못된 parentCode가 현재 버전을 건드리지 않습니다.
        AccountSubject newVersion = command.toEntity();
        newVersion.setCode(code); // 코드는 동일하게 유지

        if (command.hasParentCode()) {
            AccountSubject parent = accountSubjectPersistencePort.findByCode(command.parentCode())
                    .orElseThrow(() -> new IllegalArgumentException("상위 계정과목을 찾을 수 없습니다. 코드: " + command.parentCode()));
            newVersion.setParent(parent);
        } else {
            newVersion.setParent(null);
        }
        
        newVersion.setValidFrom(newValidFrom);
        newVersion.setValidTo(newValidTo);

        currentActive.terminate(oldValidTo);
        accountSubjectPersistencePort.save(currentActive);

        return accountSubjectPersistencePort.save(newVersion);
    }

    /**
     * 계정과목을 비활성화합니다. (현재 버전을 오늘 날짜로 종료)
     * @param code 비활성화할 계정코드
     */
    @Override
    public void deactivateAccountSubject(String code) {
        deactivateAccountSubject(code, LocalDate.now());
    }

    @Override
    public void deactivateAccountSubject(String code, LocalDate effectiveDate) {
        AccountSubject account = accountSubjectPersistencePort.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("활성화된 계정과목을 찾을 수 없습니다. 코드: " + code));

        LocalDate terminationDate = MasterDataValidityPolicy.requireTerminationDate(
                effectiveDate, account.getValidFrom(), account.getValidTo());
        account.terminate(terminationDate);
        accountSubjectPersistencePort.save(account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountSubject> getAllAccountSubjects() {
        return accountSubjectPersistencePort.findAll();
    }
}

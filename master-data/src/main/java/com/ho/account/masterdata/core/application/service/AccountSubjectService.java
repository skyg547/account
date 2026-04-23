package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.application.command.AccountSubjectCommand;
import com.ho.account.masterdata.core.application.port.in.AccountSubjectUseCase;
import com.ho.account.masterdata.core.domain.policy.MasterDataValidityPolicy;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * ?京?欐嚢??嶋崏????楈敔?楇繅 ??????京??夒挭 挅??剮???庪挆 ??曧壃???????检棷??堧枎.
 */
@Service
@Transactional
public class AccountSubjectService implements AccountSubjectUseCase {

    private final AccountSubjectPersistencePort accountSubjectPersistencePort;

    public AccountSubjectService(AccountSubjectPersistencePort accountSubjectPersistencePort) {
        this.accountSubjectPersistencePort = accountSubjectPersistencePort;
    }

    /**
     * ??堨???京?欐嚢?????龟溅??鸽暡??
     * @param requestDto ??龟溅???京?欐嚢???偒浡 ??侩 DTO
     * @return ?????京?欐嚢??????
     */
    public AccountSubject createAccountSubject(AccountSubjectCommand command) {
        if (accountSubjectPersistencePort.existsByCode(command.code())) {
            throw new IllegalArgumentException("??? 半寔???庪挆 ?京???勲毒??呺暡?? " + command.code());
        }

        AccountSubject accountSubject = command.toEntity();

        if (command.hasParentCode()) {
            AccountSubject parent = accountSubjectPersistencePort.findByCode(command.parentCode())
                    .orElseThrow(() -> new IllegalArgumentException("?胳悶 ?京???枲??????侂捒??堧枎. ?勲毒? " + command.parentCode()));
            accountSubject.setParent(parent);
        }

        MasterDataValidityPolicy.applyDefaultWindow(accountSubject::getValidFrom, accountSubject::setValidFrom,
                accountSubject::getValidTo, accountSubject::setValidTo);
        return accountSubjectPersistencePort.save(accountSubject);
    }

    /**
     * ?勲毒舵俊?????京?欐嚢???瓣碃???鸽暡??
     * @param code 瓣碃????京???勲毒?
     * @return Optional<AccountSubject>
     */
    @Transactional(readOnly = true)
    public Optional<AccountSubject> findAccountSubjectByCode(String code) {
        return accountSubjectPersistencePort.findByCode(code);
    }

    /**
     * ?槺 ??栰爮(today)???忟姎??忊懁??京?欐嚢???瓣碃???鸽暡??
     * @return ?忟姎???京?欐嚢???毖婋挭??
     */
    @Transactional(readOnly = true)
    public List<AccountSubject> findAllActiveAccountSubjects() {
        return accountSubjectPersistencePort.findAll().stream()
                .filter(MasterDataValidityPolicy.isActiveNow(AccountSubject::getValidFrom, AccountSubject::getValidTo))
                .toList();
    }

    /**
     * ?京?欐嚢???偒????忟牂??鸽暡??
     * @param code ??忟牂???京???勲毒?
     * @param requestDto ??忟牂????侅姕????侩 DTO
     * @return ??忟牂???京?欐嚢??????
     */
    public AccountSubject updateAccountSubject(String code, AccountSubjectCommand command) {
        AccountSubject account = accountSubjectPersistencePort.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("?京?欐嚢???枲??????侂捒??堧枎. ?勲毒? " + code));

        if (command.hasParentCode()) {
            AccountSubject parent = accountSubjectPersistencePort.findByCode(command.parentCode())
                    .orElseThrow(() -> new IllegalArgumentException("?胳悶 ?京???枲??????侂捒??堧枎. ?勲毒? " + command.parentCode()));
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
     * ????京?欐嚢?????惊??婌啎??鸽暡?? (??扳攣??????
     * @param code ??惊??婌啎???京???勲毒?
     */
    public void deactivateAccountSubject(String code) {
        AccountSubject account = accountSubjectPersistencePort.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("?京?欐嚢???枲??????侂捒??堧枎. ?勲毒? " + code));

        MasterDataValidityPolicy.closeIfActive(account::getValidTo, account::setValidTo);
        accountSubjectPersistencePort.save(account);
    }
}



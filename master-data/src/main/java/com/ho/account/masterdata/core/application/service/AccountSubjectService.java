package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.application.command.AccountSubjectCommand;
import com.ho.account.masterdata.core.application.usecase.AccountSubjectUseCase;
import com.ho.account.masterdata.core.domain.policy.MasterDataValidityPolicy;
import com.ho.account.masterdata.core.port.out.AccountSubjectPersistencePort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 怨꾩젙怨쇰ぉ 留덉뒪???곗씠?곗뿉 ???鍮꾩쫰?덉뒪 濡쒖쭅??泥섎━?섎뒗 ?쒕퉬???대옒?ㅼ엯?덈떎.
 */
@Service
@Transactional
public class AccountSubjectService implements AccountSubjectUseCase {

    private final AccountSubjectPersistencePort accountSubjectPersistencePort;

    public AccountSubjectService(AccountSubjectPersistencePort accountSubjectPersistencePort) {
        this.accountSubjectPersistencePort = accountSubjectPersistencePort;
    }

    /**
     * ?덈줈??怨꾩젙怨쇰ぉ???앹꽦?⑸땲??
     * @param requestDto ?앹꽦??怨꾩젙怨쇰ぉ ?뺣낫媛 ?닿릿 DTO
     * @return ??λ맂 怨꾩젙怨쇰ぉ ?뷀떚??
     */
    public AccountSubject createAccountSubject(AccountSubjectCommand command) {
        if (accountSubjectPersistencePort.existsByCode(command.code())) {
            throw new IllegalArgumentException("?대? 議댁옱?섎뒗 怨꾩젙 肄붾뱶?낅땲?? " + command.code());
        }

        AccountSubject accountSubject = command.toEntity();

        if (command.hasParentCode()) {
            AccountSubject parent = accountSubjectPersistencePort.findByCode(command.parentCode())
                    .orElseThrow(() -> new IllegalArgumentException("?곸쐞 怨꾩젙??李얠쓣 ???놁뒿?덈떎. 肄붾뱶: " + command.parentCode()));
            accountSubject.setParent(parent);
        }

        MasterDataValidityPolicy.applyDefaultWindow(accountSubject::getValidFrom, accountSubject::setValidFrom,
                accountSubject::getValidTo, accountSubject::setValidTo);
        return accountSubjectPersistencePort.save(accountSubject);
    }

    /**
     * 肄붾뱶濡??뱀젙 怨꾩젙怨쇰ぉ??議고쉶?⑸땲??
     * @param code 議고쉶??怨꾩젙 肄붾뱶
     * @return Optional<AccountSubject>
     */
    @Transactional(readOnly = true)
    public Optional<AccountSubject> findAccountSubjectByCode(String code) {
        return accountSubjectPersistencePort.findByCode(code);
    }

    /**
     * ?꾩옱 ?쒖젏(today)???좏슚??紐⑤뱺 怨꾩젙怨쇰ぉ??議고쉶?⑸땲??
     * @return ?좏슚??怨꾩젙怨쇰ぉ 由ъ뒪??
     */
    @Transactional(readOnly = true)
    public List<AccountSubject> findAllActiveAccountSubjects() {
        return accountSubjectPersistencePort.findAll().stream()
                .filter(MasterDataValidityPolicy.isActiveNow(AccountSubject::getValidFrom, AccountSubject::getValidTo))
                .toList();
    }

    /**
     * 怨꾩젙怨쇰ぉ ?뺣낫瑜??섏젙?⑸땲??
     * @param code ?섏젙??怨꾩젙 肄붾뱶
     * @param requestDto ?섏젙???댁슜???닿릿 DTO
     * @return ?섏젙??怨꾩젙怨쇰ぉ ?뷀떚??
     */
    public AccountSubject updateAccountSubject(String code, AccountSubjectCommand command) {
        AccountSubject account = accountSubjectPersistencePort.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("怨꾩젙怨쇰ぉ??李얠쓣 ???놁뒿?덈떎. 肄붾뱶: " + code));

        if (command.hasParentCode()) {
            AccountSubject parent = accountSubjectPersistencePort.findByCode(command.parentCode())
                    .orElseThrow(() -> new IllegalArgumentException("?곸쐞 怨꾩젙??李얠쓣 ???놁뒿?덈떎. 肄붾뱶: " + command.parentCode()));
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
     * ?뱀젙 怨꾩젙怨쇰ぉ??鍮꾪솢?깊솕?⑸땲?? (?쇰━????젣)
     * @param code 鍮꾪솢?깊솕??怨꾩젙 肄붾뱶
     */
    public void deactivateAccountSubject(String code) {
        AccountSubject account = accountSubjectPersistencePort.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("怨꾩젙怨쇰ぉ??李얠쓣 ???놁뒿?덈떎. 肄붾뱶: " + code));

        MasterDataValidityPolicy.closeIfActive(account::getValidTo, account::setValidTo);
        accountSubjectPersistencePort.save(account);
    }
}


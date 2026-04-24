package com.ho.account.journalledger.domain.ledger.repository;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Currency;
import com.ho.account.journalledger.domain.ledger.domain.GlBalanceType;
import com.ho.account.journalledger.domain.ledger.domain.SubledgerBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubledgerBalanceRepository extends JpaRepository<SubledgerBalance, Long> {
    Optional<SubledgerBalance> findByAccountSubjectAndBusinessPartnerAndCurrencyAndAccountingDateAndBalanceType(
            AccountSubject accountSubject, BusinessPartner businessPartner, Currency currency, LocalDate accountingDate, GlBalanceType balanceType);

    List<SubledgerBalance> findByAccountSubjectAndBusinessPartnerAndAccountingDateBetween(
            AccountSubject accountSubject, BusinessPartner businessPartner, LocalDate startDate, LocalDate endDate);

    List<SubledgerBalance> findByBusinessPartnerAndAccountingDateBetween(
            BusinessPartner businessPartner, LocalDate startDate, LocalDate endDate);
}

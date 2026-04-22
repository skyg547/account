package com.ho.account.journalledger.adapter.out.persistence.ledger;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.domain.Currency;
import com.ho.account.journalledger.domain.ledger.GlBalanceType;
import com.ho.account.journalledger.domain.ledger.SubledgerBalance;
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

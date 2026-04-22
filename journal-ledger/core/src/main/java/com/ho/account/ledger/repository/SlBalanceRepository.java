package com.ho.account.journalledger.adapter.out.persistence.ledger;

import com.ho.account.journalledger.domain.ledger.SlBalance;
import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List; // ?꾨씫??import 異붽?
import java.util.Optional;

/**
 * SlBalance ?뷀떚?곕? ?꾪븳 Spring Data JPA 由ы룷吏?좊━
 * ?뱀젙 怨꾩젙怨쇰ぉ, 嫄곕옒泥? 遺?? ?좎쭨, 湲곌컙?????蹂댁“?먯옣 ?붿븸 ?곗씠?곕? 愿由ы빀?덈떎.
 */
@Repository
public interface SlBalanceRepository extends JpaRepository<SlBalance, Long> {

    /**
     * ?뱀젙 怨꾩젙怨쇰ぉ, 嫄곕옒泥? 遺?? ?쇱옄, 湲곌컙???대떦?섎뒗 SlBalance瑜?議고쉶?⑸땲??
     * @param accountSubject 怨꾩젙怨쇰ぉ
     * @param businessPartner 嫄곕옒泥?(null ?덉슜)
     * @param department 遺??(null ?덉슜)
     * @param balanceDate ?붿븸 ?쇱옄
     * @param period ?뚭퀎 湲곌컙 (?꾩썡)
     * @return ?대떦 議곌굔??留뚯”?섎뒗 SlBalance ?뷀떚??(議댁옱?섏? ?딆쓣 寃쎌슦 Optional.empty())
     */
    Optional<SlBalance> findByAccountSubjectAndBusinessPartnerAndDepartmentAndCurrencyAndBalanceDateAndPeriod(
            AccountSubject accountSubject, BusinessPartner businessPartner, Department department,
            com.ho.account.basic.domain.Currency currency, LocalDate balanceDate, YearMonth period);

    // TODO: Add methods for querying by date ranges, or other combinations as needed for reporting
    List<SlBalance> findByBalanceDateBetween(LocalDate startDate, LocalDate endDate);

    List<SlBalance> findByBalanceDateBetweenAndAccountSubjectAndBusinessPartnerAndDepartmentAndCurrency(
            LocalDate startDate, LocalDate endDate,
            AccountSubject accountSubject, BusinessPartner businessPartner, Department department,
            com.ho.account.basic.domain.Currency currency
    );
}

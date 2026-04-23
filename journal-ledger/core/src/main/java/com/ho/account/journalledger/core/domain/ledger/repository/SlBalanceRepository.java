package com.ho.account.journalledger.adapter.out.persistence.ledger;

import com.ho.account.journalledger.domain.ledger.SlBalance;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List; // ?ê¾¨ì”«??import ?°ë¶½?
import java.util.Optional;

/**
 * SlBalance ?ë·€??ê³? ?ê¾ªë¸³ Spring Data JPA ?±Ñ‹ë£·ï§Â€?ì¢Šâ”
 * ?ë±€???¨ê¾©?™æ€¨ì‡°?? å«„ê³•?’ï§£? ?ºÂ€?? ?ì¢ì?, æ¹²ê³Œì»??????è¹‚ëŒ??ë¨?˜£ ?ë¶¿ë¸¸ ?ê³—ì” ?ê³? ?¿Â€?±Ñ‹ë???ˆë–.
 */
@Repository
public interface SlBalanceRepository extends JpaRepository<SlBalance, Long> {

    /**
     * ?ë±€???¨ê¾©?™æ€¨ì‡°?? å«„ê³•?’ï§£? ?ºÂ€?? ??±ì˜„, æ¹²ê³Œì»????€???ë’— SlBalance??è­°ê³ ???¸ë•²??
     * @param accountSubject ?¨ê¾©?™æ€¨ì‡°??
     * @param businessPartner å«„ê³•?’ï§£?(null ??‰ìŠœ)
     * @param department ?ºÂ€??(null ??‰ìŠœ)
     * @param balanceDate ?ë¶¿ë¸¸ ??±ì˜„
     * @param period ???€?æ¹²ê³Œì»?(?ê¾©ì¡)
     * @return ??€??è­°ê³Œêµ??ï§ëš¯???ë’— SlBalance ?ë·€???(è­°ëŒ???? ??†ì“£ å¯ƒìŒ??Optional.empty())
     */
    Optional<SlBalance> findByAccountSubjectAndBusinessPartnerAndDepartmentAndCurrencyAndBalanceDateAndPeriod(
            AccountSubject accountSubject, BusinessPartner businessPartner, Department department,
            com.ho.account.masterdata.core.domain.model.Currency currency, LocalDate balanceDate, YearMonth period);

    // TODO: Add methods for querying by date ranges, or other combinations as needed for reporting
    List<SlBalance> findByBalanceDateBetween(LocalDate startDate, LocalDate endDate);

    List<SlBalance> findByBalanceDateBetweenAndAccountSubjectAndBusinessPartnerAndDepartmentAndCurrency(
            LocalDate startDate, LocalDate endDate,
            AccountSubject accountSubject, BusinessPartner businessPartner, Department department,
            com.ho.account.masterdata.core.domain.model.Currency currency
    );
}

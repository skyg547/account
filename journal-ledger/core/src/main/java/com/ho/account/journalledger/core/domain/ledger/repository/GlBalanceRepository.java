package com.ho.account.journalledger.adapter.out.persistence.ledger;

import com.ho.account.journalledger.domain.ledger.GlBalance;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List; // ?ê¾¨ì”«??import ?°ë¶½?
import java.util.Optional;

/**
 * GlBalance ?ë·€??ê³? ?ê¾ªë¸³ Spring Data JPA ?±Ñ‹ë£·ï§Â€?ì¢Šâ”
 * ?ë±€???¨ê¾©?™æ€¨ì‡°?‰æ€??ì¢ì?, æ¹²ê³Œì»???????¥ì•·??ëº¤ì???ë¶¿ë¸¸ ?ê³—ì” ?ê³? ?¿Â€?±Ñ‹ë???ˆë–.
 */
@Repository
public interface GlBalanceRepository extends JpaRepository<GlBalance, Long> {

    /**
     * ?ë±€???¨ê¾©?™æ€¨ì‡°?? ??±ì˜„, æ¹²ê³Œì»????€???ë’— GlBalance??è­°ê³ ???¸ë•²??
     * @param accountSubject ?¨ê¾©?™æ€¨ì‡°??
     * @param balanceDate ?ë¶¿ë¸¸ ??±ì˜„
     * @param period ???€?æ¹²ê³Œì»?(?ê¾©ì¡)
     * @return ??€??è­°ê³Œêµ??ï§ëš¯???ë’— GlBalance ?ë·€???(è­°ëŒ???? ??†ì“£ å¯ƒìŒ??Optional.empty())
     */
    Optional<GlBalance> findByAccountSubjectAndCurrencyAndBalanceDateAndPeriod(AccountSubject accountSubject, com.ho.account.masterdata.core.domain.model.Currency currency, LocalDate balanceDate, YearMonth period);

    /**
     * ?ë±€???¨ê¾©?™æ€¨ì‡°?‰æ€???±ì˜„ è¸°ë¶¿?????€???ë’— GlBalance ?±ÑŠë’ª?ëª? è­°ê³ ???¸ë•²??
     * @param accountSubject ?¨ê¾©?™æ€¨ì‡°??
     * @param startDate ??–ì˜‰ ??±ì˜„
     * @param endDate ?«ë‚…ì¦???±ì˜„
     * @return ??€??è­°ê³Œêµ??ï§ëš¯???ë’— GlBalance ?±ÑŠë’ª??
     */
    // List<GlBalance> findByAccountSubjectAndBalanceDateBetween(AccountSubject accountSubject, LocalDate startDate, LocalDate endDate);
    List<GlBalance> findByBalanceDateBetween(LocalDate startDate, LocalDate endDate);

    List<GlBalance> findByBalanceDateBetweenAndAccountSubjectAndCurrency(
            LocalDate startDate, LocalDate endDate,
            AccountSubject accountSubject, com.ho.account.masterdata.core.domain.model.Currency currency
    );
}

package com.ho.account.journalledger.adapter.out.persistence.ledger;

import com.ho.account.journalledger.domain.ledger.GlBalance;
import com.ho.account.basic.domain.AccountSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List; // ?꾨씫??import 異붽?
import java.util.Optional;

/**
 * GlBalance ?뷀떚?곕? ?꾪븳 Spring Data JPA 由ы룷吏?좊━
 * ?뱀젙 怨꾩젙怨쇰ぉ怨??좎쭨, 湲곌컙?????珥앷퀎?뺤썝???붿븸 ?곗씠?곕? 愿由ы빀?덈떎.
 */
@Repository
public interface GlBalanceRepository extends JpaRepository<GlBalance, Long> {

    /**
     * ?뱀젙 怨꾩젙怨쇰ぉ, ?쇱옄, 湲곌컙???대떦?섎뒗 GlBalance瑜?議고쉶?⑸땲??
     * @param accountSubject 怨꾩젙怨쇰ぉ
     * @param balanceDate ?붿븸 ?쇱옄
     * @param period ?뚭퀎 湲곌컙 (?꾩썡)
     * @return ?대떦 議곌굔??留뚯”?섎뒗 GlBalance ?뷀떚??(議댁옱?섏? ?딆쓣 寃쎌슦 Optional.empty())
     */
    Optional<GlBalance> findByAccountSubjectAndCurrencyAndBalanceDateAndPeriod(AccountSubject accountSubject, com.ho.account.basic.domain.Currency currency, LocalDate balanceDate, YearMonth period);

    /**
     * ?뱀젙 怨꾩젙怨쇰ぉ怨??쇱옄 踰붿쐞???대떦?섎뒗 GlBalance 由ъ뒪?몃? 議고쉶?⑸땲??
     * @param accountSubject 怨꾩젙怨쇰ぉ
     * @param startDate ?쒖옉 ?쇱옄
     * @param endDate 醫낅즺 ?쇱옄
     * @return ?대떦 議곌굔??留뚯”?섎뒗 GlBalance 由ъ뒪??
     */
    // List<GlBalance> findByAccountSubjectAndBalanceDateBetween(AccountSubject accountSubject, LocalDate startDate, LocalDate endDate);
    List<GlBalance> findByBalanceDateBetween(LocalDate startDate, LocalDate endDate);

    List<GlBalance> findByBalanceDateBetweenAndAccountSubjectAndCurrency(
            LocalDate startDate, LocalDate endDate,
            AccountSubject accountSubject, com.ho.account.basic.domain.Currency currency
    );
}

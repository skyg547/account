package com.ho.account.journal.repository;

import com.ho.account.journalledger.domain.journal.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // ?꾨씫??import 異붽?
import org.springframework.data.repository.query.Param; // ?꾨씫??import 異붽?
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    Optional<JournalEntry> findBySlipNo(String slipNo);
    List<JournalEntry> findBySlipDateBetween(LocalDate startDate, LocalDate endDate);
    
    // ?뚭퀎?쇱옄 湲곗? 議고쉶
    List<JournalEntry> findByAccountingDateBetween(LocalDate startDate, LocalDate endDate);
    
    // ?뱀젙 ?뚭퀎?쇱옄???꾪몴 議고쉶 (梨꾨쾲??
    List<JournalEntry> findByAccountingDate(LocalDate accountingDate);

    /**
     * ?뱀젙 ?먯쿇 ?쒖뒪???좏삎怨?ID???대떦?섎뒗 紐⑤뱺 遺꾧컻 ?꾪몴瑜?議고쉶?⑸땲??
     * IFRS 16 由ъ뒪 愿??遺꾧컻 異붿쟻???ъ슜?????덉뒿?덈떎.
     * @param lineageSourceType ?먯쿇 ?쒖뒪???좏삎 (?? "IFRS16_LEASE")
     * @param lineageSourceId ?먯쿇 ?쒖뒪??ID (?? 由ъ뒪 怨꾩빟 ID)
     * @return ?대떦?섎뒗 遺꾧컻 ?꾪몴 由ъ뒪??
     */
    List<JournalEntry> findByLineageSourceTypeAndLineageSourceId(String lineageSourceType, String lineageSourceId);

    /**
     * ?뱀젙 ?뚭퀎?쇱옄? ?먯쿇 ?쒖뒪??ID???대떦?섎뒗 紐⑤뱺 遺꾧컻 ?꾪몴瑜?議고쉶?⑸땲??
     * ?붾퀎 由ъ뒪 ?뚭퀎 泥섎━ 遺꾧컻 寃利앹뿉 ?ъ슜?????덉뒿?덈떎.
     * @param accountingDate ?뚭퀎?쇱옄
     * @param lineageSourceId ?먯쿇 ?쒖뒪??ID (?? 由ъ뒪 怨꾩빟 ID)
     * @return ?대떦?섎뒗 遺꾧컻 ?꾪몴 由ъ뒪??
     */
    List<JournalEntry> findByAccountingDateAndLineageSourceId(LocalDate accountingDate, String lineageSourceId);

    @Query("SELECT je FROM JournalEntry je LEFT JOIN FETCH je.details WHERE je.id = :id")
    Optional<JournalEntry> findByIdWithDetails(@Param("id") Long id);
}

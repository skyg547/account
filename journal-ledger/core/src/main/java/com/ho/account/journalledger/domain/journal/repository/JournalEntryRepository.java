package com.ho.account.journal.repository;

import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // ?ê¾¨ì”«??import ?°ë¶½?
import org.springframework.data.repository.query.Param; // ?ê¾¨ì”«??import ?°ë¶½?
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    Optional<JournalEntry> findBySlipNo(String slipNo);
    List<JournalEntry> findBySlipDateBetween(LocalDate startDate, LocalDate endDate);
    
    // ???€??±ì˜„ æ¹²ê³—? è­°ê³ ??
    List<JournalEntry> findByAccountingDateBetween(LocalDate startDate, LocalDate endDate);
    
    // ?ë±€?????€??±ì˜„???ê¾ªëª´ è­°ê³ ??(ï§?¾¨ì¾??
    List<JournalEntry> findByAccountingDate(LocalDate accountingDate);

    /**
     * ?ë±€???ë¨?¿‡ ??–ë’ª???ì¢ì‚??ID????€???ë’— ï§â‘¤ë±??ºê¾§ì»??ê¾ªëª´??è­°ê³ ???¸ë•²??
     * IFRS 16 ?±ÑŠë’ª ?¿Â€???ºê¾§ì»??°ë¶¿????????????‰ë’¿??ˆë–.
     * @param lineageSourceType ?ë¨?¿‡ ??–ë’ª???ì¢ì‚ (?? "IFRS16_LEASE")
     * @param lineageSourceId ?ë¨?¿‡ ??–ë’ª??ID (?? ?±ÑŠë’ª ?¨ê¾©ë¹?ID)
     * @return ??€???ë’— ?ºê¾§ì»??ê¾ªëª´ ?±ÑŠë’ª??
     */
    List<JournalEntry> findByLineageSourceTypeAndLineageSourceId(String lineageSourceType, String lineageSourceId);

    /**
     * ?ë±€?????€??±ì˜„?? ?ë¨?¿‡ ??–ë’ª??ID????€???ë’— ï§â‘¤ë±??ºê¾§ì»??ê¾ªëª´??è­°ê³ ???¸ë•²??
     * ?ë¶¾í€??±ÑŠë’ª ???€?ï§£ì„???ºê¾§ì»?å¯ƒÂ€ï§ì•¹ë¿??????????‰ë’¿??ˆë–.
     * @param accountingDate ???€??±ì˜„
     * @param lineageSourceId ?ë¨?¿‡ ??–ë’ª??ID (?? ?±ÑŠë’ª ?¨ê¾©ë¹?ID)
     * @return ??€???ë’— ?ºê¾§ì»??ê¾ªëª´ ?±ÑŠë’ª??
     */
    List<JournalEntry> findByAccountingDateAndLineageSourceId(LocalDate accountingDate, String lineageSourceId);

    @Query("SELECT je FROM JournalEntry je LEFT JOIN FETCH je.details WHERE je.id = :id")
    Optional<JournalEntry> findByIdWithDetails(@Param("id") Long id);
}

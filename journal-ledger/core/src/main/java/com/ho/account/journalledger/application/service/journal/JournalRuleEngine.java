package com.ho.account.journalledger.application.service.journal;

import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

/**
 * ?꾪몴 ?앹꽦 洹쒖튃 ?붿쭊 (Journal Rule Engine)
 *
 * <p>???붿쭊? ?몃? ?대깽???? ?먯궛 痍⑤뱷, 由ъ뒪猷?吏湲?瑜??뚭퀎 ?꾪몴濡??먮룞 蹂?섑븯??洹쒖튃??愿由ы빀?덈떎.</p>
 */
@Service
@RequiredArgsConstructor
public class JournalRuleEngine {

    /**
     * ?대깽???곗씠?곕? 湲곕컲?쇰줈 ?꾪몴 珥덉븞???앹꽦?⑸땲??
     * 
     * @param eventData ?대깽???곗씠??(Map ?뺥깭)
     * @param accountingDate ?뚭퀎 ?쇱옄
     * @return ?앹꽦???꾪몴 珥덉븞 (Optional)
     */
    public Optional<JournalEntry> generateJournalEntry(Map<String, Object> eventData, LocalDate accountingDate) {
        // 鍮꾩쫰?덉뒪 洹쒖튃???곕Ⅸ ?꾪몴 ?먮룞 ?앹꽦 濡쒖쭅???꾩튂??怨녹엯?덈떎.
        // ?꾩옱???ㅼ펷?덊넠 援ы쁽留??ы븿?⑸땲??
        JournalEntry entry = new JournalEntry();
        entry.setAccountingDate(accountingDate);
        entry.setSlipDate(accountingDate);
        entry.setStatus(JournalEntryStatus.DRAFT);
        entry.setDetails(new ArrayList<>());
        
        return Optional.of(entry);
    }
}

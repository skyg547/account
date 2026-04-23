package com.ho.account.journalledger.application.service.unsettled;

import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.unsettled.UnsettledItem;
import com.ho.account.journalledger.adapter.out.persistence.unsettled.UnsettledItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class UnsettledService {

    private final UnsettledItemRepository unsettledItemRepository;

    @Autowired
    public UnsettledService(UnsettledItemRepository unsettledItemRepository) {
        this.unsettledItemRepository = unsettledItemRepository;
    }

    // 誘멸�?諛쒖�?泥섎??(?꾪몴 ?뱀?????몄텧)
    public void createUnsettledItem(JournalDetail detail) {
        UnsettledItem item = new UnsettledItem();
        item.setJournalDetail(detail);
        item.setAccountSubject(detail.getAccountSubject());
        item.setBusinessPartner(detail.getBusinessPartner());
        item.setOccurrenceDate(detail.getJournalEntry().getAccountingDate());
        item.setOriginalAmount(detail.getAmount());
        item.setRemainingAmount(detail.getAmount());
        item.setStatus("OPEN");
        
        unsettledItemRepository.save(item);
    }

    // 諛섏??泥섎??(??�룞 吏??諛섏??
    public void settleItem(Long unsettledItemId, BigDecimal amount) {
        UnsettledItem item = unsettledItemRepository.findById(unsettledItemId)
                .orElseThrow(() -> new IllegalArgumentException("誘멸�??????李얠??????�뒿??�떎."));
        
        item.settle(amount);
        unsettledItemRepository.save(item);
    }

    // ?�?�� 諛섏??(FIFO: ?좎엯?좎텧) - ??�떆
    // ?뱀??嫄곕?�泥? ?뱀???�꾩???媛????�옒??誘멸껐遺???李④�?
    public void autoSettle(String businessPartnerCode, String accountCode, BigDecimal amount) {
        List<UnsettledItem> items = unsettledItemRepository.findByBusinessPartnerBusinessPartnerCodeAndStatusNot(businessPartnerCode, "CLEARED");
        // ?좎�????뺣젹 ?꾩슂 (Repository ?�쇰???�?�� ??�???뺣젹)
        
        BigDecimal remainingToSettle = amount;

        for (UnsettledItem item : items) {
            if (!item.getAccountSubject().getCode().equals(accountCode)) continue;
            if (remainingToSettle.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal settleAmount = item.getRemainingAmount().min(remainingToSettle);
            item.settle(settleAmount);
            remainingToSettle = remainingToSettle.subtract(settleAmount);
        }
    }
    
    // 誘멸�??꾪솴 議고??
    @Transactional(readOnly = true)
    public List<UnsettledItem> getUnsettledItems(String businessPartnerCode) {
        return unsettledItemRepository.findByBusinessPartnerBusinessPartnerCodeAndStatusNot(businessPartnerCode, "CLEARED");
    }
}

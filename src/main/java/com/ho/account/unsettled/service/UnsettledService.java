package com.ho.account.unsettled.service;

import com.ho.account.journal.domain.JournalDetail;
import com.ho.account.unsettled.domain.UnsettledItem;
import com.ho.account.unsettled.repository.UnsettledItemRepository;
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

    // 미결 발생 처리 (전표 승인 시 호출)
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

    // 반제 처리 (수동 지정 반제)
    public void settleItem(Long unsettledItemId, BigDecimal amount) {
        UnsettledItem item = unsettledItemRepository.findById(unsettledItemId)
                .orElseThrow(() -> new IllegalArgumentException("미결 항목을 찾을 수 없습니다."));
        
        item.settle(amount);
        unsettledItemRepository.save(item);
    }

    // 자동 반제 (FIFO: 선입선출) - 예시
    // 특정 거래처, 특정 계정의 가장 오래된 미결부터 차감
    public void autoSettle(String businessPartnerCode, String accountCode, BigDecimal amount) {
        List<UnsettledItem> items = unsettledItemRepository.findByBusinessPartnerBusinessPartnerCodeAndStatusNot(businessPartnerCode, "CLEARED");
        // 날짜순 정렬 필요 (Repository 쿼리에서 정렬하거나 여기서 정렬)
        
        BigDecimal remainingToSettle = amount;

        for (UnsettledItem item : items) {
            if (!item.getAccountSubject().getCode().equals(accountCode)) continue;
            if (remainingToSettle.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal settleAmount = item.getRemainingAmount().min(remainingToSettle);
            item.settle(settleAmount);
            remainingToSettle = remainingToSettle.subtract(settleAmount);
        }
    }
    
    // 미결 현황 조회
    @Transactional(readOnly = true)
    public List<UnsettledItem> getUnsettledItems(String businessPartnerCode) {
        return unsettledItemRepository.findByBusinessPartnerBusinessPartnerCodeAndStatusNot(businessPartnerCode, "CLEARED");
    }
}

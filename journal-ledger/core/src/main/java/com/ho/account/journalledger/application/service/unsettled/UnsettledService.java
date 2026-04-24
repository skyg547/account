package com.ho.account.journalledger.application.service.unsettled;

import com.ho.account.journalledger.adapter.out.persistence.unsettled.UnsettledItemRepository;
import com.ho.account.journalledger.domain.unsettled.UnsettledItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 미결 항목 관리 서비스
 */
@Service
@RequiredArgsConstructor
public class UnsettledService {

    private final UnsettledItemRepository unsettledItemRepository;

    @Transactional
    public void registerUnsettledItem(UnsettledItem item) {
        unsettledItemRepository.save(item);
    }

    @Transactional
    public void resolveItem(Long id, BigDecimal amount, String reason) {
        UnsettledItem item = unsettledItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unsettled item not found: " + id));
        
        item.settle(amount);
        unsettledItemRepository.save(item);
    }

    public List<UnsettledItem> getActiveUnsettledItems() {
        return unsettledItemRepository.findByResolvedFalse();
    }
}

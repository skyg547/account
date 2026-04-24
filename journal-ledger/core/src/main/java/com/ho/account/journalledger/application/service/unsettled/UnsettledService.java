package com.ho.account.journalledger.application.service.unsettled;

import com.ho.account.journalledger.adapter.out.persistence.unsettled.UnsettledItemRepository;
import com.ho.account.journalledger.domain.unsettled.UnsettledItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 미결 항목 관리 서비스 (Unsettled Service)
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
    public void settleItem(Long id, BigDecimal amount) {
        UnsettledItem item = unsettledItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unsettled item not found: " + id));
        
        item.settle(amount);
        unsettledItemRepository.save(item);
    }

    public List<UnsettledItem> getActiveUnsettledItems() {
        return unsettledItemRepository.findByResolvedFalse();
    }

    public List<UnsettledItem> getUnsettledItems(String businessPartnerCode) {
        List<UnsettledItem> all = unsettledItemRepository.findByResolvedFalse();
        if (businessPartnerCode == null) return all;
        
        return all.stream()
                .filter(item -> item.getBusinessPartner() != null && 
                        businessPartnerCode.equals(item.getBusinessPartner().getBusinessPartnerCode()))
                .collect(Collectors.toList());
    }
}

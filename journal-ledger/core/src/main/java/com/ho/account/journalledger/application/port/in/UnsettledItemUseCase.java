package com.ho.account.journalledger.application.port.in;

import com.ho.account.journalledger.domain.unsettled.UnsettledItem;
import java.math.BigDecimal;
import java.util.List;

/**
 * 미결 조회와 반제를 외부 어댑터에 제공하는 인바운드 포트입니다.
 */
public interface UnsettledItemUseCase {

    List<UnsettledItem> getUnsettledItems(String businessPartnerCode);

    void settleItem(Long id, BigDecimal amount, String actor, String settlementReference);
}

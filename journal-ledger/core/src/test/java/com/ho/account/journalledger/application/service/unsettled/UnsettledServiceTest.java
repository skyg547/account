package com.ho.account.journalledger.application.service.unsettled;

import com.ho.account.journalledger.application.port.out.UnsettledItemPersistencePort;
import com.ho.account.journalledger.domain.unsettled.UnsettledItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnsettledServiceTest {

    @Mock
    private UnsettledItemPersistencePort persistencePort;

    private UnsettledService service;

    @BeforeEach
    void setUp() {
        service = new UnsettledService(persistencePort);
    }

    @Test
    @DisplayName("거래처 코드가 있으면 DB 조건 조회 출력 포트를 사용한다")
    void findActiveItemsByBusinessPartnerAtPersistenceBoundary() {
        UnsettledItem item = new UnsettledItem();
        item.setBusinessPartnerCode("BP-100");
        when(persistencePort.findActiveByBusinessPartnerCode("BP-100")).thenReturn(List.of(item));

        List<UnsettledItem> result = service.getUnsettledItems(" BP-100 ");

        assertThat(result).containsExactly(item);
        verify(persistencePort).findActiveByBusinessPartnerCode("BP-100");
    }

    @Test
    @DisplayName("반제는 도메인 상태를 변경한 뒤 출력 포트에 저장한다")
    void settleThroughDomainAndSavePort() {
        UnsettledItem item = new UnsettledItem();
        item.setRemainingAmount(new BigDecimal("100.00"));
        item.setSettledAmount(BigDecimal.ZERO);
        when(persistencePort.findById(10L)).thenReturn(Optional.of(item));

        service.settleItem(10L, new BigDecimal("40.00"), "collector-1", "BANK-1");

        assertThat(item.getRemainingAmount()).isEqualByComparingTo("60.00");
        assertThat(item.getStatus()).isEqualTo("PARTIAL");
        verify(persistencePort).save(item);
    }

    @Test
    @DisplayName("서로 다른 참조번호로 다단계 부분 반제 시 각각 정상 반영된다")
    void multiStepSettlementWithDistinctReferences() {
        UnsettledItem item = new UnsettledItem();
        item.setRemainingAmount(new BigDecimal("100.00"));
        item.setSettledAmount(BigDecimal.ZERO);
        when(persistencePort.findById(10L)).thenReturn(Optional.of(item));

        service.settleItem(10L, new BigDecimal("40.00"), "collector-1", "TXN-001");
        service.settleItem(10L, new BigDecimal("35.00"), "collector-2", "TXN-002");

        assertThat(item.getSettledAmount()).isEqualByComparingTo("75.00");
        assertThat(item.getRemainingAmount()).isEqualByComparingTo("25.00");
        assertThat(item.getStatus()).isEqualTo("PARTIAL");
        assertThat(item.getLastSettlementReference()).isEqualTo("TXN-002");
        assertThat(item.getSettlementReferences()).containsExactly("TXN-001", "TXN-002");
    }

    @Test
    @DisplayName("이전 참조번호가 지연 재시도되어도 멱등하게 처리되어 금액이 중복 차감되지 않는다")
    void delayedOutOfOrderDuplicateReplayIsIdempotent() {
        UnsettledItem item = new UnsettledItem();
        item.setRemainingAmount(new BigDecimal("100.00"));
        item.setSettledAmount(BigDecimal.ZERO);
        when(persistencePort.findById(10L)).thenReturn(Optional.of(item));

        // TXN-001 처리
        service.settleItem(10L, new BigDecimal("40.00"), "collector-1", "TXN-001");
        // TXN-002 처리 -> lastSettlementReference는 TXN-002로 변경
        service.settleItem(10L, new BigDecimal("35.00"), "collector-2", "TXN-002");

        // 과거 참조번호 TXN-001이 지연 재시도됨
        service.settleItem(10L, new BigDecimal("40.00"), "collector-1", "TXN-001");

        // 금액이 중복 차감되거나 누적되지 않고 no-op 처리되어야 함
        assertThat(item.getSettledAmount()).isEqualByComparingTo("75.00");
        assertThat(item.getRemainingAmount()).isEqualByComparingTo("25.00");
        assertThat(item.getStatus()).isEqualTo("PARTIAL");
        assertThat(item.getLastSettlementReference()).isEqualTo("TXN-002");
        assertThat(item.getSettlementReferences()).containsExactly("TXN-001", "TXN-002");
    }
}

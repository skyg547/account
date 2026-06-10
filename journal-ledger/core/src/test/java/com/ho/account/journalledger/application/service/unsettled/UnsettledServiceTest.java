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
}

package com.ho.account.journalledger.adapter.in.web.unsettled;

import com.ho.account.journalledger.application.port.out.UnsettledItemPersistencePort;
import com.ho.account.journalledger.application.service.unsettled.UnsettledService;
import com.ho.account.journalledger.domain.unsettled.UnsettledItem;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UnsettledSettlementPrecisionTest {

    private UnsettledItem item;
    private SyntheticPersistencePort persistencePort;
    private UnsettledService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        item = new UnsettledItem();
        item.setId(10L);
        item.setOriginalAmount(new BigDecimal("1.00"));
        item.setSettledAmount(new BigDecimal("0.00"));
        item.setRemainingAmount(new BigDecimal("1.00"));
        item.setStatus("OPEN");
        persistencePort = new SyntheticPersistencePort(item);
        // 유즈케이스 결과를 mock하지 않고 실제 서비스와 도메인의 거부/저장 순서를 통과시킵니다.
        service = new UnsettledService(persistencePort);
        mockMvc = MockMvcBuilders.standaloneSetup(new UnsettledController(service)).build();
    }

    @ParameterizedTest
    @CsvSource({"0.999, 1", "0.001, 1", "100000000000000000.00, 1",
            "null, 0", "0, 0", "-0.01, 0", "1.01, 1"})
    void invalidHttpAmountReturnsBadRequestWithoutSaving(String amount, int expectedReads) throws Exception {
        SettlementState before = SettlementState.from(item);

        mockMvc.perform(settlementRequest(amount, "NEW-REF")).andExpect(status().isBadRequest());

        assertThat(persistencePort.readCalls).isEqualTo(expectedReads);
        assertThat(persistencePort.saveCalls).isZero();
        assertThat(SettlementState.from(item)).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.999", "0.001", "100000000000000000.00", "null", "0", "-0.01", "1.01"})
    void serviceAlsoRejectsInvalidAmountsWithoutSavingWhenDtoValidationIsBypassed(String amount) {
        // 기존 반제 이력이 있는 객체로 감사 메타·참조 내용이 덮어써지지 않는지도 확인합니다.
        item.settle(new BigDecimal("0.01"), "first-collector", "FIRST-REF");
        SettlementState before = SettlementState.from(item);
        BigDecimal requestedAmount = "null".equals(amount) ? null : new BigDecimal(amount);

        assertThatIllegalArgumentException().isThrownBy(
                () -> service.settleItem(10L, requestedAmount, "new-collector", "NEW-REF"));

        assertThat(persistencePort.readCalls).isEqualTo(1);
        assertThat(persistencePort.saveCalls).isZero();
        assertThat(SettlementState.from(item)).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.00", "1.000"})
    void exactHttpAmountReturnsOkAndSavesNormalizedClearance(String amount) throws Exception {
        mockMvc.perform(settlementRequest(amount, "EXACT-REF")).andExpect(status().isOk());

        assertThat(persistencePort.readCalls).isEqualTo(1);
        assertThat(persistencePort.saveCalls).isEqualTo(1);
        assertThat(item.getSettledAmount()).isEqualTo(new BigDecimal("1.00"));
        assertThat(item.getRemainingAmount()).isEqualTo(new BigDecimal("0.00"));
        assertThat(item.getStatus()).isEqualTo("CLEARED");
        assertThat(item.isResolved()).isTrue();
        assertThat(item.getSettlementReferences()).containsExactly("EXACT-REF");
        assertThat(item.getLastSettlementReference()).isEqualTo("EXACT-REF");
        assertThat(item.getLastSettledBy()).isEqualTo("http-collector");
        assertThat(item.getLastSettledAt()).isNotNull();
    }

    @Test
    void centThenRemainderAndDelayedReplayUseRealDomainState() throws Exception {
        mockMvc.perform(settlementRequest("0.01", "FIRST-REF")).andExpect(status().isOk());

        assertThat(item.getSettledAmount()).isEqualTo(new BigDecimal("0.01"));
        assertThat(item.getRemainingAmount()).isEqualTo(new BigDecimal("0.99"));
        assertThat(item.getStatus()).isEqualTo("PARTIAL");
        assertThat(item.isResolved()).isFalse();
        assertThat(persistencePort.saveCalls).isEqualTo(1);

        mockMvc.perform(settlementRequest("0.99", "LAST-REF")).andExpect(status().isOk());

        assertThat(item.getSettledAmount()).isEqualTo(new BigDecimal("1.00"));
        assertThat(item.getRemainingAmount()).isEqualTo(new BigDecimal("0.00"));
        assertThat(item.getStatus()).isEqualTo("CLEARED");
        assertThat(item.isResolved()).isTrue();
        assertThat(item.getSettlementReferences()).containsExactly("FIRST-REF", "LAST-REF");
        assertThat(persistencePort.saveCalls).isEqualTo(2);
        SettlementState cleared = SettlementState.from(item);

        mockMvc.perform(settlementRequest("0.999", "FIRST-REF")).andExpect(status().isOk());

        assertThat(SettlementState.from(item)).isEqualTo(cleared);
        // 기존 서비스는 no-op 결과도 save에 전달하지만 금액·이력에는 다시 반영하지 않습니다.
        assertThat(persistencePort.saveCalls).isEqualTo(3);
    }

    private static MockHttpServletRequestBuilder settlementRequest(String amount, String reference) {
        return post("/api/unsettled/10/settle")
                .header("X-User-ID", "http-collector")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"amount": %s, "settlementReference": "%s"}
                        """.formatted(amount, reference));
    }

    private record SettlementState(BigDecimal original, BigDecimal settled, BigDecimal remaining,
                                   String status, boolean resolved, Set<String> references,
                                   String lastReference, String actor, LocalDateTime settledAt) {
        private static SettlementState from(UnsettledItem item) {
            return new SettlementState(item.getOriginalAmount(), item.getSettledAmount(),
                    item.getRemainingAmount(), item.getStatus(), item.isResolved(),
                    new LinkedHashSet<>(item.getSettlementReferences()), item.getLastSettlementReference(),
                    item.getLastSettledBy(), item.getLastSettledAt());
        }
    }

    private static final class SyntheticPersistencePort implements UnsettledItemPersistencePort {
        private final UnsettledItem item;
        private int readCalls;
        private int saveCalls;

        private SyntheticPersistencePort(UnsettledItem item) {
            this.item = item;
        }

        @Override
        public UnsettledItem save(UnsettledItem item) {
            assertThat(item).isSameAs(this.item);
            saveCalls++;
            return item;
        }

        @Override
        public Optional<UnsettledItem> findById(Long id) {
            readCalls++;
            return item.getId().equals(id) ? Optional.of(item) : Optional.empty();
        }

        @Override
        public List<UnsettledItem> findActive() {
            throw new UnsupportedOperationException("조회 목록은 이 반제 테스트의 범위가 아닙니다.");
        }

        @Override
        public List<UnsettledItem> findActiveByBusinessPartnerCode(String businessPartnerCode) {
            throw new UnsupportedOperationException("거래처 조회는 이 반제 테스트의 범위가 아닙니다.");
        }
    }
}

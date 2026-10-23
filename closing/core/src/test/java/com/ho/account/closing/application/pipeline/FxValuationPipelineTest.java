package com.ho.account.closing.application.pipeline;

import com.ho.account.closing.application.service.FxValuationBalance;
import com.ho.account.closing.application.service.FxValuationService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FxValuationPipelineTest {

    @Test
    void chunkFailureIsAggregatedAndPropagated() {
        FxValuationService service = mock(FxValuationService.class);
        FxValuationPipeline pipeline = new FxValuationPipeline(service);
        LocalDate valuationDate = LocalDate.of(2026, 5, 31);
        FxValuationBalance success = balance("11000", "EUR");
        FxValuationBalance failure = balance("12000", "USD");
        when(service.prepareFxValuationForAccount(success, valuationDate, 77L))
                .thenReturn(Optional.empty());
        when(service.prepareFxValuationForAccount(failure, valuationDate, 77L))
                .thenThrow(new IllegalStateException("missing rate"));

        assertThatThrownBy(() -> pipeline.processChunk(
                List.of(success, failure), valuationDate, 77L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("1 balance");

        verify(service).prepareFxValuationForAccount(success, valuationDate, 77L);
        verify(service).prepareFxValuationForAccount(failure, valuationDate, 77L);
        verify(service, never()).postPreparedFxValuation(any());
    }

    private FxValuationBalance balance(String accountCode, String currencyCode) {
        return new FxValuationBalance(
                accountCode,
                currencyCode,
                new BigDecimal("100"),
                new BigDecimal("110"));
    }
}

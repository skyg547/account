package com.ho.account.asset.infrastructure.adapter;

import com.ho.account.asset.domain.FixedAssetDepreciationResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AssetJdbcAdapterTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final AssetJdbcAdapter adapter = new AssetJdbcAdapter(jdbcTemplate);

    @Test
    void updateDepreciationBulkWritesCalculatedValuesOnce() {
        FixedAssetDepreciationResult result = new FixedAssetDepreciationResult(
                10L,
                new BigDecimal("100000.00"),
                new BigDecimal("300000.00"),
                new BigDecimal("900000.00"),
                "ACTIVE");

        adapter.updateDepreciationBulk(List.of(result), LocalDate.of(2026, 4, 30));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Object[]>> argsCaptor = ArgumentCaptor.forClass(List.class);
        verify(jdbcTemplate).batchUpdate(anyString(), argsCaptor.capture());

        Object[] args = argsCaptor.getValue().get(0);
        assertThat(args[0]).isEqualTo(new BigDecimal("300000.00"));
        assertThat(args[1]).isEqualTo(new BigDecimal("900000.00"));
        assertThat(args[2]).isEqualTo("ACTIVE");
        assertThat(args[3]).isEqualTo(LocalDate.of(2026, 4, 30));
        assertThat(args[4]).isEqualTo(10L);
    }

    @Test
    void updateDepreciationBulkSkipsEmptyResults() {
        adapter.updateDepreciationBulk(List.of(), LocalDate.of(2026, 4, 30));

        verify(jdbcTemplate, never()).batchUpdate(anyString(), org.mockito.ArgumentMatchers.<List<Object[]>>any());
    }
}
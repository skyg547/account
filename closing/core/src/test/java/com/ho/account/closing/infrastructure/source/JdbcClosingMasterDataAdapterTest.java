package com.ho.account.closing.infrastructure.source;

import com.ho.account.contracts.masterdata.AccountSubjectRef;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JdbcClosingMasterDataAdapterTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final JdbcClosingMasterDataAdapter adapter = new JdbcClosingMasterDataAdapter(jdbc);
    private static final LocalDate DATE = LocalDate.of(2026, 9, 10);

    @Test
    void historicalRatePreservesPrecisionAndBindsRequestedCurrencyAndDate() throws Exception {
        ResultSet row = mock(ResultSet.class);
        when(row.getBigDecimal("rate")).thenReturn(new BigDecimal("1370.12345678"));
        when(jdbc.query(contains("effective_date <= ?"), any(RowMapper.class),
                eq("USD"), eq("KRW"), eq(Date.valueOf(DATE))))
                .thenAnswer(call -> List.of(((RowMapper<?>) call.getArgument(1)).mapRow(row, 0)));
        assertThat(adapter.findRate(" usd ", "krw", DATE)).contains(new BigDecimal("1370.12345678"));
    }

    @Test
    void overlappingEffectiveAccountsFailRatherThanChoosingArbitraryClassification() {
        var debit = new AccountSubjectRef("10100", "asset", false, false, "DEBIT");
        var credit = new AccountSubjectRef("10100", "liability", false, false, "CREDIT");
        when(jdbc.query(contains("valid_from <= ? AND valid_to >= ?"), any(RowMapper.class),
                eq("10100"), eq(Date.valueOf(DATE)), eq(Date.valueOf(DATE))))
                .thenReturn(List.of(debit, credit));
        assertThatThrownBy(() -> adapter.findAccountSubjectAt("10100", DATE))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Overlapping");
    }

    @Test
    void absentReferenceIsEmptyAndInvalidInputNeverQueriesDatabase() {
        when(jdbc.query(anyString(), any(RowMapper.class), any(), any(), any())).thenReturn(List.of());
        assertThat(adapter.findAccountSubjectAt("10100", DATE)).isEmpty();
        assertThat(adapter.findRate("USD", "KRW", DATE)).isEmpty();
        clearInvocations(jdbc);
        assertThatThrownBy(() -> adapter.findRate("US", "KRW", DATE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.findAccountSubjectAt(" ", DATE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.findAccountSubject("10100")).isInstanceOf(UnsupportedOperationException.class);
        verifyNoInteractions(jdbc);
    }
}

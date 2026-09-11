package com.ho.account.closing.batch.config;

import com.ho.account.closing.batch.adapter.out.GlAllowanceBalanceLookupAdapter;
import com.ho.account.closing.batch.adapter.out.JdbcEclAllowanceResultAdapter;
import com.ho.account.closing.batch.adapter.out.JournalFxValuationBalanceSource;
import com.ho.account.closing.infrastructure.source.ClosingReadOnlySources;
import com.ho.account.closing.infrastructure.external.HttpClosingJournalAdapter;
import com.ho.account.closing.infrastructure.source.ClosingReadOnlySourceConfiguration;
import com.ho.account.closing.infrastructure.source.JdbcClosingMasterDataAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

/**
 * Dev keeps Batch metadata on Closing's primary database and streams source data through
 * explicitly configured read-only pools. Remote journal writes are separate transactions;
 * stable date/lineage parameters must be retained on restart after a failed chunk.
 */
@Configuration(proxyBeanMethods = false)
@Profile("dev")
@Import({ClosingReadOnlySourceConfiguration.class, HttpClosingJournalAdapter.class})
public class ClosingBatchDevConfiguration {

    @Bean
    public JournalFxValuationBalanceSource journalFxValuationBalanceSource(ClosingReadOnlySources sources) {
        return new JournalFxValuationBalanceSource(sources.journalDataSource(), sources.journalJdbcTemplate());
    }

    @Bean
    public GlAllowanceBalanceLookupAdapter glAllowanceBalanceLookupAdapter(ClosingReadOnlySources sources) {
        return new GlAllowanceBalanceLookupAdapter(sources.journalJdbcTemplate());
    }

    @Bean
    public JdbcEclAllowanceResultAdapter jdbcEclAllowanceResultAdapter(ClosingReadOnlySources sources) {
        return new JdbcEclAllowanceResultAdapter(sources.eclJdbcTemplate());
    }

    @Bean
    public JdbcClosingMasterDataAdapter jdbcClosingMasterDataAdapter(ClosingReadOnlySources sources) {
        return new JdbcClosingMasterDataAdapter(sources.masterDataJdbcTemplate());
    }
}

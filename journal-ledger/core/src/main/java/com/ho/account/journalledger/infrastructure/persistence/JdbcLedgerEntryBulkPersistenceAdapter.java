package com.ho.account.journalledger.infrastructure.persistence;

import com.ho.account.journalledger.application.port.out.LedgerEntryPersistencePort;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.ledger.domain.GlEntry;
import com.ho.account.journalledger.domain.ledger.domain.SlEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * GL/SL 엔트리를 JDBC batch insert로 저장하는 운영용 출력 어댑터입니다.
 *
 * <p>업무 규칙은 `PostingService`에서 이미 `GlEntry`/`SlEntry` 도메인 객체로 변환합니다.
 * 이 어댑터는 1억 건급 전기 처리에서 JPA 영속성 컨텍스트 비용을 피하고, DB에 여러 행을
 * 한 번에 전달하는 기술 구현만 담당합니다.</p>
 */
@Component
@ConditionalOnProperty(name = "journal-ledger.ledger.persistence-mode", havingValue = "jdbc-bulk")
@RequiredArgsConstructor
public class JdbcLedgerEntryBulkPersistenceAdapter implements LedgerEntryPersistencePort {

    private static final String INSERT_GL_ENTRY = """
            INSERT INTO gl_entries (
                journal_detail_id, account_code, currency_code, fiscal_year, fiscal_period, posting_date,
                dr_amount, cr_amount, base_dr_amount, base_cr_amount, summary, lineage_source_type, lineage_source_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String INSERT_SL_ENTRY = """
            INSERT INTO sl_entries (
                journal_detail_id, account_code, business_partner_code, dept_code, currency_code,
                fiscal_year, fiscal_period, posting_date, dr_amount, cr_amount, base_dr_amount,
                base_cr_amount, summary, lineage_source_type, lineage_source_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void saveGlEntries(List<GlEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(INSERT_GL_ENTRY, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                GlEntry entry = entries.get(i);
                ps.setObject(1, journalDetailId(entry.getJournalDetail()));
                ps.setString(2, entry.getAccountCode());
                ps.setString(3, entry.getCurrencyCode());
                ps.setString(4, entry.getFiscalYear());
                ps.setString(5, entry.getFiscalPeriod());
                ps.setObject(6, entry.getPostingDate());
                ps.setBigDecimal(7, entry.getDrAmount());
                ps.setBigDecimal(8, entry.getCrAmount());
                ps.setBigDecimal(9, entry.getBaseDrAmount());
                ps.setBigDecimal(10, entry.getBaseCrAmount());
                ps.setString(11, entry.getSummary());
                ps.setString(12, entry.getLineageSourceType());
                ps.setString(13, entry.getLineageSourceId());
            }

            @Override
            public int getBatchSize() {
                return entries.size();
            }
        });
    }

    @Override
    public void saveSlEntries(List<SlEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(INSERT_SL_ENTRY, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                SlEntry entry = entries.get(i);
                ps.setObject(1, journalDetailId(entry.getJournalDetail()));
                ps.setString(2, entry.getAccountCode());
                ps.setString(3, entry.getBusinessPartnerCode());
                ps.setString(4, entry.getDepartmentCode());
                ps.setString(5, entry.getCurrencyCode());
                ps.setString(6, entry.getFiscalYear());
                ps.setString(7, entry.getFiscalPeriod());
                ps.setObject(8, entry.getPostingDate());
                ps.setBigDecimal(9, entry.getDrAmount());
                ps.setBigDecimal(10, entry.getCrAmount());
                ps.setBigDecimal(11, entry.getBaseDrAmount());
                ps.setBigDecimal(12, entry.getBaseCrAmount());
                ps.setString(13, entry.getSummary());
                ps.setString(14, entry.getLineageSourceType());
                ps.setString(15, entry.getLineageSourceId());
            }

            @Override
            public int getBatchSize() {
                return entries.size();
            }
        });
    }

    private Long journalDetailId(JournalDetail journalDetail) {
        if (journalDetail == null || journalDetail.getId() == null) {
            throw new IllegalArgumentException("JDBC ledger entry insert requires a persisted JournalDetail id.");
        }
        return journalDetail.getId();
    }
}

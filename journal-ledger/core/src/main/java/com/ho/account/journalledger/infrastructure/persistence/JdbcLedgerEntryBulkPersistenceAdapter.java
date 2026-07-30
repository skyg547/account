package com.ho.account.journalledger.infrastructure.persistence;

import com.ho.account.journalledger.application.port.out.LedgerEntryPersistencePort;
import com.ho.account.journalledger.domain.ledger.domain.GeneralLedger;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * GL/SL 엔트리를 JDBC batch insert로 저장하는 운영용 출력 어댑터입니다.
 *
 * <p>업무 규칙은 {@link GeneralLedger}가 이미 검증한 불변 posting snapshot에 있습니다.
 * 이 어댑터는 1억 건급 전기 처리에서 JPA 영속성 컨텍스트 비용을 피하고, 같은 snapshot을
 * GL/SL SQL 파라미터로 바꾸는 기술 구현만 담당합니다.</p>
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
    public void save(GeneralLedger generalLedger) {
        if (generalLedger == null) {
            throw new IllegalArgumentException("저장할 GeneralLedger Aggregate는 필수입니다.");
        }
        saveGlEntries(generalLedger);
        saveSlEntries(generalLedger);
    }

    private void saveGlEntries(GeneralLedger generalLedger) {
        jdbcTemplate.batchUpdate(INSERT_GL_ENTRY, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                GeneralLedger.Posting posting = generalLedger.postings().get(i);
                ps.setObject(1, posting.journalDetailId());
                ps.setString(2, posting.accountCode());
                ps.setString(3, posting.currencyCode());
                ps.setString(4, posting.fiscalYear());
                ps.setString(5, posting.fiscalPeriod());
                ps.setObject(6, posting.postingDate());
                ps.setBigDecimal(7, posting.debit().amount());
                ps.setBigDecimal(8, posting.credit().amount());
                ps.setBigDecimal(9, posting.baseDebit().amount());
                ps.setBigDecimal(10, posting.baseCredit().amount());
                ps.setString(11, posting.summary());
                ps.setString(12, posting.lineageSourceType());
                ps.setString(13, posting.lineageSourceId());
            }

            @Override
            public int getBatchSize() {
                return generalLedger.postings().size();
            }
        });
    }

    private void saveSlEntries(GeneralLedger generalLedger) {
        jdbcTemplate.batchUpdate(INSERT_SL_ENTRY, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                GeneralLedger.Posting posting = generalLedger.postings().get(i);
                ps.setObject(1, posting.journalDetailId());
                ps.setString(2, posting.accountCode());
                ps.setString(3, posting.businessPartnerCode());
                ps.setString(4, posting.departmentCode());
                ps.setString(5, posting.currencyCode());
                ps.setString(6, posting.fiscalYear());
                ps.setString(7, posting.fiscalPeriod());
                ps.setObject(8, posting.postingDate());
                ps.setBigDecimal(9, posting.debit().amount());
                ps.setBigDecimal(10, posting.credit().amount());
                ps.setBigDecimal(11, posting.baseDebit().amount());
                ps.setBigDecimal(12, posting.baseCredit().amount());
                ps.setString(13, posting.summary());
                ps.setString(14, posting.lineageSourceType());
                ps.setString(15, posting.lineageSourceId());
            }

            @Override
            public int getBatchSize() {
                return generalLedger.postings().size();
            }
        });
    }
}

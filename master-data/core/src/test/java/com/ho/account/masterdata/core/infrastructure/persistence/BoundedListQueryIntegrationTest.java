package com.ho.account.masterdata.core.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.masterdata.core.application.query.BusinessPartnerSummary;
import com.ho.account.masterdata.core.application.query.PageSlice;
import com.ho.account.masterdata.core.application.query.PageWindow;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeStatus;
import com.ho.account.masterdata.core.infrastructure.persistence.mapper.MasterDataChangeRequestMapper;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.jpa.properties.hibernate.session_factory.statement_inspector=com.ho.account.masterdata.core.infrastructure.persistence.PaginationSqlInspector"
})
@ContextConfiguration(classes = BoundedListQueryIntegrationTest.TestApplication.class)
@Import({JpaBusinessPartnerPersistenceAdapter.class,
        JpaMasterDataChangeRequestPersistenceAdapter.class, MasterDataChangeRequestMapper.class})
class BoundedListQueryIntegrationTest {
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.ho.account.masterdata.core")
    static class TestApplication { }

    @Autowired private JdbcTemplate jdbc;
    @Autowired private JpaBusinessPartnerPersistenceAdapter partners;
    @Autowired private JpaMasterDataChangeRequestPersistenceAdapter changes;
    @Autowired private EntityManagerFactory entityManagerFactory;

    @Test
    void partnerHistoryUsesSqlLimitStableTieBreakAndNeverLoadsAccounts() {
        // Many SCD2 versions and child accounts amplify an accidental collection fetch.
        for (int code = 0; code < 200; code++) {
            for (int version = 0; version < 3; version++) {
                String key = "BP-" + String.format("%03d", code);
                jdbc.update("""
                        INSERT INTO business_partners
                        (business_partner_code, business_partner_name, partner_type, use_yn,
                         kyc_status, risk_rating, valid_from, valid_to)
                        VALUES (?, ?, 'VENDOR', TRUE, 'APPROVED', 'LOW', ?, ?)
                        """, key, "Search partner " + code, LocalDate.of(2020 + version, 1, 1),
                        LocalDate.of(2020 + version, 12, 31));
                Long id = jdbc.queryForObject("SELECT MAX(id) FROM business_partners", Long.class);
                for (int account = 0; account < 4; account++) {
                    jdbc.update("""
                            INSERT INTO business_partner_accounts
                            (business_partner_id, bank_name, account_number, account_holder, is_main_account)
                            VALUES (?, 'bank', ?, 'holder', ?)
                            """, id, "account-" + account, account == 0);
                }
            }
        }
        // H2 permits this overlapping fixture: code and validFrom tie, so ID must decide the order.
        jdbc.update("""
                INSERT INTO business_partners
                (business_partner_code, business_partner_name, partner_type, use_yn,
                 kyc_status, risk_rating, valid_from, valid_to)
                VALUES ('BP-000', 'Search duplicate', 'VENDOR', TRUE, 'APPROVED', 'LOW',
                        DATE '2020-01-01', DATE '2020-12-31')
                """);

        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        PaginationSqlInspector.clear();
        List<BusinessPartnerSummary> summaries = new ArrayList<>();
        int page = 0;
        boolean hasNext;
        do {
            PageSlice<BusinessPartnerSummary> slice = partners.findAllSummaries(new PageWindow(page++, 7));
            assertThat(slice.content()).hasSizeLessThanOrEqualTo(7);
            summaries.addAll(slice.content());
            hasNext = slice.hasNext();
        } while (hasNext);
        assertThat(summaries).hasSize(601).isSortedAccordingTo(
                Comparator.comparing(BusinessPartnerSummary::businessPartnerCode)
                        .thenComparing(BusinessPartnerSummary::validFrom)
                        .thenComparing(BusinessPartnerSummary::id));
        assertThat(new HashSet<>(summaries.stream().map(BusinessPartnerSummary::id).toList()))
                .hasSize(601);
        assertThat(statistics.getEntityLoadCount()).isZero();
        assertThat(statistics.getCollectionLoadCount()).isZero();
        assertBoundedSelects(PaginationSqlInspector.statements(), page, "business_partners");

        PaginationSqlInspector.clear();
        assertThat(partners.findActiveSummaries(LocalDate.of(2021, 12, 31), new PageWindow(0, 7))
                .content()).hasSize(7).allSatisfy(summary ->
                        assertThat(summary.validFrom()).isEqualTo(LocalDate.of(2021, 1, 1)));
        assertThat(partners.findActiveSummaries(LocalDate.of(2022, 1, 1), new PageWindow(0, 7))
                .content()).hasSize(7).allSatisfy(summary ->
                        assertThat(summary.validFrom()).isEqualTo(LocalDate.of(2022, 1, 1)));
        assertThat(partners.findActiveSummaries(LocalDate.of(2019, 12, 31), new PageWindow(0, 7))
                .content()).isEmpty();
        assertThat(partners.searchActiveByName("Search", LocalDate.of(2021, 6, 1),
                new PageWindow(0, 7)).content()).hasSize(7).allSatisfy(summary ->
                        assertThat(summary.validFrom()).isEqualTo(LocalDate.of(2021, 1, 1)));
        assertThat(partners.searchActiveByName("Search", LocalDate.of(2019, 6, 1),
                new PageWindow(0, 7)).content()).isEmpty();
        assertBoundedSelects(PaginationSqlInspector.statements(), 5, "business_partners");
    }

    @Test
    void pendingPagesUseSqlLimitAndRequestedAtIdTieBreak() {
        for (int i = 0; i < 13; i++) {
            jdbc.update("""
                    INSERT INTO master_data_change_requests
                    (target_type, target_key, change_type, status, lock_version, effective_date,
                     requested_version, requested_by, requested_at)
                    VALUES ('BUSINESS_PARTNER', ?, 'CREATE', 'REQUESTED', 0,
                            DATE '2026-01-01', 1, 'tester', TIMESTAMP '2025-12-01 10:00:00')
                    """, "BP-" + i);
        }
        PaginationSqlInspector.clear();
        List<Long> ids = new ArrayList<>();
        for (int page = 0; page < 4; page++) {
            PageSlice<MasterDataChangeRequest> slice = changes.findByStatus(
                    ChangeStatus.REQUESTED, new PageWindow(page, 4));
            ids.addAll(slice.content().stream().map(MasterDataChangeRequest::getId).toList());
            assertThat(slice.hasNext()).isEqualTo(page < 3);
        }
        assertThat(ids).hasSize(13).isSorted();
        assertThat(new HashSet<>(ids)).hasSize(13);
        assertBoundedSelects(PaginationSqlInspector.statements(), 4, "master_data_change_requests");
    }

    private void assertBoundedSelects(List<String> statements, int expectedCount, String table) {
        List<String> selects = statements.stream()
                .filter(sql -> sql.startsWith("select") && sql.contains(table))
                .toList();
        assertThat(selects).hasSize(expectedCount);
        // Assert the generated SQL limit itself, not just a truncated Java result.
        assertThat(selects).allSatisfy(sql -> {
            assertThat(sql).containsAnyOf(" fetch first ", " limit ");
            assertThat(sql).doesNotContain("business_partner_accounts");
            assertThat(sql).contains(" order by ");
        });
    }
}

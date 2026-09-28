package com.ho.account.closing.application.service;

import com.ho.account.contracts.journal.JournalDetailAggregateSummary;
import com.ho.account.contracts.journal.JournalDetailSummary;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.contracts.journal.JournalQueryPort;
import com.ho.account.contracts.journal.JournalSide;
import com.ho.account.contracts.journal.JournalSummary;
import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.DepartmentRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnnualClosingServiceTest {

    private static final int YEAR = 2026;
    private static final LocalDate YEAR_END = LocalDate.of(YEAR, 12, 31);
    private static final String REVENUE_ACCOUNT = "41000";
    private static final String RETAINED_EARNINGS_ACCOUNT = "35000";

    @Test
    void closesOnlyPostedIncomeStatementBaseBalancesInStableOrder() {
        StatefulJournal journal = new StatefulJournal();
        journal.addSource("POSTED", List.of(
                detail("51000", "EXPENSES", JournalSide.DEBIT, "9999.00", "300.00"),
                detail(REVENUE_ACCOUNT, "REVENUE", JournalSide.CREDIT, "8888.00", "1000.00")));
        journal.addSource("DRAFT", List.of(
                detail("49999", "REVENUE", JournalSide.CREDIT, "50000.00", "50000.00")));
        AnnualClosingService service = service(journal);

        service.performIncomeStatementClosing(YEAR, RETAINED_EARNINGS_ACCOUNT);

        assertThat(journal.createdCommands()).hasSize(1);
        JournalEntryCommand command = journal.createdCommands().get(0);
        assertThat(command.slipDate()).isEqualTo(YEAR_END);
        assertThat(command.slipNo()).startsWith("ACL20261231").hasSize(20);
        assertThat(command.lines()).extracting(line -> line.accountCode())
                .containsExactly(REVENUE_ACCOUNT, "51000", RETAINED_EARNINGS_ACCOUNT);
        assertThat(command.lines()).extracting(line -> line.drcrType())
                .containsExactly("DEBIT", "CREDIT", "CREDIT");
        assertThat(command.lines()).extracting(line -> line.amount().toPlainString())
                .containsExactly("1000.00", "300.00", "700.00");
        assertThat(journal.readDetailIds()).containsExactly(1L);
    }

    @Test
    void exactSnapshotDraftRetryReadsAndValidatesFullDraftBeforeReuse() {
        StatefulJournal journal = journalWithPostedRevenue("1000.00");
        AnnualClosingService service = service(journal);

        service.performIncomeStatementClosing(YEAR, RETAINED_EARNINGS_ACCOUNT);
        long draftId = journal.createdEntryId(0);
        JournalEntryCommand original = journal.createdCommands().get(0);
        journal.clearReadDetailIds();

        service.performIncomeStatementClosing(YEAR, RETAINED_EARNINGS_ACCOUNT);

        assertThat(journal.createdCommands()).hasSize(1);
        assertThat(journal.readDetailIds()).contains(draftId);
        assertThat(original.lineageSourceType()).isEqualTo("ANNUAL_CLOSING");
        assertThat(original.lineageSourceId()).isNotBlank().isNotEqualTo(String.valueOf(YEAR));
        assertThat(original.lines()).extracting(
                        line -> line.drcrType() + "|" + line.accountCode() + "|"
                                + line.amount().toPlainString() + "|" + line.baseAmount().toPlainString())
                .containsExactly(
                        "DEBIT|41000|1000.00|1000.00",
                        "CREDIT|35000|1000.00|1000.00");
    }

    @Test
    void changedSourceAmountRejectsStaleDraftWithoutSecondWrite() {
        StatefulJournal journal = journalWithPostedRevenue("1000.00");
        AnnualClosingService service = service(journal);
        service.performIncomeStatementClosing(YEAR, RETAINED_EARNINGS_ACCOUNT);

        journal.sourceDetails(1L).get(0).setAmount(new BigDecimal("1500.00"));
        journal.sourceDetails(1L).get(0).setBaseAmount(new BigDecimal("1500.00"));

        assertThatThrownBy(() -> service.performIncomeStatementClosing(YEAR, RETAINED_EARNINGS_ACCOUNT))
                .isInstanceOf(IllegalStateException.class);
        assertThat(journal.createdCommands()).hasSize(1);
    }

    @Test
    void changedSourceAccountRejectsStaleDraft() {
        StatefulJournal journal = journalWithPostedRevenue("1000.00");
        AnnualClosingService service = service(journal);
        service.performIncomeStatementClosing(YEAR, RETAINED_EARNINGS_ACCOUNT);

        journal.sourceDetails(1L).get(0).setAccountCode("42000");

        assertThatThrownBy(() -> service.performIncomeStatementClosing(YEAR, RETAINED_EARNINGS_ACCOUNT))
                .isInstanceOf(IllegalStateException.class);
        assertThat(journal.createdCommands()).hasSize(1);
    }

    @Test
    void netEqualClassificationChangeStillRejectsStaleDraft() {
        StatefulJournal journal = journalWithPostedRevenue("1000.00");
        AnnualClosingService service = service(journal);
        service.performIncomeStatementClosing(YEAR, RETAINED_EARNINGS_ACCOUNT);

        // A CREDIT has the same signed balance under both categories, so line-only comparison is insufficient.
        journal.sourceDetails(1L).get(0).setAccountCategory("EXPENSES");

        assertThatThrownBy(() -> service.performIncomeStatementClosing(YEAR, RETAINED_EARNINGS_ACCOUNT))
                .isInstanceOf(IllegalStateException.class);
        assertThat(journal.createdCommands()).hasSize(1);
    }

    @Test
    void postedCloseThenReopenedAdjustmentCreatesOneDistinctDeltaAndPostedRetryIsNoOp() {
        StatefulJournal journal = journalWithPostedRevenue("1000.00");
        AnnualClosingService service = service(journal);
        service.performIncomeStatementClosing(YEAR, RETAINED_EARNINGS_ACCOUNT);
        JournalEntryCommand baseClose = journal.createdCommands().get(0);
        journal.markCreatedEntryPosted(0);
        journal.addSource("POSTED", List.of(
                detail(REVENUE_ACCOUNT, "REVENUE", JournalSide.CREDIT, "500.00", "500.00")));

        service.performIncomeStatementClosing(YEAR, RETAINED_EARNINGS_ACCOUNT);

        assertThat(journal.createdCommands()).hasSize(2);
        JournalEntryCommand delta = journal.createdCommands().get(1);
        assertThat(delta.slipNo()).isNotEqualTo(baseClose.slipNo());
        assertThat(delta.lineageSourceId()).isNotEqualTo(baseClose.lineageSourceId());
        assertThat(delta.lines()).extracting(
                        line -> line.drcrType() + "|" + line.accountCode() + "|" + line.amount().toPlainString())
                .containsExactly("DEBIT|41000|500.00", "CREDIT|35000|500.00");

        journal.markCreatedEntryPosted(1);
        service.performIncomeStatementClosing(YEAR, RETAINED_EARNINGS_ACCOUNT);

        assertThat(journal.createdCommands()).hasSize(2);
    }

    @Test
    void legacyPostedAnnualCloseIsValidatedBeforeReopenedAdjustmentCreatesDelta() {
        StatefulJournal journal = journalWithPostedRevenue("1000.00");
        String legacySlipNo = journal.addLegacyPostedAnnualClose("1000.00");
        AnnualClosingService service = service(journal);

        service.performIncomeStatementClosing(YEAR, RETAINED_EARNINGS_ACCOUNT);
        assertThat(journal.createdCommands()).isEmpty();

        journal.addSource("POSTED", List.of(
                detail(REVENUE_ACCOUNT, "REVENUE", JournalSide.CREDIT, "500.00", "500.00")));
        service.performIncomeStatementClosing(YEAR, RETAINED_EARNINGS_ACCOUNT);

        assertThat(journal.createdCommands()).hasSize(1);
        JournalEntryCommand delta = journal.createdCommands().get(0);
        assertThat(delta.slipNo()).isNotEqualTo(legacySlipNo);
        assertThat(delta.lineageSourceId()).isNotEqualTo(String.valueOf(YEAR));
        assertThat(delta.lines()).extracting(
                        line -> line.drcrType() + "|" + line.accountCode() + "|" + line.amount().toPlainString())
                .containsExactly("DEBIT|41000|500.00", "CREDIT|35000|500.00");
    }

    @Test
    void providerReorderingDoesNotChangeSnapshotOrMakeExactDraftStale() {
        StatefulJournal journal = new StatefulJournal();
        journal.addSource("POSTED", List.of(
                detail("51000", "EXPENSES", JournalSide.DEBIT, "300.00", "300.00"),
                detail(REVENUE_ACCOUNT, "REVENUE", JournalSide.CREDIT, "1000.00", "1000.00")));
        journal.addSource("POSTED", List.of(
                detail("42000", "REVENUE", JournalSide.CREDIT, "200.00", "200.00"),
                detail("52000", "EXPENSES", JournalSide.DEBIT, "50.00", "50.00")));
        AnnualClosingService service = service(journal);

        service.performIncomeStatementClosing(YEAR, RETAINED_EARNINGS_ACCOUNT);
        JournalEntryCommand original = journal.createdCommands().get(0);
        journal.reverseProviderOrder();

        service.performIncomeStatementClosing(YEAR, RETAINED_EARNINGS_ACCOUNT);

        assertThat(journal.createdCommands()).singleElement().satisfies(reused -> {
            assertThat(reused.slipNo()).isEqualTo(original.slipNo());
            assertThat(reused.lineageSourceId()).isEqualTo(original.lineageSourceId());
        });
    }

    @Test
    void missingSnapshotLineageFailsClosed() {
        StatefulJournal journal = journalWithPostedRevenue("1000.00");
        AnnualClosingService service = service(journal);
        service.performIncomeStatementClosing(YEAR, RETAINED_EARNINGS_ACCOUNT);
        journal.createdSummary(0).setLineageSourceId(null);

        assertThatThrownBy(() -> service.performIncomeStatementClosing(YEAR, RETAINED_EARNINGS_ACCOUNT))
                .isInstanceOf(IllegalStateException.class);
        assertThat(journal.createdCommands()).hasSize(1);
    }

    @Test
    void malformedSnapshotLineageFailsClosed() {
        StatefulJournal journal = journalWithPostedRevenue("1000.00");
        AnnualClosingService service = service(journal);
        service.performIncomeStatementClosing(YEAR, RETAINED_EARNINGS_ACCOUNT);
        journal.createdSummary(0).setLineageSourceId("not-a-source-snapshot");

        assertThatThrownBy(() -> service.performIncomeStatementClosing(YEAR, RETAINED_EARNINGS_ACCOUNT))
                .isInstanceOf(IllegalStateException.class);
        assertThat(journal.createdCommands()).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"APPROVED", "REJECTED", "REVERSED", "UNKNOWN"})
    void unsupportedAnnualClosingStatusFailsClosed(String status) {
        StatefulJournal journal = journalWithPostedRevenue("1000.00");
        AnnualClosingService service = service(journal);
        service.performIncomeStatementClosing(YEAR, RETAINED_EARNINGS_ACCOUNT);
        journal.createdSummary(0).setStatus(status);

        assertThatThrownBy(() -> service.performIncomeStatementClosing(YEAR, RETAINED_EARNINGS_ACCOUNT))
                .isInstanceOf(IllegalStateException.class);
        assertThat(journal.createdCommands()).hasSize(1);
    }

    @Test
    void alteredDraftHeaderFailsClosed() {
        StatefulJournal journal = journalWithPostedRevenue("1000.00");
        AnnualClosingService service = service(journal);
        service.performIncomeStatementClosing(YEAR, RETAINED_EARNINGS_ACCOUNT);
        journal.createdSummary(0).setCurrencyCode("USD");

        assertThatThrownBy(() -> service.performIncomeStatementClosing(YEAR, RETAINED_EARNINGS_ACCOUNT))
                .isInstanceOf(IllegalStateException.class);
        assertThat(journal.createdCommands()).hasSize(1);
    }

    @Test
    void unbalancedDraftLinesFailClosed() {
        StatefulJournal journal = journalWithPostedRevenue("1000.00");
        AnnualClosingService service = service(journal);
        service.performIncomeStatementClosing(YEAR, RETAINED_EARNINGS_ACCOUNT);
        JournalDetailSummary retainedEarnings = journal.createdDetails(0).get(1);
        retainedEarnings.setAmount(new BigDecimal("900.00"));
        retainedEarnings.setBaseAmount(new BigDecimal("900.00"));

        assertThatThrownBy(() -> service.performIncomeStatementClosing(YEAR, RETAINED_EARNINGS_ACCOUNT))
                .isInstanceOf(IllegalStateException.class);
        assertThat(journal.createdCommands()).hasSize(1);
    }

    @Test
    void balancedButAlteredDraftLineFailsClosed() {
        StatefulJournal journal = journalWithPostedRevenue("1000.00");
        AnnualClosingService service = service(journal);
        service.performIncomeStatementClosing(YEAR, RETAINED_EARNINGS_ACCOUNT);
        journal.createdDetails(0).get(0).setDetailDescription("altered after snapshot");

        assertThatThrownBy(() -> service.performIncomeStatementClosing(YEAR, RETAINED_EARNINGS_ACCOUNT))
                .isInstanceOf(IllegalStateException.class);
        assertThat(journal.createdCommands()).hasSize(1);
    }

    @Test
    void malformedPostedCloseFailsClosedBeforeDeltaWrite() {
        StatefulJournal journal = journalWithPostedRevenue("1000.00");
        AnnualClosingService service = service(journal);
        service.performIncomeStatementClosing(YEAR, RETAINED_EARNINGS_ACCOUNT);
        journal.markCreatedEntryPosted(0);
        journal.createdDetails(0).get(1).setBaseAmount(new BigDecimal("900.00"));
        journal.addSource("POSTED", List.of(
                detail(REVENUE_ACCOUNT, "REVENUE", JournalSide.CREDIT, "500.00", "500.00")));

        assertThatThrownBy(() -> service.performIncomeStatementClosing(YEAR, RETAINED_EARNINGS_ACCOUNT))
                .isInstanceOf(IllegalStateException.class);
        assertThat(journal.createdCommands()).hasSize(1);
    }

    @Test
    void missingBaseAmountFailsClosed() {
        StatefulJournal journal = new StatefulJournal();
        JournalDetailSummary source = detail(
                REVENUE_ACCOUNT, "REVENUE", JournalSide.CREDIT, "1000.00", "1000.00");
        source.setBaseAmount(null);
        journal.addSource("POSTED", List.of(source));
        AnnualClosingService service = service(journal);

        assertThatThrownBy(() -> service.performIncomeStatementClosing(YEAR, RETAINED_EARNINGS_ACCOUNT))
                .isInstanceOfAny(IllegalStateException.class, NullPointerException.class);
        assertThat(journal.createdCommands()).isEmpty();
    }

    @Test
    void missingJournalCategoryUsesEffectiveDatedMasterCategory() {
        StatefulJournal journal = new StatefulJournal();
        JournalDetailSummary source = detail(
                REVENUE_ACCOUNT, null, JournalSide.CREDIT, "1000.00", "1000.00");
        journal.addSource("POSTED", List.of(source));
        journal.addMasterAccount(REVENUE_ACCOUNT, source.getAccountingDate(),
                account(REVENUE_ACCOUNT, "REVENUE"));

        service(journal).performIncomeStatementClosing(YEAR, RETAINED_EARNINGS_ACCOUNT);

        assertThat(journal.createdCommands()).singleElement().satisfies(command ->
                assertThat(command.lines()).extracting(line -> line.accountCode())
                        .containsExactly(REVENUE_ACCOUNT, RETAINED_EARNINGS_ACCOUNT));
        assertThat(journal.masterLookupCount(REVENUE_ACCOUNT, source.getAccountingDate())).isOne();
    }

    @Test
    void missingMasterCategoryLookupFailsClosed() {
        StatefulJournal journal = journalWithMissingSourceCategory();

        assertThatThrownBy(() -> service(journal)
                .performIncomeStatementClosing(YEAR, RETAINED_EARNINGS_ACCOUNT))
                .isInstanceOf(IllegalStateException.class);
        assertThat(journal.createdCommands()).isEmpty();
    }

    @Test
    void mismatchedMasterAccountIdentityFailsClosed() {
        StatefulJournal journal = journalWithMissingSourceCategory();
        JournalDetailSummary source = journal.sourceDetails(1L).get(0);
        journal.addMasterAccount(REVENUE_ACCOUNT, source.getAccountingDate(),
                account("42000", "REVENUE"));

        assertThatThrownBy(() -> service(journal)
                .performIncomeStatementClosing(YEAR, RETAINED_EARNINGS_ACCOUNT))
                .isInstanceOf(IllegalStateException.class);
        assertThat(journal.createdCommands()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "UNSUPPORTED_CATEGORY"})
    void blankOrUnsupportedMasterCategoryFailsClosed(String category) {
        StatefulJournal journal = journalWithMissingSourceCategory();
        JournalDetailSummary source = journal.sourceDetails(1L).get(0);
        journal.addMasterAccount(REVENUE_ACCOUNT, source.getAccountingDate(),
                account(REVENUE_ACCOUNT, category));

        assertThatThrownBy(() -> service(journal)
                .performIncomeStatementClosing(YEAR, RETAINED_EARNINGS_ACCOUNT))
                .isInstanceOf(IllegalStateException.class);
        assertThat(journal.createdCommands()).isEmpty();
    }

    @Test
    void repeatedAccountAndDateMasterLookupIsCachedOncePerRun() {
        StatefulJournal journal = new StatefulJournal();
        JournalDetailSummary credit = detail(
                REVENUE_ACCOUNT, null, JournalSide.CREDIT, "1000.00", "1000.00");
        JournalDetailSummary debit = detail(
                REVENUE_ACCOUNT, null, JournalSide.DEBIT, "200.00", "200.00");
        journal.addSource("POSTED", List.of(credit, debit));
        journal.addMasterAccount(REVENUE_ACCOUNT, credit.getAccountingDate(),
                account(REVENUE_ACCOUNT, "REVENUE"));
        journal.addMasterAccount(REVENUE_ACCOUNT, YEAR_END,
                account(REVENUE_ACCOUNT, "REVENUE"));
        AnnualClosingService service = service(journal);

        service.performIncomeStatementClosing(YEAR, RETAINED_EARNINGS_ACCOUNT);
        assertThat(journal.masterLookupCount(REVENUE_ACCOUNT, credit.getAccountingDate())).isOne();

        service.performIncomeStatementClosing(YEAR, RETAINED_EARNINGS_ACCOUNT);
        assertThat(journal.masterLookupCount(REVENUE_ACCOUNT, credit.getAccountingDate())).isEqualTo(2);
        assertThat(journal.createdCommands()).hasSize(1);
    }

    private static StatefulJournal journalWithMissingSourceCategory() {
        StatefulJournal journal = new StatefulJournal();
        journal.addSource("POSTED", List.of(
                detail(REVENUE_ACCOUNT, null, JournalSide.CREDIT, "1000.00", "1000.00")));
        return journal;
    }

    private static AccountSubjectRef account(String code, String category) {
        return new AccountSubjectRef(code, "Account " + code, false, false, "CREDIT", category);
    }

    private static AnnualClosingService service(StatefulJournal journal) {
        return new AnnualClosingService(journal, journal, journal);
    }

    private static StatefulJournal journalWithPostedRevenue(String amount) {
        StatefulJournal journal = new StatefulJournal();
        journal.addSource("POSTED", List.of(
                detail(REVENUE_ACCOUNT, "REVENUE", JournalSide.CREDIT, amount, amount)));
        return journal;
    }

    private static JournalDetailSummary detail(
            String accountCode,
            String category,
            JournalSide side,
            String amount,
            String baseAmount) {
        JournalDetailSummary detail = new JournalDetailSummary();
        detail.setAccountCode(accountCode);
        detail.setAccountCategory(category);
        detail.setSide(side);
        detail.setAmount(new BigDecimal(amount));
        detail.setBaseAmount(new BigDecimal(baseAmount));
        return detail;
    }

    private static final class StatefulJournal
            implements JournalQueryPort, JournalPostingPort, MasterDataQueryPort {

        private final Map<Long, JournalSummary> summaries = new LinkedHashMap<>();
        private final Map<Long, List<JournalDetailSummary>> details = new LinkedHashMap<>();
        private final Map<String, String> accountCategories = new LinkedHashMap<>();
        private final List<JournalEntryCommand> createdCommands = new ArrayList<>();
        private final List<Long> createdEntryIds = new ArrayList<>();
        private final List<Long> readDetailIds = new ArrayList<>();
        private final Map<AccountLookup, AccountSubjectRef> masterAccounts = new LinkedHashMap<>();
        private final Map<AccountLookup, Integer> masterLookupCounts = new LinkedHashMap<>();
        private long nextEntryId = 1L;
        private long nextDetailId = 1_000L;
        private boolean reverseProviderOrder;

        long addSource(String status, List<JournalDetailSummary> sourceDetails) {
            long id = nextEntryId++;
            JournalSummary summary = new JournalSummary();
            summary.setId(id);
            summary.setSlipNo("SRC-" + id);
            summary.setSlipDate(LocalDate.of(YEAR, 6, 30));
            summary.setAccountingDate(LocalDate.of(YEAR, 6, 30));
            summary.setDescription("Source " + id);
            summary.setEntryType("NORMAL");
            summary.setStatus(status);
            summary.setCurrencyCode("KRW");
            summaries.put(id, summary);
            List<JournalDetailSummary> stored = new ArrayList<>(sourceDetails);
            stored.forEach(detail -> {
                detail.setId(nextDetailId++);
                detail.setAccountingDate(summary.getAccountingDate());
                detail.setSlipNo(summary.getSlipNo());
                detail.setHeaderDescription(summary.getDescription());
                accountCategories.putIfAbsent(detail.getAccountCode(), detail.getAccountCategory());
            });
            details.put(id, stored);
            return id;
        }

        String addLegacyPostedAnnualClose(String amount) {
            long id = nextEntryId++;
            String slipNo = ClosingSlipNoFactory.annualClosing(
                    YEAR_END, YEAR, RETAINED_EARNINGS_ACCOUNT);
            JournalSummary summary = new JournalSummary();
            summary.setId(id);
            summary.setSlipNo(slipNo);
            summary.setSlipDate(YEAR_END);
            summary.setAccountingDate(YEAR_END);
            summary.setDescription(YEAR + "년 손익 대체 분개");
            summary.setEntryType("TRANSFER");
            summary.setStatus("POSTED");
            summary.setCurrencyCode("KRW");
            summary.setLineageSourceType("ANNUAL_CLOSING");
            summary.setLineageSourceId(String.valueOf(YEAR));
            summaries.put(id, summary);

            List<JournalDetailSummary> stored = new ArrayList<>(List.of(
                    detail(REVENUE_ACCOUNT, "REVENUE", JournalSide.DEBIT, amount, amount),
                    detail(RETAINED_EARNINGS_ACCOUNT, "EQUITY", JournalSide.CREDIT, amount, amount)));
            stored.get(0).setDetailDescription(YEAR + "년 손익 대체");
            stored.get(1).setDetailDescription(YEAR + "년 이익잉여금 대체");
            stored.forEach(detail -> {
                detail.setId(nextDetailId++);
                detail.setAccountingDate(YEAR_END);
                detail.setSlipNo(slipNo);
                detail.setHeaderDescription(summary.getDescription());
            });
            details.put(id, stored);
            return slipNo;
        }

        List<JournalDetailSummary> sourceDetails(long id) {
            return details.get(id);
        }

        List<JournalEntryCommand> createdCommands() {
            return createdCommands;
        }

        long createdEntryId(int index) {
            return createdEntryIds.get(index);
        }

        JournalSummary createdSummary(int index) {
            return summaries.get(createdEntryId(index));
        }

        List<JournalDetailSummary> createdDetails(int index) {
            return details.get(createdEntryId(index));
        }

        void markCreatedEntryPosted(int index) {
            createdSummary(index).setStatus("POSTED");
        }

        List<Long> readDetailIds() {
            return readDetailIds;
        }

        void clearReadDetailIds() {
            readDetailIds.clear();
        }

        void reverseProviderOrder() {
            reverseProviderOrder = true;
        }

        void addMasterAccount(String accountCode, LocalDate effectiveDate, AccountSubjectRef account) {
            masterAccounts.put(new AccountLookup(accountCode, effectiveDate), account);
        }

        int masterLookupCount(String accountCode, LocalDate effectiveDate) {
            return masterLookupCounts.getOrDefault(new AccountLookup(accountCode, effectiveDate), 0);
        }

        @Override
        public List<JournalSummary> getJournalSummaries(LocalDate startDate, LocalDate endDate) {
            List<JournalSummary> result = new ArrayList<>(summaries.values().stream()
                    .filter(summary -> !summary.getAccountingDate().isBefore(startDate))
                    .filter(summary -> !summary.getAccountingDate().isAfter(endDate))
                    .toList());
            if (reverseProviderOrder) {
                java.util.Collections.reverse(result);
            }
            return result;
        }

        @Override
        public List<JournalDetailSummary> getJournalDetails(Long journalEntryId) {
            readDetailIds.add(journalEntryId);
            List<JournalDetailSummary> result = details.get(journalEntryId);
            if (result == null) {
                throw new IllegalStateException("Missing fixture details for journal " + journalEntryId);
            }
            if (!reverseProviderOrder) {
                return result;
            }
            List<JournalDetailSummary> reordered = new ArrayList<>(result);
            java.util.Collections.reverse(reordered);
            return reordered;
        }

        @Override
        public JournalPostingResult createDraftEntry(JournalEntryCommand command) {
            long id = nextEntryId++;
            createdCommands.add(command);
            createdEntryIds.add(id);
            JournalSummary summary = new JournalSummary();
            summary.setId(id);
            summary.setSlipNo(command.slipNo());
            summary.setSlipDate(command.slipDate());
            summary.setAccountingDate(command.accountingDate());
            summary.setDescription(command.description());
            summary.setEntryType(command.entryType());
            summary.setStatus("DRAFT");
            summary.setCurrencyCode(command.currencyCode());
            summary.setLineageSourceType(command.lineageSourceType());
            summary.setLineageSourceId(command.lineageSourceId());
            summaries.put(id, summary);
            List<JournalDetailSummary> stored = command.lines().stream()
                    .map(line -> {
                        JournalDetailSummary detail = new JournalDetailSummary();
                        detail.setId(nextDetailId++);
                        detail.setSide(JournalSide.valueOf(line.drcrType()));
                        detail.setAccountCode(line.accountCode());
                        detail.setAccountCategory(accountCategories.getOrDefault(
                                line.accountCode(), "EQUITY"));
                        detail.setAmount(line.amount());
                        detail.setBaseAmount(line.baseAmount());
                        detail.setDepartmentCode(line.departmentCode());
                        detail.setBusinessPartnerCode(line.businessPartnerCode());
                        detail.setDetailDescription(line.detailDescription());
                        detail.setAccountingDate(command.accountingDate());
                        detail.setSlipNo(command.slipNo());
                        detail.setHeaderDescription(command.description());
                        return detail;
                    })
                    .toList();
            details.put(id, new ArrayList<>(stored));
            return new JournalPostingResult(id, command.slipNo(), "DRAFT");
        }

        @Override
        public void approveAndPost(Long journalEntryId, String actor) {
            throw new UnsupportedOperationException("Not used by annual closing tests");
        }

        @Override
        public List<JournalDetailSummary> getJournalDetailsByAccountCodes(
                LocalDate startDate, LocalDate endDate, List<String> accountCodes) {
            throw new UnsupportedOperationException("Not used by annual closing tests");
        }

        @Override
        public JournalDetailAggregateSummary getJournalDetailAggregate(
                LocalDate startDate, LocalDate endDate, JournalSide side) {
            throw new UnsupportedOperationException("Not used by annual closing tests");
        }

        @Override
        public JournalDetailAggregateSummary getJournalDetailAggregateByAccount(
                LocalDate startDate, LocalDate endDate, JournalSide side, String accountCode) {
            throw new UnsupportedOperationException("Not used by annual closing tests");
        }

        @Override
        public JournalSummary getJournalSummary(Long journalEntryId) {
            return summaries.get(journalEntryId);
        }

        @Override
        public Optional<JournalSummary> findBySlipNo(String slipNo) {
            return summaries.values().stream()
                    .filter(summary -> slipNo.equals(summary.getSlipNo()))
                    .findFirst();
        }

        @Override
        public Optional<AccountSubjectRef> findAccountSubject(String accountCode) {
            return Optional.empty();
        }

        @Override
        public Optional<AccountSubjectRef> findAccountSubjectAt(
                String accountCode, LocalDate effectiveDate) {
            AccountLookup lookup = new AccountLookup(accountCode, effectiveDate);
            masterLookupCounts.merge(lookup, 1, Integer::sum);
            return Optional.ofNullable(masterAccounts.get(lookup));
        }

        @Override
        public Optional<BusinessPartnerRef> findBusinessPartner(String businessPartnerCode) {
            return Optional.empty();
        }

        @Override
        public Optional<DepartmentRef> findDepartment(String departmentCode) {
            return Optional.empty();
        }

        private record AccountLookup(String accountCode, LocalDate effectiveDate) {
        }
    }
}

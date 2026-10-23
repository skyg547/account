package com.ho.account.closing.application.service;

import com.ho.account.closing.application.port.out.RetainedEarningsMappingPort;
import com.ho.account.closing.domain.ApprovedRetainedEarningsMapping;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalQueryPort;
import com.ho.account.closing.application.port.out.AnnualJournalReadPort;
import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AnnualClosingDestinationControlRegressionTest {

    private static final int YEAR = 2026;
    private static final LocalDate YEAR_END = LocalDate.of(YEAR, 12, 31);
    private static final String RETAINED_EARNINGS_ACCOUNT = "35000";

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidDestinations")
    void rejectsInvalidDatedDestinationBeforeAnyJournalReadOrDraft(
            String scenario,
            Optional<AccountSubjectRef> datedAccount) {
        JournalQueryPort journalQuery = mock(JournalQueryPort.class);
        AnnualJournalReadPort annualRead = mock(AnnualJournalReadPort.class);
        JournalPostingPort journalPosting = mock(JournalPostingPort.class);
        MasterDataQueryPort masterData = mock(MasterDataQueryPort.class);
        RetainedEarningsMappingPort mappings = mock(RetainedEarningsMappingPort.class);
        ApprovedRetainedEarningsMapping mapping = approvedMapping();
        when(mappings.requireForYear(YEAR)).thenReturn(mapping);
        when(masterData.findAccountSubjectAt(RETAINED_EARNINGS_ACCOUNT, YEAR_END))
                .thenReturn(datedAccount);
        AnnualClosingService service = new AnnualClosingService(
                journalPosting, masterData, mappings, annualRead);

        assertThatThrownBy(() -> service.performIncomeStatementClosing(YEAR))
                .as(scenario)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("retained earnings");

        verify(mappings).requireForYear(YEAR);
        verify(masterData).findAccountSubjectAt(RETAINED_EARNINGS_ACCOUNT, YEAR_END);
        verifyNoInteractions(journalQuery, journalPosting, annualRead);
    }

    private static Stream<Arguments> invalidDestinations() {
        return Stream.of(
                destination("ASSETS category", RETAINED_EARNINGS_ACCOUNT, "CREDIT", "ASSETS"),
                destination("LIABILITIES category", RETAINED_EARNINGS_ACCOUNT, "CREDIT", "LIABILITIES"),
                destination("REVENUE category", RETAINED_EARNINGS_ACCOUNT, "CREDIT", "REVENUE"),
                destination("EXPENSES category", RETAINED_EARNINGS_ACCOUNT, "DEBIT", "EXPENSES"),
                destination("unknown category", RETAINED_EARNINGS_ACCOUNT, "CREDIT", "UNKNOWN"),
                Arguments.of("missing dated account", Optional.empty()),
                destination("mismatched account code", "99999", "CREDIT", "EQUITY"),
                destination("blank category", RETAINED_EARNINGS_ACCOUNT, "CREDIT", " "),
                destination("debit-normal EQUITY", RETAINED_EARNINGS_ACCOUNT, "DEBIT", "EQUITY"));
    }

    private static Arguments destination(
            String scenario,
            String accountCode,
            String normalBalanceSide,
            String category) {
        return Arguments.of(scenario, Optional.of(new AccountSubjectRef(
                accountCode,
                "Destination account",
                false,
                false,
                normalBalanceSide,
                category)));
    }

    private static ApprovedRetainedEarningsMapping approvedMapping() {
        return new ApprovedRetainedEarningsMapping(
                "ENTITY-01",
                YEAR,
                RETAINED_EARNINGS_ACCOUNT,
                true,
                "closing-controller",
                "CHG-776");
    }
}

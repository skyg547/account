package com.ho.account.closing.application.service;

import com.ho.account.closing.application.port.in.AnnualClosingUseCase;
import com.ho.account.contracts.journal.JournalDetailSummary;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalQueryPort;
import com.ho.account.contracts.journal.JournalSide;
import com.ho.account.contracts.journal.JournalSummary;
import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 연차 결산(Annual Closing) 관련 비즈니스 로직을 처리하는 서비스입니다.
 */
@Service
@Transactional
public class AnnualClosingService implements AnnualClosingUseCase {

    private static final String ANNUAL_LINEAGE_TYPE = "ANNUAL_CLOSING";
    private static final String ENTRY_TYPE = "TRANSFER";
    private static final String CURRENCY_CODE = "KRW";
    private static final String DRAFT = "DRAFT";
    private static final String POSTED = "POSTED";
    private static final String REVENUE = "REVENUE";
    private static final String EXPENSES = "EXPENSES";
    private static final String EQUITY = "EQUITY";
    private static final Set<String> ACCOUNT_CATEGORIES = Set.of(
            "ASSETS", "LIABILITIES", EQUITY, REVENUE, EXPENSES,
            "NON_OPERATING_INCOME", "NON_OPERATING_EXPENSES");
    private static final Pattern SNAPSHOT_HASH = Pattern.compile("[0-9A-F]{64}");

    private final JournalQueryPort journalQueryPort;
    private final JournalPostingPort journalPostingPort;
    private final MasterDataQueryPort masterDataQueryPort;

    @Autowired
    public AnnualClosingService(
            JournalQueryPort journalQueryPort,
            JournalPostingPort journalPostingPort,
            MasterDataQueryPort masterDataQueryPort) {
        this.journalQueryPort = Objects.requireNonNull(journalQueryPort, "journalQueryPort must not be null");
        this.journalPostingPort = Objects.requireNonNull(journalPostingPort, "journalPostingPort must not be null");
        this.masterDataQueryPort = Objects.requireNonNull(
                masterDataQueryPort, "masterDataQueryPort must not be null");
    }

    /**
     * 최신 전기 원천을 동결 식별한 뒤, 이미 전기된 연차 결산과의 잔여분만 임시 전표로 생성합니다.
     * 재시도는 헤더가 아니라 스냅샷 lineage와 전체 라인 내용이 모두 같을 때만 멱등합니다.
     */
    @Override
    public void performIncomeStatementClosing(int year, String retainedEarningsAccountCode) {
        validateYear(year);
        if (retainedEarningsAccountCode == null || retainedEarningsAccountCode.isBlank()) {
            throw new IllegalArgumentException("retainedEarningsAccountCode must not be blank");
        }
        String retainedAccount = retainedEarningsAccountCode.trim();
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        List<JournalSummary> summaries = journalQueryPort.getJournalSummaries(startDate, endDate);
        if (summaries == null) {
            throw invalid("journal summaries must not be null");
        }
        summaries.forEach(this::validateSummaryIdentity);
        List<JournalSummary> annualSummaries = summaries.stream()
                .filter(this::isAnnualCandidate)
                .toList();
        List<JournalSummary> sourceSummaries = summaries.stream()
                .filter(summary -> !isAnnualCandidate(summary))
                .filter(summary -> POSTED.equals(summary == null ? null : summary.getStatus()))
                .toList();
        Map<AccountLookupKey, String> accountCategoryCache = new HashMap<>();

        // @todo Replace the per-entry N+1 query with a posted base-currency aggregate/snapshot port.
        // Completion requires provider-side stable pagination, source identity projection, prior annual
        // exclusion, and a 100M-row PostgreSQL plan/load test without weakening this fail-closed check.
        SourceSnapshot source = loadSourceSnapshot(
                sourceSummaries, startDate, endDate, year, retainedAccount, accountCategoryCache);
        List<AnnualEntry> annualEntries = loadAnnualEntries(
                annualSummaries, endDate, year, retainedAccount, source.incomeCategories(),
                accountCategoryCache);

        Map<String, BigDecimal> postedSignedBalances = aggregatePostedBalances(annualEntries);
        Map<String, BigDecimal> residual = subtract(
                source.requiredSignedBalances(), postedSignedBalances);
        String currentLineageId = annualLineageId(year, retainedAccount, source.identity());
        String currentSlipNo = ClosingSlipNoFactory.annualClosing(
                endDate, year, retainedAccount, source.identity());

        List<AnnualEntry> drafts = annualEntries.stream()
                .filter(entry -> DRAFT.equals(entry.summary().getStatus()))
                .toList();
        if (drafts.size() > 1) {
            throw invalid("multiple pending annual closing drafts exist");
        }
        if (!drafts.isEmpty()) {
            AnnualEntry draft = drafts.get(0);
            if (!currentLineageId.equals(draft.summary().getLineageSourceId())
                    || !currentSlipNo.equals(draft.summary().getSlipNo())
                    || !sameBalances(residual, draft.signedBalances())) {
                throw invalid("a stale or content-mismatched annual closing draft exists");
            }
            return;
        }

        if (residual.isEmpty()) {
            return;
        }
        boolean currentSnapshotAlreadyPosted = annualEntries.stream()
                .filter(entry -> POSTED.equals(entry.summary().getStatus()))
                .anyMatch(entry -> currentLineageId.equals(entry.summary().getLineageSourceId()));
        if (currentSnapshotAlreadyPosted) {
            throw invalid("the current snapshot is posted but its closing is incomplete");
        }

        List<JournalLineCommand> lines = createResidualLines(
                residual, source.incomeCategories(), retainedAccount, year);
        JournalEntryCommand command = new JournalEntryCommand(
                endDate,
                endDate,
                annualDescription(year),
                ENTRY_TYPE,
                CURRENCY_CODE,
                BigDecimal.ONE,
                "SYSTEM",
                "SYSTEM",
                ANNUAL_LINEAGE_TYPE,
                currentLineageId,
                currentSlipNo,
                lines);
        journalPostingPort.createDraftEntry(command);
    }

    private SourceSnapshot loadSourceSnapshot(
            List<JournalSummary> summaries,
            LocalDate startDate,
            LocalDate endDate,
            int year,
            String retainedAccount,
            Map<AccountLookupKey, String> accountCategoryCache) {
        Set<Long> summaryIds = new HashSet<>();
        Set<String> slipNumbers = new HashSet<>();
        List<String> canonicalEntries = new ArrayList<>();
        Map<String, BigDecimal> sourceSignedBalances = new HashMap<>();
        Map<String, String> incomeCategories = new HashMap<>();

        for (JournalSummary summary : summaries) {
            validatePostedSourceSummary(summary, startDate, endDate);
            if (!summaryIds.add(summary.getId()) || !slipNumbers.add(summary.getSlipNo())) {
                throw invalid("duplicate posted source journal identity");
            }
            List<JournalDetailSummary> details = requireDetails(summary);
            Set<Long> detailIds = new HashSet<>();
            List<String> canonicalDetails = new ArrayList<>();
            for (JournalDetailSummary detail : details) {
                validateSourceDetail(summary, detail, detailIds);
                String category = resolveAccountCategory(detail, accountCategoryCache);
                if (REVENUE.equals(category) || EXPENSES.equals(category)) {
                    String accountCode = detail.getAccountCode().trim();
                    incomeCategories.merge(accountCode, category, (left, right) -> {
                        if (!left.equals(right)) {
                            throw invalid("an income statement account has conflicting classifications: "
                                    + accountCode);
                        }
                        return left;
                    });
                    sourceSignedBalances.merge(
                            accountCode, signed(detail.getSide(), detail.getBaseAmount()), BigDecimal::add);
                }
                canonicalDetails.add(canonicalSourceDetail(detail, category));
            }
            canonicalDetails.sort(Comparator.naturalOrder());
            canonicalEntries.add(canonicalSourceEntry(summary, canonicalDetails));
        }

        if (incomeCategories.containsKey(retainedAccount)) {
            throw invalid("retained earnings account must not be an income statement account");
        }
        canonicalEntries.sort(Comparator.naturalOrder());
        String identity = sha256(canonical(
                "ANNUAL_SOURCE_SNAPSHOT_V1",
                Integer.toString(year),
                retainedAccount,
                String.join("", canonicalEntries)));
        return new SourceSnapshot(
                identity,
                requiredAnnualBalances(sourceSignedBalances, retainedAccount),
                Map.copyOf(incomeCategories));
    }

    private List<AnnualEntry> loadAnnualEntries(
            List<JournalSummary> summaries,
            LocalDate endDate,
            int year,
            String retainedAccount,
            Map<String, String> incomeCategories,
            Map<AccountLookupKey, String> accountCategoryCache) {
        Set<Long> ids = new HashSet<>();
        Set<String> slipNumbers = new HashSet<>();
        Set<String> lineageIds = new HashSet<>();
        List<AnnualEntry> entries = new ArrayList<>();
        for (JournalSummary summary : summaries) {
            validateAnnualHeader(summary, endDate, year, retainedAccount);
            if (!ids.add(summary.getId()) || !slipNumbers.add(summary.getSlipNo())
                    || !lineageIds.add(summary.getLineageSourceId())) {
                throw invalid("duplicate annual closing identity");
            }
            if (!(DRAFT.equals(summary.getStatus()) || POSTED.equals(summary.getStatus()))) {
                throw invalid("annual closing status is not reusable: " + summary.getStatus());
            }
            List<JournalDetailSummary> details = requireDetails(summary);
            Map<String, BigDecimal> signedBalances = validateAnnualDetails(
                    summary, details, endDate, year, retainedAccount, incomeCategories,
                    accountCategoryCache);
            entries.add(new AnnualEntry(summary, Map.copyOf(signedBalances)));
        }
        return List.copyOf(entries);
    }

    private Map<String, BigDecimal> validateAnnualDetails(
            JournalSummary summary,
            List<JournalDetailSummary> details,
            LocalDate endDate,
            int year,
            String retainedAccount,
            Map<String, String> incomeCategories,
            Map<AccountLookupKey, String> accountCategoryCache) {
        if (details.size() < 2) {
            throw invalid("annual closing must contain at least two lines");
        }
        Set<Long> detailIds = new HashSet<>();
        Set<String> accountCodes = new HashSet<>();
        Map<String, BigDecimal> signedBalances = new LinkedHashMap<>();
        BigDecimal amountBalance = BigDecimal.ZERO;
        BigDecimal baseAmountBalance = BigDecimal.ZERO;

        for (JournalDetailSummary detail : details) {
            validateCommonDetail(summary, detail, detailIds);
            String accountCode = detail.getAccountCode().trim();
            if (!accountCodes.add(accountCode)) {
                throw invalid("annual closing contains duplicate account lines");
            }
            if (!endDate.equals(detail.getAccountingDate())) {
                throw invalid("annual closing line accounting date differs from its header");
            }
            if (detail.getAmount().compareTo(detail.getBaseAmount()) != 0) {
                throw invalid("KRW annual closing line amount differs from baseAmount");
            }
            if (detail.getDepartmentCode() != null || detail.getBusinessPartnerCode() != null) {
                throw invalid("annual closing lines must not contain department or partner dimensions");
            }
            if (detail.getAccountNo() != null) {
                throw invalid("annual closing lines must not contain an account number dimension");
            }
            String category = resolveAccountCategory(detail, accountCategoryCache);
            String expectedDescription;
            if (retainedAccount.equals(accountCode)) {
                if (!EQUITY.equals(category)) {
                    throw invalid("retained earnings annual line must be classified as EQUITY");
                }
                expectedDescription = retainedDescription(year);
            } else {
                String sourceCategory = incomeCategories.get(accountCode);
                if (sourceCategory == null || !sourceCategory.equals(category)) {
                    throw invalid("annual closing line is not backed by the current source classification: "
                            + accountCode);
                }
                expectedDescription = incomeStatementLineDescription(year);
            }
            if (!expectedDescription.equals(detail.getDetailDescription())) {
                throw invalid("annual closing line description is invalid");
            }

            BigDecimal signedAmount = signed(detail.getSide(), detail.getAmount());
            BigDecimal signedBaseAmount = signed(detail.getSide(), detail.getBaseAmount());
            amountBalance = amountBalance.add(signedAmount);
            baseAmountBalance = baseAmountBalance.add(signedBaseAmount);
            signedBalances.put(accountCode, signedBaseAmount);
        }
        if (amountBalance.compareTo(BigDecimal.ZERO) != 0
                || baseAmountBalance.compareTo(BigDecimal.ZERO) != 0) {
            throw invalid("annual closing lines are unbalanced");
        }
        return removeZeroBalances(signedBalances);
    }

    private void validatePostedSourceSummary(
            JournalSummary summary,
            LocalDate startDate,
            LocalDate endDate) {
        validateSummaryIdentity(summary);
        if (!POSTED.equals(summary.getStatus())) {
            throw invalid("source journal is not POSTED");
        }
        if (summary.getAccountingDate().isBefore(startDate)
                || summary.getAccountingDate().isAfter(endDate)) {
            throw invalid("posted source journal is outside the closing year");
        }
        requireText(summary.getDescription(), "source journal description");
        requireText(summary.getEntryType(), "source journal entryType");
        requireText(summary.getCurrencyCode(), "source journal currencyCode");
        requireLineagePair(summary.getLineageSourceType(), summary.getLineageSourceId());
    }

    private void validateAnnualHeader(
            JournalSummary summary,
            LocalDate endDate,
            int year,
            String retainedAccount) {
        validateSummaryIdentity(summary);
        if (!endDate.equals(summary.getSlipDate()) || !endDate.equals(summary.getAccountingDate())
                || !annualDescription(year).equals(summary.getDescription())
                || !ENTRY_TYPE.equals(summary.getEntryType())
                || !CURRENCY_CODE.equals(summary.getCurrencyCode())
                || !ANNUAL_LINEAGE_TYPE.equals(summary.getLineageSourceType())) {
            throw invalid("annual closing header is malformed");
        }

        String lineageId = requireText(summary.getLineageSourceId(), "annual lineageSourceId");
        String expectedSlipNo;
        if (Integer.toString(year).equals(lineageId)) {
            expectedSlipNo = ClosingSlipNoFactory.annualClosing(endDate, year, retainedAccount);
            if (!POSTED.equals(summary.getStatus())) {
                throw invalid("legacy annual closing lineage is accepted only when POSTED");
            }
        } else {
            String prefix = year + "|" + retainedIdentity(retainedAccount) + "|";
            if (!lineageId.startsWith(prefix)) {
                throw invalid("annual closing lineage does not match year and retained earnings account");
            }
            String snapshotIdentity = lineageId.substring(prefix.length());
            if (!SNAPSHOT_HASH.matcher(snapshotIdentity).matches()) {
                throw invalid("annual closing lineage has an invalid snapshot identity");
            }
            expectedSlipNo = ClosingSlipNoFactory.annualClosing(
                    endDate, year, retainedAccount, snapshotIdentity);
        }
        if (!expectedSlipNo.equals(summary.getSlipNo())) {
            throw invalid("annual closing slip number does not match its lineage");
        }
    }

    private void validateSourceDetail(
            JournalSummary summary,
            JournalDetailSummary detail,
            Set<Long> detailIds) {
        validateCommonDetail(summary, detail, detailIds);
        if (!summary.getAccountingDate().equals(detail.getAccountingDate())) {
            throw invalid("source journal line accounting date differs from its header");
        }
    }

    private void validateCommonDetail(
            JournalSummary summary,
            JournalDetailSummary detail,
            Set<Long> detailIds) {
        if (detail == null || detail.getId() == null || detail.getId() < 1
                || !detailIds.add(detail.getId())) {
            throw invalid("journal detail identity is missing or duplicated");
        }
        requireText(detail.getAccountCode(), "journal detail accountCode");
        if (detail.getSide() == null || detail.getAmount() == null || detail.getBaseAmount() == null
                || detail.getAmount().signum() <= 0 || detail.getBaseAmount().signum() <= 0
                || detail.getAccountingDate() == null
                || !summary.getSlipNo().equals(detail.getSlipNo())
                || !summary.getDescription().equals(detail.getHeaderDescription())) {
            throw invalid("journal detail financial content or header lineage is invalid");
        }
    }

    private void validateSummaryIdentity(JournalSummary summary) {
        if (summary == null || summary.getId() == null || summary.getId() < 1
                || summary.getSlipDate() == null || summary.getAccountingDate() == null) {
            throw invalid("journal summary identity or dates are missing");
        }
        requireText(summary.getSlipNo(), "journal summary slipNo");
        requireText(summary.getStatus(), "journal summary status");
    }

    private List<JournalDetailSummary> requireDetails(JournalSummary summary) {
        List<JournalDetailSummary> details = journalQueryPort.getJournalDetails(summary.getId());
        if (details == null || details.isEmpty()) {
            throw invalid("journal has no detail lines: " + summary.getSlipNo());
        }
        return details;
    }

    private Map<String, BigDecimal> aggregatePostedBalances(List<AnnualEntry> entries) {
        Map<String, BigDecimal> result = new HashMap<>();
        entries.stream()
                .filter(entry -> POSTED.equals(entry.summary().getStatus()))
                .forEach(entry -> entry.signedBalances().forEach(
                        (account, amount) -> result.merge(account, amount, BigDecimal::add)));
        return removeZeroBalances(result);
    }

    private Map<String, BigDecimal> requiredAnnualBalances(
            Map<String, BigDecimal> sourceSignedBalances,
            String retainedAccount) {
        Map<String, BigDecimal> result = new HashMap<>();
        BigDecimal sourceTotal = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : sourceSignedBalances.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.ZERO) != 0) {
                result.put(entry.getKey(), entry.getValue().negate());
                sourceTotal = sourceTotal.add(entry.getValue());
            }
        }
        if (sourceTotal.compareTo(BigDecimal.ZERO) != 0) {
            result.merge(retainedAccount, sourceTotal, BigDecimal::add);
        }
        return removeZeroBalances(result);
    }

    private Map<String, BigDecimal> subtract(
            Map<String, BigDecimal> required,
            Map<String, BigDecimal> posted) {
        Map<String, BigDecimal> result = new HashMap<>(required);
        posted.forEach((account, amount) -> result.merge(account, amount.negate(), BigDecimal::add));
        return removeZeroBalances(result);
    }

    private List<JournalLineCommand> createResidualLines(
            Map<String, BigDecimal> residual,
            Map<String, String> incomeCategories,
            String retainedAccount,
            int year) {
        List<String> accountCodes = residual.keySet().stream()
                .filter(account -> !retainedAccount.equals(account))
                .sorted()
                .toList();
        List<JournalLineCommand> lines = new ArrayList<>();
        for (String accountCode : accountCodes) {
            if (!incomeCategories.containsKey(accountCode)) {
                throw invalid("residual annual line lacks a source classification: " + accountCode);
            }
            lines.add(toLine(accountCode, residual.get(accountCode), incomeStatementLineDescription(year)));
        }
        BigDecimal retainedResidual = residual.get(retainedAccount);
        if (retainedResidual != null && retainedResidual.compareTo(BigDecimal.ZERO) != 0) {
            lines.add(toLine(retainedAccount, retainedResidual, retainedDescription(year)));
        }
        if (lines.size() < 2 || !isBalanced(lines)) {
            throw invalid("annual closing residual is incomplete or unbalanced");
        }
        return List.copyOf(lines);
    }

    private JournalLineCommand toLine(
            String accountCode,
            BigDecimal signedAmount,
            String description) {
        String side = signedAmount.signum() > 0
                ? JournalSide.DEBIT.name()
                : JournalSide.CREDIT.name();
        BigDecimal amount = signedAmount.abs();
        return new JournalLineCommand(
                side, accountCode, amount, amount, null, null, description);
    }

    private boolean isBalanced(List<JournalLineCommand> lines) {
        BigDecimal balance = lines.stream()
                .map(line -> signed(JournalSide.valueOf(line.drcrType()), line.baseAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return balance.compareTo(BigDecimal.ZERO) == 0;
    }

    private boolean sameBalances(
            Map<String, BigDecimal> left,
            Map<String, BigDecimal> right) {
        if (!left.keySet().equals(right.keySet())) {
            return false;
        }
        return left.entrySet().stream()
                .allMatch(entry -> entry.getValue().compareTo(right.get(entry.getKey())) == 0);
    }

    private Map<String, BigDecimal> removeZeroBalances(Map<String, BigDecimal> balances) {
        Map<String, BigDecimal> result = new HashMap<>();
        balances.forEach((account, amount) -> {
            if (amount != null && amount.compareTo(BigDecimal.ZERO) != 0) {
                result.put(account, amount);
            }
        });
        return result;
    }

    private String canonicalSourceEntry(JournalSummary summary, List<String> details) {
        return canonical(
                "SUMMARY",
                summary.getId().toString(),
                summary.getSlipNo(),
                summary.getSlipDate().toString(),
                summary.getAccountingDate().toString(),
                summary.getDescription(),
                summary.getStatus(),
                summary.getEntryType(),
                summary.getCurrencyCode(),
                nullableTrimmed(summary.getLineageSourceType()),
                nullableTrimmed(summary.getLineageSourceId()),
                String.join("", details));
    }

    private String canonicalSourceDetail(JournalDetailSummary detail, String accountCategory) {
        return canonical(
                "DETAIL",
                detail.getId().toString(),
                detail.getAccountCode().trim(),
                accountCategory,
                detail.getSide().name(),
                decimal(detail.getAmount()),
                decimal(detail.getBaseAmount()),
                detail.getAccountingDate().toString(),
                detail.getSlipNo(),
                detail.getHeaderDescription(),
                detail.getDetailDescription(),
                detail.getDepartmentCode(),
                detail.getBusinessPartnerCode(),
                detail.getAccountNo());
    }

    private String resolveAccountCategory(
            JournalDetailSummary detail,
            Map<AccountLookupKey, String> accountCategoryCache) {
        String suppliedCategory = detail.getAccountCategory();
        String category;
        if (suppliedCategory != null && !suppliedCategory.isBlank()) {
            category = suppliedCategory.trim();
        } else {
            String accountCode = detail.getAccountCode().trim();
            AccountLookupKey key = new AccountLookupKey(accountCode, detail.getAccountingDate());
            category = accountCategoryCache.computeIfAbsent(
                    key, ignored -> lookupAccountCategory(accountCode, detail.getAccountingDate()));
        }
        if (!ACCOUNT_CATEGORIES.contains(category)) {
            throw invalid("journal detail accountCategory is unsupported: " + category);
        }
        return category;
    }

    private String lookupAccountCategory(String accountCode, LocalDate accountingDate) {
        AccountSubjectRef account = masterDataQueryPort.findAccountSubjectAt(accountCode, accountingDate)
                .orElseThrow(() -> invalid(
                        "account classification is missing for " + accountCode + " at " + accountingDate));
        if (account.code() == null || !accountCode.equals(account.code().trim())) {
            throw invalid("master-data returned a different or invalid account code");
        }
        String category = account.accountCategory();
        if (category == null || category.isBlank()) {
            throw invalid("master-data returned an account without classification: " + accountCode);
        }
        return category.trim();
    }

    private String canonical(String... values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (value == null) {
                builder.append("-1:");
            } else {
                builder.append(value.length()).append(':').append(value);
            }
        }
        return builder.toString();
    }

    private String annualLineageId(int year, String retainedAccount, String snapshotIdentity) {
        // The retained account participates in both hashes, keeping the lineage under VARCHAR(100)
        // even when an account code reaches its full supported length.
        return year + "|" + retainedIdentity(retainedAccount) + "|" + snapshotIdentity;
    }

    private String retainedIdentity(String retainedAccount) {
        return sha256(canonical("ANNUAL_RETAINED_V1", retainedAccount)).substring(0, 16);
    }

    private boolean isAnnualCandidate(JournalSummary summary) {
        if (summary == null) {
            return false;
        }
        return ANNUAL_LINEAGE_TYPE.equals(summary.getLineageSourceType())
                || (summary.getSlipNo() != null && summary.getSlipNo().startsWith("ACL"));
    }

    private BigDecimal signed(JournalSide side, BigDecimal amount) {
        return side == JournalSide.DEBIT ? amount : amount.negate();
    }

    private String decimal(BigDecimal value) {
        BigDecimal normalized = value.stripTrailingZeros();
        return normalized.signum() == 0 ? "0" : normalized.toPlainString();
    }

    private String nullableTrimmed(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void requireLineagePair(String sourceType, String sourceId) {
        boolean typePresent = sourceType != null && !sourceType.isBlank();
        boolean idPresent = sourceId != null && !sourceId.isBlank();
        if (typePresent != idPresent) {
            throw invalid("source journal lineage type and ID must be supplied together");
        }
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).toUpperCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private void validateYear(int year) {
        if (year < 1900 || year > 9999) {
            throw new IllegalArgumentException("year must be between 1900 and 9999");
        }
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw invalid(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private IllegalStateException invalid(String message) {
        return new IllegalStateException("Annual closing validation failed: " + message);
    }

    private String annualDescription(int year) {
        return year + "년 손익 대체 분개";
    }

    private String incomeStatementLineDescription(int year) {
        return year + "년 손익 대체";
    }

    private String retainedDescription(int year) {
        return year + "년 이익잉여금 대체";
    }

    private record SourceSnapshot(
            String identity,
            Map<String, BigDecimal> requiredSignedBalances,
            Map<String, String> incomeCategories) {
    }

    private record AnnualEntry(
            JournalSummary summary,
            Map<String, BigDecimal> signedBalances) {
    }

    private record AccountLookupKey(String accountCode, LocalDate accountingDate) {
    }
}

package com.ho.account.journalledger.domain.journal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JournalEntryAggregateTest {

    @Test
    @DisplayName("대소문자와 공백이 다른 동일 canonical 작성자의 자기 승인을 거부한다.")
    void rejectsCanonicalSelfApproval() {
        JournalEntry entry = balancedDraft("Maker.One");
        entry.requestApproval(" maker.one ");

        assertThatThrownBy(() -> entry.approve("MAKER.ONE"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("작성자와 승인자");

        assertThat(entry.getStatus()).isEqualTo(JournalEntryStatus.REQUESTED);
        assertThat(entry.getApprovedBy()).isNull();
    }

    @Test
    @DisplayName("DRAFT를 승인 요청 없이 직접 승인할 수 없다.")
    void rejectsApprovalDirectlyFromDraft() {
        JournalEntry entry = balancedDraft("maker");

        assertThatThrownBy(() -> entry.approve("checker"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("승인 가능한 상태");
    }

    @Test
    @DisplayName("최초 저장한 maker identity를 다른 actor로 바꿀 수 없다.")
    void preventsMakerIdentityReplacement() {
        JournalEntry entry = balancedDraft("maker-one");

        assertThatThrownBy(() -> entry.setCreatedBy("maker-two"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("변경할 수 없습니다");

        assertThat(entry.getCreatedBy()).isEqualTo("maker-one");
    }

    @Test
    @DisplayName("별도 checker 승인 증거는 poster가 auditUser를 바꾼 뒤에도 보존된다.")
    void preservesApprovalEvidenceAfterPosting() {
        JournalEntry entry = balancedDraft(" Service:Closing-Maker ");
        entry.requestApproval("service:closing-maker");
        entry.approve(" Service:Closing-Checker ");
        entry.post(" Posting.Operator ");

        assertThat(entry.getStatus()).isEqualTo(JournalEntryStatus.POSTED);
        assertThat(entry.getCreatedBy()).isEqualTo("service:closing-maker");
        assertThat(entry.getApprovedBy()).isEqualTo("service:closing-checker");
        assertThat(entry.getAuditUser()).isEqualTo("posting.operator");
    }

    @Test
    @DisplayName("JournalEntry가 상세 라인의 소유권을 설정하고 외부 컬렉션 변경을 차단한다.")
    void ownsJournalDetailsAsAggregateChildren() {
        JournalEntry entry = new JournalEntry();
        JournalDetail debit = detail(JournalSide.DEBIT, "10100", "10.00", "10.00");
        JournalDetail credit = detail(JournalSide.CREDIT, "40100", "10.00", "10.00");

        entry.setDetails(List.of(debit, credit));

        assertThat(debit.getJournalEntry()).isSameAs(entry);
        assertThat(credit.getJournalEntry()).isSameAs(entry);
        assertThatThrownBy(() -> entry.getDetails().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("거래통화가 같아도 기준통화 차대가 다르면 승인을 차단한다.")
    void rejectsBaseCurrencyImbalance() {
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(java.time.LocalDate.now());
        entry.setCreatedBy("maker");
        entry.addDetail(detail(JournalSide.DEBIT, "10100", "100.00", "135000.00"));
        entry.addDetail(detail(JournalSide.CREDIT, "40100", "100.00", "134999.99"));

        assertThatThrownBy(entry::validateInvariants)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("기준통화");
    }

    @Test
    @DisplayName("차변 또는 대변 한쪽만 있는 목록은 합계가 우연히 0이어도 전표가 될 수 없다.")
    void requiresBothAccountingSides() {
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(java.time.LocalDate.now());
        entry.setCreatedBy("maker");
        entry.addDetail(detail(JournalSide.DEBIT, "10100", "10.00", "10.00"));
        entry.addDetail(detail(JournalSide.DEBIT, "10200", "10.00", "10.00"));

        assertThatThrownBy(entry::validateInvariants)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("차변과 대변");
    }

    @Test
    @DisplayName("전표 작성일(slipDate)이 누락되면 도메인 불변성 검증 시 예외가 발생한다.")
    void rejectsMissingSlipDateInvariants() {
        JournalEntry entry = new JournalEntry();
        entry.addDetail(detail(JournalSide.DEBIT, "10100", "10.00", "10.00"));
        entry.addDetail(detail(JournalSide.CREDIT, "40100", "10.00", "10.00"));

        assertThatThrownBy(entry::validateInvariants)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("전표 작성일");
    }

    @Test
    @DisplayName("사람·이벤트·기계 전표 모두 maker identity 없이는 생성할 수 없다.")
    void rejectsMissingMakerIdentity() {
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(LocalDate.of(2026, 9, 25));
        entry.addDetail(detail(JournalSide.DEBIT, "10100", "10.00", "10.00"));
        entry.addDetail(detail(JournalSide.CREDIT, "40100", "10.00", "10.00"));

        assertThatThrownBy(entry::validateInvariants)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("actor is required");
    }

    @Test
    @DisplayName("정상적인 헤더 및 차대변 라인이 설정된 전표는 불변성 검증을 통과한다.")
    void passesValidInvariants() {
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(java.time.LocalDate.now());
        entry.setCreatedBy("maker");
        entry.addDetail(detail(JournalSide.DEBIT, "10100", "10.00", "10.00"));
        entry.addDetail(detail(JournalSide.CREDIT, "40100", "10.00", "10.00"));

        entry.validateInvariants();

        assertThat(entry.getAccountingDate()).isEqualTo(entry.getSlipDate());
    }

    @Test
    @DisplayName("POSTED 전표의 모든 헤더 setter는 거부되고 기존 값을 보존한다.")
    void postedEntryRejectsEveryHeaderSetter() {
        JournalEntry entry = postedEntry();
        HeaderState before = HeaderState.capture(entry);

        List<Mutation> mutations = List.of(
                mutation("id", () -> entry.setId(999L)),
                mutation("slipNo", () -> entry.setSlipNo("JE-CHANGED")),
                mutation("slipDate", () -> entry.setSlipDate(entry.getSlipDate().plusDays(1))),
                mutation("accountingDate", () -> entry.setAccountingDate(entry.getAccountingDate().plusDays(1))),
                mutation("description", () -> entry.setDescription("changed")),
                mutation("entryType", () -> entry.setEntryType("CHANGED")),
                mutation("currencyCode", () -> entry.setCurrencyCode("USD")),
                mutation("exchangeRate", () -> entry.setExchangeRate(new BigDecimal("1350.12345678"))),
                mutation("rejectionReason", () -> entry.setRejectionReason("changed")),
                mutation("updatedAt", () -> entry.setUpdatedAt(LocalDateTime.of(2026, 9, 26, 1, 2))),
                mutation("createdBy", () -> entry.setCreatedBy("another-maker")),
                mutation("auditUser", () -> entry.setAuditUser("changed-auditor")),
                mutation("lineageSourceType", () -> entry.setLineageSourceType("CHANGED")),
                mutation("lineageSourceId", () -> entry.setLineageSourceId("CHANGED-1")));

        mutations.forEach(mutation -> assertPostedMutationRejected(mutation.name(), mutation.action()));

        assertThat(HeaderState.capture(entry)).isEqualTo(before);
    }

    @Test
    @DisplayName("POSTED 전표 라인의 모든 setter와 detach/reparent/attach를 거부한다.")
    void postedEntryRejectsEveryDetailSetterAndAssociationChange() {
        JournalEntry entry = postedEntry();
        JournalDetail detail = entry.getDetails().get(0);
        JournalEntry anotherDraft = balancedDraft("other-maker");
        DetailState before = DetailState.capture(detail);

        List<Mutation> mutations = List.of(
                mutation("id", () -> detail.setId(999L)),
                mutation("detach", () -> detail.setJournalEntry(null)),
                mutation("reparent", () -> detail.setJournalEntry(anotherDraft)),
                mutation("side", () -> detail.setSide(JournalSide.CREDIT)),
                mutation("accountCode", () -> detail.setAccountCode("99999")),
                mutation("amount", () -> detail.setAmount(new BigDecimal("11.00"))),
                mutation("baseAmount", () -> detail.setBaseAmount(new BigDecimal("11.00"))),
                mutation("departmentCode", () -> detail.setDepartmentCode("D-CHANGED")),
                mutation("businessPartnerCode", () -> detail.setBusinessPartnerCode("BP-CHANGED")),
                mutation("detailDescription", () -> detail.setDetailDescription("changed")),
                mutation("updatedAt", () -> detail.setUpdatedAt(LocalDateTime.of(2026, 9, 26, 1, 2))),
                mutation("auditUser", () -> detail.setAuditUser("changed-auditor")));

        mutations.forEach(mutation -> assertPostedMutationRejected(mutation.name(), mutation.action()));

        JournalDetail unattached = detail(JournalSide.DEBIT, "10200", "1.00", "1.00");
        assertPostedMutationRejected("attach", () -> unattached.setJournalEntry(entry));
        assertThat(unattached.getJournalEntry()).isNull();
        assertThat(DetailState.capture(detail)).isEqualTo(before);
    }

    @Test
    @DisplayName("POSTED 전표의 add/remove/clear/setDetails는 모두 거부되고 멤버십을 보존한다.")
    void postedEntryRejectsEveryMembershipMutation() {
        JournalEntry entry = postedEntry();
        List<JournalDetail> before = entry.getDetails();
        JournalDetail first = before.get(0);
        JournalDetail added = detail(JournalSide.DEBIT, "10200", "1.00", "1.00");

        assertPostedMutationRejected("addDetail", () -> entry.addDetail(added));
        assertPostedMutationRejected("removeDetail", () -> entry.removeDetail(first));
        assertPostedMutationRejected("clearDetails", entry::clearDetails);
        assertPostedMutationRejected("setDetails", () -> entry.setDetails(List.of(added)));

        assertThat(entry.getDetails()).containsExactlyElementsOf(before);
        assertThat(first.getJournalEntry()).isSameAs(entry);
        assertThat(added.getJournalEntry()).isNull();
    }

    @Test
    @DisplayName("DRAFT에서는 헤더, 라인, 소유 컬렉션을 정상적으로 편집할 수 있다.")
    void draftRemainsMutable() {
        JournalEntry draft = new JournalEntry();
        draft.initializeDraft();
        draft.setId(77L);
        draft.setSlipNo("JE-DRAFT-77");
        draft.setSlipDate(LocalDate.of(2026, 9, 25));
        draft.setAccountingDate(LocalDate.of(2026, 9, 26));
        draft.setDescription("draft description");
        draft.setEntryType("NORMAL");
        draft.setCurrencyCode("usd");
        draft.setExchangeRate(new BigDecimal("1350.12345678"));
        draft.setRejectionReason("draft reason");
        draft.setUpdatedAt(LocalDateTime.of(2026, 9, 25, 12, 0));
        draft.setCreatedBy("draft-maker");
        draft.setAuditUser("draft-auditor");
        draft.setLineageSourceType("SOURCE");
        draft.setLineageSourceId("SOURCE-77");

        JournalDetail debit = detail(JournalSide.DEBIT, "10100", "10.00", "10.00");
        debit.setId(701L);
        debit.setSide(JournalSide.DEBIT);
        debit.setAccountCode(" 10101 ");
        debit.setAmount(new BigDecimal("12.00"));
        debit.setBaseAmount(new BigDecimal("13.00"));
        debit.setDepartmentCode("D-1");
        debit.setBusinessPartnerCode("BP-1");
        debit.setDetailDescription("draft detail");
        debit.setUpdatedAt(LocalDateTime.of(2026, 9, 25, 12, 1));
        debit.setAuditUser("line-auditor");

        JournalDetail credit = detail(JournalSide.CREDIT, "40100", "12.00", "13.00");
        draft.addDetail(debit);
        draft.addDetail(credit);
        draft.removeDetail(credit);
        draft.setDetails(List.of(debit, credit));
        draft.clearDetails();
        draft.addDetail(debit);
        draft.addDetail(credit);

        assertThat(draft.getDescription()).isEqualTo("draft description");
        assertThat(draft.getCurrencyCode()).isEqualTo("USD");
        assertThat(debit.getAccountCode()).isEqualTo("10101");
        assertThat(debit.getJournalEntry()).isSameAs(draft);
        assertThat(draft.getDetails()).containsExactly(debit, credit);
    }

    @Test
    @DisplayName("POSTED 원본은 그대로 두고 금액·계정·차원을 정확히 복사해 차대만 뒤집은 역분개를 만든다.")
    void reversalCreationRemainsAppendOnly() {
        JournalEntry original = precisePostedEntry();
        HeaderState headerBefore = HeaderState.capture(original);
        List<DetailState> detailsBefore = original.getDetails().stream().map(DetailState::capture).toList();

        JournalEntry reversal = original.createReversal(
                "reversal-maker", LocalDate.of(2026, 9, 30), "correction");

        assertThat(HeaderState.capture(original)).isEqualTo(headerBefore);
        assertThat(original.getDetails().stream().map(DetailState::capture).toList()).isEqualTo(detailsBefore);
        assertThat(reversal).isNotSameAs(original);
        assertThat(reversal.getStatus()).isNull();
        assertThat(reversal.getEntryType()).isEqualTo("REVERSAL");
        assertThat(reversal.getLineageSourceId()).isEqualTo(original.getId().toString());
        assertThat(reversal.getDetails()).extracting(JournalDetail::getSide)
                .containsExactly(JournalSide.CREDIT, JournalSide.DEBIT);
        assertThat(reversal.getDetails()).satisfiesExactly(
                detail -> assertCopiedReversalLine(detail, detailsBefore.get(0), JournalSide.CREDIT),
                detail -> assertCopiedReversalLine(detail, detailsBefore.get(1), JournalSide.DEBIT));
    }

    @Test
    @DisplayName("수정 전 POSTED 외화 이력은 실제 금액을 그대로 뒤집어 균형 잡힌 관리형 역분개를 만든다.")
    void legacyPostedForeignHistoryCanBeReversedWithoutRevaluingIt() {
        JournalEntry legacyOriginal = inconsistentHistoricalForeignEntry();

        // 같은 값의 신규 NORMAL 전표는 계속 거부되고, reflection은 과거 DB 이력만 표현합니다.
        assertThatThrownBy(legacyOriginal::validateInvariants)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("기준통화");
        markAsPersistedPostedHistory(legacyOriginal);

        JournalEntry reversal = legacyOriginal.createReversal(
                "legacy-reversal-maker", LocalDate.of(2026, 9, 30), "reverse pre-remediation history");

        assertThat(reversal.getEntryType()).isEqualTo("REVERSAL");
        assertThat(reversal.getCurrencyCode()).isEqualTo("USD");
        assertThat(reversal.getExchangeRate()).isEqualByComparingTo("1300");
        assertThat(reversal.getDetails()).satisfiesExactly(
                line -> {
                    assertThat(line.getSide()).isEqualTo(JournalSide.CREDIT);
                    assertThat(line.getAmount()).isEqualByComparingTo("100.00");
                    assertThat(line.getBaseAmount()).isEqualByComparingTo("100.00");
                },
                line -> {
                    assertThat(line.getSide()).isEqualTo(JournalSide.DEBIT);
                    assertThat(line.getAmount()).isEqualByComparingTo("100.00");
                    assertThat(line.getBaseAmount()).isEqualByComparingTo("100.00");
                });
        reversal.validateInvariants();
    }

    @Test
    @DisplayName("취소된 역분개 작업만 다른 전표 ID로 한 번 재개할 수 있다.")
    void cancelledReversalOperationCanRestartWithOneReplacement() {
        JournalReversalOperation operation = JournalReversalOperation.create(759L, 760L);

        operation.cancel(760L, " Cancel.Operator ", " abandoned draft ");

        assertThat(operation.getStatus()).isEqualTo(ReversalOperationStatus.CANCELLED);
        assertThat(operation.getCancelledBy()).isEqualTo("cancel.operator");
        assertThat(operation.getCancellationReason()).isEqualTo("abandoned draft");
        assertThat(operation.getCancelledAt()).isNotNull();

        operation.restart(761L);

        assertThat(operation.getStatus()).isEqualTo(ReversalOperationStatus.PENDING);
        assertThat(operation.getReversalJournalEntryId()).isEqualTo(761L);
        assertThat(operation.getCancelledBy()).isNull();
        assertThat(operation.getCancellationReason()).isNull();
        assertThat(operation.getCancelledAt()).isNull();
        assertThatThrownBy(() -> operation.restart(762L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("취소된 역분개 작업");
    }

    @Test
    @DisplayName("전기 완료된 역분개 작업은 취소하거나 다시 열 수 없다.")
    void postedReversalOperationCannotCancelOrReopen() {
        JournalReversalOperation operation = JournalReversalOperation.create(759L, 760L);
        operation.markPosted(760L);

        assertThat(operation.getStatus()).isEqualTo(ReversalOperationStatus.POSTED);
        assertThat(operation.getPostedAt()).isNotNull();
        assertThatThrownBy(() -> operation.cancel(760L, "operator", "late cancel"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("진행 중인 역분개 작업");
        assertThatThrownBy(() -> operation.restart(761L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("취소된 역분개 작업");
        assertThat(operation.getStatus()).isEqualTo(ReversalOperationStatus.POSTED);
        assertThat(operation.getReversalJournalEntryId()).isEqualTo(760L);
    }

    @Test
    @DisplayName("역분개 작업은 유효한 원본·역분개 ID와 현재 연결 일치를 강제한다.")
    void reversalOperationValidatesPersistentIdsAndCurrentRelationship() {
        assertThatThrownBy(() -> JournalReversalOperation.create(null, 760L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("원본 전표 ID");
        assertThatThrownBy(() -> JournalReversalOperation.create(759L, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("역분개 전표 ID");

        JournalReversalOperation operation = JournalReversalOperation.create(759L, 760L);
        assertThatThrownBy(() -> operation.cancel(761L, "operator", "wrong relation"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("일치하지 않습니다");
        assertThatThrownBy(() -> operation.markPosted(761L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("일치하지 않습니다");
        assertThat(operation.getStatus()).isEqualTo(ReversalOperationStatus.PENDING);
        assertThat(operation.getReversalJournalEntryId()).isEqualTo(760L);
    }

    @Test
    @DisplayName("취소된 역분개 전표는 REJECTED가 되어 정상 승인·전기 경로로 다시 사용할 수 없다.")
    void cancelledReversalJournalBecomesNonPostable() {
        JournalEntry reversal = precisePostedEntry().createReversal(
                "reversal-maker", LocalDate.of(2026, 9, 30), "correction");
        reversal.initializeDraft();

        reversal.cancelReversal(" Cancel.Operator ", " abandoned draft ");

        assertThat(reversal.getStatus()).isEqualTo(JournalEntryStatus.REJECTED);
        assertThat(reversal.getAuditUser()).isEqualTo("cancel.operator");
        assertThat(reversal.getRejectionReason()).isEqualTo("abandoned draft");
        assertThatThrownBy(() -> reversal.post("poster"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("승인된 전표만");
        assertThatThrownBy(() -> reversal.cancelReversal("operator", "again"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("진행 중인 역분개 전표");
    }

    @Test
    @DisplayName("POSTED 역분개 전표는 취소할 수 없고 최종 이력을 그대로 보존한다.")
    void postedReversalJournalCannotBeCancelled() {
        JournalEntry reversal = precisePostedEntry().createReversal(
                "reversal-maker", LocalDate.of(2026, 9, 30), "correction");
        reversal.initializeDraft();
        reversal.requestApproval("reversal-maker");
        reversal.approve("reversal-checker");
        reversal.post("reversal-poster");
        HeaderState before = HeaderState.capture(reversal);

        assertThatThrownBy(() -> reversal.cancelReversal("operator", "too late"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("POSTED");
        assertThat(HeaderState.capture(reversal)).isEqualTo(before);
    }

    private static void assertCopiedReversalLine(
            JournalDetail reversal,
            DetailState original,
            JournalSide expectedSide) {
        assertThat(reversal.getSide()).isEqualTo(expectedSide);
        assertThat(reversal.getAccountCode()).isEqualTo(original.accountCode());
        assertThat(reversal.getAmount()).isEqualByComparingTo(original.amount());
        assertThat(reversal.getBaseAmount()).isEqualByComparingTo(original.baseAmount());
        assertThat(reversal.getDepartmentCode()).isEqualTo(original.departmentCode());
        assertThat(reversal.getBusinessPartnerCode()).isEqualTo(original.businessPartnerCode());
        assertThat(reversal.getDetailDescription()).isEqualTo("[역분개] " + original.description());
    }

    private static JournalDetail detail(
            JournalSide side,
            String accountCode,
            String amount,
            String baseAmount) {
        JournalDetail detail = new JournalDetail();
        detail.setSide(side);
        detail.setAccountCode(accountCode);
        detail.setAmount(new BigDecimal(amount));
        detail.setBaseAmount(new BigDecimal(baseAmount));
        return detail;
    }

    private static JournalEntry balancedDraft(String maker) {
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(LocalDate.of(2026, 9, 25));
        entry.setCreatedBy(maker);
        entry.addDetail(detail(JournalSide.DEBIT, "10100", "10.00", "10.00"));
        entry.addDetail(detail(JournalSide.CREDIT, "40100", "10.00", "10.00"));
        entry.initializeDraft();
        return entry;
    }

    private static JournalEntry postedEntry() {
        JournalEntry entry = balancedDraft("maker");
        entry.setId(758L);
        entry.setSlipNo("JE-POSTED-758");
        entry.setAccountingDate(LocalDate.of(2026, 9, 25));
        entry.setDescription("posted fixture");
        entry.setEntryType("NORMAL");
        entry.setCurrencyCode("KRW");
        entry.setExchangeRate(BigDecimal.ONE);
        entry.setAuditUser("maker");
        entry.setLineageSourceType("TEST");
        entry.setLineageSourceId("ISSUE-758");
        entry.getDetails().forEach(detail -> {
            detail.setDepartmentCode("D-ORIGINAL");
            detail.setBusinessPartnerCode("BP-ORIGINAL");
            detail.setDetailDescription("original detail");
            detail.setAuditUser("maker");
        });
        entry.requestApproval("maker");
        entry.approve("checker");
        entry.post("poster");
        return entry;
    }

    private static JournalEntry precisePostedEntry() {
        JournalEntry entry = new JournalEntry();
        entry.setId(759L);
        entry.setSlipNo("JE-POSTED-759");
        entry.setSlipDate(LocalDate.of(2026, 9, 25));
        entry.setAccountingDate(LocalDate.of(2026, 9, 25));
        entry.setDescription("precision reversal fixture");
        entry.setEntryType("NORMAL");
        entry.setCurrencyCode("USD");
        entry.setExchangeRate(new BigDecimal("1.23456789"));
        entry.setCreatedBy("maker");
        entry.setLineageSourceType("TEST");
        entry.setLineageSourceId("ISSUE-759");
        // 12.34 USD × 1.23456789 rounds to 15.23 KRW under the common conversion policy.
        JournalDetail debit = detail(JournalSide.DEBIT, "10123", "12.34", "15.23");
        debit.setDepartmentCode("D-DEBIT");
        debit.setBusinessPartnerCode("BP-DEBIT");
        debit.setDetailDescription("debit source detail");
        JournalDetail credit = detail(JournalSide.CREDIT, "40987", "12.34", "15.23");
        credit.setDepartmentCode("D-CREDIT");
        credit.setBusinessPartnerCode("BP-CREDIT");
        credit.setDetailDescription("credit source detail");
        entry.addDetail(debit);
        entry.addDetail(credit);
        entry.initializeDraft();
        entry.requestApproval("maker");
        entry.approve("checker");
        entry.post("poster");
        return entry;
    }

    private static JournalEntry inconsistentHistoricalForeignEntry() {
        JournalEntry entry = new JournalEntry();
        entry.setId(763L);
        entry.setSlipNo("JE-LEGACY-FX-763");
        entry.setSlipDate(LocalDate.of(2026, 9, 24));
        entry.setAccountingDate(LocalDate.of(2026, 9, 24));
        entry.setDescription("pre-remediation foreign history");
        entry.setEntryType("NORMAL");
        entry.setCurrencyCode("USD");
        entry.setExchangeRate(new BigDecimal("1300"));
        entry.setCreatedBy("legacy-maker");
        entry.setLineageSourceType("LEGACY_TEST");
        entry.setLineageSourceId("ISSUE-763");
        entry.addDetail(detail(JournalSide.DEBIT, "11000", "100.00", "100.00"));
        entry.addDetail(detail(JournalSide.CREDIT, "21000", "100.00", "100.00"));
        return entry;
    }

    private static void markAsPersistedPostedHistory(JournalEntry entry) {
        setHistoricalField(entry, "status", JournalEntryStatus.POSTED);
        setHistoricalField(entry, "persistedStatus", JournalEntryStatus.POSTED);
    }

    private static void setHistoricalField(JournalEntry entry, String fieldName, Object value) {
        try {
            Field field = JournalEntry.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(entry, value);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("failed to construct persisted legacy history", exception);
        }
    }

    private static Mutation mutation(String name, Runnable action) {
        return new Mutation(name, action);
    }

    private static void assertPostedMutationRejected(String name, Runnable mutation) {
        assertThatThrownBy(mutation::run)
                .as(name)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("POSTED");
    }

    private record Mutation(String name, Runnable action) { }

    private record HeaderState(Long id, String slipNo, LocalDate slipDate, LocalDate accountingDate,
                               String description, JournalEntryStatus status, String entryType,
                               String currencyCode, BigDecimal exchangeRate, String rejectionReason,
                               LocalDateTime updatedAt, String createdBy, String approvedBy, String auditUser,
                               String lineageSourceType, String lineageSourceId) {
        private static HeaderState capture(JournalEntry entry) {
            return new HeaderState(entry.getId(), entry.getSlipNo(), entry.getSlipDate(), entry.getAccountingDate(),
                    entry.getDescription(), entry.getStatus(), entry.getEntryType(), entry.getCurrencyCode(),
                    entry.getExchangeRate(), entry.getRejectionReason(), entry.getUpdatedAt(), entry.getCreatedBy(),
                    entry.getApprovedBy(), entry.getAuditUser(), entry.getLineageSourceType(),
                    entry.getLineageSourceId());
        }
    }

    private record DetailState(Long id, JournalEntry owner, JournalSide side, String accountCode,
                               BigDecimal amount, BigDecimal baseAmount, String departmentCode,
                               String businessPartnerCode, String description, LocalDateTime updatedAt,
                               String auditUser) {
        private static DetailState capture(JournalDetail detail) {
            return new DetailState(detail.getId(), detail.getJournalEntry(), detail.getSide(),
                    detail.getAccountCode(), detail.getAmount(), detail.getBaseAmount(), detail.getDepartmentCode(),
                    detail.getBusinessPartnerCode(), detail.getDetailDescription(), detail.getUpdatedAt(),
                    detail.getAuditUser());
        }
    }
}

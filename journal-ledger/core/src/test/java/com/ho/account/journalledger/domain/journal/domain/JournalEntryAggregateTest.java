package com.ho.account.journalledger.domain.journal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    @DisplayName("POSTED 원본은 그대로 두고 createReversal은 별도의 DRAFT 역분개만 추가 생성한다.")
    void reversalCreationRemainsAppendOnly() {
        JournalEntry original = postedEntry();
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

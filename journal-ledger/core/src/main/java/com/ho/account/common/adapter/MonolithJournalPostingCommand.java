package com.ho.account.common.adapter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 모놀리스 전표 전기 커맨드 (Monolith Journal Posting Command).
 *
 * ─────────────────────────────────────────────────
 * [업무 설명]
 * 다른 모듈(매입, 지출결의, 리스 등)이 journal-ledger 모듈에
 * 전표 생성을 요청할 때 사용하는 데이터 전달 객체(DTO)입니다.
 *
 * 예시 사용:
 *   매입채무 모듈에서 세금계산서 승인 시 전표를 생성하려면:
 *   MonolithJournalPostingCommand command = new MonolithJournalPostingCommand(
 *       LocalDate.of(2026, 1, 15),       // 회계 반영일
 *       "세금계산서 매입 - INV-2026-001",  // 전표 적요
 *       "KRW",                            // 통화
 *       List.of(
 *           new JournalLine("51000", BigDecimal.valueOf(100000), null, "DEPT01", "BP001", "원재료 매입"),
 *           new JournalLine("21100", null, BigDecimal.valueOf(100000), null, "BP001", "매입채무")
 *       )
 *   );
 *   monolithJournalPostingAdapter.post(command);
 *
 * ─────────────────────────────────────────────────
 * [개발 설명]
 * Java Record 타입으로 불변(Immutable) 객체입니다.
 * 생성 후 필드 값을 변경할 수 없습니다 (안전한 데이터 전달 보장).
 *
 * 내부 Record JournalLine:
 *   전표 1개의 분개 라인(차변 또는 대변) 1개를 나타냅니다.
 *   - debitAmount와 creditAmount 중 하나만 값을 가져야 합니다.
 *   - 둘 다 0이거나 null이면 MonolithJournalPostingAdapter에서 무시됩니다.
 *
 * 이 Record를 사용하는 곳:
 *   - MonolithJournalPostingAdapter.post() 파라미터
 *   - expenditure-resolution, ap-payable, lease 모듈에서 생성
 * ─────────────────────────────────────────────────
 */
public record MonolithJournalPostingCommand(
        /**
         * 회계 반영일.
         * 전표의 accountingDate와 slipDate에 사용됩니다.
         * 예: LocalDate.of(2026, 1, 15)
         */
        LocalDate accountingDate,

        /**
         * 전표 전체 적요(설명).
         * 전표의 description 필드에 저장됩니다.
         * 예: "세금계산서 매입 - INV-2026-001"
         */
        String description,

        /**
         * 거래 통화 코드.
         * 예: "KRW"(원화), "USD"(달러), "EUR"(유로)
         * CurrencyPersistencePort를 통해 Currency 엔티티로 변환됩니다.
         */
        String currencyCode,

        /**
         * 분개 라인 목록.
         * 차변 라인과 대변 라인을 모두 포함해야 합니다.
         * 복식부기 원칙: 차변 합계 = 대변 합계여야 합니다.
         */
        List<JournalLine> lines
) {
    /**
     * 전표 분개 라인 1개 (차변 또는 대변).
     *
     * [업무 설명]
     * 하나의 전표는 최소 2개의 분개 라인을 가집니다 (차변 1개 + 대변 1개).
     * debitAmount와 creditAmount 중 하나만 0보다 큰 값을 가져야 합니다.
     *
     * 예시 (매입 전표):
     *   라인1: accountCode="51000"(매입비용), debitAmount=100000 → 차변
     *   라인2: accountCode="21100"(매입채무), creditAmount=100000 → 대변
     */
    public record JournalLine(
            /**
             * 계정과목 코드.
             * AccountSubjectPersistencePort를 통해 AccountSubject 엔티티로 변환됩니다.
             * 예: "51000"(매입비용), "21100"(매입채무), "11000"(매출채권)
             */
            String accountCode,

            /**
             * 차변 금액.
             * 차변 항목이면 이 필드에 금액을 설정하고, creditAmount는 null 또는 0.
             * 대변 항목이면 null 또는 0으로 설정합니다.
             */
            BigDecimal debitAmount,

            /**
             * 대변 금액.
             * 대변 항목이면 이 필드에 금액을 설정하고, debitAmount는 null 또는 0.
             * 차변 항목이면 null 또는 0으로 설정합니다.
             */
            BigDecimal creditAmount,

            /**
             * 귀속 부서 코드 (선택).
             * null 가능: 부서 귀속이 필요 없는 계정(매입채무 등)은 null.
             * 설정 시 DepartmentPersistencePort를 통해 Department 엔티티로 변환됩니다.
             * 예: "DEPT01"(생산부서), "DEPT02"(영업부서)
             */
            String departmentCode,

            /**
             * 거래처 코드 (선택).
             * null 가능: 거래처 없는 계정(복리후생비 등)은 null.
             * 설정 시 BusinessPartnerPersistencePort를 통해 BusinessPartner 엔티티로 변환됩니다.
             * 예: "BP001"(삼성전자), "BP002"(LG전자)
             */
            String businessPartnerCode,

            /**
             * 라인 적요 (선택).
             * JournalDetail.detailDescription에 저장됩니다.
             * 예: "원재료 매입", "매입채무 발생"
             */
            String description
    ) {}
}

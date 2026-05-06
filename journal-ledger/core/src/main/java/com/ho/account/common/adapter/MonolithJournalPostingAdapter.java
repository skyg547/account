package com.ho.account.common.adapter;

import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.application.port.out.CurrencyPersistencePort;
import com.ho.account.masterdata.core.application.port.out.DepartmentPersistencePort;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Currency;
import com.ho.account.masterdata.core.domain.model.Department;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * 모놀리스 전표 전기 어댑터 (Monolith Journal Posting Adapter).
 *
 * ─────────────────────────────────────────────────
 * [업무 설명]
 * 다른 모듈(매입채무, 지출결의, 리스 등)에서 전표 생성이 필요할 때
 * journal-ledger 모듈에 전표를 생성하는 진입점(Entry Point) 역할을 합니다.
 *
 * 왜 이 어댑터가 필요한가?
 *   각 업무 모듈은 자신의 핵심 로직(매입 처리, 지출결의 등)만 담당하고,
 *   회계 전표 생성은 journal-ledger 모듈에 위임합니다.
 *   이때 다른 모듈이 journal-ledger 내부 구조(JournalEntry, JournalDetail 등)를
 *   직접 다루지 않도록 단순화된 인터페이스(MonolithJournalPostingCommand)를 제공합니다.
 *
 * 사용 흐름:
 *   [지출결의 모듈] → MonolithJournalPostingAdapter.post(command)
 *                   → JournalEntry + JournalDetail 빌드
 *                   → JournalUseCase.createJournalEntry() 호출
 *                   → 전표 저장 (검증 포함)
 *
 * ─────────────────────────────────────────────────
 * [개발 설명]
 * 헥사고날 아키텍처에서 "Contract Adapter" 역할입니다.
 * 다른 모듈의 계약(Port)을 구현하거나, 타 모듈의 UseCase를 내부적으로 호출합니다.
 *
 * 이 어댑터가 하는 일:
 *   1. MonolithJournalPostingCommand(단순 DTO)를 JournalEntry(도메인 객체)로 변환
 *   2. 계정코드 → AccountSubject 엔티티 변환 (AccountSubjectPersistencePort 사용)
 *   3. 통화코드 → Currency 엔티티 변환 (CurrencyPersistencePort 사용)
 *   4. 부서코드 → Department 엔티티 변환 (DepartmentPersistencePort 사용)
 *   5. 거래처코드 → BusinessPartner 엔티티 변환 (BusinessPartnerPersistencePort 사용)
 *   6. JournalUseCase.createJournalEntry() 호출 (차대변 검증 + 저장)
 *
 * 주의:
 *   이 어댑터는 journal-ledger core 모듈 내의 common/adapter 패키지에 위치합니다.
 *   향후 헥사고날 구조 고도화 시 adapter/in/contract/ 패키지로 이동을 권장합니다.
 * ─────────────────────────────────────────────────
 */
@Component
@RequiredArgsConstructor
public class MonolithJournalPostingAdapter {

    /**
     * 전표 유스케이스 (Inbound Port).
     * 전표 생성은 이 인터페이스를 통해서만 수행합니다.
     * 실제 구현체: JournalEntryService
     */
    private final JournalUseCase journalUseCase;

    /**
     * 계정과목 조회 포트.
     * command의 accountCode(String) → AccountSubject 엔티티 변환에 사용합니다.
     */
    private final AccountSubjectPersistencePort accountSubjectPersistencePort;

    /**
     * 부서 조회 포트.
     * command의 departmentCode(String) → Department 엔티티 변환에 사용합니다.
     */
    private final DepartmentPersistencePort departmentPersistencePort;

    /**
     * 거래처 조회 포트.
     * command의 businessPartnerCode(String) → BusinessPartner 엔티티 변환에 사용합니다.
     */
    private final BusinessPartnerPersistencePort businessPartnerPersistencePort;

    /**
     * 통화 조회 포트.
     * command의 currencyCode(String) → Currency 엔티티 변환에 사용합니다.
     */
    private final CurrencyPersistencePort currencyPersistencePort;

    /**
     * 커맨드를 기반으로 전표를 생성합니다.
     *
     * [업무 설명]
     * 다른 모듈에서 전표 생성이 필요할 때 이 메서드를 호출합니다.
     * 커맨드의 간단한 코드값(문자열)을 엔티티로 변환하고, 전표를 조립하여 저장합니다.
     *
     * [개발 설명]
     * 처리 흐름:
     *   1. currencyCode → Currency 엔티티 조회 (없으면 예외)
     *   2. JournalEntry 헤더 생성 (accountingDate, description, currency 설정)
     *   3. 각 분개 라인(JournalLine) 처리:
     *      - accountCode → AccountSubject 조회 (없으면 예외)
     *      - debitAmount > 0이면 DEBIT, creditAmount > 0이면 CREDIT으로 설정
     *      - departmentCode가 있으면 Department 조회 후 설정
     *      - businessPartnerCode가 있으면 BusinessPartner 조회 후 설정
     *      - JournalDetail 생성 후 JournalEntry에 연결
     *   4. JournalUseCase.createJournalEntry() 호출
     *      → validateBalance() (차변합계 = 대변합계 검증)
     *      → 저장
     *
     * @param command 전표 생성에 필요한 정보 (회계일, 통화, 분개 라인 목록)
     * @throws IllegalArgumentException 계정코드, 통화코드, 부서코드, 거래처코드 조회 실패 시
     * @throws IllegalStateException    차변합계 ≠ 대변합계 시 (복식부기 위반)
     */
    public void post(MonolithJournalPostingCommand command) {
        // ─── 1단계: 전표 헤더 생성 ────────────────────────────────────────
        JournalEntry entry = new JournalEntry();
        entry.setAccountingDate(command.accountingDate());  // 회계 반영일
        entry.setSlipDate(command.accountingDate());         // 전표 작성일 = 회계 반영일
        entry.setDescription(command.description());         // 전표 적요

        // 통화 코드 → Currency 엔티티 변환 (없으면 즉시 예외)
        Currency currency = currencyPersistencePort.findByCode(command.currencyCode())
                .orElseThrow(() -> new IllegalArgumentException("Currency not found: " + command.currencyCode()));
        entry.setCurrency(currency);

        // ─── 2단계: 분개 라인 변환 및 JournalDetail 생성 ──────────────────
        entry.setDetails(command.lines().stream().map(line -> {
            JournalDetail detail = new JournalDetail();

            // 계정코드 → AccountSubject 엔티티 변환 (없으면 즉시 예외)
            AccountSubject account = accountSubjectPersistencePort.findByCode(line.accountCode())
                    .orElseThrow(() -> new IllegalArgumentException("Account subject not found: " + line.accountCode()));
            detail.setAccountSubject(account);

            // 차변/대변 구분: debitAmount > 0이면 DEBIT, creditAmount > 0이면 CREDIT
            if (line.debitAmount() != null && line.debitAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                detail.setAmount(line.debitAmount());
                detail.setSide(JournalSide.DEBIT);    // 차변 항목
            } else if (line.creditAmount() != null && line.creditAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                detail.setAmount(line.creditAmount());
                detail.setSide(JournalSide.CREDIT);   // 대변 항목
            }
            // 둘 다 0 또는 null이면 amount/side 미설정 (validateBalance에서 검증됨)

            // 부서코드가 있으면 Department 엔티티 변환 (null이면 건너뜀)
            if (line.departmentCode() != null) {
                Department dept = departmentPersistencePort.findActiveByCode(line.departmentCode())
                        .orElseThrow(() -> new IllegalArgumentException("Department not found: " + line.departmentCode()));
                detail.setDepartment(dept);
            }

            // 거래처코드가 있으면 BusinessPartner 엔티티 변환 (null이면 건너뜀)
            if (line.businessPartnerCode() != null) {
                BusinessPartner bp = businessPartnerPersistencePort.findByBusinessPartnerCode(line.businessPartnerCode())
                        .orElseThrow(() -> new IllegalArgumentException("Business partner not found: " + line.businessPartnerCode()));
                detail.setBusinessPartner(bp);
            }

            detail.setDetailDescription(line.description());  // 라인 적요
            detail.setJournalEntry(entry);                     // 부모 전표 역참조 설정
            return detail;
        }).collect(Collectors.toList()));

        // ─── 3단계: 전표 생성 (검증 + 저장) ───────────────────────────────
        // JournalEntryService.createJournalEntry()가 호출됩니다.
        // 내부에서 validateBalance() 수행: 차변합계 ≠ 대변합계이면 예외 발생
        journalUseCase.createJournalEntry(entry);
    }
}

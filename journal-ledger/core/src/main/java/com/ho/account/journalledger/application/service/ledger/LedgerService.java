package com.ho.account.journalledger.application.service.ledger;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Currency;
import com.ho.account.masterdata.core.domain.model.Department;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.journalledger.domain.journal.repository.JournalDetailRepository;
import com.ho.account.journalledger.domain.ledger.domain.GlBalance;
import com.ho.account.journalledger.domain.ledger.domain.SlBalance;
import com.ho.account.journalledger.domain.ledger.repository.GlBalanceRepository;
import com.ho.account.journalledger.domain.ledger.repository.SlBalanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 원장 잔액 서비스 (Ledger Service).
 *
 * ─────────────────────────────────────────────────
 * [업무 설명]
 * 원장(Ledger)은 회계에서 각 계정과목별로 거래를 기록하고 잔액을 관리하는 장부입니다.
 *
 * 이 서비스는 두 종류의 원장 잔액을 관리합니다:
 *
 * 1. GL Balance (총계정원장 잔액)
 *    - 관리 단위: 계정과목 + 통화 + 날짜
 *    - 예: "현금(10100), KRW, 2026-01-15" 잔액 = 5,000,000원
 *    - 재무상태표(BS), 손익계산서(PL), 시산표(Trial Balance) 작성의 기초
 *
 * 2. SL Balance (보조원장 잔액)
 *    - 관리 단위: 계정과목 + 거래처 + 부서 + 통화 + 날짜
 *    - 예: "매출채권(11000) + 거래처A(BP001), KRW, 2026-01-15" 잔액 = 2,000,000원
 *    - GL Balance보다 세분화된 잔액 — 거래처별 채권·채무 현황 파악에 사용
 *
 * 잔액 계산 공식 (GL/SL 공통):
 *   기말잔액 = 기초잔액 + 차변합계 - 대변합계
 *
 * 기초잔액 이월(Carry-forward):
 *   특정 날짜에 처음 전기가 발생하면, 해당 날짜의 잔액 레코드가 없습니다.
 *   이 경우 직전 날짜의 기말잔액을 새 레코드의 기초잔액으로 자동 이월합니다.
 *   예: 1월 15일에 처음 전기 → 1월 14일 기말잔액을 1월 15일 기초잔액으로 복사
 *
 * ─────────────────────────────────────────────────
 * [개발 설명]
 * - PostingService에서 전기 처리 시 호출됩니다: updateLedgerBalances()
 * - reaggregateLedgerBalancesForPeriod(): 잔액 데이터 오류 수정/재집계 시 사용
 *   기존 잔액을 삭제하고 전기된 전표 라인으로 재계산합니다.
 * - getGlBalances() / getSlBalances(): 조회 및 집계 반환 (화면/API 용)
 *   동일 키의 여러 날짜 잔액을 하나로 집계합니다.
 * ─────────────────────────────────────────────────
 */
@Service
@RequiredArgsConstructor
public class LedgerService {

    /** GL Balance(총계정원장 잔액) JPA 저장소 */
    private final GlBalanceRepository glBalanceRepository;

    /** SL Balance(보조원장 잔액) JPA 저장소 */
    private final SlBalanceRepository slBalanceRepository;

    /** 전표 상세 라인 저장소 — 재집계 시 전기된 라인 목록 조회에 사용 */
    private final JournalDetailRepository journalDetailRepository;

    /**
     * 전표 상세 라인 1개에 대해 GL Balance와 SL Balance를 갱신합니다.
     *
     * [업무 설명]
     * 전표가 전기될 때 각 분개 라인(차변 또는 대변)에 따라
     * 해당 계정과목의 원장 잔액을 실시간으로 갱신합니다.
     *
     * [개발 설명]
     * PostingService의 루프에서 JournalDetail 하나당 이 메서드를 한 번씩 호출합니다.
     * 내부적으로 updateGlBalance()와 updateSlBalance()를 차례로 실행합니다.
     *
     * @param journalDetail 전표 상세 라인 (계정과목, 거래처, 부서, 금액, 차대 구분 포함)
     * @param accountingDate 회계 반영일 (잔액 레코드의 balanceDate)
     */
    @Transactional
    public void updateLedgerBalances(JournalDetail journalDetail, LocalDate accountingDate) {
        AccountSubject accountSubject = journalDetail.getAccountSubject();
        BusinessPartner businessPartner = journalDetail.getBusinessPartner();
        Department department = journalDetail.getDepartment();
        Currency currency = journalDetail.getJournalEntry().getCurrency();
        // baseAmount: 기본통화(KRW) 환산 금액 — 원장 잔액 계산 기준
        BigDecimal amount = journalDetail.getBaseAmount();
        // 차변 여부: DEBIT이면 true, CREDIT이면 false
        boolean isDebit = JournalSide.DEBIT.equals(journalDetail.getSide());

        // GL Balance 갱신: 계정과목+통화 단위
        updateGlBalance(accountSubject, currency, amount, isDebit, accountingDate);
        // SL Balance 갱신: 계정과목+거래처+부서+통화 단위 (더 세분화)
        updateSlBalance(accountSubject, businessPartner, department, currency, amount, isDebit, accountingDate);
    }

    /**
     * GL Balance(총계정원장 잔액)를 조회하거나 신규 생성하여 차변/대변을 누적합니다.
     *
     * [업무 설명]
     * 계정과목+통화+날짜 조합으로 잔액 레코드를 찾아 금액을 누적합니다.
     * 해당 날짜에 잔액 레코드가 없으면 신규 생성하며, 이때 Carry-forward(기초잔액 이월)를 수행합니다.
     *
     * [Carry-forward 예시]
     *   현금(10100) 계정, 1월 15일에 처음 전기:
     *   → 1월 14일 기말잔액(endingBalance)을 1월 15일 기초잔액(beginningBalance)으로 복사
     *   → 기초잔액 5,000,000원에서 차변 3,000,000원 → 기말잔액 8,000,000원
     *
     * @param accountSubject 계정과목
     * @param currency       거래 통화
     * @param amount         기본통화(KRW) 기준 금액
     * @param isDebit        차변이면 true, 대변이면 false
     * @param accountingDate 회계 반영일
     */
    private void updateGlBalance(AccountSubject accountSubject,
                                 Currency currency,
                                 BigDecimal amount,
                                 boolean isDebit,
                                 LocalDate accountingDate) {
        // 회계 기간(연월) 계산: 예) 2026-01-15 → 2026-01
        YearMonth period = YearMonth.from(accountingDate);

        // 해당 날짜+계정+통화 조합 잔액 레코드 조회
        Optional<GlBalance> optionalGlBalance = glBalanceRepository.findByAccountSubjectAndCurrencyAndBalanceDateAndPeriod(
                accountSubject, currency, accountingDate, period);

        GlBalance glBalance = optionalGlBalance.orElseGet(() -> {
            // ─ 잔액 레코드 없음 → 신규 생성 + Carry-forward ─────────────
            GlBalance newGlBalance = new GlBalance();
            newGlBalance.setAccountSubject(accountSubject);
            newGlBalance.setCurrency(currency);
            newGlBalance.setBalanceDate(accountingDate);
            newGlBalance.setPeriod(period);

            // Carry-forward: 직전 날짜의 기말잔액을 기초잔액으로 이월
            // findFirst...OrderByBalanceDateDesc: 현재 날짜 이전 중 가장 최근 잔액 조회
            glBalanceRepository.findFirstByAccountSubjectAndCurrencyAndBalanceDateBeforeOrderByBalanceDateDesc(
                    accountSubject, currency, accountingDate)
                .ifPresent(prev -> newGlBalance.setBeginningBalance(prev.getEndingBalance()));

            return newGlBalance;
        });

        // 차변/대변 누적 (내부에서 endingBalance 자동 재계산)
        if (isDebit) {
            glBalance.addDebit(amount);   // endingBalance += amount
        } else {
            glBalance.addCredit(amount);  // endingBalance -= amount
        }

        glBalanceRepository.save(glBalance);
    }

    /**
     * SL Balance(보조원장 잔액)를 조회하거나 신규 생성하여 차변/대변을 누적합니다.
     *
     * [업무 설명]
     * 계정과목+거래처+부서+통화+날짜 조합의 잔액을 관리합니다.
     * GL Balance보다 세분화되어 있어 거래처별 채권·채무 현황 파악에 사용됩니다.
     *
     * 예시:
     *   매출채권(11000) + 거래처A → 60,000,000원 (거래처A에게 받을 돈)
     *   매출채권(11000) + 거래처B → 40,000,000원 (거래처B에게 받을 돈)
     *   합계 = GL Balance의 매출채권(11000) 잔액 100,000,000원
     *
     * [개발 설명]
     * updateGlBalance()와 동일한 Carry-forward 로직을 적용합니다.
     * businessPartner, department는 null 가능 (null도 독립적인 집계 키로 처리됩니다).
     *
     * @param accountSubject  계정과목
     * @param businessPartner 거래처 (null 가능)
     * @param department      귀속 부서 (null 가능)
     * @param currency        거래 통화
     * @param amount          기본통화(KRW) 기준 금액
     * @param isDebit         차변이면 true, 대변이면 false
     * @param accountingDate  회계 반영일
     */
    private void updateSlBalance(AccountSubject accountSubject,
                                 BusinessPartner businessPartner,
                                 Department department,
                                 Currency currency,
                                 BigDecimal amount,
                                 boolean isDebit,
                                 LocalDate accountingDate) {
        YearMonth period = YearMonth.from(accountingDate);

        Optional<SlBalance> optionalSlBalance =
                slBalanceRepository.findByAccountSubjectAndBusinessPartnerAndDepartmentAndCurrencyAndBalanceDateAndPeriod(
                        accountSubject, businessPartner, department, currency, accountingDate, period);

        SlBalance slBalance = optionalSlBalance.orElseGet(() -> {
            // 잔액 레코드 없음 → 신규 생성 + Carry-forward
            SlBalance newSlBalance = new SlBalance();
            newSlBalance.setAccountSubject(accountSubject);
            newSlBalance.setBusinessPartner(businessPartner);
            newSlBalance.setDepartment(department);
            newSlBalance.setCurrency(currency);
            newSlBalance.setBalanceDate(accountingDate);
            newSlBalance.setPeriod(period);

            // Carry-forward: 직전 날짜 기말잔액 → 기초잔액 이월
            slBalanceRepository.findFirstByAccountSubjectAndBusinessPartnerAndDepartmentAndCurrencyAndBalanceDateBeforeOrderByBalanceDateDesc(
                    accountSubject, businessPartner, department, currency, accountingDate)
                .ifPresent(prev -> newSlBalance.setBeginningBalance(prev.getEndingBalance()));

            return newSlBalance;
        });

        if (isDebit) {
            slBalance.addDebit(amount);
        } else {
            slBalance.addCredit(amount);
        }

        slBalanceRepository.save(slBalance);
    }

    /**
     * 특정 기간의 원장 잔액을 완전히 재집계합니다. (최적화 버전)
     *
     * @param startDate 재집계 시작일 (포함)
     * @param endDate   재집계 종료일 (포함)
     */
    @Transactional
    public void reaggregateLedgerBalancesForPeriod(LocalDate startDate, LocalDate endDate) {
        // 1. 해당 기간의 기존 GL/SL Balance 전체 삭제
        glBalanceRepository.deleteAllInBatch(glBalanceRepository.findByBalanceDateBetween(startDate, endDate));
        slBalanceRepository.deleteAllInBatch(slBalanceRepository.findByBalanceDateBetween(startDate, endDate));

        // 2. 전기 완료된 전표 상세 라인 조회
        List<JournalDetail> postedJournalDetails =
                journalDetailRepository.findPostedJournalDetailsByAccountingDateBetween(startDate, endDate);

        // 3. 벌크 갱신 로직 호출 (성능 최적화)
        updateLedgerBalancesBulk(postedJournalDetails);
    }

    /**
     * 대량의 전표 상세 라인을 원장 잔액에 한 번에 반영합니다. (N+1 문제 해결)
     * 
     * @param journalDetails 처리할 상세 라인 목록
     */
    @Transactional
    public void updateLedgerBalancesBulk(List<JournalDetail> journalDetails) {
        if (journalDetails == null || journalDetails.isEmpty()) return;

        // 날짜별로 그룹화하여 순차 처리 (Carry-forward 정합성 유지)
        Map<LocalDate, List<JournalDetail>> groupedByDate = journalDetails.stream()
                .collect(Collectors.groupingBy(d -> d.getJournalEntry().getAccountingDate(), LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<LocalDate, List<JournalDetail>> entry : groupedByDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<JournalDetail> details = entry.getValue();

            // 메모리 내에서 일별 집계 후 저장
            updateDailyBalances(date, details);
        }
    }

    private void updateDailyBalances(LocalDate date, List<JournalDetail> details) {
        // 1. GL Balance 집계 및 반영
        Map<String, BigDecimal> glDebitMap = new LinkedHashMap<>();
        Map<String, BigDecimal> glCreditMap = new LinkedHashMap<>();
        Map<String, GlBalanceKey> glKeys = new LinkedHashMap<>();

        // 2. SL Balance 집계 및 반영
        Map<String, BigDecimal> slDebitMap = new LinkedHashMap<>();
        Map<String, BigDecimal> slCreditMap = new LinkedHashMap<>();
        Map<String, SlBalanceKey> slKeys = new LinkedHashMap<>();

        for (JournalDetail detail : details) {
            AccountSubject account = detail.getAccountSubject();
            Currency currency = detail.getJournalEntry().getCurrency();
            BigDecimal amount = detail.getBaseAmount();
            boolean isDebit = JournalSide.DEBIT.equals(detail.getSide());

            // GL Key
            String glKeyStr = account.getCode() + "|" + currency.getCode();
            glKeys.putIfAbsent(glKeyStr, new GlBalanceKey(account, currency));
            if (isDebit) {
                glDebitMap.merge(glKeyStr, amount, BigDecimal::add);
            } else {
                glCreditMap.merge(glKeyStr, amount, BigDecimal::add);
            }

            // SL Key
            BusinessPartner partner = detail.getBusinessPartner();
            Department dept = detail.getDepartment();
            String slKeyStr = glKeyStr + "|" + (partner != null ? partner.getBusinessPartnerCode() : "NULL") + "|" + (dept != null ? dept.getCode() : "NULL");
            slKeys.putIfAbsent(slKeyStr, new SlBalanceKey(account, partner, dept, currency));
            if (isDebit) {
                slDebitMap.merge(slKeyStr, amount, BigDecimal::add);
            } else {
                slCreditMap.merge(slKeyStr, amount, BigDecimal::add);
            }
        }

        // GL 반영
        for (Map.Entry<String, GlBalanceKey> entry : glKeys.entrySet()) {
            GlBalanceKey key = entry.getValue();
            BigDecimal debitSum = glDebitMap.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            BigDecimal creditSum = glCreditMap.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            updateGlBalanceWithSums(key.account(), key.currency(), debitSum, creditSum, date);
        }

        // SL 반영
        for (Map.Entry<String, SlBalanceKey> entry : slKeys.entrySet()) {
            SlBalanceKey key = entry.getValue();
            BigDecimal debitSum = slDebitMap.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            BigDecimal creditSum = slCreditMap.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            updateSlBalanceWithSums(key.account(), key.partner(), key.dept(), key.currency(), debitSum, creditSum, date);
        }
    }

    private void updateGlBalanceWithSums(AccountSubject account, Currency currency, BigDecimal debitSum, BigDecimal creditSum, LocalDate date) {
        YearMonth period = YearMonth.from(date);
        GlBalance glBalance = glBalanceRepository.findByAccountSubjectAndCurrencyAndBalanceDateAndPeriod(account, currency, date, period)
                .orElseGet(() -> {
                    GlBalance newBal = new GlBalance();
                    newBal.setAccountSubject(account);
                    newBal.setCurrency(currency);
                    newBal.setBalanceDate(date);
                    newBal.setPeriod(period);
                    glBalanceRepository.findFirstByAccountSubjectAndCurrencyAndBalanceDateBeforeOrderByBalanceDateDesc(account, currency, date)
                            .ifPresent(prev -> newBal.setBeginningBalance(prev.getEndingBalance()));
                    return newBal;
                });
        glBalance.addDebit(debitSum);
        glBalance.addCredit(creditSum);
        glBalanceRepository.save(glBalance);
    }

    private void updateSlBalanceWithSums(AccountSubject account, BusinessPartner partner, Department dept, Currency currency, BigDecimal debitSum, BigDecimal creditSum, LocalDate date) {
        YearMonth period = YearMonth.from(date);
        SlBalance slBalance = slBalanceRepository.findByAccountSubjectAndBusinessPartnerAndDepartmentAndCurrencyAndBalanceDateAndPeriod(account, partner, dept, currency, date, period)
                .orElseGet(() -> {
                    SlBalance newBal = new SlBalance();
                    newBal.setAccountSubject(account);
                    newBal.setBusinessPartner(partner);
                    newBal.setDepartment(dept);
                    newBal.setCurrency(currency);
                    newBal.setBalanceDate(date);
                    newBal.setPeriod(period);
                    slBalanceRepository.findFirstByAccountSubjectAndBusinessPartnerAndDepartmentAndCurrencyAndBalanceDateBeforeOrderByBalanceDateDesc(account, partner, dept, currency, date)
                            .ifPresent(prev -> newBal.setBeginningBalance(prev.getEndingBalance()));
                    return newBal;
                });
        slBalance.addDebit(debitSum);
        slBalance.addCredit(creditSum);
        slBalanceRepository.save(slBalance);
    }

    private record GlBalanceKey(AccountSubject account, Currency currency) {}
    private record SlBalanceKey(AccountSubject account, BusinessPartner partner, Department dept, Currency currency) {}

    /**
     * GL Balance 목록을 조회하고 집계합니다.
     *
     * [업무 설명]
     * 특정 기간, 계정과목, 통화 조건으로 총계정원장 잔액을 조회합니다.
     * 시산표(Trial Balance), 재무상태표, 손익계산서 작성 화면에서 사용됩니다.
     * 같은 계정과목+통화의 여러 날짜 잔액을 하나의 집계 레코드로 합산하여 반환합니다.
     *
     * [개발 설명]
     * 조건 파라미터 null 허용: null이면 해당 조건을 무시합니다.
     * 예: accountSubject=null, currency=null → 기간 내 전체 계정/통화 조회
     *
     * @param startDate      조회 시작일 (포함)
     * @param endDate        조회 종료일 (포함)
     * @param accountSubject 특정 계정과목 필터 (null = 전체)
     * @param currency       특정 통화 필터 (null = 전체)
     * @return 집계된 GL Balance 목록 (계정과목+통화 단위로 합산)
     */
    @Transactional(readOnly = true)
    public List<GlBalance> getGlBalances(LocalDate startDate,
                                         LocalDate endDate,
                                         AccountSubject accountSubject,
                                         Currency currency) {
        // 조건 조합에 따라 조회 방법 분기
        List<GlBalance> balances;
        if (accountSubject != null && currency != null) {
            // 계정과목 + 통화 모두 지정: 인덱스 최적화 쿼리 사용
            balances = glBalanceRepository.findByBalanceDateBetweenAndAccountSubjectAndCurrency(
                    startDate, endDate, accountSubject, currency);
        } else if (accountSubject != null) {
            // 계정과목만 지정: 전체 조회 후 인-메모리 필터
            balances = glBalanceRepository.findByBalanceDateBetween(startDate, endDate).stream()
                    .filter(balance -> balance.getAccountSubject().equals(accountSubject))
                    .collect(Collectors.toList());
        } else if (currency != null) {
            // 통화만 지정: 전체 조회 후 인-메모리 필터
            balances = glBalanceRepository.findByBalanceDateBetween(startDate, endDate).stream()
                    .filter(balance -> balance.getCurrency().equals(currency))
                    .collect(Collectors.toList());
        } else {
            // 조건 없음: 기간 내 전체 조회
            balances = glBalanceRepository.findByBalanceDateBetween(startDate, endDate);
        }
        // 계정과목+통화 키로 집계 (여러 날짜 레코드를 하나로 합산)
        return aggregateGlBalances(balances, startDate);
    }

    /**
     * SL Balance 목록을 조회하고 집계합니다.
     *
     * [업무 설명]
     * 거래처별, 부서별 잔액 현황을 조회합니다.
     * 채권·채무 잔액 현황표, 부서별 비용 현황 화면에서 사용됩니다.
     *
     * [개발 설명]
     * 현재 구현: 전체 조회 후 인-메모리 필터 방식입니다.
     * 데이터가 많아지면 JPA 쿼리로 DB 레벨 필터링하도록 최적화가 필요합니다.
     *
     * @param startDate       조회 시작일 (포함)
     * @param endDate         조회 종료일 (포함)
     * @param accountSubject  계정과목 필터 (null = 전체)
     * @param businessPartner 거래처 필터 (null = 전체)
     * @param department      부서 필터 (null = 전체)
     * @param currency        통화 필터 (null = 전체)
     * @return 집계된 SL Balance 목록
     */
    @Transactional(readOnly = true)
    public List<SlBalance> getSlBalances(LocalDate startDate,
                                         LocalDate endDate,
                                         AccountSubject accountSubject,
                                         BusinessPartner businessPartner,
                                         Department department,
                                         Currency currency) {
        // 기간 내 전체 SL Balance 조회 후 조건별 인-메모리 필터 적용
        List<SlBalance> results = slBalanceRepository.findByBalanceDateBetween(startDate, endDate);

        if (accountSubject != null) {
            results = results.stream()
                    .filter(balance -> balance.getAccountSubject() != null
                            && accountSubject.getCode().equals(balance.getAccountSubject().getCode()))
                    .collect(Collectors.toList());
        }
        if (businessPartner != null) {
            results = results.stream()
                    .filter(balance -> balance.getBusinessPartner() != null
                            && businessPartner.getBusinessPartnerCode()
                                    .equals(balance.getBusinessPartner().getBusinessPartnerCode()))
                    .collect(Collectors.toList());
        }
        if (department != null) {
            results = results.stream()
                    .filter(balance -> balance.getDepartment() != null
                            && department.getCode().equals(balance.getDepartment().getCode()))
                    .collect(Collectors.toList());
        }
        if (currency != null) {
            results = results.stream()
                    .filter(balance -> balance.getCurrency() != null
                            && currency.getCode().equals(balance.getCurrency().getCode()))
                    .collect(Collectors.toList());
        }

        // 계정과목+거래처+부서+통화 키로 집계 (여러 날짜 레코드를 하나로 합산)
        return aggregateSlBalances(results, startDate);
    }

    /**
     * GL Balance 목록을 계정과목+통화 단위로 집계합니다.
     *
     * [업무 설명]
     * 조회 기간 내 동일 계정과목의 여러 날짜 잔액 레코드를
     * 하나의 집계 레코드(기초잔액 합산, 차변 합산, 대변 합산, 기말잔액 재계산)로 변환합니다.
     * 시산표의 "계정과목별 합계" 표현에 사용됩니다.
     *
     * [개발 설명]
     * LinkedHashMap으로 입력 순서를 유지합니다.
     * 집계 키: "계정코드|통화코드" (예: "10100|KRW")
     *
     * @param balances    집계할 GL Balance 목록
     * @param balanceDate 집계 결과의 대표 날짜
     * @return 집계된 GL Balance 목록
     */
    private List<GlBalance> aggregateGlBalances(List<GlBalance> balances, LocalDate balanceDate) {
        Map<String, GlBalance> aggregated = new LinkedHashMap<>();
        for (GlBalance balance : balances) {
            // 집계 키: "계정코드|통화코드"
            String key = balance.getAccountSubject().getCode() + "|"
                    + (balance.getCurrency() != null ? balance.getCurrency().getCode() : "");
            // 키가 없으면 신규 집계 객체 생성, 있으면 기존 객체에 누적
            GlBalance target = aggregated.computeIfAbsent(key, ignored -> createGlAggregate(balance, balanceDate));
            mergeIntoGlBalance(target, balance);
        }
        return new ArrayList<>(aggregated.values());
    }

    /**
     * SL Balance 목록을 계정과목+거래처+부서+통화 단위로 집계합니다.
     *
     * @param balances    집계할 SL Balance 목록
     * @param balanceDate 집계 결과의 대표 날짜
     * @return 집계된 SL Balance 목록
     */
    private List<SlBalance> aggregateSlBalances(List<SlBalance> balances, LocalDate balanceDate) {
        Map<String, SlBalance> aggregated = new LinkedHashMap<>();
        for (SlBalance balance : balances) {
            // 집계 키: "계정코드|거래처코드|부서코드|통화코드"
            String key = balance.getAccountSubject().getCode() + "|"
                    + (balance.getBusinessPartner() != null ? balance.getBusinessPartner().getBusinessPartnerCode() : "") + "|"
                    + (balance.getDepartment() != null ? balance.getDepartment().getCode() : "") + "|"
                    + (balance.getCurrency() != null ? balance.getCurrency().getCode() : "");
            SlBalance target = aggregated.computeIfAbsent(key, ignored -> createSlAggregate(balance, balanceDate));
            mergeIntoSlBalance(target, balance);
        }
        return new ArrayList<>(aggregated.values());
    }

    /**
     * GL Balance 집계용 빈 객체를 생성합니다.
     * 모든 금액 필드는 0으로 초기화됩니다 (mergeIntoGlBalance로 누적 예정).
     */
    private GlBalance createGlAggregate(GlBalance source, LocalDate balanceDate) {
        GlBalance aggregated = new GlBalance();
        aggregated.setAccountSubject(source.getAccountSubject());
        aggregated.setCurrency(source.getCurrency());
        aggregated.setBalanceDate(balanceDate);
        aggregated.setPeriod(YearMonth.from(balanceDate));
        aggregated.setBeginningBalance(BigDecimal.ZERO);
        aggregated.setDebitAmount(BigDecimal.ZERO);
        aggregated.setCreditAmount(BigDecimal.ZERO);
        aggregated.setEndingBalance(BigDecimal.ZERO);
        return aggregated;
    }

    /**
     * SL Balance 집계용 빈 객체를 생성합니다.
     */
    private SlBalance createSlAggregate(SlBalance source, LocalDate balanceDate) {
        SlBalance aggregated = new SlBalance();
        aggregated.setAccountSubject(source.getAccountSubject());
        aggregated.setBusinessPartner(source.getBusinessPartner());
        aggregated.setDepartment(source.getDepartment());
        aggregated.setCurrency(source.getCurrency());
        aggregated.setBalanceDate(balanceDate);
        aggregated.setPeriod(YearMonth.from(balanceDate));
        aggregated.setBeginningBalance(BigDecimal.ZERO);
        aggregated.setDebitAmount(BigDecimal.ZERO);
        aggregated.setCreditAmount(BigDecimal.ZERO);
        aggregated.setEndingBalance(BigDecimal.ZERO);
        return aggregated;
    }

    /**
     * source GL Balance의 금액을 target에 누적합니다.
     * 기초잔액, 차변합계, 대변합계를 더하고 기말잔액을 재계산합니다.
     */
    private void mergeIntoGlBalance(GlBalance target, GlBalance source) {
        target.setBeginningBalance(target.getBeginningBalance().add(zeroIfNull(source.getBeginningBalance())));
        target.setDebitAmount(target.getDebitAmount().add(zeroIfNull(source.getDebitAmount())));
        target.setCreditAmount(target.getCreditAmount().add(zeroIfNull(source.getCreditAmount())));
        // 기말잔액 재계산: endingBalance = beginningBalance + debitAmount - creditAmount
        target.recalculate();
    }

    /**
     * source SL Balance의 금액을 target에 누적합니다.
     */
    private void mergeIntoSlBalance(SlBalance target, SlBalance source) {
        target.setBeginningBalance(target.getBeginningBalance().add(zeroIfNull(source.getBeginningBalance())));
        target.setDebitAmount(target.getDebitAmount().add(zeroIfNull(source.getDebitAmount())));
        target.setCreditAmount(target.getCreditAmount().add(zeroIfNull(source.getCreditAmount())));
        target.recalculate();
    }

    /**
     * null 안전 처리: null이면 BigDecimal.ZERO 반환.
     * BigDecimal.add(null)은 NullPointerException이 발생하므로 이 메서드로 방어합니다.
     */
    private BigDecimal zeroIfNull(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}

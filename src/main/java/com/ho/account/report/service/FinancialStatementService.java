package com.ho.account.report.service;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.journal.domain.JournalDetail;
import com.ho.account.journal.domain.JournalEntryStatus;
import com.ho.account.journal.repository.JournalDetailRepository;
import com.ho.account.report.dto.FinancialStatementDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 재무제표(재무상태표, 손익계산서) 생성을 담당하는 서비스 클래스입니다.
 * 
 * @Service: 스프링 컨테이너에 빈(Bean)으로 등록되어 비즈니스 로직을 처리함을 나타냅니다.
 * @Transactional(readOnly = true): 이 클래스의 모든 메서드는 기본적으로 읽기 전용 트랜잭션으로 실행되어 성능을
 *                         최적화합니다.
 */
@Service
@Transactional(readOnly = true)
public class FinancialStatementService {

    private final JournalDetailRepository journalDetailRepository;
    private final AccountSubjectRepository accountSubjectRepository;

    // 생성자 주입 방식: 의존성을 명시적으로 주입받아 테스트 용이성과 불변성을 확보합니다.
    @Autowired
    public FinancialStatementService(JournalDetailRepository journalDetailRepository,
            AccountSubjectRepository accountSubjectRepository) {
        this.journalDetailRepository = journalDetailRepository;
        this.accountSubjectRepository = accountSubjectRepository;
    }

    /**
     * 재무상태표(Balance Sheet) 생성
     * 특정 시점(asOfDate)까지의 자산, 부채, 자본 계정 잔액을 집계합니다.
     *
     * @param asOfDate 기준일 (보통 기말)
     * @return 재무상태표 항목 리스트
     */
    public List<FinancialStatementDTO> generateBalanceSheet(LocalDate asOfDate) {
        // 1. 기준일 이전의 모든 승인된 전표 상세 내역 조회 (자산, 부채, 자본 계정만 필터링 필요하지만 여기선 전체 조회 후 로직 처리)
        // 실제 운영 환경에서는 쿼리 레벨에서 필터링하는 것이 성능상 유리합니다.
        List<JournalDetail> details = journalDetailRepository.findAll().stream()
                .filter(d -> d.getJournalEntry().getAccountingDate().compareTo(asOfDate) <= 0) // 기준일 이전
                .filter(d -> JournalEntryStatus.APPROVED.equals(d.getJournalEntry().getStatus())) // 승인된 전표만
                .collect(Collectors.toList());

        // 2. 계정별 잔액 계산
        // Map<계정코드, 잔액> 형태로 집계
        Map<String, BigDecimal> balanceMap = details.stream()
                .filter(d -> isBalanceSheetAccount(d.getAccountSubject().getCategory().name())) // BS 계정만 필터링
                .collect(Collectors.toMap(
                        d -> d.getAccountSubject().getCode(), // Key: 계정코드
                        d -> calculateSignedAmount(d), // Value: 부호가 적용된 금액
                        BigDecimal::add // Merge Function: 같은 키가 있을 경우 금액 합산
                ));

        // 3. DTO 변환
        List<FinancialStatementDTO> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : balanceMap.entrySet()) {
            AccountSubject account = accountSubjectRepository.findById(entry.getKey()).orElse(null);
            if (account != null) {
                result.add(new FinancialStatementDTO(account.getCode(), account.getName(), entry.getValue()));
            }
        }
        return result;
    }

    /**
     * 손익계산서(Income Statement) 생성
     * 특정 기간(startDate ~ endDate) 동안의 수익, 비용 계정 발생액을 집계합니다.
     *
     * @param startDate 시작일
     * @param endDate   종료일
     * @return 손익계산서 항목 리스트
     */
    public List<FinancialStatementDTO> generateIncomeStatement(LocalDate startDate, LocalDate endDate) {
        // 1. 기간 내 승인된 전표 상세 내역 조회
        List<JournalDetail> details = journalDetailRepository.findByAccountAndDateRangeForIS(startDate, endDate); // 별도
                                                                                                                  // 쿼리
                                                                                                                  // 메서드
                                                                                                                  // 필요

        // 2. 계정별 합계 계산
        Map<String, BigDecimal> sumMap = details.stream()
                .filter(d -> isIncomeStatementAccount(d.getAccountSubject().getCategory().name())) // IS 계정만 필터링
                .collect(Collectors.toMap(
                        d -> d.getAccountSubject().getCode(),
                        d -> d.getAmount(), // IS는 발생액 기준이므로 차/대 구분 없이 합산 (단, 수익은 대변, 비용은 차변이 정상 잔액)
                        BigDecimal::add));

        // 3. DTO 변환
        List<FinancialStatementDTO> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : sumMap.entrySet()) {
            AccountSubject account = accountSubjectRepository.findById(entry.getKey()).orElse(null);
            if (account != null) {
                result.add(new FinancialStatementDTO(account.getCode(), account.getName(), entry.getValue()));
            }
        }
        return result;
    }

    // 헬퍼 메서드: 차대변 구분에 따른 부호 계산 (자산: 차변+, 대변- / 부채,자본: 대변+, 차변-)
    private BigDecimal calculateSignedAmount(JournalDetail detail) {
        String type = detail.getAccountSubject().getCategory().name();
        boolean isDebitPositive = AccountSubject.AccountCategory.ASSETS.name().equals(type); // 자산은 차변이 증가

        if ("DEBIT".equals(detail.getDrcrType())) {
            return isDebitPositive ? detail.getAmount() : detail.getAmount().negate();
        } else {
            return isDebitPositive ? detail.getAmount().negate() : detail.getAmount();
        }
    }

    // 헬퍼 메서드: BS 계정 여부 확인
    private boolean isBalanceSheetAccount(String type) {
        return AccountSubject.AccountCategory.ASSETS.name().equals(type) ||
                AccountSubject.AccountCategory.LIABILITIES.name().equals(type) ||
                AccountSubject.AccountCategory.EQUITY.name().equals(type);
    }

    // 헬퍼 메서드: IS 계정 여부 확인
    private boolean isIncomeStatementAccount(String type) {
        return AccountSubject.AccountCategory.REVENUE.name().equals(type) ||
                AccountSubject.AccountCategory.EXPENSES.name().equals(type);
    }
}

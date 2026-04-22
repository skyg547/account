package com.ho.account.journalledger.adapter.in.web.ledger;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.domain.Department;
import com.ho.account.basic.domain.Currency;
import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.basic.repository.BusinessPartnerRepository;
import com.ho.account.basic.repository.DepartmentRepository;
import com.ho.account.basic.repository.CurrencyRepository;
import com.ho.account.journalledger.domain.ledger.GlBalance;
import com.ho.account.journalledger.domain.ledger.SlBalance;
import com.ho.account.journalledger.application.service.ledger.LedgerService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * GL(총계?�원?? �?SL(보조?�장) ?�이?��? 조회?�고 관리하??REST 컨트롤러.
 */
@RestController
@RequestMapping("/api/ledger")
public class LedgerController {

    private final LedgerService ledgerService;
    private final AccountSubjectRepository accountSubjectRepository;
    private final BusinessPartnerRepository businessPartnerRepository;
    private final DepartmentRepository departmentRepository;
    private final CurrencyRepository currencyRepository;

    public LedgerController(LedgerService ledgerService,
                            AccountSubjectRepository accountSubjectRepository,
                            BusinessPartnerRepository businessPartnerRepository,
                            DepartmentRepository departmentRepository,
                            CurrencyRepository currencyRepository) {
        this.ledgerService = ledgerService;
        this.accountSubjectRepository = accountSubjectRepository;
        this.businessPartnerRepository = businessPartnerRepository;
        this.departmentRepository = departmentRepository;
        this.currencyRepository = currencyRepository;
    }

    /**
     * ?�정 기간 ?�안??GL ?�액??조회?�니??
     * @param startDate 조회 ?�작??
     * @param endDate 조회 종료??
     * @param accountCode 계정과목 코드 (?�택 ?�항)
     * @param currencyCode ?�화 코드 (?�택 ?�항)
     * @return GL ?�액 목록
     */
    @GetMapping("/gl-balances")
    public ResponseEntity<List<GlBalance>> getGlBalances(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "accountCode", required = false) String accountCode,
            @RequestParam(value = "currencyCode", required = false) String currencyCode) {

        AccountSubject accountSubject = null;
        if (accountCode != null) {
            accountSubject = accountSubjectRepository.findById(accountCode)
                    .orElseThrow(() -> new IllegalArgumentException("AccountSubject not found with code: " + accountCode));
        }

        Currency currency = null;
        if (currencyCode != null) {
            currency = currencyRepository.findById(currencyCode)
                    .orElseThrow(() -> new IllegalArgumentException("Currency not found with code: " + currencyCode));
        }

        List<GlBalance> glBalances = ledgerService.getGlBalances(startDate, endDate, accountSubject, currency);
        return ResponseEntity.ok(glBalances);
    }

    /**
     * ?�정 기간 ?�안??SL ?�액??조회?�니??
     * @param startDate 조회 ?�작??
     * @param endDate 조회 종료??
     * @param accountCode 계정과목 코드 (?�택 ?�항)
     * @param businessPartnerCode 거래�?코드 (?�택 ?�항)
     * @param deptCode 부??코드 (?�택 ?�항)
     * @param currencyCode ?�화 코드 (?�택 ?�항)
     * @return SL ?�액 목록
     */
    @GetMapping("/sl-balances")
    public ResponseEntity<List<SlBalance>> getSlBalances(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "accountCode", required = false) String accountCode,
            @RequestParam(value = "businessPartnerCode", required = false) String businessPartnerCode,
            @RequestParam(value = "deptCode", required = false) String deptCode,
            @RequestParam(value = "currencyCode", required = false) String currencyCode) {

        AccountSubject accountSubject = null;
        if (accountCode != null) {
            accountSubject = accountSubjectRepository.findById(accountCode)
                    .orElseThrow(() -> new IllegalArgumentException("AccountSubject not found with code: " + accountCode));
        }

        BusinessPartner businessPartner = null;
        if (businessPartnerCode != null) {
            businessPartner = businessPartnerRepository.findByBusinessPartnerCode(businessPartnerCode)
                    .orElseThrow(() -> new IllegalArgumentException("BusinessPartner not found with code: " + businessPartnerCode));
        }

        Department department = null;
        if (deptCode != null) {
            department = departmentRepository.findByCode(deptCode)
                    .orElseThrow(() -> new IllegalArgumentException("Department not found with code: " + deptCode));
        }

        Currency currency = null;
        if (currencyCode != null) {
            currency = currencyRepository.findById(currencyCode)
                    .orElseThrow(() -> new IllegalArgumentException("Currency not found with code: " + currencyCode));
        }

        List<SlBalance> slBalances = ledgerService.getSlBalances(startDate, endDate, accountSubject, businessPartner, department, currency);
        return ResponseEntity.ok(slBalances);
    }

    /**
     * GL �?SL ?�액???�집계합?�다. (마감 배치 ?�책)
     * @param startDate ?�집�??�작??
     * @param endDate ?�집�?종료??
     * @return ?�공 메시지
     */
    @PostMapping("/reaggregate-balances")
    public ResponseEntity<String> reaggregateBalances(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        ledgerService.reaggregateLedgerBalancesForPeriod(startDate, endDate);
        return ResponseEntity.ok("Ledger balances re-aggregated successfully for the period " + startDate + " to " + endDate);
    }
}

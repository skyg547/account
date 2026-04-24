package com.ho.account.journalledger.adapter.in.web.ledger;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Department;
import com.ho.account.masterdata.core.domain.model.Currency;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.application.port.out.DepartmentPersistencePort;
import com.ho.account.masterdata.core.application.port.out.CurrencyPersistencePort;
import com.ho.account.journalledger.core.domain.ledger.domain.GlBalance;
import com.ho.account.journalledger.core.domain.ledger.domain.SlBalance;
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
    private final accountSubjectPersistencePort accountSubjectPersistencePort;
    private final businessPartnerPersistencePort businessPartnerPersistencePort;
    private final departmentPersistencePort departmentPersistencePort;
    private final currencyPersistencePort currencyPersistencePort;

    public LedgerController(LedgerService ledgerService,
                            accountSubjectPersistencePort accountSubjectPersistencePort,
                            businessPartnerPersistencePort businessPartnerPersistencePort,
                            departmentPersistencePort departmentPersistencePort,
                            currencyPersistencePort currencyPersistencePort) {
        this.ledgerService = ledgerService;
        this.accountSubjectPersistencePort = accountSubjectPersistencePort;
        this.businessPartnerPersistencePort = businessPartnerPersistencePort;
        this.departmentPersistencePort = departmentPersistencePort;
        this.currencyPersistencePort = currencyPersistencePort;
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
            accountSubject = accountSubjectPersistencePort.findById(accountCode)
                    .orElseThrow(() -> new IllegalArgumentException("AccountSubject not found with code: " + accountCode));
        }

        Currency currency = null;
        if (currencyCode != null) {
            currency = currencyPersistencePort.findById(currencyCode)
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
            accountSubject = accountSubjectPersistencePort.findById(accountCode)
                    .orElseThrow(() -> new IllegalArgumentException("AccountSubject not found with code: " + accountCode));
        }

        BusinessPartner businessPartner = null;
        if (businessPartnerCode != null) {
            businessPartner = businessPartnerPersistencePort.findByBusinessPartnerCode(businessPartnerCode)
                    .orElseThrow(() -> new IllegalArgumentException("BusinessPartner not found with code: " + businessPartnerCode));
        }

        Department department = null;
        if (deptCode != null) {
            department = departmentPersistencePort.findByCode(deptCode)
                    .orElseThrow(() -> new IllegalArgumentException("Department not found with code: " + deptCode));
        }

        Currency currency = null;
        if (currencyCode != null) {
            currency = currencyPersistencePort.findById(currencyCode)
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

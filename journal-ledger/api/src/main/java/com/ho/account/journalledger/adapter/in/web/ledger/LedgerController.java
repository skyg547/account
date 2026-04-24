package com.ho.account.journalledger.adapter.in.web.ledger;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Department;
import com.ho.account.masterdata.core.domain.model.Currency;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.application.port.out.DepartmentPersistencePort;
import com.ho.account.masterdata.core.application.port.out.CurrencyPersistencePort;
import com.ho.account.journalledger.domain.ledger.domain.GlBalance;
import com.ho.account.journalledger.domain.ledger.domain.SlBalance;
import com.ho.account.journalledger.application.service.ledger.LedgerService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * GL(총계정원장) 및 SL(보조원장) 데이터를 조회하고 관리하는 REST 컨트롤러.
 */
@RestController
@RequestMapping("/api/ledger")
public class LedgerController {

    private final LedgerService ledgerService;
    private final AccountSubjectPersistencePort accountSubjectPersistencePort;
    private final BusinessPartnerPersistencePort businessPartnerPersistencePort;
    private final DepartmentPersistencePort departmentPersistencePort;
    private final CurrencyPersistencePort currencyPersistencePort;

    public LedgerController(LedgerService ledgerService,
                            AccountSubjectPersistencePort accountSubjectPersistencePort,
                            BusinessPartnerPersistencePort businessPartnerPersistencePort,
                            DepartmentPersistencePort departmentPersistencePort,
                            CurrencyPersistencePort currencyPersistencePort) {
        this.ledgerService = ledgerService;
        this.accountSubjectPersistencePort = accountSubjectPersistencePort;
        this.businessPartnerPersistencePort = businessPartnerPersistencePort;
        this.departmentPersistencePort = departmentPersistencePort;
        this.currencyPersistencePort = currencyPersistencePort;
    }

    /**
     * 특정 기간 동안의 GL 잔액을 조회합니다.
     * @param startDate 조회 시작일
     * @param endDate 조회 종료일
     * @param accountCode 계정과목 코드 (선택 사항)
     * @param currencyCode 통화 코드 (선택 사항)
     * @return GL 잔액 목록
     */
    @GetMapping("/gl-balances")
    public ResponseEntity<List<GlBalance>> getGlBalances(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "accountCode", required = false) String accountCode,
            @RequestParam(value = "currencyCode", required = false) String currencyCode) {

        AccountSubject accountSubject = null;
        if (accountCode != null) {
            accountSubject = accountSubjectPersistencePort.findByCode(accountCode)
                    .orElseThrow(() -> new IllegalArgumentException("AccountSubject not found with code: " + accountCode));
        }

        Currency currency = null;
        if (currencyCode != null) {
            currency = currencyPersistencePort.findByCode(currencyCode)
                    .orElseThrow(() -> new IllegalArgumentException("Currency not found with code: " + currencyCode));
        }

        List<GlBalance> glBalances = ledgerService.getGlBalances(startDate, endDate, accountSubject, currency);
        return ResponseEntity.ok(glBalances);
    }

    /**
     * 특정 기간 동안의 SL 잔액을 조회합니다.
     * @param startDate 조회 시작일
     * @param endDate 조회 종료일
     * @param accountCode 계정과목 코드 (선택 사항)
     * @param businessPartnerCode 거래처 코드 (선택 사항)
     * @param deptCode 부서 코드 (선택 사항)
     * @param currencyCode 통화 코드 (선택 사항)
     * @return SL 잔액 목록
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
            accountSubject = accountSubjectPersistencePort.findByCode(accountCode)
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
            currency = currencyPersistencePort.findByCode(currencyCode)
                    .orElseThrow(() -> new IllegalArgumentException("Currency not found with code: " + currencyCode));
        }

        List<SlBalance> slBalances = ledgerService.getSlBalances(startDate, endDate, accountSubject, businessPartner, department, currency);
        return ResponseEntity.ok(slBalances);
    }

    /**
     * GL 및 SL 잔액을 재집계합니다. (마감 배치 정책)
     * @param startDate 재집계 시작일
     * @param endDate 재집계 종료일
     * @return 성공 메시지
     */
    @PostMapping("/reaggregate-balances")
    public ResponseEntity<String> reaggregateBalances(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        ledgerService.reaggregateLedgerBalancesForPeriod(startDate, endDate);
        return ResponseEntity.ok("Ledger balances re-aggregated successfully for the period " + startDate + " to " + endDate);
    }
}

package com.ho.account.journalledger.adapter.in.web.ledger;

import com.ho.account.journalledger.application.service.ledger.LedgerService;
import com.ho.account.journalledger.domain.ledger.domain.GlBalance;
import com.ho.account.journalledger.domain.ledger.domain.SlBalance;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ledger")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerService ledgerService;

    @GetMapping("/gl/balances")
    public ResponseEntity<List<GlBalance>> getGlBalances(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "accountCode", required = false) String accountCode,
            @RequestParam(value = "currencyCode", required = false) String currencyCode) {

        List<GlBalance> glBalances = ledgerService.getGlBalances(startDate, endDate, accountCode, currencyCode);
        return ResponseEntity.ok(glBalances);
    }

    @GetMapping("/sl/balances")
    public ResponseEntity<List<SlBalance>> getSlBalances(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "accountCode", required = false) String accountCode,
            @RequestParam(value = "businessPartnerCode", required = false) String businessPartnerCode,
            @RequestParam(value = "deptCode", required = false) String deptCode,
            @RequestParam(value = "currencyCode", required = false) String currencyCode) {

        List<SlBalance> slBalances = ledgerService.getSlBalances(startDate, endDate, accountCode, businessPartnerCode, deptCode, currencyCode);
        return ResponseEntity.ok(slBalances);
    }
}
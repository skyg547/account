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
@RequestMapping("/api/v1/ledger/balances")
@RequiredArgsConstructor
public class GlSlController {

    private final LedgerService ledgerService;

    @GetMapping("/gl")
    public ResponseEntity<List<GlBalance>> getGlBalances(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String accountCode) {

        List<GlBalance> balances = ledgerService.getGlBalances(startDate, endDate, accountCode, null);
        return ResponseEntity.ok(balances);
    }

    @GetMapping("/sl")
    public ResponseEntity<List<SlBalance>> getSlBalances(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String accountCode,
            @RequestParam(required = false) String businessPartnerCode) {

        List<SlBalance> balances = ledgerService.getSlBalances(startDate, endDate, accountCode, businessPartnerCode, null, null);
        return ResponseEntity.ok(balances);
    }
}
package com.ho.account.ledger.web;

import com.ho.account.ledger.dto.LedgerDTO;
import com.ho.account.ledger.service.LedgerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/ledgers")
public class LedgerController {

    private final LedgerService ledgerService;

    @Autowired
    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    // 총계정원장 조회
    @GetMapping("/{accountCode}")
    public List<LedgerDTO> getGeneralLedger(
            @PathVariable String accountCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ledgerService.getGeneralLedger(accountCode, startDate, endDate);
    }
}

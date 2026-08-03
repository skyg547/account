package com.ho.account.masterdata.api.web;

import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.FiscalPeriodRef;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/basic/fiscal-periods")
public class FiscalPeriodController {

    private final FiscalPeriodControlPort fiscalPeriodControlPort;

    public FiscalPeriodController(FiscalPeriodControlPort fiscalPeriodControlPort) {
        this.fiscalPeriodControlPort = fiscalPeriodControlPort;
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<FiscalPeriodRef> findById(@PathVariable Long id) {
        return fiscalPeriodControlPort.findFiscalPeriodById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{fiscalYear}/{fiscalPeriod}")
    public ResponseEntity<FiscalPeriodRef> findByPeriod(
            @PathVariable String fiscalYear,
            @PathVariable String fiscalPeriod) {
        return fiscalPeriodControlPort.findFiscalPeriod(fiscalYear, fiscalPeriod)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

}

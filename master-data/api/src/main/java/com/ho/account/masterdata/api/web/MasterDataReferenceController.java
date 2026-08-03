package com.ho.account.masterdata.api.web;

import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.DepartmentRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Date-explicit reference API used by independently deployed business services. */
@RestController
@RequestMapping("/api/basic/references")
public class MasterDataReferenceController {

    private final MasterDataQueryPort masterDataQueryPort;

    public MasterDataReferenceController(MasterDataQueryPort masterDataQueryPort) {
        this.masterDataQueryPort = masterDataQueryPort;
    }

    @GetMapping("/account-subjects/{code}")
    public ResponseEntity<AccountSubjectRef> findAccountSubjectAt(
            @PathVariable String code,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveDate) {
        return masterDataQueryPort.findAccountSubjectAt(code, effectiveDate)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/business-partners/{code}")
    public ResponseEntity<BusinessPartnerRef> findBusinessPartnerAt(
            @PathVariable String code,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveDate) {
        return masterDataQueryPort.findBusinessPartnerAt(code, effectiveDate)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/departments/{code}")
    public ResponseEntity<DepartmentRef> findDepartmentAt(
            @PathVariable String code,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveDate) {
        return masterDataQueryPort.findDepartmentAt(code, effectiveDate)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

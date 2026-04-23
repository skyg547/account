package com.ho.account.asset.web;

import com.ho.account.asset.domain.FixedAsset;
import com.ho.account.asset.dto.FixedAssetDisposalRequest;
import com.ho.account.asset.dto.FixedAssetRequest;
import com.ho.account.asset.repository.FixedAssetRepository;
import com.ho.account.asset.service.FixedAssetService;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.DepartmentPersistencePort;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 고정자산 관리 컨트롤러
 */
@RestController
@RequestMapping("/api/fixed-assets")
public class FixedAssetController {

    private final FixedAssetService fixedAssetService;
    private final FixedAssetRepository fixedAssetRepository;
    private final AccountSubjectPersistencePort accountSubjectPersistencePort;
    private final DepartmentPersistencePort departmentPersistencePort;

    public FixedAssetController(FixedAssetService fixedAssetService,
                                FixedAssetRepository fixedAssetRepository,
                                AccountSubjectPersistencePort accountSubjectPersistencePort,
                                DepartmentPersistencePort departmentPersistencePort) {
        this.fixedAssetService = fixedAssetService;
        this.fixedAssetRepository = fixedAssetRepository;
        this.accountSubjectPersistencePort = accountSubjectPersistencePort;
        this.departmentPersistencePort = departmentPersistencePort;
    }

    @PostMapping
    public ResponseEntity<FixedAsset> registerFixedAsset(@Valid @RequestBody FixedAssetRequest request) {
        FixedAsset fixedAsset = new FixedAsset();
        fixedAsset.setAssetCode(request.getAssetCode());
        fixedAsset.setAssetName(request.getAssetName());
        fixedAsset.setAcquisitionDate(request.getAcquisitionDate());
        fixedAsset.setAcquisitionCost(request.getAcquisitionCost());
        fixedAsset.setUsefulLife(request.getUsefulLife());
        fixedAsset.setDepreciationMethod(request.getDepreciationMethod());
        fixedAsset.setResidualValue(request.getResidualValue());

        fixedAsset.setAccountSubject(accountSubjectPersistencePort.findByCode(request.getAccountSubjectCode())
                .orElseThrow(() -> new IllegalArgumentException("자산 계정 과목을 찾을 수 없습니다: " + request.getAccountSubjectCode())));
        fixedAsset.setAccumulatedAccount(accountSubjectPersistencePort.findByCode(request.getAccumulatedAccountCode())
                .orElseThrow(() -> new IllegalArgumentException("감가상각누계액 계정 과목을 찾을 수 없습니다: " + request.getAccumulatedAccountCode())));
        fixedAsset.setExpenseAccount(accountSubjectPersistencePort.findByCode(request.getExpenseAccountCode())
                .orElseThrow(() -> new IllegalArgumentException("감가상각비 계정 과목을 찾을 수 없습니다: " + request.getExpenseAccountCode())));
        fixedAsset.setDepartment(departmentPersistencePort.findByCode(request.getDepartmentCode())
                .orElseThrow(() -> new IllegalArgumentException("관리 부서를 찾을 수 없습니다: " + request.getDepartmentCode())));

        FixedAsset registeredAsset = fixedAssetService.registerAsset(fixedAsset);
        return new ResponseEntity<>(registeredAsset, HttpStatus.CREATED);
    }

    @PostMapping("/depreciate/{processDate}")
    public ResponseEntity<String> runMonthlyDepreciation(@PathVariable LocalDate processDate) {
        fixedAssetService.processMonthlyDepreciation(processDate);
        return ResponseEntity.ok("Monthly depreciation processed for " + processDate);
    }

    @PostMapping("/dispose")
    public ResponseEntity<FixedAsset> disposeFixedAsset(@Valid @RequestBody FixedAssetDisposalRequest request) {
        FixedAsset disposedAsset = fixedAssetService.disposeFixedAsset(
                request.getAssetId(),
                request.getDisposalDate(),
                request.getSalePrice()
        );
        return ResponseEntity.ok(disposedAsset);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FixedAsset> getFixedAssetById(@PathVariable Long id) {
        return fixedAssetRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

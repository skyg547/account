package com.ho.account.asset.web;

import com.ho.account.asset.application.port.in.FixedAssetUseCase;
import com.ho.account.asset.domain.FixedAsset;
import com.ho.account.asset.dto.FixedAssetDisposalRequest;
import com.ho.account.asset.dto.FixedAssetRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 고정자산 관리 컨트롤러 (Inbound Adapter)
 */
@RestController
@RequestMapping("/api/fixed-assets")
@RequiredArgsConstructor
public class FixedAssetController {

    private final FixedAssetUseCase fixedAssetUseCase;

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

        fixedAsset.setAccountCode(request.getAccountSubjectCode());
        fixedAsset.setAccumulatedAccountCode(request.getAccumulatedAccountCode());
        fixedAsset.setExpenseAccountCode(request.getExpenseAccountCode());
        fixedAsset.setDepartmentCode(request.getDepartmentCode());

        FixedAsset registeredAsset = fixedAssetUseCase.registerAsset(fixedAsset);
        return new ResponseEntity<>(registeredAsset, HttpStatus.CREATED);
    }

    @PostMapping("/depreciate/{processDate}")
    public ResponseEntity<String> runMonthlyDepreciation(@PathVariable("processDate") LocalDate processDate) {
        fixedAssetUseCase.processMonthlyDepreciation(processDate);
        return ResponseEntity.ok("Monthly depreciation processed for " + processDate);
    }

    @PostMapping("/dispose")
    public ResponseEntity<FixedAsset> disposeFixedAsset(@Valid @RequestBody FixedAssetDisposalRequest request) {
        FixedAsset disposedAsset = fixedAssetUseCase.disposeFixedAsset(
                request.getAssetId(),
                request.getDisposalDate(),
                request.getSalePrice()
        );
        return ResponseEntity.ok(disposedAsset);
    }

    @GetMapping
    public ResponseEntity<List<FixedAsset>> getAllFixedAssets(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(fixedAssetUseCase.findByStatus(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FixedAsset> getFixedAssetById(@PathVariable("id") Long id) {
        return fixedAssetUseCase.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

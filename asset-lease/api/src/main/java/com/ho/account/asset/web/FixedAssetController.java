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
 * [헥사고날 아키텍처 - 인바운드 어댑터 (Inbound Web Adapter)]
 * 고정자산 관리 기능을 제공하는 웹 컨트롤러입니다.
 * 
 * 🐣 [초보자를 위한 설명]
 * 이 클래스는 시스템의 '자산 관리 창구'입니다.
 * 사용자가 화면에서 "새 노트북을 샀어요"라고 등록하거나 "이번 달 감가상각을 실행해줘"라고 요청하면, 
 * 그 신호를 받아서 내부 비즈니스 서비스(`FixedAssetEntryService`)가 일을 할 수 있게 연결해줍니다.
 * 외부 세계(Web)와 내부 비즈니스 로직(Core) 사이의 다리 역할을 합니다.
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

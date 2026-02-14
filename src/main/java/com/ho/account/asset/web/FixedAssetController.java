package com.ho.account.asset.web;

import com.ho.account.asset.domain.FixedAsset;
import com.ho.account.asset.dto.FixedAssetDisposalRequest;
import com.ho.account.asset.dto.FixedAssetRequest;
import com.ho.account.asset.repository.FixedAssetRepository; // For fetching by ID in GET method
import com.ho.account.asset.service.FixedAssetService;
import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.basic.repository.DepartmentRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/fixed-assets")
public class FixedAssetController {

    private final FixedAssetService fixedAssetService;
    private final FixedAssetRepository fixedAssetRepository; // For convenience in GET by ID
    private final AccountSubjectRepository accountSubjectRepository; // To fetch AccountSubject for FixedAsset conversion
    private final DepartmentRepository departmentRepository; // To fetch Department for FixedAsset conversion

    public FixedAssetController(FixedAssetService fixedAssetService,
                                FixedAssetRepository fixedAssetRepository,
                                AccountSubjectRepository accountSubjectRepository,
                                DepartmentRepository departmentRepository) {
        this.fixedAssetService = fixedAssetService;
        this.fixedAssetRepository = fixedAssetRepository;
        this.accountSubjectRepository = accountSubjectRepository;
        this.departmentRepository = departmentRepository;
    }

    /**
     * 새로운 고정자산을 등록하고 초기 취득 전표를 생성합니다.
     * @param request 고정자산 등록 요청 DTO
     * @return 등록된 고정자산 정보
     */
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

        // AccountSubject, Department 조회 및 설정
        fixedAsset.setAccountSubject(accountSubjectRepository.findById(request.getAccountSubjectCode())
                .orElseThrow(() -> new IllegalArgumentException("자산 계정 과목을 찾을 수 없습니다: " + request.getAccountSubjectCode())));
        fixedAsset.setAccumulatedAccount(accountSubjectRepository.findById(request.getAccumulatedAccountCode())
                .orElseThrow(() -> new IllegalArgumentException("감가상각누계액 계정 과목을 찾을 수 없습니다: " + request.getAccumulatedAccountCode())));
        fixedAsset.setExpenseAccount(accountSubjectRepository.findById(request.getExpenseAccountCode())
                .orElseThrow(() -> new IllegalArgumentException("감가상각비 계정 과목을 찾을 수 없습니다: " + request.getExpenseAccountCode())));
        fixedAsset.setDepartment(departmentRepository.findById(request.getDepartmentCode())
                .orElseThrow(() -> new IllegalArgumentException("관리 부서를 찾을 수 없습니다: " + request.getDepartmentCode())));

        FixedAsset registeredAsset = fixedAssetService.registerAsset(fixedAsset);
        return new ResponseEntity<>(registeredAsset, HttpStatus.CREATED);
    }

    /**
     * 월별 감가상각 처리를 수동으로 트리거합니다. (보통은 스케줄링된 작업으로 실행)
     * @param processDate 처리 기준일
     * @return 처리 결과 메시지
     */
    @PostMapping("/depreciate/{processDate}")
    public ResponseEntity<String> runMonthlyDepreciation(@PathVariable LocalDate processDate) {
        fixedAssetService.processMonthlyDepreciation(processDate);
        return ResponseEntity.ok("Monthly depreciation processed for " + processDate);
    }

    /**
     * 고정자산을 처분하고 관련 전표를 생성합니다.
     * @param request 고정자산 처분 요청 DTO
     * @return 처분된 고정자산 정보
     */
    @PostMapping("/dispose")
    public ResponseEntity<FixedAsset> disposeFixedAsset(@Valid @RequestBody FixedAssetDisposalRequest request) {
        FixedAsset disposedAsset = fixedAssetService.disposeFixedAsset(
                request.getAssetId(),
                request.getDisposalDate(),
                request.getSalePrice()
        );
        return ResponseEntity.ok(disposedAsset);
    }

    /**
     * 특정 고정자산의 상세 정보를 조회합니다.
     * @param id 고정자산 ID
     * @return 고정자산 정보
     */
    @GetMapping("/{id}")
    public ResponseEntity<FixedAsset> getFixedAssetById(@PathVariable Long id) {
        return fixedAssetRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

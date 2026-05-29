package com.ho.account.ecl.api.controller;

import com.ho.account.ecl.core.domain.exposure.CrAccount;
import com.ho.account.ecl.core.application.port.out.CrAccountRepository;
import com.ho.account.shared.finance.dto.ApiResponse;
import com.ho.account.shared.finance.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * [API] IFRS 9 대손충당금 익스포저 및 계좌 관리 컨트롤러.
 *
 * 💡 [초보자를 위한 개념 설명]
 * 익스포저(Exposure)란 기대신용손실 산출 대상 금액을 의미합니다.
 * 고객에게 1억을 빌려주기로 약정(Limit)했으나 현재 고객이 7천만원만 쓰고 있다면(Outstanding),
 * 은행이 대손충당금을 산출해야 하는 '계좌'의 상태를 이 컨트롤러를 통해 확인하고 관리할 수 있습니다.
 */
@RestController
@RequestMapping("/api/v1/ifrs/allowance/exposures")
@RequiredArgsConstructor
public class AllowanceExposureController {

    private final CrAccountRepository accountRepository;
    private final com.ho.account.ecl.core.application.port.out.CrAccountCollateralRepository accountCollateralRepository;


    @GetMapping
    public ResponseEntity<ApiResponse<List<CrAccount>>> getAllAccounts() {
        return ResponseEntity.ok(ApiResponse.success(accountRepository.findByIsActiveTrue()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CrAccount>> getAccount(@PathVariable @NonNull Long id) {
        CrAccount account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CrAccount", id));
        return ResponseEntity.ok(ApiResponse.success(account));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CrAccount>> createAccount(
            @Valid @RequestBody @NonNull CrAccount account) {
        CrAccount saved = accountRepository.save(account);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(saved));
    }

    /**
     * 특정 계좌번호에 매핑된 담보 배분 내역을 조회한다.
     */
    @GetMapping("/{accountNo}/collaterals")
    public ResponseEntity<ApiResponse<List<com.ho.account.ecl.core.domain.collateral.CrAccountCollateral>>> getAccountCollaterals(
            @PathVariable String accountNo) {
        CrAccount account = accountRepository.findByAccountNo(accountNo)
                .orElseThrow(() -> new com.ho.account.shared.finance.exception.ResourceNotFoundException("CrAccount", accountNo));
        return ResponseEntity.ok(ApiResponse.success(accountCollateralRepository.findByAccount(account)));
    }
}

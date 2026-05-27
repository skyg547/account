package com.risk.mart.api.controller;

import com.risk.mart.core.application.port.out.OdsProductMstRepository;
import com.risk.mart.core.domain.ods.common.OdsProductMst;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * [API] 금융 상품 마스터 관리 컨트롤러 (Product Master Controller)
 */
@RestController
@RequestMapping("/api/v1/mart/products")
@RequiredArgsConstructor

public class OdsProductController {

    private final OdsProductMstRepository repository;

    /**
     * 등록된 모든 금융 상품 목록을 조회한다.
     */
    @GetMapping
    public List<OdsProductMst> getAllProducts() {
        return repository.findAll();
    }

    /**
     * 상품 코드를 기반으로 특정 상품의 상세 정보를 조회한다.
     */
    @GetMapping("/{code}")
    public OdsProductMst getProduct(@PathVariable @NonNull String code) {
        return repository.findById(code).orElseThrow();
    }

    /**
     * 상품의 리스크 속성(금리 유형, CCF 등)을 업데이트한다.
     */
    @PutMapping("/{code}")
    public OdsProductMst updateProduct(@PathVariable @NonNull String code, @RequestBody @NonNull OdsProductMst product) {
        OdsProductMst existing = repository.findById(code).orElseThrow();
        existing.setProductName(product.getProductName());
        existing.setProductType(product.getProductType());
        existing.setAssetLiabilityType(product.getAssetLiabilityType());
        existing.setIsActive(product.getIsActive());
        return repository.save(existing);
    }
}

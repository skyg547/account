package com.ho.account.masterdata.api.web;

import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.masterdata.core.application.usecase.BusinessPartnerUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/basic/businesspartners")
public class BusinessPartnerController {

    private final BusinessPartnerUseCase businessPartnerUseCase;

    public BusinessPartnerController(BusinessPartnerUseCase businessPartnerUseCase) {
        this.businessPartnerUseCase = businessPartnerUseCase;
    }

    // 거래처 생성
    @PostMapping
    public ResponseEntity<BusinessPartner> createBusinessPartner(@RequestBody BusinessPartner businessPartner) {
        try {
            BusinessPartner createdBusinessPartner = businessPartnerUseCase.createBusinessPartner(businessPartner);
            return ResponseEntity.ok(createdBusinessPartner);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // 전체 거래처 조회
    @GetMapping
    public List<BusinessPartner> getAllBusinessPartners() {
        return businessPartnerUseCase.getAllBusinessPartners();
    }

    // 사용 중인 거래처만 조회
    @GetMapping("/active")
    public List<BusinessPartner> getActiveBusinessPartners() {
        return businessPartnerUseCase.getActiveBusinessPartners();
    }

    // 거래처 상세 조회 (코드)
    @GetMapping("/{businessPartnerCode}")
    public ResponseEntity<BusinessPartner> getBusinessPartnerByCode(@PathVariable String businessPartnerCode) {
        return businessPartnerUseCase.getBusinessPartnerByCode(businessPartnerCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 거래처 검색 (이름)
    @GetMapping("/search")
    public List<BusinessPartner> searchBusinessPartners(@RequestParam String name) {
        return businessPartnerUseCase.searchBusinessPartnersByName(name);
    }

    // 거래처 정보 수정
    @PutMapping("/{id}")
    public ResponseEntity<BusinessPartner> updateBusinessPartner(@PathVariable Long id,
            @RequestBody BusinessPartner businessPartnerDetails) {
        try {
            BusinessPartner updatedBusinessPartner = businessPartnerUseCase.updateBusinessPartner(id,
                    businessPartnerDetails);
            return ResponseEntity.ok(updatedBusinessPartner);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 거래처 삭제 (논리적 삭제)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBusinessPartner(@PathVariable Long id) {
        try {
            businessPartnerUseCase.deleteBusinessPartner(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

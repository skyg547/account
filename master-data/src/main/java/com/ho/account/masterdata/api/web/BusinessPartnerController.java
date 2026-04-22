package com.ho.account.masterdata.api.web;

import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.masterdata.api.dto.BusinessPartnerDto;
import com.ho.account.masterdata.api.dto.BusinessPartnerRequestDto;
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
    public ResponseEntity<BusinessPartnerDto> createBusinessPartner(@RequestBody BusinessPartnerRequestDto requestDto) {
        try {
            BusinessPartner createdBusinessPartner = businessPartnerUseCase.createBusinessPartner(requestDto.toCommand());
            return ResponseEntity.ok(BusinessPartnerDto.fromEntity(createdBusinessPartner));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // 전체 거래처 조회
    @GetMapping
    public List<BusinessPartnerDto> getAllBusinessPartners() {
        return businessPartnerUseCase.getAllBusinessPartners().stream()
                .map(BusinessPartnerDto::fromEntity)
                .toList();
    }

    // 사용 중인 거래처만 조회
    @GetMapping("/active")
    public List<BusinessPartnerDto> getActiveBusinessPartners() {
        return businessPartnerUseCase.getActiveBusinessPartners().stream()
                .map(BusinessPartnerDto::fromEntity)
                .toList();
    }

    // 거래처 상세 조회 (코드)
    @GetMapping("/{businessPartnerCode}")
    public ResponseEntity<BusinessPartnerDto> getBusinessPartnerByCode(@PathVariable String businessPartnerCode) {
        return businessPartnerUseCase.getBusinessPartnerByCode(businessPartnerCode)
                .map(BusinessPartnerDto::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 거래처 검색 (이름)
    @GetMapping("/search")
    public List<BusinessPartnerDto> searchBusinessPartners(@RequestParam String name) {
        return businessPartnerUseCase.searchBusinessPartnersByName(name).stream()
                .map(BusinessPartnerDto::fromEntity)
                .toList();
    }

    // 거래처 정보 수정
    @PutMapping("/{id}")
    public ResponseEntity<BusinessPartnerDto> updateBusinessPartner(@PathVariable Long id,
            @RequestBody BusinessPartnerRequestDto requestDto) {
        try {
            BusinessPartner updatedBusinessPartner = businessPartnerUseCase.updateBusinessPartner(id,
                    requestDto.toCommand());
            return ResponseEntity.ok(BusinessPartnerDto.fromEntity(updatedBusinessPartner));
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

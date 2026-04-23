package com.ho.account.masterdata.api.web;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
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

    // 嫄곕옒泥??앹꽦
    @PostMapping
    public ResponseEntity<BusinessPartnerDto> createBusinessPartner(@RequestBody BusinessPartnerRequestDto requestDto) {
        try {
            BusinessPartner createdBusinessPartner = businessPartnerUseCase.createBusinessPartner(requestDto.toCommand());
            return ResponseEntity.ok(BusinessPartnerDto.fromEntity(createdBusinessPartner));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // ?꾩껜 嫄곕옒泥?議고쉶
    @GetMapping
    public List<BusinessPartnerDto> getAllBusinessPartners() {
        return businessPartnerUseCase.getAllBusinessPartners().stream()
                .map(BusinessPartnerDto::fromEntity)
                .toList();
    }

    // ?ъ슜 以묒씤 嫄곕옒泥섎쭔 議고쉶
    @GetMapping("/active")
    public List<BusinessPartnerDto> getActiveBusinessPartners() {
        return businessPartnerUseCase.getActiveBusinessPartners().stream()
                .map(BusinessPartnerDto::fromEntity)
                .toList();
    }

    // 嫄곕옒泥??곸꽭 議고쉶 (肄붾뱶)
    @GetMapping("/{businessPartnerCode}")
    public ResponseEntity<BusinessPartnerDto> getBusinessPartnerByCode(@PathVariable String businessPartnerCode) {
        return businessPartnerUseCase.getBusinessPartnerByCode(businessPartnerCode)
                .map(BusinessPartnerDto::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 嫄곕옒泥?寃??(?대쫫)
    @GetMapping("/search")
    public List<BusinessPartnerDto> searchBusinessPartners(@RequestParam String name) {
        return businessPartnerUseCase.searchBusinessPartnersByName(name).stream()
                .map(BusinessPartnerDto::fromEntity)
                .toList();
    }

    // 嫄곕옒泥??뺣낫 ?섏젙
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

    // 嫄곕옒泥???젣 (?쇰━????젣)
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

package com.ho.account.masterdata.api.web;

import com.ho.account.masterdata.api.dto.BusinessPartnerDto;
import com.ho.account.masterdata.api.dto.BusinessPartnerRequestDto;
import com.ho.account.masterdata.core.application.port.in.BusinessPartnerUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * °ë˜??Š¸Ÿ¬
 */
@RestController
@RequestMapping("/api/basic/businesspartners")
public class BusinessPartnerController {

    private final BusinessPartnerUseCase businessPartnerUseCase;

    public BusinessPartnerController(BusinessPartnerUseCase businessPartnerUseCase) {
        this.businessPartnerUseCase = businessPartnerUseCase;
    }

    /**
     * °ë˜??±ë¡
     */
    @PostMapping
    public ResponseEntity<BusinessPartnerDto> createBusinessPartner(@RequestBody BusinessPartnerRequestDto requestDto) {
        try {
            return ResponseEntity.ok(BusinessPartnerDto.fromEntity(
                    businessPartnerUseCase.createBusinessPartner(requestDto.toCommand())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    /**
     * ?„ì²´ °ë˜?°íšŒ
     */
    @GetMapping
    public List<BusinessPartnerDto> getAllBusinessPartners() {
        return businessPartnerUseCase.getAllBusinessPartners().stream()
                .map(BusinessPartnerDto::fromEntity)
                .toList();
    }

    /**
     * ?œì„± ?íƒœ??°ë˜˜ë§Œ °íšŒ
     */
    @GetMapping("/active")
    public List<BusinessPartnerDto> getActiveBusinessPartners() {
        return businessPartnerUseCase.getActiveBusinessPartners().stream()
                .map(BusinessPartnerDto::fromEntity)
                .toList();
    }

    /**
     * °ë˜??ì„¸ °íšŒ (”ë“œ °ë°˜)
     */
    @GetMapping("/{businessPartnerCode}")
    public ResponseEntity<BusinessPartnerDto> getBusinessPartnerByCode(@PathVariable String businessPartnerCode) {
        return businessPartnerUseCase.getBusinessPartnerByCode(businessPartnerCode)
                .map(BusinessPartnerDto::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * °ë˜???(?´ë¦„ °ë°˜)
     */
    @GetMapping("/search")
    public List<BusinessPartnerDto> searchBusinessPartners(@RequestParam String name) {
        return businessPartnerUseCase.searchBusinessPartnersByName(name).stream()
                .map(BusinessPartnerDto::fromEntity)
                .toList();
    }

    /**
     * °ë˜??•ë³´ ?˜ì •
     */
    @PutMapping("/{id}")
    public ResponseEntity<BusinessPartnerDto> updateBusinessPartner(@PathVariable Long id,
            @RequestBody BusinessPartnerRequestDto requestDto) {
        try {
            return ResponseEntity.ok(BusinessPartnerDto.fromEntity(
                    businessPartnerUseCase.updateBusinessPartner(id, requestDto.toCommand())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * °ë˜??? œ
     */
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

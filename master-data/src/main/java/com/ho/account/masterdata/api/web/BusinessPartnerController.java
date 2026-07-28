package com.ho.account.masterdata.api.web;

import com.ho.account.masterdata.api.dto.BusinessPartnerDto;
import com.ho.account.masterdata.api.dto.BusinessPartnerRequestDto;
import com.ho.account.masterdata.core.application.port.in.BusinessPartnerUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 거래처 관리 컨트롤러
 *
 * <p> 전체 이력/이름 검색 응답은 아직 List를 한 번에 반환합니다. 완료 조건은 최대 page size,
 * 안정적인 {@code businessPartnerCode, validFrom, id} 정렬과 cursor/page 계약을 추가하고 대량 API
 * 통합 테스트에서 제한 없는 조회가 발생하지 않음을 검증하는 것입니다.</p>
 */
@RestController
@RequestMapping("/api/basic/businesspartners")
public class BusinessPartnerController {

    private final BusinessPartnerUseCase businessPartnerUseCase;

    public BusinessPartnerController(BusinessPartnerUseCase businessPartnerUseCase) {
        this.businessPartnerUseCase = businessPartnerUseCase;
    }

    /**
     * 거래처 등록
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
     * 전체 거래처 조회
     */
    @GetMapping
    public List<BusinessPartnerDto> getAllBusinessPartners() {
        return businessPartnerUseCase.getAllBusinessPartners().stream()
                .map(BusinessPartnerDto::fromEntity)
                .toList();
    }

    /**
     * 활성 상태의 거래처만 조회
     */
    @GetMapping("/active")
    public List<BusinessPartnerDto> getActiveBusinessPartners() {
        return businessPartnerUseCase.getActiveBusinessPartners().stream()
                .map(BusinessPartnerDto::fromEntity)
                .toList();
    }

    /**
     * 거래처 상세 조회 (코드 기반)
     */
    @GetMapping("/{businessPartnerCode}")
    public ResponseEntity<BusinessPartnerDto> getBusinessPartnerByCode(@PathVariable String businessPartnerCode) {
        return businessPartnerUseCase.getBusinessPartnerByCode(businessPartnerCode)
                .map(BusinessPartnerDto::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 거래처 검색 (이름 기반)
     */
    @GetMapping("/search")
    public List<BusinessPartnerDto> searchBusinessPartners(@RequestParam String name) {
        return businessPartnerUseCase.searchBusinessPartnersByName(name).stream()
                .map(BusinessPartnerDto::fromEntity)
                .toList();
    }

    /**
     * 거래처 정보 수정
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
     * 거래처 삭제
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

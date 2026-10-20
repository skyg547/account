package com.ho.account.masterdata.api.web;

import com.ho.account.masterdata.api.dto.BusinessPartnerDto;
import com.ho.account.masterdata.api.dto.BusinessPartnerRequestDto;
import com.ho.account.masterdata.core.application.port.in.BusinessPartnerUseCase;
import com.ho.account.masterdata.core.application.query.PageWindow;
import com.ho.account.masterdata.core.domain.policy.MasterDataFieldRules.FieldValidationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 거래처 관리 컨트롤러
 *
 * <p>목록은 기본 20건, 최대 100건의 0 기반 페이지와 연속 여부 헤더를 반환합니다.</p>
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
    public ResponseEntity<BusinessPartnerDto> createBusinessPartner(
            @RequestHeader(value = "X-Auth-Roles", required = false) String authenticatedRoles,
            @RequestBody BusinessPartnerRequestDto requestDto) {
        MasterDataDirectWritePolicy.requireAdminRole(authenticatedRoles);
        try {
            return ResponseEntity.ok(BusinessPartnerDto.fromDomain(
                    businessPartnerUseCase.createBusinessPartner(requestDto.toCommand())));
        } catch (FieldValidationException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    /**
     * 전체 거래처 조회
     */
    @GetMapping
    public ResponseEntity<List<BusinessPartnerDto>> getAllBusinessPartners(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageWindow window = ListPageResponses.window(page, size);
        return ListPageResponses.respond(businessPartnerUseCase.getAllBusinessPartners(window),
                window, BusinessPartnerDto::fromSummary);
    }

    /**
     * 활성 상태의 거래처만 조회
     */
    @GetMapping("/active")
    public ResponseEntity<List<BusinessPartnerDto>> getActiveBusinessPartners(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageWindow window = ListPageResponses.window(page, size);
        return ListPageResponses.respond(businessPartnerUseCase.getActiveBusinessPartners(window),
                window, BusinessPartnerDto::fromSummary);
    }

    /**
     * 거래처 상세 조회 (코드 기반)
     */
    @GetMapping("/{businessPartnerCode}")
    public ResponseEntity<BusinessPartnerDto> getBusinessPartnerByCode(@PathVariable String businessPartnerCode) {
        return businessPartnerUseCase.getBusinessPartnerByCode(businessPartnerCode)
                .map(BusinessPartnerDto::fromDomain)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 거래처 검색 (이름 기반)
     */
    @GetMapping("/search")
    public ResponseEntity<List<BusinessPartnerDto>> searchBusinessPartners(@RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageWindow window = ListPageResponses.window(page, size);
        return ListPageResponses.respond(businessPartnerUseCase.searchBusinessPartnersByName(name, window),
                window, BusinessPartnerDto::fromSummary);
    }

    /**
     * 거래처 정보 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<BusinessPartnerDto> updateBusinessPartner(@PathVariable Long id,
            @RequestHeader(value = "X-Auth-Roles", required = false) String authenticatedRoles,
            @RequestBody BusinessPartnerRequestDto requestDto) {
        MasterDataDirectWritePolicy.requireAdminRole(authenticatedRoles);
        try {
            return ResponseEntity.ok(BusinessPartnerDto.fromDomain(
                    businessPartnerUseCase.updateBusinessPartner(id, requestDto.toCommand())));
        } catch (FieldValidationException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 거래처 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBusinessPartner(
            @PathVariable Long id,
            @RequestHeader(value = "X-Auth-Roles", required = false) String authenticatedRoles) {
        MasterDataDirectWritePolicy.requireAdminRole(authenticatedRoles);
        try {
            businessPartnerUseCase.deleteBusinessPartner(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

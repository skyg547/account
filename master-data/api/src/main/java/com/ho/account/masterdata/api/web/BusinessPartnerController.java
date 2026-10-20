package com.ho.account.masterdata.api.web;

import com.ho.account.masterdata.api.dto.BusinessPartnerDto;
import com.ho.account.masterdata.api.dto.BusinessPartnerRequestDto;
import com.ho.account.masterdata.api.dto.BusinessPartnerEligibilityDto;
import com.ho.account.masterdata.api.dto.BusinessPartnerScreeningDecisionDto;
import com.ho.account.masterdata.api.dto.BusinessPartnerScreeningSubmissionDto;
import com.ho.account.masterdata.core.application.port.in.BusinessPartnerUseCase;
import com.ho.account.masterdata.core.application.port.in.BusinessPartnerScreeningUseCase;
import com.ho.account.masterdata.core.domain.exception.ScreeningMutationForbiddenException;
import com.ho.account.masterdata.core.domain.policy.MasterDataFieldRules.FieldValidationException;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * 거래처 관리 컨트롤러
 *
 * <p>@todo 전체 이력/이름 검색 응답은 아직 List를 한 번에 반환합니다. 완료 조건은 최대 page size,
 * 안정적인 {@code businessPartnerCode, validFrom, id} 정렬과 cursor/page 계약을 추가하고 대량 API
 * 통합 테스트에서 제한 없는 조회가 발생하지 않음을 검증하는 것입니다.</p>
 */
@RestController
@RequestMapping("/api/basic/businesspartners")
public class BusinessPartnerController {

    private final BusinessPartnerUseCase businessPartnerUseCase;
    private final BusinessPartnerScreeningUseCase screeningUseCase;

    @Autowired
    public BusinessPartnerController(BusinessPartnerUseCase businessPartnerUseCase,
            BusinessPartnerScreeningUseCase screeningUseCase) {
        this.businessPartnerUseCase = businessPartnerUseCase;
        this.screeningUseCase = screeningUseCase;
    }

    /** Retains source compatibility for standalone tests of the existing partner endpoints. */
    public BusinessPartnerController(BusinessPartnerUseCase businessPartnerUseCase) {
        this(businessPartnerUseCase, null);
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
    public List<BusinessPartnerDto> getAllBusinessPartners() {
        return businessPartnerUseCase.getAllBusinessPartners().stream()
                .map(BusinessPartnerDto::fromDomain)
                .toList();
    }

    /**
     * 활성 상태의 거래처만 조회
     */
    @GetMapping("/active")
    public List<BusinessPartnerDto> getActiveBusinessPartners() {
        return businessPartnerUseCase.getActiveBusinessPartners().stream()
                .map(BusinessPartnerDto::fromDomain)
                .toList();
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
     * Accept screening evidence using the JWT-verified actor. The body cannot choose its requester.
     */
    @PostMapping("/{businessPartnerCode}/screening-submission")
    public ResponseEntity<Void> submitScreening(@PathVariable String businessPartnerCode,
            @RequestHeader("X-Auth-User") String authenticatedUser,
            @Valid @RequestBody BusinessPartnerScreeningSubmissionDto requestDto) {
        try {
            screeningUseCase.submitScreening(requestDto.toCommand(businessPartnerCode, authenticatedUser));
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
    }

    /** Reviewer authority and identity are derived from verified JWT headers. */
    @PostMapping("/{businessPartnerCode}/screening-decision")
    public ResponseEntity<Void> decideScreening(@PathVariable String businessPartnerCode,
            @RequestHeader("X-Auth-User") String authenticatedUser,
            @RequestHeader(value = "X-Auth-Roles", required = false) String authenticatedRoles,
            @Valid @RequestBody BusinessPartnerScreeningDecisionDto requestDto) {
        MasterDataDirectWritePolicy.requireScreeningReviewerRole(authenticatedRoles);
        try {
            screeningUseCase.decideScreening(requestDto.toCommand(businessPartnerCode, authenticatedUser));
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
    }

    /** Defaults to today's eligibility when the caller omits an as-of date. */
    @GetMapping("/{businessPartnerCode}/eligibility")
    public BusinessPartnerEligibilityDto getEligibility(@PathVariable String businessPartnerCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        LocalDate effectiveDate = asOfDate == null ? LocalDate.now() : asOfDate;
        return new BusinessPartnerEligibilityDto(businessPartnerCode, effectiveDate,
                screeningUseCase.isEligible(businessPartnerCode, effectiveDate));
    }

    /**
     * 거래처 검색 (이름 기반)
     */
    @GetMapping("/search")
    public List<BusinessPartnerDto> searchBusinessPartners(@RequestParam String name) {
        return businessPartnerUseCase.searchBusinessPartnersByName(name).stream()
                .map(BusinessPartnerDto::fromDomain)
                .toList();
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
        } catch (ScreeningMutationForbiddenException e) {
            // A screening elevation is invalid on the generic metadata endpoint.
            return ResponseEntity.badRequest().build();
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

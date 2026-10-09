package com.ho.account.masterdata.api.web;

import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.FiscalPeriodRef;
import com.ho.account.masterdata.api.web.dto.FiscalPeriodStatusUpdateRequestDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 서비스간(Closing -> MasterData) 전용 회계기간 상태 변경 컨트롤러입니다.
 * 외부 퍼블릭 HTTP Gateway 및 일반 사용자 CRUD 경로에서는 직접 노출이 차단됩니다.
 */
@RestController
@RequestMapping("/api/internal/fiscal-periods")
public class InternalFiscalPeriodController {

    private final FiscalPeriodControlPort fiscalPeriodControlPort;

    public InternalFiscalPeriodController(FiscalPeriodControlPort fiscalPeriodControlPort) {
        this.fiscalPeriodControlPort = fiscalPeriodControlPort;
    }

    @PutMapping("/{id}/closing-status")
    public ResponseEntity<FiscalPeriodRef> updateClosingStatus(
            @PathVariable Long id,
            HttpServletRequest request,
            @Valid @RequestBody FiscalPeriodStatusUpdateRequestDto requestDto) {
        String service = (String) request.getAttribute(MasterDataIngressAuthenticationFilter.VERIFIED_SERVICE);
        String actor = (String) request.getAttribute(MasterDataIngressAuthenticationFilter.VERIFIED_ACTOR);
        Object signedPeriodId = request.getAttribute(MasterDataIngressAuthenticationFilter.VERIFIED_PERIOD_ID);
        String signedStatus = (String) request.getAttribute(
                MasterDataIngressAuthenticationFilter.VERIFIED_CLOSING_STATUS);
        if (!"closing".equals(service) || actor == null || actor.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only authenticated 'closing' service is authorized to update fiscal period status.");
        }
        // Bind the parsed body to the signed command before entering the fiscal-period use case.
        if (!(signedPeriodId instanceof Long verifiedId) || !verifiedId.equals(id)
                || !requestDto.getClosingStatus().equals(signedStatus)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Closing assertion does not match requested fiscal period mutation.");
        }
        // The column is 50 characters; the filter caps actor at 42 to retain both verified identities.
        FiscalPeriodRef updatedRef = fiscalPeriodControlPort.updateClosingStatus(
                id,
                requestDto.getClosingStatus(),
                service + ":" + actor);

        return ResponseEntity.ok(updatedRef);
    }
}

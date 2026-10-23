package com.ho.account.masterdata.api.web;

import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.FiscalPeriodRef;
import com.ho.account.contracts.masterdata.FiscalPeriodTransitionCommand;
import com.ho.account.contracts.masterdata.FiscalPeriodTransitionReceipt;
import com.ho.account.masterdata.api.web.dto.FiscalPeriodStatusUpdateRequestDto;
import com.ho.account.masterdata.api.web.dto.FiscalPeriodTransitionRequestDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
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
        // Legacy direct mutation bypasses durable operation identity and is intentionally retired.
        throw new ResponseStatusException(HttpStatus.GONE, "Use the fiscal transition operation protocol");
    }

    @PutMapping("/{id}/transitions/{operationId}")
    public ResponseEntity<FiscalPeriodTransitionReceipt> prepareTransition(
            @PathVariable Long id, @PathVariable String operationId, HttpServletRequest request,
            @Valid @RequestBody FiscalPeriodTransitionRequestDto body) {
        verifyOperation(request, operationId);
        Object signedId = request.getAttribute(MasterDataIngressAuthenticationFilter.VERIFIED_PERIOD_ID);
        if (!(signedId instanceof Long periodId) || !periodId.equals(id)
                || !body.expectedStatus().name().equals(request.getAttribute(
                        MasterDataIngressAuthenticationFilter.VERIFIED_EXPECTED_STATUS))
                || !body.targetStatus().name().equals(request.getAttribute(
                        MasterDataIngressAuthenticationFilter.VERIFIED_TARGET_STATUS))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Signed transition payload mismatch");
        }
        String actor = (String) request.getAttribute(MasterDataIngressAuthenticationFilter.VERIFIED_ACTOR);
        return ResponseEntity.ok(fiscalPeriodControlPort.prepareClosingTransition(
                new FiscalPeriodTransitionCommand(operationId, id, body.expectedStatus(),
                        body.targetStatus(), actor)));
    }

    @PostMapping("/transitions/{operationId}")
    public ResponseEntity<FiscalPeriodTransitionReceipt> applyTransition(
            @PathVariable String operationId, HttpServletRequest request) {
        verifyOperation(request, operationId);
        return ResponseEntity.ok(fiscalPeriodControlPort.applyClosingTransition(operationId));
    }

    @GetMapping("/transitions/{operationId}")
    public ResponseEntity<FiscalPeriodTransitionReceipt> findTransition(
            @PathVariable String operationId, HttpServletRequest request) {
        verifyOperation(request, operationId);
        return fiscalPeriodControlPort.findTransitionReceipt(operationId)
                .map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/transitions/{operationId}")
    public ResponseEntity<FiscalPeriodTransitionReceipt> cancelTransition(
            @PathVariable String operationId, HttpServletRequest request) {
        verifyOperation(request, operationId);
        String actor = (String) request.getAttribute(MasterDataIngressAuthenticationFilter.VERIFIED_ACTOR);
        return ResponseEntity.ok(fiscalPeriodControlPort.cancelClosingTransition(operationId, actor));
    }

    @PostMapping("/transitions/{operationId}/confirm")
    public ResponseEntity<FiscalPeriodTransitionReceipt> confirmTransition(
            @PathVariable String operationId, HttpServletRequest request) {
        verifyOperation(request, operationId);
        return ResponseEntity.ok(fiscalPeriodControlPort.confirmClosingTransition(operationId));
    }

    private void verifyOperation(HttpServletRequest request, String operationId) {
        if (!"closing".equals(request.getAttribute(MasterDataIngressAuthenticationFilter.VERIFIED_SERVICE))
                || !operationId.equals(request.getAttribute(
                        MasterDataIngressAuthenticationFilter.VERIFIED_OPERATION_ID))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Signed operation identity required");
        }
    }
}

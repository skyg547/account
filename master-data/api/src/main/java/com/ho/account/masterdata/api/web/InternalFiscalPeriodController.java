package com.ho.account.masterdata.api.web;

import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.FiscalPeriodRef;
import com.ho.account.masterdata.api.web.dto.FiscalPeriodStatusUpdateRequestDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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

    private static final String ALLOWED_SERVICE_IDENTITY = "closing";

    private final FiscalPeriodControlPort fiscalPeriodControlPort;

    public InternalFiscalPeriodController(FiscalPeriodControlPort fiscalPeriodControlPort) {
        this.fiscalPeriodControlPort = fiscalPeriodControlPort;
    }

    @PutMapping("/{id}/closing-status")
    public ResponseEntity<FiscalPeriodRef> updateClosingStatus(
            @PathVariable Long id,
            @RequestHeader(value = "X-Service-Identity", required = false) String serviceIdentity,
            @Valid @RequestBody FiscalPeriodStatusUpdateRequestDto requestDto) {

        if (serviceIdentity == null || !ALLOWED_SERVICE_IDENTITY.equalsIgnoreCase(serviceIdentity.trim())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only authenticated 'closing' service is authorized to update fiscal period status.");
        }

        FiscalPeriodRef updatedRef = fiscalPeriodControlPort.updateClosingStatus(
                id,
                requestDto.getClosingStatus(),
                requestDto.getAuditUser());

        return ResponseEntity.ok(updatedRef);
    }
}

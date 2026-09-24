package com.ho.account.closing.web;

import com.ho.account.closing.application.port.in.ClosingAdmissionQuery;
import com.ho.account.closing.dto.ClosingAdmissionStatusDto;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** 일반 전표의 회계일자별 마감 통제를 읽기 전용으로 조회하는 API입니다. */
@RestController
@RequestMapping("/api/closing/admission")
public class ClosingAdmissionController {

    private final ClosingAdmissionQuery closingAdmissionQuery;

    public ClosingAdmissionController(ClosingAdmissionQuery closingAdmissionQuery) {
        this.closingAdmissionQuery = closingAdmissionQuery;
    }

    @GetMapping
    public ResponseEntity<ClosingAdmissionStatusDto> findAdmissionStatus(
            @RequestParam(name = "accountingDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate accountingDate) {
        boolean ordinaryPostingAllowed = !closingAdmissionQuery.isClosed(accountingDate);
        // 조회 뒤 마감 상태가 바뀔 수 있으므로 허용 응답은 저장하거나 전기 커밋 허가로 재사용할 수 없습니다.
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new ClosingAdmissionStatusDto(accountingDate, ordinaryPostingAllowed));
    }

    @ExceptionHandler({MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, String>> handleInvalidAccountingDate() {
        return ResponseEntity.badRequest()
                .cacheControl(CacheControl.noStore())
                .body(Map.of(
                        "code", "INVALID_ACCOUNTING_DATE",
                        "message", "accountingDate must be an ISO date (YYYY-MM-DD)."));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleUnavailableAdmission() {
        // 조회 불확실성은 허용으로 바꾸지 않으며 외부 서비스의 URL/본문을 오류 응답에 노출하지 않습니다.
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .cacheControl(CacheControl.noStore())
                .body(Map.of(
                        "code", "CLOSING_ADMISSION_UNAVAILABLE",
                        "message", "Closing admission status is unavailable."));
    }
}

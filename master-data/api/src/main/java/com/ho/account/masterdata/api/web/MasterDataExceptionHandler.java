package com.ho.account.masterdata.api.web;

import com.ho.account.masterdata.core.domain.exception.MasterDataIdempotencyConflictException;
import com.ho.account.masterdata.core.domain.exception.MasterDataVersionConflictException;
import com.ho.account.shared.finance.dto.ApiResponse;
import com.ho.account.shared.finance.exception.GlobalExceptionAdvice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * 기준정보 API가 업무 충돌을 서버 장애와 구분해 전달하는 인바운드 예외 어댑터입니다.
 */
@RestControllerAdvice
public class MasterDataExceptionHandler extends GlobalExceptionAdvice {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Void> handleAccessStatus(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).build();
    }

    @ExceptionHandler(MasterDataVersionConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleVersionConflict(MasterDataVersionConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(exception.getMessage()));
    }

    @ExceptionHandler(MasterDataIdempotencyConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleIdempotencyConflict(
            MasterDataIdempotencyConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(exception.getMessage()));
    }
}

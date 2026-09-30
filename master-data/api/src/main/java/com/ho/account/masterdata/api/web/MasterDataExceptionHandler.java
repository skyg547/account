package com.ho.account.masterdata.api.web;

import com.ho.account.masterdata.core.domain.exception.MasterDataIdempotencyConflictException;
import com.ho.account.masterdata.core.domain.exception.MasterDataVersionConflictException;
import com.ho.account.shared.finance.dto.ApiResponse;
import com.ho.account.shared.finance.exception.GlobalExceptionAdvice;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Set;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 기준정보 API가 업무 충돌을 서버 장애와 구분해 전달하는 인바운드 예외 어댑터입니다.
 */
@RestControllerAdvice
public class MasterDataExceptionHandler extends GlobalExceptionAdvice {

    private static final Set<String> SCD2_TABLES = Set.of(
            "account_subjects", "business_partners", "departments", "products");

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

    @Override
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
        if (isScd2PeriodConflict(exception)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.failure("Master-data validity period conflict."));
        }
        return super.handleUnexpected(exception);
    }

    private static boolean isScd2PeriodConflict(Throwable failure) {
        // Commit/flush wrappers and JDBC batch chains differ. Inspect structured metadata only:
        // classifying all integrity errors as 409 would conceal FK, nullability and schema defects.
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Deque<Throwable> pending = new ArrayDeque<>();
        pending.add(failure);
        while (!pending.isEmpty()) {
            Throwable current = pending.removeFirst();
            if (!visited.add(current)) {
                continue;
            }
            if (current instanceof PSQLException postgres) {
                ServerErrorMessage error = postgres.getServerErrorMessage();
                if (error != null && error.getTable() != null && SCD2_TABLES.contains(error.getTable())) {
                    String table = error.getTable();
                    if (("23P01".equals(postgres.getSQLState())
                            && ("ex_" + table + "_no_overlap").equals(error.getConstraint()))
                            || ("23514".equals(postgres.getSQLState())
                            && ("ck_" + table + "_validity").equals(error.getConstraint()))) {
                        return true;
                    }
                }
            }
            if (current.getCause() != null) {
                pending.add(current.getCause());
            }
            if (current instanceof SQLException sql && sql.getNextException() != null) {
                pending.add(sql.getNextException());
            }
        }
        return false;
    }
}

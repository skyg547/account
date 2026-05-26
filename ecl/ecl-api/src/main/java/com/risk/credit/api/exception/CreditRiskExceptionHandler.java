package com.risk.credit.api.exception;

import com.risk.common.exception.GlobalExceptionAdvice;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * [Exception Handler] 신용리스크 모듈 전용 예외 처리기
 * 
 * 💡 [초보자를 위한 개념 설명]
 * 이 클래스는 신용리스크 시스템에서 발생하는 모든 에러를 가로채서 
 * 전사 표준 규격(ApiResponse)에 맞춰 클라이언트에게 돌려줍니다.
 * GlobalExceptionAdvice를 상속받아 공통 에러 처리 로직을 재사용합니다.
 */
@RestControllerAdvice
public class CreditRiskExceptionHandler extends GlobalExceptionAdvice {
}

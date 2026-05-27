package com.ho.account.mart.api.exception;

import com.ho.account.shared.finance.exception.GlobalExceptionAdvice;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * [Exception Handler] 데이터 마트 모듈 전용 예외 처리기
 */
@RestControllerAdvice
public class DataMartExceptionHandler extends GlobalExceptionAdvice {
    // 💡 힌트: 데이터 품질 오류 등 마트 특유의 예외는 여기에 추가 정의합니다.
}

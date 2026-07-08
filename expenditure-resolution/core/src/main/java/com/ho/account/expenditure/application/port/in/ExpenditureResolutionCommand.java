package com.ho.account.expenditure.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 지출결의 유즈케이스로 들어오는 업무 명령입니다.
 *
 * 초보자용 설명:
 * API JSON DTO는 화면/HTTP 입력 형식이고, 이 Command는 core 업무 흐름이 이해하는 입력 형식입니다.
 * core가 Bean Validation이나 Controller를 몰라도 지출결의 생성/수정 순서를 처리하도록 분리합니다.
 */
public record ExpenditureResolutionCommand(
        String title,
        LocalDate resolutionDate,
        LocalDate paymentDate,
        String departmentCode,
        String paymentAccountCode,
        Long taxInvoiceId,
        List<DetailCommand> details) {

    public record DetailCommand(
            String accountSubjectCode,
            BigDecimal amount,
            String businessPartnerCode,
            String description) {
    }
}
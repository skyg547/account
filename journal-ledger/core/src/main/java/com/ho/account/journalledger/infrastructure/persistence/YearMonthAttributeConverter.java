package com.ho.account.journalledger.infrastructure.persistence;

import jakarta.persistence.AttributeConverter;

import java.time.YearMonth;

/**
 * 원장 잔액의 회계기간(`period`)을 DB에는 `yyyy-MM` 문자열로 저장합니다.
 *
 * <p>초보자 관점에서 보면 `YearMonth`는 "2026년 5월"처럼 월까지만 있는 날짜입니다.
 * JPA와 JDBC bulk upsert가 서로 다른 타입 표현을 쓰면 같은 잔액 키를 다르게 인식할 수 있으므로
 * 저장 표현을 명시적으로 고정합니다.</p>
 */
public class YearMonthAttributeConverter implements AttributeConverter<YearMonth, String> {

    @Override
    public String convertToDatabaseColumn(YearMonth attribute) {
        return attribute == null ? null : attribute.toString();
    }

    @Override
    public YearMonth convertToEntityAttribute(String dbData) {
        return dbData == null || dbData.isBlank() ? null : YearMonth.parse(dbData);
    }
}

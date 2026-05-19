package com.ho.account.journalledger.application.service.journal.validator;

import com.ho.account.journalledger.domain.journal.domain.JournalEntry;

/**
 * [JournalValidationFilter]
 * 전표 검증을 위한 개별 필터 인터페이스.
 * 
 * 🐣 [초보자를 위한 설명]
 * 이 필터는 '전표를 장부에 기록하기 전의 최종 체크리스트'와 같습니다.
 * 시스템은 전표 한 건을 저장하기 전에 여러 명의 전문가(필터들)에게 차례로 물어봅니다.
 * "금액이 맞나요?", "이미 마감된 달인가요?", "없는 계정인가요?" 등 각자의 전문 영역을 검사하고,
 * 한 명이라도 "안 돼요!"라고 하면 전표는 저장되지 않습니다.
 */
public interface JournalValidationFilter {
    /**
     * 전표의 유효성을 검증합니다.
     * @param journalEntry 검증할 전표
     * @throws RuntimeException 검증 실패 시 관련 예외 발생
     */
    void validate(JournalEntry journalEntry);
}

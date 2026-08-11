package com.ho.account.contracts.outbox;

/**
 * [Transactional Outbox 패턴 - Outbox 이벤트 처리 상태]
 * 
 * 🐣 [초보자를 위한 설명]
 * MSA 환경에서는 로컬 DB에 데이터를 저장함과 동시에 외부 서비스(예: 전표 원장 서비스)로 이벤트를 전송해야 하는 경우가 많습니다.
 * 이때 DB 저장에는 성공했지만 네트워크 장애 등으로 외부 전송에 실패하는 Dual Write 정합성 문제가 발생할 수 있습니다.
 * 
 * OutboxStatus는 이 문제를 해결하기 위해 이벤트의 생명주기를 관리하는 상태값입니다:
 * 1. PENDING: 로컬 DB 트랜잭션 내에서 비즈니스 데이터와 함께 원자적으로 Outbox 테이블에 저장된 상태 (아직 외부 전송 전)
 * 2. PUBLISHED: 비동기 릴레이(Outbox Relay Processor)가 이벤트를 읽어 외부 서비스/메시지 브로커로 성공적으로 발행한 상태
 * 3. FAILED: 지정된 재시도 횟수를 초과하거나 치명적 오류로 인해 이벤트 발행에 실패하여 보상 작업이나 수동 개입이 필요한 상태
 */
public enum OutboxStatus {
    /**
     * 로컬 트랜잭션 내 저장 완료, 비동기 릴레이 발행 대기 중
     */
    PENDING,

    /**
     * 외부 메시지 브로커 또는 전표 서비스로 성공적으로 전달 완료
     */
    PUBLISHED,

    /**
     * 이벤트 발행 실패 (재시도 횟수 초과 등)
     */
    FAILED
}

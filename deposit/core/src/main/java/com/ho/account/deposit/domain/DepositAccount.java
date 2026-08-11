package com.ho.account.deposit.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * [DDD(도메인 주도 설계) - Aggregate Root]
 * 예금 계좌 (Deposit Account) 엔티티.
 *
 * 🐣 [초보자를 위한 설명]
 * 은행의 가장 기본적인 '예금 통장'을 나타내는 클래스입니다.
 * 객체지향 설계(Rich Domain Model) 원칙에 따라, 단순히 데이터만 담고 있는 깡통(Data Bag)이 아닙니다.
 * 이 통장에 돈을 입금(deposit)하거나 출금(withdraw)하는 행위 자체를 이 클래스 내부의 '메서드'로 구현합니다.
 * 즉, 잔액이 마이너스가 될 수 없다는 등의 핵심 비즈니스 규칙은 서비스(Service) 레이어가 아니라 이 통장 객체 자신이 직접 책임집니다.
 *
 * 📌 [SCD2 (이력 관리) 적용]
 * 계좌의 상태나 이자율 등이 변경될 경우 기존 레코드를 덮어쓰지 않고,
 * validFrom, validTo(유효기간)를 조절하여 과거 이력을 보존합니다.
 */
@Entity
@Table(name = "deposit_accounts")
@Getter @Setter @NoArgsConstructor
public class DepositAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * [동시성 제어 - 낙관적 잠금 (Optimistic Locking)]
     *
     * 🐣 [초보자를 위한 설명]
     * 금융 예금 시스템에서는 여러 트랜잭션(예: 동시 입금, 출금, 이자 자동 지급 등)이 동일한 계좌의 잔액(balance)을
     * 동시에 변경하려고 시도할 수 있습니다.
     *
     * 만약 동시성 제어가 없다면 다음과 같은 '갱신 손실(Lost Update)' 문제가 발생합니다:
     * 1. 트랜잭션 A가 잔액 1,000원 계좌 조회 (balance = 1,000, version = 1)
     * 2. 트랜잭션 B가 잔액 1,000원 계좌 조회 (balance = 1,000, version = 1)
     * 3. 트랜잭션 A가 500원 입금 후 저장 -> DB balance = 1,500, version = 2
     * 4. 트랜잭션 B가 300원 입금 후 저장 -> DB balance = 1,300, version = 2 (트랜잭션 A의 500원 입금 내역이 덮어씌워져 손실됨!)
     *
     * [낙관적 잠금(Optimistic Locking) vs 비관적 잠금(Pessimistic Locking) 비교]
     * - 비관적 잠금(Pessimistic Locking, SELECT ... FOR UPDATE):
     *   DB 차원에서 데이터 행(Row)에 Exclusive Lock을 걸어 다른 트랜잭션의 접근을 물리적으로 대기(Blocking)시킵니다.
     *   장점: 충돌이 극심한 환경에서 데이터 일관성을 강력하게 보장.
     *   단점: 락 대기 시간으로 인한 데드락(Deadlock) 위험 및 처리량(Throughput) 저하.
     *
     * - 낙관적 잠금(Optimistic Locking, @Version):
     *   DB 락을 잡지 않고, 엔티티의 버전(version) 번호를 이용하여 데이터 수정 시점에 충돌 여부를 검증합니다.
     *   UPDATE deposit_accounts SET balance = ?, version = version + 1 WHERE id = ? AND version = ?
     *   장점: DB 락 대기가 없어 읽기/쓰기 성능이 우수하고 데드락이 발생하지 않음.
     *   단점: 동시 수정 충돌 시 예외(OptimisticLockingFailureException)가 발생하므로 애플리케이션 레벨에서 재시도(Retry) 처리가 필요.
     */
    @Version
    private Long version;

    @Column(nullable = false, unique = true, length = 20)
    private String accountNumber;

    @Column(nullable = false, length = 20)
    private String customerCode;

    @Column(nullable = false, length = 20)
    private String productCode;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    private String currencyCode;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal interestRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DepositStatus status;

    @Column(nullable = false)
    private LocalDate openedAt;

    private LocalDate closedAt;

    // SCD2를 위한 이력 유효기간 관리 필드
    @Column(nullable = false)
    private LocalDate validFrom;

    @Column(nullable = false)
    private LocalDate validTo;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) this.status = DepositStatus.ACTIVE;
        if (this.validFrom == null) this.validFrom = LocalDate.now();
        if (this.validTo == null) this.validTo = LocalDate.of(9999, 12, 31);
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * [Rich Domain Model] 입금 처리
     * 서비스 레이어에서 balance 값을 직접 더하지 않고, 반드시 도메인의 행동 메서드를 호출하여 무결성을 유지합니다.
     */
    public void deposit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("입금액은 0보다 커야 합니다.");
        }
        if (this.status != DepositStatus.ACTIVE) {
            throw new IllegalStateException("활성 상태의 계좌에만 입금할 수 있습니다.");
        }
        this.balance = this.balance.add(amount);
    }

    /**
     * [Rich Domain Model] 출금 처리
     * 출금 시 잔액이 부족하면 예외를 던져 도메인 규칙을 강제합니다.
     */
    public void withdraw(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("출금액은 0보다 커야 합니다.");
        }
        if (this.status != DepositStatus.ACTIVE) {
            throw new IllegalStateException("활성 상태의 계좌에서만 출금할 수 있습니다.");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalStateException("잔액이 부족합니다.");
        }
        this.balance = this.balance.subtract(amount);
    }
}

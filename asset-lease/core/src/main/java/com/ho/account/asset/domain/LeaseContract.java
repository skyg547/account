package com.ho.account.asset.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 리스 계약 엔티티 (IFRS 16)
 */
@Entity
@Table(name = "lease_contracts")
@Getter @Setter
@NoArgsConstructor
public class LeaseContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String contractNo;

    @Column(nullable = false)
    private String contractName;

    @Column(name = "lessor_code", length = 20)
    private String lessorCode;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private BigDecimal monthlyPayment;

    private int paymentDay;

    @Column(nullable = false)
    private BigDecimal discountRate; // 증분차입이자율

    private BigDecimal initialRightOfUseAssetValue; // 초기 사용권자산 PV
    private BigDecimal initialLeaseLiabilityValue; // 초기 리스부채 PV

    private String status; // ACTIVE, TERMINATED, MODIFIED

    @Column(name = "dept_code", length = 20)
    private String departmentCode;

    @Column(name = "expense_account_code", length = 20)
    private String expenseAccountCode;

    private boolean ifrs16Applicable = true;
    private boolean shortTermLease = false;
    private boolean lowValueLease = false;
}

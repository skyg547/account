package com.ho.account.journalledger.domain.ledger.domain;

import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.Currency;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 총계정원장 엔트리 (General Ledger Entry)
 */
@Entity
@Table(name = "gl_entries")
@Getter @Setter
@NoArgsConstructor
public class GlEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_detail_id")
    private JournalDetail journalDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id") // ID 기반 조인
    private AccountSubject account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_code") // Currency는 코드가 PK임
    private Currency currency;

    private String fiscalYear;
    private String fiscalPeriod;
    private LocalDate postingDate;

    private BigDecimal drAmount = BigDecimal.ZERO;
    private BigDecimal crAmount = BigDecimal.ZERO;
    private BigDecimal baseDrAmount = BigDecimal.ZERO;
    private BigDecimal baseCrAmount = BigDecimal.ZERO;

    private String summary;

    // --- 편의 메서드 (기존 서비스 호환용) ---
    public void setJournalDetail(JournalDetail detail) {
        this.journalDetail = detail;
    }
}

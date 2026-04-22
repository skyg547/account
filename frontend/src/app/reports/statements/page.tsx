import React from 'react';
import { FileBarChart, Layers, Download, Search, ChevronRight, ChevronDown } from 'lucide-react';
import styles from './FinancialStatements.module.css';

/**
 * [재무제표 보고서 조회 화면]
 * 재무상태표(BS) 및 손익계산서(PL)를 계층적으로 조회하고 비교 분석합니다.
 * 설계서 파트 4-⑪ 기반.
 */
export default function FinancialStatementsPage() {
  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className={styles.titleArea}>
          <h2>재무제표 보고서 조회</h2>
          <p>전사 재무상태표 및 손익계산서를 통합 조회하고 비교 분석합니다.</p>
        </div>
        <div className={styles.actions}>
          <button className={styles.printBtn}><Download size={18} /> PDF 출력</button>
          <button className={styles.exportBtn}><Download size={18} /> Excel 내보내기</button>
        </div>
      </header>

      {/* 리포트 설정 바 */}
      <section className={`glass-card ${styles.configBar}`}>
        <div className={styles.configRow}>
          <div className={styles.configGroup}>
            <label>보고서 종류</label>
            <div className={styles.tabGroup}>
              <button className={styles.activeTab}>재무상태표 (BS)</button>
              <button>손익계산서 (PL)</button>
            </div>
          </div>
          <div className={styles.configGroup}>
            <label>결산 기수</label>
            <select>
              <option>2026년 04월 (현재)</option>
              <option>2026년 03월</option>
              <option>2025년 (연간)</option>
            </select>
          </div>
          <button className={styles.queryBtn}>보고서 생성</button>
        </div>
      </section>

      {/* 리포트 본문 (BS 예시) */}
      <section className={`glass-card ${styles.reportSection}`}>
        <div className={styles.reportHeader}>
          <h3>재무상태표 (Balance Sheet)</h3>
          <p>2026년 04월 22일 현재 (단위: 원)</p>
        </div>

        <div className={styles.statementTable}>
          <div className={styles.tableHead}>
            <div className={styles.colAccount}>계정 항목</div>
            <div className={styles.colAmount}>당기 금액</div>
            <div className={styles.colAmount}>전기 금액</div>
            <div className={styles.colChange}>증감(%)</div>
          </div>

          {/* 자산 섹션 (Collapsible 예시) */}
          <div className={styles.groupLine}>
            <div className={styles.colAccount}><ChevronDown size={14} /> [ I ] 자산 (Assets)</div>
            <div className={styles.colAmount}>1,540,200,000</div>
            <div className={styles.colAmount}>1,480,000,000</div>
            <div className={`${styles.colChange} ${styles.up}`}>+4.1%</div>
          </div>
          <div className={styles.childLine}>
            <div className={styles.colAccount}>1. 유동자산</div>
            <div className={styles.colAmount}>840,200,000</div>
            <div className={styles.colAmount}>780,000,000</div>
            <div className={`${styles.colChange} ${styles.up}`}>+7.7%</div>
          </div>
          <div className={styles.grandChildLine}>
            <div className={styles.colAccount}>현금 및 현금성자산</div>
            <div className={styles.colAmount}>320,000,000</div>
            <div className={styles.colAmount}>210,000,000</div>
            <div className={`${styles.colChange} ${styles.up}`}>+52.4%</div>
          </div>
          <div className={styles.grandChildLine}>
            <div className={styles.colAccount}>단기금융상품</div>
            <div className={styles.colAmount}>520,200,000</div>
            <div className={styles.colAmount}>570,000,000</div>
            <div className={`${styles.colChange} ${styles.down}`}>-8.7%</div>
          </div>

          <div className={styles.childLine}>
            <div className={styles.colAccount}>2. 비유동자산</div>
            <div className={styles.colAmount}>700,000,000</div>
            <div className={styles.colAmount}>700,000,000</div>
            <div className={styles.colChange}>0.0%</div>
          </div>

          {/* 부채 섹션 */}
          <div className={styles.groupLine}>
            <div className={styles.colAccount}><ChevronRight size={14} /> [ II ] 부채 (Liabilities)</div>
            <div className={styles.colAmount}>820,000,000</div>
            <div className={styles.colAmount}>800,000,000</div>
            <div className={`${styles.colChange} ${styles.up}`}>+2.5%</div>
          </div>
        </div>
      </section>
    </div>
  );
}

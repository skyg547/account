import React from 'react';
import { FileSearch, Calculator, Download, AlertCircle } from 'lucide-react';
import styles from './TaxVatSupport.module.css';

/**
 * [세무/부가세 신고 지원 화면]
 * 부가가치세 신고를 위해 매입/매출 증빙 데이터를 집계하고 국세청 데이터와 대조합니다.
 * 설계서 파트 6-⑮ 기반.
 */
export default function TaxVatSupportPage() {
  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className={styles.titleArea}>
          <h2>세무/부가세 신고 지원</h2>
          <p>분기별 부가가치세 신고를 위한 매입/매출 집계 및 정합성 검증을 수행합니다.</p>
        </div>
      </header>

      {/* 부가세 요약 현황 */}
      <section className={styles.vatDashboard}>
        <div className={`glass-card ${styles.vatCard}`}>
          <h4>2026년 1기 확정 부가세 현황</h4>
          <div className={styles.vatValueGrid}>
            <div className={styles.vatItem}>
              <span>매출 부가세 (A)</span>
              <strong>₩84,200,000</strong>
            </div>
            <div className={styles.vatItem}>
              <span>- 매입 부가세 (B)</span>
              <strong>₩52,500,000</strong>
            </div>
            <div className={`${styles.vatItem} ${styles.total}`}>
              <span>= 납부 세액 (A-B)</span>
              <strong>₩31,700,000</strong>
            </div>
          </div>
        </div>

        <div className={`glass-card ${styles.infoCard}`}>
          <h3><AlertCircle size={18} /> 신고 안내</h3>
          <p>1기 확정 신고 기간: 7월 1일 ~ 7월 25일</p>
          <button className={styles.prepBtn}>신고 기초자료 생성</button>
        </div>
      </section>

      {/* 불일치 대조 그리드 */}
      <section className={`glass-card ${styles.checkSection}`}>
        <div className={styles.sectionHeader}>
          <h3><Calculator size={18} /> 불일치 내역 대조 (Hometax vs System)</h3>
          <span>시스템과 국세청 데이터가 일치하지 않는 2건이 발견되었습니다.</span>
        </div>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>구분</th>
              <th>일자</th>
              <th>거래처</th>
              <th>공급가액</th>
              <th>시스템 세액</th>
              <th>국세청 세액</th>
              <th>차이</th>
            </tr>
          </thead>
          <tbody>
            <tr className={styles.errorRow}>
              <td>매입</td>
              <td>2026-04-12</td>
              <td>삼선기술(주)</td>
              <td>₩1,000,000</td>
              <td>₩100,000</td>
              <td>₩0</td>
              <td className={styles.danger}>₩100,000</td>
            </tr>
            <tr>
              <td>매출</td>
              <td>2026-04-15</td>
              <td>글로벌샵</td>
              <td>₩500,000</td>
              <td>₩50,000</td>
              <td>₩50,000</td>
              <td>₩0</td>
            </tr>
          </tbody>
        </table>
      </section>
    </div>
  );
}

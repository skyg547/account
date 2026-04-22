import React from 'react';
import { Download, Search, Filter, TrendingUp, ArrowRight } from 'lucide-react';
import styles from './GeneralLedger.module.css';

/**
 * [총계정원장 조회 화면]
 * 모든 계정 과목의 잔액 변화를 총괄적으로 확인하는 화면입니다.
 * 설계서 파트 4-⑧ 기반.
 */
export default function GeneralLedgerPage() {
  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className={styles.titleArea}>
          <h2>총계정원장 조회</h2>
          <p>계정별 잔액 현황 및 기초/기말 잔액을 관리합니다.</p>
        </div>
        <button className={styles.downloadBtn}><Download size={18} /> Excel 다운로드</button>
      </header>

      {/* 요약 카드 (기초/차변/대변/기말) */}
      <section className={styles.summaryGrid}>
        <div className={`glass-card ${styles.summaryCard}`}>
          <span>기초 잔액 (Opening)</span>
          <h3>₩1,540,000,000</h3>
        </div>
        <div className={`glass-card ${styles.summaryCard}`}>
          <span>기간 차변 합계</span>
          <h3 className={styles.drText}>₩240,500,000</h3>
        </div>
        <div className={`glass-card ${styles.summaryCard}`}>
          <span>기간 대변 합계</span>
          <h3 className={styles.crText}>₩180,200,000</h3>
        </div>
        <div className={`glass-card ${styles.summaryCard}`}>
          <span>기말 잔액 (Closing)</span>
          <h3>₩1,600,300,000</h3>
        </div>
      </section>

      {/* 검색 필터 */}
      <section className={`glass-card ${styles.filterBar}`}>
        <div className={styles.filterRow}>
          <div className={styles.filterGroup}>
            <label>조회 기간</label>
            <input type="month" defaultValue="2026-04" />
          </div>
          <div className={styles.filterGroup}>
            <label>계정 과목</label>
            <div className={styles.searchBox}>
              <Search size={16} />
              <input type="text" placeholder="계정 코드 또는 명칭" />
            </div>
          </div>
          <button className={styles.queryBtn}>데이터 집계</button>
        </div>
      </section>

      {/* 상세 내역 그리드 */}
      <section className={`glass-card ${styles.gridSection}`}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>계정코드</th>
              <th>계정명</th>
              <th>기초잔액</th>
              <th>차변(Dr)</th>
              <th>대변(Cr)</th>
              <th>기말잔액</th>
              <th>상세</th>
            </tr>
          </thead>
          <tbody>
            {[
              { code: '1101', name: '현금', open: '₩1,200,000', dr: '₩450,000', cr: '₩320,000', close: '₩1,330,000' },
              { code: '1102', name: '당좌예금', open: '₩45,000,000', dr: '₩12,000,000', cr: '₩8,500,000', close: '₩48,500,000' },
              { code: '2101', name: '외상매입금', open: '₩8,900,000', dr: '₩1,200,000', cr: '₩2,400,000', close: '₩10,100,000' },
            ].map((row, idx) => (
              <tr key={idx}>
                <td className={styles.code}>{row.code}</td>
                <td className={styles.bold}>{row.name}</td>
                <td className={styles.amount}>{row.open}</td>
                <td className={`${styles.amount} ${styles.drText}`}>{row.dr}</td>
                <td className={`${styles.amount} ${styles.crText}`}>{row.cr}</td>
                <td className={styles.amount}>{row.close}</td>
                <td><button className={styles.viewBtn}><ArrowRight size={16} /></button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  );
}

import React from 'react';
import { CalendarCheck, Printer, Search } from 'lucide-react';
import styles from './DailySummary.module.css';

export default function DailySummaryPage() {
  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className={styles.titleArea}>
          <h2>은행 일계표 조회</h2>
          <p>특정 일자의 전체 계정 과목별 발생액 및 잔액 현황을 집계합니다.</p>
        </div>
        <div className={styles.filterBar}>
          <div className={styles.datePicker}>
            <CalendarCheck size={18} />
            <input type="date" defaultValue="2026-04-22" />
          </div>
          <button className={styles.searchBtn}><Search size={18} /> 조회</button>
          <button className={styles.printBtn}><Printer size={18} /> 출력</button>
        </div>
      </header>

      <section className={styles.tableSection + " glass-card"}>
        <div className={styles.tableHeader}>
          <h3>일일 총계정 내역 (2026-04-22 기준)</h3>
          <span className={styles.unit}>단위: 원(KRW)</span>
        </div>
        <table className={styles.table}>
          <thead>
            <tr>
              <th rowSpan={2}>계정과목명</th>
              <th colSpan={2} className={styles.center}>전일잔액</th>
              <th colSpan={2} className={styles.center}>금일발생</th>
              <th colSpan={2} className={styles.center}>금일잔액</th>
            </tr>
            <tr>
              <th className={styles.sub}>차변</th>
              <th className={styles.sub}>대변</th>
              <th className={styles.sub}>차변</th>
              <th className={styles.sub}>대변</th>
              <th className={styles.sub}>차변</th>
              <th className={styles.sub}>대변</th>
            </tr>
          </thead>
          <tbody>
            <tr className={styles.groupRow}>
              <td className={styles.indent0}>[1000] 자산</td>
              <td>1.2T</td>
              <td>0</td>
              <td>45.2B</td>
              <td>38.1B</td>
              <td>1.207T</td>
              <td>0</td>
            </tr>
            <tr>
              <td className={styles.indent1}>현금 및 예치금</td>
              <td>450.2B</td>
              <td>0</td>
              <td>12.5B</td>
              <td>10.2B</td>
              <td>452.5B</td>
              <td>0</td>
            </tr>
            <tr>
              <td className={styles.indent2}>현금</td>
              <td>12.8B</td>
              <td>0</td>
              <td>2.4B</td>
              <td>2.1B</td>
              <td>13.1B</td>
              <td>0</td>
            </tr>
          </tbody>
          <tfoot>
            <tr className={styles.totalRow}>
              <td>합계</td>
              <td>4.5T</td>
              <td>4.5T</td>
              <td>125.4B</td>
              <td>125.4B</td>
              <td>4.582T</td>
              <td>4.582T</td>
            </tr>
          </tfoot>
        </table>
      </section>
    </div>
  );
}

import React from 'react';
import { Activity, Download, ArrowUpRight, ArrowDownRight } from 'lucide-react';
import styles from './Cashflow.module.css';

export default function CashflowPage() {
  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className={styles.titleArea}>
          <h2>자금 수지 계획 및 현황</h2>
          <p>미래의 입금 및 출금 예정 스케줄을 분석하여 일자별 자금 과부족을 예측합니다.</p>
        </div>
        <div className={styles.actions}>
          <button className={styles.exportBtn}><Download size={18} /> 엑셀 다운로드</button>
        </div>
      </header>

      <div className={styles.preview}>
        <div className={styles.previewCard + " glass-card"}>
          <div className={styles.cardHeader}>
            <ArrowUpRight color="#22c55e" />
            <span>차주 입금 예정</span>
          </div>
          <h3>₩2,450,000,000</h3>
        </div>
        <div className={styles.previewCard + " glass-card"}>
          <div className={styles.cardHeader}>
            <ArrowDownRight color="#ef4444" />
            <span>차주 출금 예정</span>
          </div>
          <h3>₩1,820,000,000</h3>
        </div>
        <div className={styles.previewCard + " glass-card"}>
          <div className={styles.cardHeader}>
            <Activity color="var(--primary)" />
            <span>예상 기말 잔액</span>
          </div>
          <h3 className={styles.primary}>₩630,000,000</h3>
        </div>
      </div>

      <section className={styles.chartSection + " glass-card"}>
        <h3>일자별 자금 수지 추이</h3>
        <div className={styles.chartArea}>
          {/* 차트 스켈레톤 */}
          <div className={styles.chartLine}></div>
          <div className={styles.chartGrid}></div>
        </div>
      </section>

      <section className={styles.gridSection + " glass-card"}>
        <h3>상세 입출금 스케줄 (최근 7일)</h3>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>예정일자</th>
              <th>관리항목</th>
              <th>거래처</th>
              <th>구분</th>
              <th>금액</th>
              <th>비고</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>2026-04-25</td>
              <td>매출채권 회수</td>
              <td>(주)글로벌테크</td>
              <td><span className={styles.inflow}>입금</span></td>
              <td className={styles.bold}>₩850,000,000</td>
              <td>4월 정기 대금</td>
            </tr>
            <tr>
              <td>2026-04-26</td>
              <td>임차료 지급</td>
              <td>그레이스 타워</td>
              <td><span className={styles.outflow}>출금</span></td>
              <td className={styles.bold}>₩45,000,000</td>
              <td>본사 임차료</td>
            </tr>
          </tbody>
        </table>
      </section>
    </div>
  );
}

import React from 'react';
import { TrendingUp, Users, DollarSign, Calendar, AlertTriangle } from 'lucide-react';
import styles from './ReceivableAging.module.css';

/**
 * [매출채권 및 연령 분석 화면]
 * 미수금 현황을 파악하고 채권 연령(Aging)을 분석하여 수급 관리를 수행합니다.
 * 설계서 파트 6-⑬ 기반.
 */
export default function ReceivableAgingPage() {
  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className={styles.titleArea}>
          <h2>매출채권 관리 및 연령 분석</h2>
          <p>미수 채권의 회수 상태를 모니터링하고 연령별 리스크를 관리합니다.</p>
        </div>
      </header>

      {/* 연령별 대시보드 (Aging Chart) */}
      <section className={styles.agingDashboard}>
        <div className={`glass-card ${styles.agingCard}`}>
          <div className={styles.chartHeader}>
            <TrendingUp size={18} /> <span>채권 연령 분포 (Aging Summary)</span>
          </div>
          <div className={styles.chartArea}>
            {/* Simple CSS Chart Replacement for Skeleton */}
            <div className={styles.barGroup}>
              <div className={styles.bar} style={{ height: '70%', background: '#22c55e' }}></div>
              <div className={styles.bar} style={{ height: '20%', background: '#3b82f6' }}></div>
              <div className={styles.bar} style={{ height: '8%', background: '#f59e0b' }}></div>
              <div className={styles.bar} style={{ height: '2%', background: '#ef4444' }}></div>
            </div>
            <div className={styles.labels}>
              <span>0-30일</span>
              <span>31-60일</span>
              <span>61-90일</span>
              <span>90일+</span>
            </div>
          </div>
        </div>
        
        <div className={styles.kpiGrid}>
          <div className={`glass-card ${styles.kpiCard}`}>
            <span className={styles.kpiLabel}>총 미수채권</span>
            <h3 className={styles.kpiValue}>₩840,200,000</h3>
          </div>
          <div className={`glass-card ${styles.kpiCard}`}>
            <span className={styles.kpiLabel}>연체 채권 (90일+)</span>
            <h3 className={`${styles.kpiValue} ${styles.danger}`}>₩16,800,000</h3>
          </div>
        </div>
      </section>

      {/* 상세 채권 리스트 */}
      <section className={`glass-card ${styles.listSection}`}>
        <h3>거래처별 채권 상세 현황</h3>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>거래처명</th>
              <th>총 청구액</th>
              <th>수금액</th>
              <th>잔액</th>
              <th>가장 오래된 전표</th>
              <th>상태</th>
            </tr>
          </thead>
          <tbody>
            {[
              { name: '(주)글로벌네트워크', total: '₩450,000,000', collected: '₩450,000,000', balance: '₩0', oldest: '-', status: '회수완료' },
              { name: '대박소프트', total: '₩120,500,000', collected: '₩80,000,000', balance: '₩40,500,000', oldest: '2026-03-15', status: '30일 경과' },
              { name: '부도위기컴퍼니', total: '₩25,000,000', collected: '₩0', balance: '₩25,000,000', oldest: '2025-12-01', status: '장기미수' },
            ].map((row, idx) => (
              <tr key={idx}>
                <td className={styles.bold}>{row.name}</td>
                <td>{row.total}</td>
                <td>{row.collected}</td>
                <td className={styles.bold}>{row.balance}</td>
                <td className={styles.date}>{row.oldest}</td>
                <td>
                  <span className={row.status === '장기미수' ? styles.statusDanger : styles.statusInfo}>
                    {row.status}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  );
}

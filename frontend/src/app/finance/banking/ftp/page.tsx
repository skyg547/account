import React from 'react';
import styles from './FtpPerformance.module.css';

export default function FtpPerformancePage() {
  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className={styles.titleArea}>
          <h2>내부 금리(FTP) 성과 분석</h2>
          <p>자금 조달 및 운용 부서 간의 내부 이전 가격을 통한 실질 수익성을 분석합니다.</p>
        </div>
        <div className={styles.actions}>
          <button className={styles.calcBtn}>FTP 일괄 재산출</button>
        </div>
      </header>

      <div className={styles.dashboard}>
        <div className={styles.chartArea + " glass-card"}>
          <h3>부서별 FTP 이익 기여도</h3>
          <div className={styles.barChartPlaceholder}>
            <div className={styles.chartBar} style={{height: '80%', background: 'var(--primary)'}}><span>IB본부</span></div>
            <div className={styles.chartBar} style={{height: '60%', background: '#4f46e5'}}><span>WM사업부</span></div>
            <div className={styles.chartBar} style={{height: '45%', background: '#818cf8'}}><span>기업영업</span></div>
            <div className={styles.chartBar} style={{height: '30%', background: '#c7d2fe'}}><span>준법지원</span></div>
          </div>
        </div>
        
        <div className={styles.infoArea}>
          <div className={styles.miniCard + " glass-card"}>
            <h4>표준 FTP 기본금리</h4>
            <h2>3.45%</h2>
            <p>전일 대비 +0.05%</p>
          </div>
          <div className={styles.miniCard + " glass-card"}>
            <h4>운용수익 대비 FTP 비중</h4>
            <h2>62.4%</h2>
            <p>목표치 60% 상회</p>
          </div>
        </div>
      </div>

      <section className={styles.listArea + " glass-card"}>
        <h3>상품군별 FTP 배분 상세</h3>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>상품군</th>
              <th>평균잔액</th>
              <th>운용/조달 수익</th>
              <th>FTP 적용금리</th>
              <th>FTP 배분손익</th>
              <th>순수익기여</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td className={styles.bold}>기업대출 (Medium Risk)</td>
              <td>₩1.2T</td>
              <td>₩48.2B</td>
              <td>3.82%</td>
              <td>-₩42.0B</td>
              <td className={styles.success}>+₩6.2B</td>
            </tr>
            <tr>
              <td className={styles.bold}>가계예금 (Retail)</td>
              <td>₩2.5T</td>
              <td>₩12.4B</td>
              <td>2.15%</td>
              <td>+₩52.0B</td>
              <td className={styles.success}>+₩64.4B</td>
            </tr>
          </tbody>
        </table>
      </section>
    </div>
  );
}

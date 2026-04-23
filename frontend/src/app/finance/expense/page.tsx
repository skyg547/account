import React from 'react';
import { Camera, ClipboardCheck, Wallet, History, FileText } from 'lucide-react';
import styles from './ExpenseResolution.module.css';

/**
 * [지출결의 및 경비 포털 화면]
 * 임직원이 사용한 경비를 청구하고 승인 워크플로우를 관리합니다.
 * 설계서 파트 6-⑰ 기반.
 */
export default function ExpenseResolutionPage() {
  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className={styles.titleArea}>
          <h2>지출결의 및 경비 포털</h2>
          <p>법인카드 및 개인 경비 사용 내역을 청구하고 승인 현황을 확인합니다.</p>
        </div>
        <button className={styles.claimBtn}><FileText size={18} /> 신규 경비 청구</button>
      </header>

      {/* 개인 경비 요약 위젯 */}
      <section className={styles.personalSummary}>
        <div className={`glass-card ${styles.summaryCard}`}>
          <span>이번 달 사용 금액</span>
          <h3>₩1,240,500</h3>
        </div>
        <div className={`glass-card ${styles.summaryCard}`}>
          <span>부서 예산 잔액</span>
          <h3>₩5,800,000</h3>
        </div>
        <div className={`glass-card ${styles.summaryCard}`}>
          <span>진행 중인 청구</span>
          <h3 className={styles.blue}>2건</h3>
        </div>
      </section>

      {/* 최근 청구 내역 및 카드 내역 */}
      <div className={styles.layout}>
        <main className={`glass-card ${styles.mainList}`}>
          <div className={styles.listHeader}>
            <h3>최근 지출결의 내역</h3>
            <button className={styles.historyBtn}><History size={16} /> 전체 이력</button>
          </div>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>청구일자</th>
                <th>적요</th>
                <th>금액</th>
                <th>승인상태</th>
                <th>상세</th>
              </tr>
            </thead>
            <tbody>
              {[
                { date: '2026-04-20', desc: '점심 식대 (A식당)', amount: '₩12,000', status: '승인완료' },
                { date: '2026-04-18', desc: '영업용 택시비', amount: '₩24,500', status: '검토중' },
                { date: '2026-04-15', desc: '도서 구입 (클린 코드)', amount: '₩35,000', status: '반려' },
              ].map((row, idx) => (
                <tr key={idx}>
                  <td className={styles.date}>{row.date}</td>
                  <td className={styles.bold}>{row.desc}</td>
                  <td className={styles.bold}>{row.amount}</td>
                  <td>
                    <span className={
                      row.status === '승인완료' ? styles.statusSuccess :
                      row.status === '검토중' ? styles.statusWarning : styles.statusDanger
                    }>
                      {row.status}
                    </span>
                  </td>
                  <td><button className={styles.detailBtn}>보기</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </main>

        <aside className={styles.sideArea}>
          <section className={`glass-card ${styles.cardFeed}`}>
            <h3><Wallet size={18} /> 미청구 법인카드 내역</h3>
            <div className={styles.cardItem}>
              <div className={styles.cardDate}>04.22 12:30</div>
              <div className={styles.cardInfo}>
                <div className={styles.vendor}>무한갈비 정식</div>
                <div className={styles.price}>₩45,000</div>
              </div>
              <button className={styles.quickClaim}>청구</button>
            </div>
            <div className={styles.cardItem}>
              <div className={styles.cardDate}>04.21 08:45</div>
              <div className={styles.cardInfo}>
                <div className={styles.vendor}>스타벅스 강남역</div>
                <div className={styles.price}>₩5,600</div>
              </div>
              <button className={styles.quickClaim}>청구</button>
            </div>
          </section>
        </aside>
      </div>
    </div>
  );
}

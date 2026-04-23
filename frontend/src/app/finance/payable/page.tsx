import React from 'react';
import { CreditCard, Calendar, Clock, CheckCircle2, ChevronRight } from 'lucide-react';
import styles from './PayableManagement.module.css';

/**
 * [매입채무 및 지급 관리 화면]
 * 협력사에 지급해야 할 대금을 관리하고 주별/월별 지급 스케줄을 조정합니다.
 * 설계서 파트 6-⑭ 기반.
 */
export default function PayableManagementPage() {
  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className={styles.titleArea}>
          <h2>매입채무 및 지급 관리</h2>
          <p>공급업체 지급 대기 내역을 확인하고 자금 계획에 맞춰 지급을 실행합니다.</p>
        </div>
        <button className={styles.payBtn}><CreditCard size={18} /> 일괄 지급 실행</button>
      </header>

      {/* 지급 스케줄 대시보드 */}
      <section className={styles.paymentDashboard}>
        <div className={`glass-card ${styles.scheduleCard}`}>
          <h3><Calendar size={18} /> 금주 지급 예정 (This Week)</h3>
          <div className={styles.amountList}>
            <div className={styles.amountItem}>
              <span>4월 22일 (수)</span>
              <strong>₩4,500,000</strong>
            </div>
            <div className={styles.amountItem}>
              <span>4월 24일 (금)</span>
              <strong>₩12,800,000</strong>
            </div>
          </div>
          <div className={styles.totalArea}>
            <span>주간 총계</span>
            <strong>₩17,300,000</strong>
          </div>
        </div>

        <div className={`glass-card ${styles.statusSummary}`}>
          <div className={styles.summaryItem}>
            <Clock size={20} className={styles.yellow} />
            <div>
              <span>지급 대기</span>
              <h4>45건</h4>
            </div>
          </div>
          <div className={styles.summaryItem}>
            <CheckCircle2 size={20} className={styles.green} />
            <div>
              <span>금월 지급 완료</span>
              <h4>128,500,000원</h4>
            </div>
          </div>
        </div>
      </section>

      {/* 미지급 상세 그리드 */}
      <section className={`glass-card ${styles.listSection}`}>
        <div className={styles.listHeader}>
          <h3>지급 대기 상세 내역</h3>
          <div className={styles.filters}>
            <select><option>전체 거래처</option></select>
            <select><option>전체 지급 계좌</option></select>
          </div>
        </div>
        <table className={styles.table}>
          <thead>
            <tr>
              <th><input type="checkbox" /></th>
              <th>지급기한</th>
              <th>공급업체</th>
              <th>적요</th>
              <th>지급액</th>
              <th>상태</th>
            </tr>
          </thead>
          <tbody>
            {[
              { due: '2026-04-22', vendor: '서버팩토리(주)', desc: '클라우드 비용(4월)', amount: '₩4,500,000', status: '오늘 마감' },
              { due: '2026-04-24', vendor: '문구나라', desc: '사무용품 구입비', amount: '₩120,000', status: '대기' },
              { due: '2026-04-28', vendor: '인테리어디자인', desc: '사무실 리모델링 2차', amount: '₩15,000,000', status: '대기' },
            ].map((row, idx) => (
              <tr key={idx}>
                <td><input type="checkbox" /></td>
                <td className={idx === 0 ? styles.danger : ''}>{row.due}</td>
                <td className={styles.bold}>{row.vendor}</td>
                <td>{row.desc}</td>
                <td className={styles.bold}>{row.amount}</td>
                <td><span className={idx === 0 ? styles.badgeDanger : styles.badgeDefault}>{row.status}</span></td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  );
}

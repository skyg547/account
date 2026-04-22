import React from 'react';
import { CheckCircle2, XCircle, ArrowRight, Eye, Diff } from 'lucide-react';
import styles from './MasterApproval.md.module.css';

/**
 * [기준 정보 승인 관리 화면]
 * 계정 과목, 거래처 등 중요 기준 정보의 신규/수정 내역을 검토하고 최종 승인합니다.
 * 설계서 파트 3-⑩ 기반.
 */
export default function MasterApprovalPage() {
  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className={styles.titleArea}>
          <h2>기준 정보 승인 관리</h2>
          <p>계정 과목 및 거래처의 변경 내역을 검토하고 승격 처리합니다.</p>
        </div>
      </header>

      <section className={`glass-card ${styles.inboxSection}`}>
        <div className={styles.inboxHeader}>
          <h3>승인 대기함 (Inbox)</h3>
          <div className={styles.badge}>3건 대기 중</div>
        </div>

        <table className={styles.table}>
          <thead>
            <tr>
              <th>구분</th>
              <th>항목명</th>
              <th>기안자</th>
              <th>기안일시</th>
              <th>유형</th>
              <th>검토</th>
            </tr>
          </thead>
          <tbody>
            {[
              { type: '계정과목', name: '현금(대체)', user: '박대리', date: '2026-04-22 14:20', mode: '신규' },
              { type: '거래처', name: '(주)코드기술', user: '이과장', date: '2026-04-22 11:05', mode: '수정' },
              { type: '계정과목', name: '외상매입금', user: '최팀장', date: '2026-04-21 16:40', mode: '수정' },
            ].map((item, idx) => (
              <tr key={idx}>
                <td><span className={styles.typeBadge}>{item.type}</span></td>
                <td className={styles.bold}>{item.name}</td>
                <td>{item.user}</td>
                <td>{item.date}</td>
                <td><span className={item.mode === '신규' ? styles.newBadge : styles.modifyBadge}>{item.mode}</span></td>
                <td><button className={styles.viewBtn}><Eye size={16} /> 변경 내역 확인</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      {/* 변경 내역 대비 (Diff Viewer) - 기획상 예시 */}
      <section className={`glass-card ${styles.diffSection}`}>
        <div className={styles.diffHeader}>
          <Diff size={18} /> <h3>상세 변경 내역 대비 (Account: 외상매입금)</h3>
        </div>
        <div className={styles.diffBody}>
          <div className={styles.diffRow}>
            <label>항목</label>
            <div className={styles.diffBefore}>변경 전 (Before)</div>
            <div className={styles.diffAfter}>변경 후 (After)</div>
          </div>
          <div className={styles.diffRow}>
            <label>관리 형태</label>
            <div className={styles.valOld}>일반 계정</div>
            <div className={styles.valNew}>미결 관리 계정</div>
          </div>
          <div className={styles.diffRow}>
            <label>보고 항목</label>
            <div className={styles.valOld}>매입 채무</div>
            <div className={styles.valNew}>운전 자본 - 매입 채무</div>
          </div>
        </div>
        <footer className={styles.diffFooter}>
          <button className={styles.rejectBtn}><XCircle size={16} /> 반려</button>
          <button className={styles.approveBtn}><CheckCircle2 size={16} /> 최종 승인</button>
        </footer>
      </section>
    </div>
  );
}

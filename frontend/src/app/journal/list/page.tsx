import React from 'react';
import { Search, Calendar, Filter, FileText, CheckCircle2, Clock } from 'lucide-react';
import styles from './JournalList.module.css';

/**
 * [전표 목록 조회 화면]
 * 입력된 모든 전표를 조회하고 승인 상태를 확인하는 화면입니다.
 * 설계서 파트 2-③ 기반.
 */
export default function JournalListPage() {
  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className={styles.titleArea}>
          <h2>전표 조회</h2>
          <p>회계 장부에 기록된 모든 전표 내역을 조회합니다.</p>
        </div>
      </header>

      {/* 종합 검색 필터 */}
      <section className={`glass-card ${styles.filterBar}`}>
        <div className={styles.filterRow}>
          <div className={styles.filterGroup}>
            <label><Calendar size={14} /> 기간 설정</label>
            <div className={styles.dateRange}>
              <input type="date" defaultValue="2026-04-01" />
              <span>~</span>
              <input type="date" defaultValue="2026-04-22" />
            </div>
          </div>
          <div className={styles.filterGroup}>
            <label><Filter size={14} /> 전표 상태</label>
            <select>
              <option>전체</option>
              <option>임시저장</option>
              <option>승인대기</option>
              <option>승인완료</option>
            </select>
          </div>
          <div className={styles.searchBox}>
            <Search size={18} className={styles.searchIcon} />
            <input type="text" placeholder="적요, 전표번호 검색" />
          </div>
          <button className={styles.queryBtn}>조회하기</button>
        </div>
      </section>

      {/* 전표 리스트 */}
      <section className={`glass-card ${styles.gridSection}`}>
        <table className={styles.listTable}>
          <thead>
            <tr>
              <th>번호</th>
              <th>일자</th>
              <th>적요</th>
              <th>합계 금액</th>
              <th>상태</th>
              <th>상세</th>
            </tr>
          </thead>
          <tbody>
            {[
              { id: 'J-20260422-001', date: '2026-04-22', desc: '4월 소모품 매입', amount: '₩125,000', status: '승인완료' },
              { id: 'J-20260422-002', date: '2026-04-22', desc: '영업부 여비교통비 정산', amount: '₩45,000', status: '승인대기' },
              { id: 'J-20260421-005', date: '2026-04-21', desc: '임차료 납부 (4월분)', amount: '₩2,500,000', status: '승인완료' },
            ].map((j, idx) => (
              <tr key={idx}>
                <td><span className={styles.journalId}>{j.id}</span></td>
                <td>{j.date}</td>
                <td className={styles.bold}>{j.desc}</td>
                <td className={styles.amount}>{j.amount}</td>
                <td>
                  <span className={j.status === '승인완료' ? styles.statusSuccess : styles.statusPending}>
                    {j.status === '승인완료' ? <CheckCircle2 size={14} /> : <Clock size={14} />}
                    {j.status}
                  </span>
                </td>
                <td><button className={styles.viewBtn}><FileText size={16} /></button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  );
}

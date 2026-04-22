import React from 'react';
import { Users, Search, Filter, ArrowUpDown } from 'lucide-react';
import styles from './SubLedger.module.css';

/**
 * [보조원장 / 거래처원장 조회 화면]
 * 특정 계정(예: 외상매입금)에 대해 거래처별 상세 잔액을 확인하는 화면입니다.
 * 설계서 파트 4-⑨ 기반.
 */
export default function SubLedgerPage() {
  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className={styles.titleArea}>
          <h2>보조원장(거래처) 조회</h2>
          <p>특정 계정 과목에 대한 거래처별 상세 관리 내역입니다.</p>
        </div>
      </header>

      {/* 복합 필터 바 */}
      <section className={`glass-card ${styles.filterBar}`}>
        <div className={styles.filterRow}>
          <div className={styles.filterGroup}>
            <label>관리 계정</label>
            <select>
              <option>1101 현금</option>
              <option defaultValue="2101">2101 외상매입금</option>
              <option>1104 외상매출금</option>
            </select>
          </div>
          <div className={styles.filterGroup}>
            <label>거래처 검색</label>
            <div className={styles.searchBox}>
              <Users size={16} />
              <input type="text" placeholder="거래처명 입력" />
            </div>
          </div>
          <div className={styles.filterGroup}>
            <label>조회 기간</label>
            <div className={styles.datePicker}>
              <input type="date" defaultValue="2026-04-01" />
              <span>~</span>
              <input type="date" defaultValue="2026-04-22" />
            </div>
          </div>
          <button className={styles.queryBtn}>필터 적용</button>
        </div>
      </section>

      {/* 거래처별 합산 그리드 */}
      <section className={`glass-card ${styles.gridSection}`}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>거래처코드</th>
              <th>거래처명</th>
              <th>기초잔액</th>
              <th>기간 증가</th>
              <th>기간 감소</th>
              <th>현재잔액</th>
              <th>미결건수</th>
            </tr>
          </thead>
          <tbody>
            {[
              { code: 'BP-001', name: '코드마스터(주)', open: '₩5,000,000', inc: '₩1,200,000', dec: '₩500,000', close: '₩5,700,000', count: 3 },
              { code: 'BP-002', name: '(주)재무기술', open: '₩8,500', inc: '₩45,000', dec: '₩0', close: '₩53,500', count: 1 },
              { code: 'BP-003', name: '신한은행(강남)', open: '₩120,000', inc: '₩0', dec: '₩120,000', close: '₩0', count: 0 },
            ].map((row, idx) => (
              <tr key={idx}>
                <td className={styles.code}>{row.code}</td>
                <td className={styles.bold}>{row.name}</td>
                <td className={styles.amount}>{row.open}</td>
                <td className={styles.amount}>{row.inc}</td>
                <td className={styles.amount}>{row.dec}</td>
                <td className={`${styles.amount} ${styles.bold}`}>{row.close}</td>
                <td><span className={row.count > 0 ? styles.countBadge : styles.zeroBadge}>{row.count}건</span></td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  );
}

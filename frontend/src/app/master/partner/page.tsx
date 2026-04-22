import React from 'react';
import { Search, Plus, Filter, Download } from 'lucide-react';
import styles from './Partner.module.css';

/**
 * [거래처 관리 화면]
 * 외부 비즈니스 파트너(매입처, 매출처) 정보를 관리하는 화면입니다.
 * 설계서 파트 1-② 기반.
 */
export default function PartnerPage() {
  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className={styles.titleArea}>
          <h2>거래처 관리</h2>
          <p>회사의 모든 외부 거래 파트너 내역입니다.</p>
        </div>
        <div className={styles.actionArea}>
          <button className={styles.addBtn}><Plus size={18} /> 신규 등록</button>
        </div>
      </header>

      {/* 필터 및 검색 바 */}
      <section className={`glass-card ${styles.filterBar}`}>
        <div className={styles.searchBox}>
          <Search size={18} className={styles.searchIcon} />
          <input type="text" placeholder="거래처명, 사업자번호 검색" />
        </div>
        <div className={styles.filters}>
          <button className={styles.filterBtn}><Filter size={16} /> 필터</button>
          <button className={styles.filterBtn}><Download size={16} /> 엑셀</button>
        </div>
      </section>

      {/* 거래처 목록 그리드 (Placeholder) */}
      <section className={`glass-card ${styles.gridSection}`}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>유형</th>
              <th>거래처명</th>
              <th>사업자번호</th>
              <th>대표자</th>
              <th>상태</th>
              <th>관리</th>
            </tr>
          </thead>
          <tbody>
            {[
              { type: '매출처', name: '코드마스터(주)', id: '123-45-67890', owner: '이코드', status: '활성' },
              { type: '매입처', name: '(주)재무기술', id: '220-81-12345', owner: '김재무', status: '활성' },
              { type: '금융', name: '신한은행(강남)', id: '110-22-33333', owner: '은행장', status: '활성' },
            ].map((p, idx) => (
              <tr key={idx}>
                <td><span className={styles.badge}>{p.type}</span></td>
                <td className={styles.bold}>{p.name}</td>
                <td>{p.id}</td>
                <td>{p.owner}</td>
                <td><span className={styles.activeDot}></span> {p.status}</td>
                <td><button className={styles.editBtn}>상세</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  );
}

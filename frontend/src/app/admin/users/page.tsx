import React from 'react';
import { UserPlus, Shield, Key, Search } from 'lucide-react';
import styles from './UserManagement.module.css';

/**
 * [사용자 관리 화면]
 * 시스템 접속 권한을 가진 사용자를 관리하고 직무별 권한(Role)을 할당합니다.
 * 설계서 파트 3-⑤ 기반.
 */
export default function UserManagementPage() {
  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className={styles.titleArea}>
          <h2>사용자 및 권한 관리</h2>
          <p>시스템 접근 권한 및 사용자 계정 상태를 관리합니다.</p>
        </div>
        <button className={styles.addBtn}><UserPlus size={18} /> 사용자 추가</button>
      </header>

      <section className={`glass-card ${styles.mainSection}`}>
        <div className={styles.toolbar}>
          <div className={styles.searchBox}>
            <Search size={18} />
            <input type="text" placeholder="이름, 아이디, 부서 검색" />
          </div>
        </div>

        <table className={styles.table}>
          <thead>
            <tr>
              <th>사용자</th>
              <th>부서</th>
              <th>권한 그룹</th>
              <th>최근 접속</th>
              <th>상태</th>
              <th>관리</th>
            </tr>
          </thead>
          <tbody>
            {[
              { name: '관리자', id: 'admin', dept: '경영지원팀', role: '시스템관리자', last: '2026-04-22 10:30', status: '활성' },
              { name: '홍길동', id: 'hong.gd', dept: '재무팀', role: '회계팀장', last: '2026-04-22 09:15', status: '활성' },
              { name: '이순신', id: 'lee.ss', dept: '영업부', role: '일반사용자', last: '2026-04-21 17:40', status: '잠금' },
            ].map((u, idx) => (
              <tr key={idx}>
                <td>
                  <div className={styles.userInfo}>
                    <span className={styles.userName}>{u.name}</span>
                    <span className={styles.userId}>{u.id}</span>
                  </div>
                </td>
                <td>{u.dept}</td>
                <td><span className={styles.roleBadge}><Shield size={12} /> {u.role}</span></td>
                <td>{u.last}</td>
                <td><span className={u.status === '활성' ? styles.statusActive : styles.statusLocked}>{u.status}</span></td>
                <td>
                  <div className={styles.actions}>
                    <button title="비밀번호 초기화"><Key size={16} /></button>
                    <button title="권한 수정"><Shield size={16} /></button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  );
}

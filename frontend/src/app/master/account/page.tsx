import React from 'react';
import styles from './AccountSubject.module.css';

/**
 * [계정 과목 관리 화면]
 * 회계의 가장 기초가 되는 '계정 과목'을 트리 형태로 관리하는 화면입니다.
 * 설계서 파트 1-① 기반.
 */
export default function AccountSubjectPage() {
  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <h2>계정 과목 관리</h2>
        <p>재무제표 구성을 위한 표준 계정 체계를 정의합니다.</p>
      </header>

      <div className={styles.mainContent}>
        {/* 좌측: 계정 트리 (Placeholder) */}
        <aside className={`glass-card ${styles.treeSection}`}>
          <h3>계정 체계 트리</h3>
          <div className={styles.placeholderTree}>
            <ul>
              <li>📂 1000 자산
                <ul>
                  <li>📂 1100 유동자산
                    <ul>
                      <li>📄 1101 현금</li>
                      <li>📄 1102 당좌예금</li>
                    </ul>
                  </li>
                </ul>
              </li>
              <li>📂 2000 부채</li>
            </ul>
          </div>
        </aside>

        {/* 우측: 계정 상세 설정 */}
        <section className={`glass-card ${styles.detailSection}`}>
          <h3>계정 상세 정보</h3>
          <form className={styles.form}>
            <div className={styles.formGroup}>
              <label>계정 코드</label>
              <input type="text" placeholder="예: 1101" />
            </div>
            <div className={styles.formGroup}>
              <label>계정 명칭</label>
              <input type="text" placeholder="예: 현금" />
            </div>
            <div className={styles.formGroup}>
              <label>속성 (차/대)</label>
              <select>
                <option>차변</option>
                <option>대변</option>
              </select>
            </div>
            <div className={styles.formActions}>
              <button className={styles.saveBtn}>저장하기</button>
            </div>
          </form>
        </section>
      </div>
    </div>
  );
}

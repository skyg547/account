import React from 'react';
import { GitBranch, User, MapPin } from 'lucide-react';
import styles from './DepartmentManagement.module.css';

/**
 * [귀속부서 관리 화면]
 * 회계상의 비용 귀속점(Cost Center)과 조직도의 부서 체계를 관리합니다.
 * 설계서 파트 3-⑥ 기반.
 */
export default function DepartmentManagementPage() {
  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className={styles.titleArea}>
          <h2>귀속부서(조직) 관리</h2>
          <p>전사 조직 체계 및 비용 귀속 거점을 정의합니다.</p>
        </div>
      </header>

      <div className={styles.layout}>
        {/* 조직도 트리 영역 */}
        <aside className={`glass-card ${styles.orgSection}`}>
          <h3>전사 조직도</h3>
          <div className={styles.orgTree}>
            <ul>
              <li>🏢 (주)어카운트AI
                <ul>
                  <li>📂 경영지원본부
                    <ul>
                      <li>📄 인사팀</li>
                      <li>📄 재무팀 (Cost Center)</li>
                    </ul>
                  </li>
                  <li>📂 영업본부
                    <ul>
                      <li>📄 영업1팀</li>
                      <li>📄 영업2팀</li>
                    </ul>
                  </li>
                </ul>
              </li>
            </ul>
          </div>
        </aside>

        {/* 부서 상세 및 귀속 정보 */}
        <main className={styles.mainArea}>
          <section className={`glass-card ${styles.detailCard}`}>
            <h3>부서 상세 정보</h3>
            <div className={styles.infoGrid}>
              <div className={styles.infoItem}>
                <label>부서명</label>
                <input type="text" defaultValue="재무팀" />
              </div>
              <div className={styles.infoItem}>
                <label>부서코드</label>
                <input type="text" defaultValue="DEPT-001" />
              </div>
              <div className={styles.infoItem}>
                <label>부서장</label>
                <div className={styles.valWithIcon}><User size={14} /> 홍길동</div>
              </div>
              <div className={styles.infoItem}>
                <label>위치</label>
                <div className={styles.valWithIcon}><MapPin size={14} /> 본사 8층</div>
              </div>
            </div>
          </section>

          <section className={`glass-card ${styles.attributionCard}`}>
            <h3>회계 귀속 설정</h3>
            <p className={styles.hint}>이 부서에서 발생하는 비용이 최종적으로 합산되는 단위입니다.</p>
            <div className={styles.attrForm}>
              <div className={styles.formGroup}>
                <label>상위 귀속 부서 (Profit Center)</label>
                <select>
                  <option>경영지원본부</option>
                  <option>전사공통</option>
                </select>
              </div>
              <div className={styles.formGroup}>
                <label>비용 센터 유형</label>
                <div className={styles.radioGroup}>
                  <label><input type="radio" defaultChecked /> 직접비용</label>
                  <label><input type="radio" /> 간접비용(배부대상)</label>
                </div>
              </div>
              <button className={styles.saveBtn}>귀속 정보 업데이트</button>
            </div>
          </section>
        </main>
      </div>
    </div>
  );
}

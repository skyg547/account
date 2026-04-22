import React from 'react';
import { Save, AlertCircle, Plus, Trash2 } from 'lucide-react';
import styles from './JournalEntry.module.css';

/**
 * [전표 입력 화면]
 * 수동으로 차/대 전표를 행 단위로 입력하는 핵심 화면입니다.
 * 설계서 파트 2-③ 기반.
 */
export default function JournalEntryPage() {
  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className={styles.titleArea}>
          <h2>전표 입력</h2>
          <p>새로운 분개 전표를 수동으로 생성합니다.</p>
        </div>
        <div className={styles.headerActions}>
          <button className={styles.draftBtn}>임시 저장</button>
          <button className={styles.saveBtn}><Save size={18} /> 전표 확정</button>
        </div>
      </header>

      {/* 전표 기본 정보 (Master) */}
      <section className={`glass-card ${styles.masterSection}`}>
        <div className={styles.formRow}>
          <div className={styles.inputGroup}>
            <label>전표 일자</label>
            <input type="date" defaultValue="2026-04-22" />
          </div>
          <div className={styles.inputGroup}>
            <label>기안 부서</label>
            <select>
              <option>재무팀</option>
              <option>인사팀</option>
              <option>영업부</option>
            </select>
          </div>
          <div className={styles.inputGroup}>
            <label>전표 유형</label>
            <select>
              <option>일반 전표</option>
              <option>결산 전표</option>
            </select>
          </div>
        </div>
        <div className={styles.formRow}>
          <div className={styles.inputFull}>
            <label>전표 적요 (설명)</label>
            <input type="text" placeholder="예: 4월 업무추진비 정산" />
          </div>
        </div>
      </section>

      {/* 전표 행 입력 (Detail Grid) */}
      <section className={`glass-card ${styles.detailSection}`}>
        <div className={styles.cardHeader}>
          <h3>분개 상세 내역</h3>
          <button className={styles.addLineBtn}><Plus size={16} /> 행 추가</button>
        </div>
        
        <table className={styles.detailTable}>
          <thead>
            <tr>
              <th>구분</th>
              <th>계정 과목</th>
              <th>차변 (Dr)</th>
              <th>대변 (Cr)</th>
              <th>거래처</th>
              <th>삭제</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><select><option>차변</option><option>대변</option></select></td>
              <td><input type="text" placeholder="계정 검색..." /></td>
              <td><input type="number" placeholder="0" className={styles.numInput} /></td>
              <td><input type="number" placeholder="0" disabled className={styles.numInput} /></td>
              <td><input type="text" placeholder="거래처 검색..." /></td>
              <td><button className={styles.deleteBtn}><Trash2 size={16} /></button></td>
            </tr>
            <tr>
              <td><select><option defaultValue="대변">대변</option><option>차변</option></select></td>
              <td><input type="text" placeholder="계정 검색..." /></td>
              <td><input type="number" placeholder="0" disabled className={styles.numInput} /></td>
              <td><input type="number" placeholder="0" className={styles.numInput} /></td>
              <td><input type="text" placeholder="거래처 검색..." /></td>
              <td><button className={styles.deleteBtn}><Trash2 size={16} /></button></td>
            </tr>
          </tbody>
        </table>

        {/* 대차 불일치 검증 바 */}
        <div className={styles.validationBar}>
          <div className={styles.valInfo}>
            <span>차변 합계: ₩0</span>
            <span>대변 합계: ₩0</span>
            <span className={styles.diffText}>차액: ₩0</span>
          </div>
          <div className={styles.valStatus}>
            <AlertCircle size={18} className={styles.warningIcon} />
            <span>대차가 일치하지 않습니다.</span>
          </div>
        </div>
      </section>
    </div>
  );
}

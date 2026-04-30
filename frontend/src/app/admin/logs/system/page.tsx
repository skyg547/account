import React from 'react';
import { Terminal } from 'lucide-react';
import styles from './SystemLog.module.css';

/**
 * [시스템 로그 조회 화면]
 * 서버 에러, 경고, 주요 상태 변경 기록을 시스템 운영자 관점에서 조회합니다.
 * 설계서 파트 3-⑦ 기반.
 */
export default function SystemLogPage() {
  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className={styles.titleArea}>
          <h2>시스템 로그 조회</h2>
          <p>마이크로서비스별 런타임 로그 및 장애 이력을 분석합니다.</p>
        </div>
      </header>

      <section className={`glass-card ${styles.filterBar}`}>
        <div className={styles.filterRow}>
          <div className={styles.filterGroup}>
            <label>서비스 선택</label>
            <select>
              <option>전체 서비스</option>
              <option>auth-service</option>
              <option>journal-ledger-service</option>
              <option>master-data-service</option>
            </select>
          </div>
          <div className={styles.filterGroup}>
            <label>로그 레벨</label>
            <select>
              <option>ERROR</option>
              <option>WARN</option>
              <option>INFO</option>
            </select>
          </div>
          <button className={styles.searchBtn}>로그 스트림 조회</button>
        </div>
      </section>

      <section className={`glass-card ${styles.logConsole}`}>
        <div className={styles.consoleHeader}>
          <Terminal size={16} /> <span>Live Log Console (Last 100 lines)</span>
        </div>
        <div className={styles.consoleBody}>
          <div className={styles.logLine}>
            <span className={styles.lvlInfo}>[INFO]</span>
            <span className={styles.timestamp}>2026-04-22 16:45:01</span>
            <span className={styles.msg}>Journal Ledger 서비스 정상 가동 중...</span>
          </div>
          <div className={styles.logLine}>
            <span className={styles.lvlWarn}>[WARN]</span>
            <span className={styles.timestamp}>2026-04-22 16:44:30</span>
            <span className={styles.msg}>Kafka 브로커 연결 지연 발생 (Retry 1/3)</span>
          </div>
          <div className={`${styles.logLine} ${styles.errorLine}`}>
            <span className={styles.lvlError}>[ERROR]</span>
            <span className={styles.timestamp}>2026-04-22 16:40:15</span>
            <span className={styles.msg}>JournalEntry 저장 실패: 대차 불일치 (Diff: 5,000)</span>
          </div>
          <div className={styles.logLine}>
            <span className={styles.lvlInfo}>[INFO]</span>
            <span className={styles.timestamp}>2026-04-22 16:35:22</span>
            <span className={styles.msg}>Master Data 캐시 갱신 완료 (AccountSubject)</span>
          </div>
        </div>
      </section>
    </div>
  );
}

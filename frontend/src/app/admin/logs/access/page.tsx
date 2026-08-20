import React from 'react';
import { Monitor, Globe, CheckCircle2, XCircle } from 'lucide-react';
import styles from './AccessLog.module.css';

/**
 * [접속 기록 조회 화면]
 * 사용자들의 시스템 로그인/로그아웃 및 세션 유지 기록을 감사(Audit) 목적으로 조회합니다.
 * 설계서 파트 3-⑦ 기반.
 */
export default function AccessLogPage() {
  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className={styles.titleArea}>
          <h2>접속 기록 조회</h2>
          <p>사용자 로그인 및 시스템 접근 이력을 모니터링합니다.</p>
        </div>
      </header>

      <section className={`glass-card ${styles.filterBar}`}>
        <div className={styles.filterRow}>
          <div className={styles.filterGroup}>
            <label>기간 선택</label>
            <select>
              <option>최근 1시간</option>
              <option>최근 24시간</option>
              <option>최근 7일</option>
            </select>
          </div>
          <div className={styles.filterGroup}>
            <label>성공 여부</label>
            <select>
              <option>전체</option>
              <option>성공</option>
              <option>실패</option>
            </select>
          </div>
          <button className={styles.searchBtn}>로그 검색</button>
        </div>
      </section>

      <section className={`glass-card ${styles.listSection}`}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>접속 시각</th>
              <th>사용자 ID</th>
              <th>IP 주소</th>
              <th>기기/브라우저</th>
              <th>결과</th>
            </tr>
          </thead>
          <tbody>
            {[
              { time: '2026-04-22 16:30:12', user: 'admin', ip: '192.168.0.1', agent: 'Chrome / Windows', success: true },
              { time: '2026-04-22 16:28:45', user: 'hong.gd', ip: '112.145.2.11', agent: 'Safari / macOS', success: true },
              { time: '2026-04-22 16:15:30', user: 'unknown', ip: '203.23.45.1', agent: 'Firefox / Linux', success: false },
            ].map((log, idx) => (
              <tr key={idx}>
                <td className={styles.time}>{log.time}</td>
                <td className={styles.userId}>{log.user}</td>
                <td><div className={styles.ip}><Globe size={14} /> {log.ip}</div></td>
                <td><div className={styles.agent}><Monitor size={14} /> {log.agent}</div></td>
                <td>
                  {log.success ? (
                    <span className={styles.success}><CheckCircle2 size={16} /> 성공</span>
                  ) : (
                    <span className={styles.fail}><XCircle size={16} /> 실패</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  );
}

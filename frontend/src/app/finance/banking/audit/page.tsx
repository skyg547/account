import React from 'react';
import { ShieldAlert, Eye, UserX, Terminal, ShieldCheck } from 'lucide-react';
import styles from './AuditMonitoring.module.css';

export default function AuditMonitoringPage() {
  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className={styles.titleArea}>
          <h2>감사 및 내부 통제 모니터링</h2>
          <p>고위험 전표, 비정상 거래 패턴 및 SOD 위반 사항을 실시간 감시합니다.</p>
        </div>
        <div className={styles.status}>
          <div className={styles.pulseDot}></div>
          <span>REAL-TIME MONITORING ACTIVE</span>
        </div>
      </header>

      <div className={styles.grid}>
        <section className={styles.alertPanel + " glass-card"}>
          <div className={styles.panelHeader}>
            <h3>고위험 거래 알림 (Priority)</h3>
            <span className={styles.count}>3 New</span>
          </div>
          <div className={styles.alertList}>
            <div className={styles.alertItem + " " + styles.high}>
              <ShieldAlert size={20} />
              <div className={styles.alertInfo}>
                <strong>거액 현금 출금 탐지</strong>
                <p>강남지점 | ₩500,000,000 | 14:20:05</p>
              </div>
              <button className={styles.actionBtn}>조사</button>
            </div>
            <div className={styles.alertItem + " " + styles.medium}>
              <UserX size={20} />
              <div className={styles.alertInfo}>
                <strong>SOD 규정 위반 의심</strong>
                <p>본점영업부 | 기안/승인자 동일 (ID: ho123)</p>
              </div>
              <button className={styles.actionBtn}>차단</button>
            </div>
          </div>
        </section>

        <section className={styles.logPanel + " glass-card"}>
          <div className={styles.panelHeader}>
            <h3>시스템 감사 로그 (Live Feed)</h3>
          </div>
          <div className={styles.console}>
            <div className={styles.logLine}>
              <span className={styles.timestamp}>[14:45:22]</span>
              <span className={styles.tag}>AUTH</span> USER login success: admin_01 (IP: 10.2.14.55)
            </div>
            <div className={styles.logLine}>
              <span className={styles.timestamp}>[14:45:10]</span>
              <span className={styles.tagSuccess}>VAL</span> Balance check passed for Journal #99824
            </div>
            <div className={styles.logLine}>
              <span className={styles.timestamp}>[14:44:02]</span>
              <span className={styles.tagError}>ERR</span> API timeout: exchange-service (RequestID: AX-992)
            </div>
          </div>
        </section>
      </div>

      <div className={styles.summaryRow}>
        <div className={styles.miniCard + " glass-card"}>
          <ShieldCheck size={24} color="#22c55e" />
          <div>
            <h4>전표 정합성 점검</h4>
            <p>99.98% Healthy</p>
          </div>
        </div>
        <div className={styles.miniCard + " glass-card"}>
          <Terminal size={24} color="var(--primary)" />
          <div>
            <h4>일일 로그 수집량</h4>
            <p>1.2 GB / 254k events</p>
          </div>
        </div>
      </div>
    </div>
  );
}

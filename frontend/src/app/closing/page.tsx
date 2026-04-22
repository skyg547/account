import React from 'react';
import { Calendar, PlayCircle, AlertCircle, CheckCircle2, Loader2 } from 'lucide-react';
import styles from './ClosingControl.module.css';

/**
 * [결산 프로세스 관리 화면]
 * 월/년 결산 마감을 실행하고 각 단계별 진행 상태를 모니터링합니다.
 * 설계서 파트 5-⑫ 기반.
 */
export default function ClosingControlPage() {
  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className={styles.titleArea}>
          <h2>결산 프로세스 관리</h2>
          <p>회계 기별 마감을 정기적으로 수행하고 이월 데이터를 확정합니다.</p>
        </div>
      </header>

      <section className={styles.statusBanner}>
        <div className={`glass-card ${styles.currentPeriod}`}>
          <span>현재 활성 회계 기수</span>
          <h3>2026년 04월 (제 12기)</h3>
          <div className={styles.statusBadge}>운영 중</div>
        </div>
        <div className={`glass-card ${styles.lastClosing}`}>
          <span>최종 결산 완료일</span>
          <h3>2026년 04월 05일</h3>
          <p>대상: 2026년 03월 결산</p>
        </div>
      </section>

      <div className={styles.layout}>
        {/* 결산 단계별 타임라인 */}
        <main className={`glass-card ${styles.timelineSection}`}>
          <h3>결산 체크리스트 및 마감 단계</h3>
          <div className={styles.timeline}>
            <div className={`${styles.step} ${styles.completed}`}>
              <div className={styles.stepIcon}><CheckCircle2 size={24} /></div>
              <div className={styles.stepContent}>
                <h4>1. 미승인 전표 체크</h4>
                <p>승인 대기 중인 모든 전표를 승인 또는 반려 처리합니다.</p>
                <span className={styles.timestamp}>완료됨 (2026-04-20 09:00)</span>
              </div>
            </div>
            <div className={`${styles.step} ${styles.inProgress}`}>
              <div className={styles.stepIcon}><Loader2 size={24} className={styles.spin} /></div>
              <div className={styles.stepContent}>
                <h4>2. 전표 마감 (Closing Journals)</h4>
                <p>해당 기수의 신규 전표 입력을 차단하고 일시 마감합니다.</p>
                <div className={styles.stepActions}>
                  <button className={styles.executeBtn}><PlayCircle size={16} /> 단계 실행</button>
                </div>
              </div>
            </div>
            <div className={styles.step}>
              <div className={styles.stepIcon}><Calendar size={24} /></div>
              <div className={styles.stepContent}>
                <h4>3. 결산 분개 및 이월</h4>
                <p>기말 평가, 손익 대체 분개를 생성하고 차기로 이월합니다.</p>
              </div>
            </div>
            <div className={styles.step}>
              <div className={styles.stepIcon}><AlertCircle size={24} /></div>
              <div className={styles.stepContent}>
                <h4>4. 최종 기수 마감</h4>
                <p>장부를 완전히 봉인하고 결산 보고서를 확정합니다.</p>
              </div>
            </div>
          </div>
        </main>

        {/* 결산 시 주의사항 및 정보 */}
        <aside className={styles.sideArea}>
          <section className={`glass-card ${styles.warningCard}`}>
            <h3><AlertCircle size={18} /> 결산 전 필수 체크</h3>
            <ul>
              <li>대차 불일치 전표 존재 여부</li>
              <li>은행/법인카드 자동 연동 누락분</li>
              <li>미완료 감상(Depreciation) 계산</li>
            </ul>
          </section>
        </aside>
      </div>
    </div>
  );
}

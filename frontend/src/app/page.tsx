import React from 'react';
import { 
  TrendingUp, 
  TrendingDown, 
  DollarSign, 
  CreditCard, 
  Clock, 
  ArrowUpRight 
} from 'lucide-react';
import styles from './page.module.css';

/**
 * [대시보드 메인 페이지]
 * 접속하자마자 우리를 반겨주는 첫 화면입니다. 
 * 주요 돈의 흐름(자산, 부채 등)을 한눈에 보여주는 역할을 해요.
 */
export default function Dashboard() {
  return (
    <div className={styles.dashboard}>
      
      {/* 1. 상단 타이틀 영역: 현재 보고 있는 화면이 무엇인지 알려줍니다. */}
      <header className={styles.header}>
        <h1 className={styles.title}>재무 현황 대시보드</h1>
        <p className={styles.subtitle}>실시간 데이터 분석 및 전표 현황입니다.</p>
      </header>

      {/* 2. 요약 카드 섹션 (그리드 레이아웃)
          - statsGrid를 통해 3개의 카드를 일정한 간격으로 배치합니다. */}
      <section className={styles.statsGrid}>
        
        {/* 2-1. 총 자산 카드: 회사의 총 재산을 보여줍니다. */}
        <div className={`glass-card ${styles.statCard}`}>
          <div className={styles.statHeader}>
            <div className={styles.iconBox} style={{ backgroundColor: 'rgba(99, 102, 241, 0.1)', color: '#6366f1' }}>
              <DollarSign size={24} />
            </div>
            <span className={styles.statLabel}>총 자산</span>
          </div>
          <div className={styles.statValue}>₩1,240,500,000</div>
          <div className={styles.statChange}>
            <TrendingUp size={16} /> <span>12.5% vs 지난달</span>
          </div>
        </div>

        {/* 2-2. 총 부채 카드: 갚아야 할 돈을 보여줍니다. */}
        <div className={`glass-card ${styles.statCard}`}>
          <div className={styles.statHeader}>
            <div className={styles.iconBox} style={{ backgroundColor: 'rgba(239, 68, 68, 0.1)', color: '#ef4444' }}>
              <CreditCard size={24} />
            </div>
            <span className={styles.statLabel}>총 부채</span>
          </div>
          <div className={styles.statValue}>₩450,200,000</div>
          <div className={styles.statChange}>
            <TrendingDown size={16} /> <span>3.2% vs 지난달</span>
          </div>
        </div>

        {/* 2-3. 당기순이익 카드: 자산에서 부채를 뺀 진짜 남은 이익입니다. */}
        <div className={`glass-card ${styles.statCard}`}>
          <div className={styles.statHeader}>
            <div className={styles.iconBox} style={{ backgroundColor: 'rgba(34, 197, 94, 0.1)', color: '#22c55e' }}>
              <ArrowUpRight size={24} />
            </div>
            <span className={styles.statLabel}>당기순이익</span>
          </div>
          <div className={styles.statValue}>₩790,300,000</div>
          <div className={styles.statChange}>
            <TrendingUp size={16} /> <span style={{ color: '#22c55e' }}>8.1% vs 지난달</span>
          </div>
        </div>
      </section>

      {/* 3. 최근 활동 내역 섹션
          - 전표(장부 기록)가 실시간으로 들어오는 모습을 보여주는 곳입니다. */}
      <section className={styles.activitySection}>
        <div className={`glass-card ${styles.activityCard}`}>
          <div className={styles.cardHeader}>
            <h3>최근 전표 유입 현황</h3>
            <button className={styles.moreBtn}>전체 보기</button>
          </div>
          
          {/* 리스트 출력: 데이터 배열을 돌며 화면에 하나씩 그려줍니다. */}
          <div className={styles.activityList}>
            {[
              { id: 'J-20240422001', desc: '삼성전자 비품 매입', amount: '₩12,500,000', status: '자동분개완료', time: '5분 전' },
              { id: 'J-20240422002', desc: '커피빈 운영비 지출', amount: '₩8,500', status: '검토대기', time: '12분 전' },
              { id: 'J-20240422003', desc: '스타트업 클라우드 결제', amount: '₩1,200,000', status: '자동분개완료', time: '1시간 전' },
            ].map((item) => (
              <div key={item.id} className={styles.activityItem}>
                <div className={styles.activityInfo}>
                  <div className={styles.activityId}>{item.id}</div>
                  <div className={styles.activityDesc}>{item.desc}</div>
                </div>
                <div className={styles.activityRight}>
                  <div className={styles.activityAmount}>{item.amount}</div>
                  <div className={styles.activityMeta}>
                    <Clock size={12} /> {item.time} · <span className={item.status === '검토대기' ? styles.pending : styles.success}>{item.status}</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
}

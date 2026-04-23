"use client";

import React from 'react';
import { 
  PieChart, Pie, Cell, ResponsiveContainer, Tooltip,
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Legend
} from 'recharts';
import { 
  Droplets, 
  ShieldCheck, 
  AlertTriangle, 
  TrendingUp,
  FileText,
  Clock
} from 'lucide-react';
import styles from './LiquidityRisk.module.css';

const lcrData = [
  { name: '고유동성자산(HQLA)', value: 450, color: '#4ade80' },
  { name: '순현금유출액', value: 380, color: '#3b82f6' },
];

const maturityGapData = [
  { bucket: '7일 이내', gap: 12.5 },
  { bucket: '15일', gap: 8.4 },
  { bucket: '1개월', gap: -4.2 },
  { bucket: '3개월', gap: 15.6 },
  { bucket: '6개월', gap: 22.8 },
];

export default function LiquidityRiskPage() {
  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className="titleArea">
          <h2 className="text-2xl font-bold">유동성 리스크 모니터링</h2>
          <p className="text-sm text-gray-400">LCR, NSFR 규제 대응 및 단기 유동성 과부족 현황을 집계합니다.</p>
        </div>
        <div className="flex gap-2">
          <button className="icon-btn-secondary"><Clock size={18} /> 실시간 갱신: 14:30:22</button>
          <button className="btn-primary flex items-center gap-2"><FileText size={18} /> 규제 보고서 출력</button>
        </div>
      </header>

      <section className={styles.gaugeGrid}>
        {/* LCR Gauge */}
        <div className={styles.gaugeCard + " glass-card"}>
          <h3 className={styles.gaugeTitle}>LCR (유동성커버리지비율)</h3>
          <div className={styles.gaugeValue}>118.4%</div>
          <div className={styles.gaugeLabel}>법적 규제치: 100% 이상</div>
          <div className={styles.statusBadge + " " + styles.safe}>
            COMPLIANT
          </div>
        </div>

        {/* NSFR Gauge */}
        <div className={styles.gaugeCard + " glass-card"}>
          <h3 className={styles.gaugeTitle}>NSFR (순안정자금조달비율)</h3>
          <div className={styles.gaugeValue}>105.2%</div>
          <div className={styles.gaugeLabel}>법적 규제치: 100% 이상</div>
          <div className={styles.statusBadge + " " + styles.warning}>
            NEAR LIMIT
          </div>
        </div>
      </section>

      <section className={styles.detailsGrid}>
        {/* 현금유출입 갭 분석 */}
        <div className={styles.card + " glass-card"}>
          <h3 className={styles.cardTitle}>
            <TrendingUp size={20} className="text-green-400" /> 기간별 순현금유출입(Gap) 분석
          </h3>
          <div style={{ width: '100%', height: 300 }}>
            <ResponsiveContainer>
              <BarChart data={maturityGapData}>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
                <XAxis dataKey="bucket" stroke="#888" fontSize={12} />
                <YAxis stroke="#888" fontSize={12} />
                <Tooltip 
                   contentStyle={{ background: '#111', border: '1px solid #333' }}
                />
                <Bar dataKey="gap" name="순유입액" fill="#3b82f6" radius={[4, 4, 0, 0]}>
                  {maturityGapData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.gap > 0 ? '#3b82f6' : '#ef4444'} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* 구성 내역 */}
        <div className={styles.card + " glass-card"}>
          <h3 className={styles.cardTitle}>
            <Droplets size={20} className="text-blue-400" /> 고유동성자산(HQLA) 구성
          </h3>
          <div className={styles.metricList}>
            <div className={styles.metricItem}>
              <span className={styles.metricName}>Level 1 자산 (현금, 국채)</span>
              <span className={styles.metricValue}>320.5B</span>
            </div>
            <div className={styles.metricItem}>
              <span className={styles.metricName}>Level 2A 자산 (공공기관채)</span>
              <span className={styles.metricValue}>85.2B</span>
            </div>
            <div className={styles.metricItem}>
              <span className={styles.metricName}>Level 2B 자산 (우량회사채)</span>
              <span className={styles.metricValue}>44.3B</span>
            </div>
            <div className={styles.metricItem + " border-t border-white/10 mt-4 pt-4"}>
              <span className={styles.metricName}>총 가용 유동성 자산</span>
              <span className="font-bold text-blue-400 text-lg">450.0B</span>
            </div>
          </div>
          <div className="mt-8 p-4 rounded-xl bg-amber-500/10 border border-amber-500/20 text-xs text-amber-500 flex gap-3">
             <AlertTriangle size={24} />
             <div>
               <p className="font-bold">조기경보 알림</p>
               <p className="mt-1 opacity-80">최근 3일간 외화 예수금 유출량이 임계치를 초과했습니다. 주의 깊은 모니터링이 필요합니다.</p>
             </div>
          </div>
        </div>
      </section>
    </div>
  );
}

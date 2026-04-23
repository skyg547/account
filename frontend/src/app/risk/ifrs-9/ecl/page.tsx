"use client";

import React, { useState } from 'react';
import { 
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer,
  ComposedChart, Line, Area
} from 'recharts';
import { 
  Calculator, 
  RefreshCcw, 
  Info, 
  TrendingUp, 
  AlertCircle,
  FileSpreadsheet
} from 'lucide-react';
import styles from './EclSimulator.module.css';

// [Mock Data] 전이 행렬 시뮬레이션
const transitionData = [
  { name: 'S1 -> S1', value: 92 },
  { name: 'S1 -> S2', value: 7 },
  { name: 'S1 -> S3', value: 1 },
];

const stageComparisonData = [
  { group: '가계대출', stage1: 12.5, stage2: 8.4, stage3: 45.2 },
  { group: '기업대출', stage1: 18.2, stage2: 12.1, stage3: 62.8 },
  { group: '카드채권', stage1: 5.4, stage2: 15.2, stage3: 88.4 },
];

export default function EclSimulatorPage() {
  const [activeScenario, setActiveScenario] = useState('Standard');

  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className="titleArea">
          <h2 className="text-2xl font-bold">IFRS 9 ECL 시뮬레이션</h2>
          <p className="text-sm text-gray-400">거시경제 시나리오별 기대신용손실(ECL) 및 충당금 변동성을 분석합니다.</p>
        </div>
        <div className="flex gap-2">
          <button className="icon-btn-secondary"><RefreshCcw size={18} /> 초기화</button>
          <button className="btn-primary flex items-center gap-2"><Calculator size={18} /> 분석 실행</button>
        </div>
      </header>

      <main className={styles.simulatorGrid}>
        {/* 파라미터 입력 패널 */}
        <aside className={styles.inputPanel + " glass-card"}>
          <h3 className="text-lg font-semibold flex items-center gap-2">
            <Info size={18} className="text-blue-400" /> 시뮬레이션 변수
          </h3>
          
          <div className={styles.inputGroup}>
            <label>경제 시나리오 가중치</label>
            <select value={activeScenario} onChange={(e) => setActiveScenario(e.target.value)}>
              <option value="Optimistic">낙관적 (20%)</option>
              <option value="Standard">표준 (50%)</option>
              <option value="Pessimistic">비관적 (30%)</option>
            </select>
          </div>

          <div className={styles.inputGroup}>
            <label>부도율(PD) 보정 계수(%)</label>
            <input type="number" defaultValue="1.2" />
          </div>

          <div className={styles.inputGroup}>
            <label>부도시손실률(LGD) 타겟</label>
            <input type="range" min="0" max="100" defaultValue="45" />
            <div className="flex justify-between text-xs text-gray-500">
              <span>0%</span>
              <span>45%</span>
              <span>100%</span>
            </div>
          </div>

          <div className={styles.inputGroup}>
            <label>보유 기간 (Lifetime)</label>
            <input type="number" defaultValue="12" />
          </div>

          <button className={styles.calcBtn}>
            시나리오 적용 및 재계산
          </button>

          <div className="mt-6 border-t border-gray-800 pt-6">
            <h4 className="text-sm font-medium mb-3 text-gray-300">최근 분석 이력</h4>
            <ul className="text-xs space-y-2 text-gray-500">
              <li className="flex justify-between"><span>2026-Q1 결산용</span> <span>04.20</span></li>
              <li className="flex justify-between"><span>금리인상 시나리오</span> <span>04.15</span></li>
            </ul>
          </div>
        </aside>

        {/* 결과 분석 패널 */}
        <section className={styles.resultPanel}>
          <div className={styles.summaryCards}>
            <div className={styles.summaryCard}>
              <div className={styles.cardLabel}>총 기대신용손실(ECL)</div>
              <div className={styles.cardValue}>42.5B</div>
              <div className="text-xs text-red-400 mt-1 flex items-center gap-1">
                <TrendingUp size={12} /> 전회차 대비 +2.1B
              </div>
            </div>
            <div className={styles.summaryCard}>
              <div className={styles.cardLabel}>평균 부도율(WAPD)</div>
              <div className={styles.cardValue}>0.84%</div>
              <div className="text-xs text-gray-500 mt-1">S1 익스포저 가중 평균</div>
            </div>
            <div className={styles.summaryCard}>
              <div className={styles.cardLabel}>전이 대상 자산(S2/S3)</div>
              <div className={styles.cardValue}>12.4%</div>
              <div className="text-xs text-amber-400 mt-1 flex items-center gap-1">
                <AlertCircle size={12} /> 모니터링 필요
              </div>
            </div>
          </div>

          <div className={styles.chartArea + " glass-card"}>
            <h3 className="text-md font-semibold mb-6 flex items-center gap-2">
              <FileSpreadsheet size={18} className="text-green-400" /> 자산군 및 단계 별 충당금 적립률 (%)
            </h3>
            <div style={{ width: '100%', height: 350 }}>
              <ResponsiveContainer>
                <ComposedChart data={stageComparisonData} layout="vertical">
                  <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
                  <XAxis type="number" stroke="#888" fontSize={12} />
                  <YAxis dataKey="group" type="category" stroke="#888" fontSize={12} width={80} />
                  <Tooltip 
                    contentStyle={{ background: '#111', border: '1px solid #333' }}
                  />
                  <Legend />
                  <Bar dataKey="stage1" fill="#3b82f6" name="Stage 1" stackId="a" barSize={30} />
                  <Bar dataKey="stage2" fill="#fbbf24" name="Stage 2 (SICR)" stackId="a" />
                  <Bar dataKey="stage3" fill="#ef4444" name="Stage 3 (Default)" stackId="a" />
                </ComposedChart>
              </ResponsiveContainer>
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}

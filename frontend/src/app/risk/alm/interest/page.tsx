"use client";

import React from 'react';
import { 
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer,
  LineChart, Line, ComposedChart, Area
} from 'recharts';
import { 
  Scale, 
  ArrowRightLeft, 
  TrendingUp, 
  Settings2,
  Calendar,
  AlertCircle
} from 'lucide-react';
import styles from './AlmManagement.module.css';

// [Mock Data] 금리 갭 현황
const gapData = [
  { period: '1개월', asset: 15.2, liability: 12.4, gap: 2.8 },
  { period: '3개월', asset: 24.5, liability: 28.1, gap: -3.6 },
  { period: '6개월', asset: 42.8, liability: 35.2, gap: 7.6 },
  { period: '1년', asset: 68.4, liability: 72.5, gap: -4.1 },
  { period: '3년', asset: 120.5, liability: 98.4, gap: 22.1 },
  { period: '5년', asset: 85.2, liability: 110.2, gap: -25.0 },
];

export default function AlmInterestPage() {
  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className="titleArea">
          <h2 className="text-2xl font-bold">ALM / 금리 리스크 관리</h2>
          <p className="text-sm text-gray-400">자산 및 부채의 금리 민감도 분석과 금리 갭 현황을 관리합니다.</p>
        </div>
        <div className="flex gap-2">
          <button className="icon-btn-secondary"><Calendar size={18} /> 기준일자: 2026-04-23</button>
          <button className="btn-primary flex items-center gap-2"><TrendingUp size={18} /> 시나리오 분석</button>
        </div>
      </header>

      <section className={styles.mainGrid}>
        {/* 금리 갭 추이 그래프 */}
        <div className={styles.chartCard + " glass-card"}>
          <div className="flex justify-between mb-6">
            <h3 className="font-semibold flex items-center gap-2">
              <ArrowRightLeft size={18} className="text-blue-400" /> 기간별 자산-부채 금리 갭 추이
            </h3>
            <span className="text-xs text-gray-500">단위: 조원 (KRW)</span>
          </div>
          <div style={{ width: '100%', height: 350 }}>
            <ResponsiveContainer>
              <ComposedChart data={gapData}>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
                <XAxis dataKey="period" stroke="#888" fontSize={12} />
                <YAxis stroke="#888" fontSize={12} />
                <Tooltip 
                  contentStyle={{ background: '#111', border: '1px solid #333' }}
                />
                <Legend />
                <Bar dataKey="asset" name="자산(RS)" fill="#3b82f6" opacity={0.8} />
                <Bar dataKey="liability" name="부채(RSA)" fill="#ef4444" opacity={0.8} />
                <Line type="monotone" dataKey="gap" name="금리 갭" stroke="#fbbf24" strokeWidth={3} dot={{ r: 4 }} />
              </ComposedChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* 시나리오 설정 패널 */}
        <div className={styles.chartCard + " glass-card"}>
          <h3 className="font-semibold mb-6 flex items-center gap-2">
            <Settings2 size={18} className="text-purple-400" /> 금리 충격 시나리오
          </h3>
          <div className={styles.scenarioPanel}>
            <div className={styles.inputGroup}>
              <label>Parallel Shift (bp)</label>
              <input type="number" defaultValue="100" />
            </div>
            <div className={styles.inputGroup}>
              <label>시장금리 변동 전망</label>
              <select className="bg-transparent border border-white/10 rounded p-2 text-sm">
                <option>급격한 금리 인상</option>
                <option>점진적 인상</option>
                <option>금리 동결</option>
              </select>
            </div>
            <div className="mt-4 p-4 rounded-xl bg-blue-500/10 border border-blue-500/20">
              <div className="text-xs font-medium text-blue-400 mb-1">예상 순이자이익(NII) 변동</div>
              <div className="text-xl font-bold">+125.4B</div>
              <p className="text-[10px] text-gray-500 mt-2">100bp 금리 상승 시 자산 유리 포지션</p>
            </div>
            <div className="mt-4 p-4 rounded-xl bg-red-500/10 border border-red-500/20">
              <div className="text-xs font-medium text-red-400 mb-1">예상 경제적 가치(EvE) 변동</div>
              <div className="text-xl font-bold">-45.2B</div>
              <p className="text-[10px] text-gray-500 mt-2">금리 민감도 계수 기준 산출</p>
            </div>
          </div>
        </div>
      </section>

      {/* 금리 갭 명세 분석표 */}
      <section className={styles.gapTableSection + " glass-card"}>
        <h3 className="font-semibold flex items-center gap-2">
          <Scale size={18} className="text-amber-400" /> ALM 금리 갭 분석 상세표
        </h3>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>구분</th>
              <th>1개월 이하</th>
              <th>1~3개월</th>
              <th>3~6개월</th>
              <th>6~1년</th>
              <th>1~3년</th>
              <th>3~5년</th>
              <th>합계</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td className={styles.label}>금리민감자산 (RSA)</td>
              <td>15.2</td>
              <td>24.5</td>
              <td>42.8</td>
              <td>68.4</td>
              <td>120.5</td>
              <td>85.2</td>
              <td>356.6</td>
            </tr>
            <tr>
              <td className={styles.label}>금리민감부채 (RSL)</td>
              <td>12.4</td>
              <td>28.1</td>
              <td>35.2</td>
              <td>72.5</td>
              <td>98.4</td>
              <td>110.2</td>
              <td>356.8</td>
            </tr>
            <tr className="bg-white/5 font-bold">
              <td className={styles.label}>금리 갭 (Interest Gap)</td>
              <td className={styles.positive}>2.8</td>
              <td className={styles.negative}>-3.6</td>
              <td className={styles.positive}>7.6</td>
              <td className={styles.negative}>-4.1</td>
              <td className={styles.positive}>22.1</td>
              <td className={styles.negative}>-25.0</td>
              <td className={styles.negative}>-0.2</td>
            </tr>
            <tr>
              <td className={styles.label}>누적 금리 갭</td>
              <td>2.8</td>
              <td>-0.8</td>
              <td>6.8</td>
              <td>2.7</td>
              <td>24.8</td>
              <td>-0.2</td>
              <td>-</td>
            </tr>
          </tbody>
        </table>
      </section>
    </div>
  );
}

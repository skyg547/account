"use client";

import React from 'react';
import { 
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, 
  PieChart, Pie, Cell, LineChart, Line 
} from 'recharts';
import { 
  ShieldCheck, 
  AlertTriangle, 
  TrendingUp, 
  TrendingDown, 
  Activity, 
  PieChart as PieIcon,
  Download,
  Filter
} from 'lucide-react';
import styles from './RwaDashboard.module.css';

// [Mock Data] 리스크 비중
const riskTypeData = [
  { name: '신용 리스크', value: 72, color: '#3b82f6' },
  { name: '시장 리스크', value: 15, color: '#ef4444' },
  { name: '운영 리스크', value: 13, color: '#fbbf24' },
];

// [Mock Data] 월별 RWA 추이
const monthlyTrendData = [
  { month: '1월', rwa: 450, ratio: 14.2 },
  { month: '2월', rwa: 462, ratio: 14.5 },
  { month: '3월', rwa: 458, ratio: 14.1 },
  { month: '4월', rwa: 475, ratio: 13.8 },
  { month: '5월', rwa: 490, ratio: 13.9 },
  { month: '6월', rwa: 512, ratio: 13.5 },
];

export default function RwaDashboardPage() {
  return (
    <div className={styles.container}>
      {/* 타이틀 및 상단 액션 */}
      <header className={styles.header}>
        <div className={styles.titleArea}>
          <h2>Basel III RWA 모니터링</h2>
          <p>위험가중자산(RWA) 산출 및 자본적정성 지표(BIS 비율) 실시간 분석 현황입니다.</p>
        </div>
        <div className="flex gap-3">
          <button className="icon-btn-secondary"><Filter size={18} /> 필터</button>
          <button className="btn-primary flex items-center gap-2"><Download size={18} /> 보고서 다운로드</button>
        </div>
      </header>

      {/* KPI 카드 그리드 */}
      <section className={styles.kpiGrid}>
        <div className={styles.kpiCard}>
          <div className={styles.kpiLabel}>
            <span>BIS 자기자본비율</span>
            <div className={styles.kpiIcon}><ShieldCheck size={18} /></div>
          </div>
          <div className={styles.kpiValue}>13.52%</div>
          <div className={styles.kpiTrend + " " + styles.down}>
            <TrendingDown size={14} /> 전월 대비 -0.38%
          </div>
        </div>

        <div className={styles.kpiCard}>
          <div className={styles.kpiLabel}>
            <span>보통주자본비율(CET1)</span>
            <div className={styles.kpiIcon}><Activity size={18} /></div>
          </div>
          <div className={styles.kpiValue}>11.24%</div>
          <div className={styles.kpiTrend + " " + styles.up}>
            <TrendingUp size={14} /> 전월 대비 +0.12%
          </div>
        </div>

        <div className={styles.kpiCard}>
          <div className={styles.kpiLabel}>
            <span>전체 RWA 규모</span>
            <div className={styles.kpiIcon}><PieIcon size={18} /></div>
          </div>
          <div className={styles.kpiValue}>512.4B</div>
          <div className={styles.kpiTrend + " " + styles.up}>
            <TrendingUp size={14} /> 전월 대비 +22.4B
          </div>
        </div>

        <div className={styles.kpiCard}>
          <div className={styles.kpiLabel}>
            <span>리스크 익스포저</span>
            <div className={styles.kpiIcon}><AlertTriangle size={18} /></div>
          </div>
          <div className={styles.kpiValue}>684.2B</div>
          <div className={styles.kpiTrend + " " + styles.down}>
            <TrendingDown size={14} /> 전월 대비 -4.5B
          </div>
        </div>
      </section>

      {/* 메인 차트 영역 */}
      <section className={styles.mainGrid}>
        {/* 리스크 유형별 비중 (Pie) */}
        <div className={styles.chartCard + " glass-card"}>
          <div className={styles.chartHeader}>
            <h3>리스크 유형별 비중</h3>
            <PieIcon size={16} className="text-gray-500" />
          </div>
          <div style={{ width: '100%', height: 300 }}>
            <ResponsiveContainer>
              <PieChart>
                <Pie
                  data={riskTypeData}
                  cx="50%"
                  cy="50%"
                  innerRadius={60}
                  outerRadius={80}
                  paddingAngle={5}
                  dataKey="value"
                >
                  {riskTypeData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip />
                <Legend layout="horizontal" verticalAlign="bottom" align="center" />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* 월별 RWA 및 BIS 비율 추이 */}
        <div className={styles.chartCard + " glass-card"}>
          <div className={styles.chartHeader}>
            <h3>월별 RWA 및 BIS 비율 추이</h3>
            <TrendingUp size={16} className="text-gray-500" />
          </div>
          <div style={{ width: '100%', height: 300 }}>
            <ResponsiveContainer>
              <BarChart data={monthlyTrendData}>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.1)" />
                <XAxis dataKey="month" stroke="#888" fontSize={12} />
                <YAxis yAxisId="left" stroke="#888" fontSize={12} />
                <YAxis yAxisId="right" orientation="right" stroke="#888" fontSize={12} />
                <Tooltip 
                  contentStyle={{ background: '#111', border: '1px solid #333' }}
                  itemStyle={{ fontSize: 12 }}
                />
                <Legend />
                <Bar yAxisId="left" dataKey="rwa" fill="#3b82f6" name="RWA(B)" radius={[4, 4, 0, 0]} />
                <Line yAxisId="right" type="monotone" dataKey="ratio" stroke="#4ade80" name="BIS비율(%)" strokeWidth={2} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </section>

      {/* 세부 자산군별 RWA 내역 */}
      <section className={styles.tableSection + " glass-card"}>
        <div className={styles.chartHeader}>
          <h3>세부 자산군별 RWA 산출 내역</h3>
          <span className="text-xs text-gray-500">기준일자: 2026-04-23</span>
        </div>
        <div className={styles.tableContainer}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>자산군 (Asset Class)</th>
                <th>익스포저(EAD)</th>
                <th>위험가중치(RW)</th>
                <th>신용 RWA</th>
                <th>리스크 등급</th>
                <th>전월 대비 변동</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>기업대출 (Corporate)</td>
                <td>245.2B</td>
                <td>75%</td>
                <td>183.9B</td>
                <td><span className={styles.riskLevel + " " + styles.mid}>MODERATE</span></td>
                <td className={styles.up}>+12.4B</td>
              </tr>
              <tr>
                <td>소매대출 (Retail)</td>
                <td>120.5B</td>
                <td>45%</td>
                <td>54.2B</td>
                <td><span className={styles.riskLevel + " " + styles.low}>STABLE</span></td>
                <td className={styles.down}>-2.1B</td>
              </tr>
              <tr>
                <td>주택담보대출 (Mortgage)</td>
                <td>185.7B</td>
                <td>35%</td>
                <td>65.0B</td>
                <td><span className={styles.riskLevel + " " + styles.low}>STABLE</span></td>
                <td className={styles.up}>+4.8B</td>
              </tr>
              <tr>
                <td>고위험 자산 (High Risk)</td>
                <td>45.8B</td>
                <td>150%</td>
                <td>68.7B</td>
                <td><span className={styles.riskLevel + " " + styles.high}>CRITICAL</span></td>
                <td className={styles.up}>+8.5B</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}

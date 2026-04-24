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
    <div className="flex flex-col gap-8">
      {/* 타이틀 및 상단 액션 */}
      <header className="flex justify-between items-center">
        <div>
          <h2 className="text-3xl font-black text-white italic tracking-tight">IFRS 9 ECL 시뮬레이션</h2>
          <p className="text-slate-500 mt-2 text-sm font-medium leading-none">거시경제 시나리오별 기대신용손실(ECL) 및 충당금 변동성 정밀 분석</p>
        </div>
        <div className="flex gap-2">
          <button className="bg-slate-800 hover:bg-slate-700 text-slate-300 px-4 py-2 rounded-xl border border-white/5 transition-all flex items-center gap-2 text-sm font-bold">
            <RefreshCcw size={18} /> 초기화
          </button>
          <button className="bg-blue-600 hover:bg-blue-500 text-white px-5 py-2 rounded-xl border border-blue-500/50 shadow-lg shadow-blue-500/20 transition-all flex items-center gap-2 text-sm font-black">
            <Calculator size={18} /> 분석 실행
          </button>
        </div>
      </header>

      <main className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        {/* 파라미터 입력 패널 */}
        <aside className="lg:col-span-4 bg-[#1e293b]/50 backdrop-blur-xl border border-white/10 p-8 rounded-[32px] shadow-2xl">
          <h3 className="text-lg font-black text-white flex items-center gap-3 mb-8 tracking-tight">
            <div className="w-2 h-6 bg-blue-500 rounded-full" /> 시뮬레이션 변수 설정
          </h3>
          
          <div className="space-y-6">
            <div className="flex flex-col gap-2">
              <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">경제 시나리오 가중치</label>
              <select 
                value={activeScenario} 
                onChange={(e) => setActiveScenario(e.target.value)}
                className="bg-slate-900 border border-white/10 text-white text-sm rounded-xl p-3 focus:border-blue-500 outline-none transition-all cursor-pointer font-bold"
              >
                <option value="Optimistic">낙관적 (20%)</option>
                <option value="Standard">표준 (50%)</option>
                <option value="Pessimistic">비관적 (30%)</option>
              </select>
            </div>

            <div className="flex flex-col gap-2">
              <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">부도율(PD) 보정 계수(%)</label>
              <input 
                type="number" 
                defaultValue="1.2" 
                className="bg-slate-900 border border-white/10 text-white text-sm rounded-xl p-3 focus:border-blue-500 outline-none transition-all font-mono font-bold"
              />
            </div>

            <div className="flex flex-col gap-2">
               <div className="flex justify-between items-center px-1">
                  <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest">부도시손실률(LGD) 타겟</label>
                  <span className="text-blue-400 font-mono font-bold text-xs">45%</span>
               </div>
              <input type="range" min="0" max="100" defaultValue="45" className="w-full h-1.5 bg-slate-900 rounded-lg appearance-none cursor-pointer accent-blue-500 border border-white/5" />
              <div className="flex justify-between text-[10px] text-slate-600 font-bold tracking-tighter">
                <span>0%</span>
                <span>TARGET</span>
                <span>100%</span>
              </div>
            </div>

            <div className="flex flex-col gap-2">
              <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">보유 기간 (Lifetime)</label>
              <input 
                type="number" 
                defaultValue="12" 
                className="bg-slate-900 border border-white/10 text-white text-sm rounded-xl p-3 focus:border-blue-500 outline-none transition-all font-mono font-bold"
              />
            </div>

            <button className="w-full bg-slate-100 hover:bg-white text-slate-950 font-black text-sm py-4 rounded-2xl transition-all shadow-xl active:scale-95 mt-4">
              시나리오 적용 및 재계산
            </button>
          </div>

          <div className="mt-10 pt-8 border-t border-white/10">
            <h4 className="text-[10px] font-black text-slate-500 uppercase tracking-widest mb-4 px-1">최근 분석 이력</h4>
            <div className="space-y-2">
              {[
                { label: '2026-Q1 결산용', date: '04.20', type: 'Official' },
                { label: '금리인상 시나리오', date: '04.15', type: 'Simulation' },
              ].map((item, i) => (
                <div key={i} className="flex justify-between items-center p-3 rounded-xl bg-white/5 border border-transparent hover:border-white/10 transition-all cursor-pointer group">
                  <div className="flex flex-col">
                    <span className="text-xs font-bold text-slate-300 group-hover:text-white transition-colors">{item.label}</span>
                    <span className="text-[9px] text-slate-600 font-black uppercase">{item.type}</span>
                  </div>
                  <span className="text-[10px] font-mono text-slate-500 font-bold">{item.date}</span>
                </div>
              ))}
            </div>
          </div>
        </aside>

        {/* 결과 분석 패널 */}
        <section className="lg:col-span-8 flex flex-col gap-8">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="bg-white/5 border border-white/10 p-6 rounded-[28px] hover:bg-white/[0.08] transition-all">
              <div className="text-[10px] font-black text-slate-500 uppercase tracking-widest mb-4">Total ECL</div>
              <div className="text-3xl font-black italic text-white leading-none">₩42.5B</div>
              <div className="text-[10px] text-rose-400 mt-4 font-bold flex items-center gap-1">
                <TrendingUp size={12} /> VS PREV +2.1B
              </div>
            </div>
            <div className="bg-white/5 border border-white/10 p-6 rounded-[28px] hover:bg-white/[0.08] transition-all">
              <div className="text-[10px] font-black text-slate-500 uppercase tracking-widest mb-4">AVG WAPD</div>
              <div className="text-3xl font-black italic text-white leading-none">0.84%</div>
              <div className="text-[10px] text-slate-500 mt-4 font-bold">STAGE 1 WEIGHTED AVG</div>
            </div>
            <div className="bg-white/5 border border-white/10 p-6 rounded-[28px] hover:bg-white/[0.08] transition-all border-amber-500/20">
              <div className="text-[10px] font-black text-slate-500 uppercase tracking-widest mb-4">SICR TRANSITION</div>
              <div className="text-3xl font-black italic text-white leading-none">12.4%</div>
              <div className="text-[10px] text-amber-500 mt-4 font-bold flex items-center gap-1">
                <AlertCircle size={12} /> CRITICAL MONITORING
              </div>
            </div>
          </div>

          <div className="flex-1 bg-white/5 border border-white/10 rounded-[32px] p-8 backdrop-blur-xl relative overflow-hidden group">
            <h3 className="text-lg font-black text-white flex items-center gap-3 mb-10 tracking-tight">
               <FileSpreadsheet size={20} className="text-emerald-400" /> 자산군 및 단계 별 충당금 적립률 분석
            </h3>
            <div style={{ width: '100%', height: 380 }}>
              <ResponsiveContainer>
                <ComposedChart data={stageComparisonData} layout="vertical">
                  <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" horizontal={false} />
                  <XAxis type="number" stroke="#475569" fontSize={11} fontWeight="bold" axisLine={false} tickLine={false} />
                  <YAxis dataKey="group" type="category" stroke="#475569" fontSize={11} width={80} axisLine={false} tickLine={false} fontWeight="black" />
                  <Tooltip 
                    cursor={{ fill: 'rgba(255,255,255,0.03)' }}
                    contentStyle={{ background: '#0f172a', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '16px' }}
                  />
                  <Legend iconType="circle" wrapperStyle={{ paddingTop: '20px', fontSize: '11px', fontWeight: 'bold' }} />
                  <Bar dataKey="stage1" fill="#3b82f6" name="Stage 1" stackId="a" barSize={34} radius={[0, 0, 0, 0]} />
                  <Bar dataKey="stage2" fill="#f59e0b" name="Stage 2 (SICR)" stackId="a" />
                  <Bar dataKey="stage3" fill="#ef4444" name="Stage 3 (Default)" stackId="a" radius={[0, 6, 6, 0]} />
                </ComposedChart>
              </ResponsiveContainer>
            </div>
            
            <div className="absolute -bottom-10 -right-10 opacity-[0.03] group-hover:opacity-[0.05] transition-opacity duration-700 pointer-events-none">
               <Calculator size={300} className="text-white" />
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}

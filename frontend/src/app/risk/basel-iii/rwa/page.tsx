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
    <div className="flex flex-col gap-8">
      {/* 타이틀 및 상단 액션 */}
      <header className="flex justify-between items-center">
        <div>
          <h2 className="text-3xl font-black text-white italic tracking-tight">Basel III RWA 모니터링</h2>
          <p className="text-slate-500 mt-2 text-sm font-medium">위험가중자산(RWA) 산출 및 자본적정성 지표(BIS 비율) 실시간 통합 관리</p>
        </div>
        <div className="flex gap-3">
          <button className="bg-slate-800 hover:bg-slate-700 text-slate-300 px-4 py-2 rounded-xl border border-white/5 transition-all flex items-center gap-2 text-sm font-bold shadow-lg">
             <Filter size={18} /> 필터
          </button>
          <button className="bg-blue-600 hover:bg-blue-500 text-white px-5 py-2 rounded-xl transition-all flex items-center gap-2 text-sm font-black shadow-lg shadow-blue-500/20">
             <Download size={18} /> 보고서 생성
          </button>
        </div>
      </header>

      {/* KPI 카드 그리드 */}
      <section className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <div className="bg-white/5 border border-white/10 p-6 rounded-3xl hover:bg-white/[0.08] transition-all group">
          <div className="flex justify-between items-start mb-4">
            <span className="text-slate-500 text-xs font-black uppercase tracking-widest leading-none">BIS Capital Ratio</span>
            <div className="p-2 bg-emerald-500/10 rounded-lg text-emerald-400 group-hover:bg-emerald-500 group-hover:text-white transition-all"><ShieldCheck size={18} /></div>
          </div>
          <div className="text-3xl font-black italic text-white leading-none">13.52%</div>
          <div className="mt-4 flex items-center gap-1.5 text-xs font-bold text-red-400">
            <TrendingDown size={14} /> -0.38% MoM
          </div>
        </div>

        <div className="bg-white/5 border border-white/10 p-6 rounded-3xl hover:bg-white/[0.08] transition-all group">
          <div className="flex justify-between items-start mb-4">
            <span className="text-slate-500 text-xs font-black uppercase tracking-widest leading-none">CET1 Ratio</span>
            <div className="p-2 bg-blue-500/10 rounded-lg text-blue-400 group-hover:bg-blue-500 group-hover:text-white transition-all"><Activity size={18} /></div>
          </div>
          <div className="text-3xl font-black italic text-white leading-none">11.24%</div>
          <div className="mt-4 flex items-center gap-1.5 text-xs font-bold text-emerald-400">
            <TrendingUp size={14} /> +0.12% MoM
          </div>
        </div>

        <div className="bg-white/5 border border-white/10 p-6 rounded-3xl hover:bg-white/[0.08] transition-all group">
          <div className="flex justify-between items-start mb-4">
            <span className="text-slate-500 text-xs font-black uppercase tracking-widest leading-none">Total RWA</span>
            <div className="p-2 bg-amber-500/10 rounded-lg text-amber-400 group-hover:bg-amber-500 group-hover:text-white transition-all"><PieIcon size={18} /></div>
          </div>
          <div className="text-3xl font-black italic text-white leading-none">₩512.4B</div>
          <div className="mt-4 flex items-center gap-1.5 text-xs font-bold text-emerald-400">
            <TrendingUp size={14} /> +22.4B MoM
          </div>
        </div>

        <div className="bg-white/5 border border-white/10 p-6 rounded-3xl hover:bg-white/[0.08] transition-all group">
          <div className="flex justify-between items-start mb-4">
            <span className="text-slate-500 text-xs font-black uppercase tracking-widest leading-none">Exposure (EAD)</span>
            <div className="p-2 bg-rose-500/10 rounded-lg text-rose-400 group-hover:bg-rose-500 group-hover:text-white transition-all"><AlertTriangle size={18} /></div>
          </div>
          <div className="text-3xl font-black italic text-white leading-none">₩684.2B</div>
          <div className="mt-4 flex items-center gap-1.5 text-xs font-bold text-red-400">
            <TrendingDown size={14} /> -4.5B MoM
          </div>
        </div>
      </section>

      {/* 메인 차트 영역 */}
      <section className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* 리스크 유형별 비중 */}
        <div className="lg:col-span-4 bg-white/5 border border-white/10 rounded-[32px] p-8 backdrop-blur-xl">
          <div className="flex justify-between items-center mb-8">
            <h3 className="text-lg font-bold text-white leading-none">리스크 유형별 비중</h3>
            <PieIcon size={18} className="text-slate-500" />
          </div>
          <div style={{ width: '100%', height: 260 }}>
            <ResponsiveContainer>
              <PieChart>
                <Pie
                  data={riskTypeData}
                  cx="50%"
                  cy="50%"
                  innerRadius={60}
                  outerRadius={85}
                  paddingAngle={8}
                  dataKey="value"
                  stroke="none"
                >
                  {riskTypeData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip 
                  contentStyle={{ background: '#1e293b', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '12px', fontSize: '12px', fontWeight: 'bold' }}
                />
              </PieChart>
            </ResponsiveContainer>
          </div>
          <div className="grid grid-cols-1 gap-2 mt-4 px-4">
             {riskTypeData.map((entry, i) => (
               <div key={i} className="flex items-center justify-between text-xs font-bold">
                  <div className="flex items-center gap-2">
                     <div className="w-2 h-2 rounded-full" style={{ backgroundColor: entry.color }} />
                     <span className="text-slate-400">{entry.name}</span>
                  </div>
                  <span className="text-white italic">{entry.value}%</span>
               </div>
             ))}
          </div>
        </div>

        {/* 월별 RWA 추이 */}
        <div className="lg:col-span-8 bg-white/5 border border-white/10 rounded-[32px] p-8 backdrop-blur-xl">
          <div className="flex justify-between items-center mb-8">
             <h3 className="text-lg font-bold text-white leading-none">월별 RWA 및 BIS 비율 추감상세</h3>
             <TrendingUp size={18} className="text-slate-500" />
          </div>
          <div style={{ width: '100%', height: 320 }}>
            <ResponsiveContainer>
              <BarChart data={monthlyTrendData}>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" vertical={false} />
                <XAxis dataKey="month" stroke="#475569" fontSize={11} fontWeight="bold" axisLine={false} tickLine={false} />
                <YAxis yAxisId="left" stroke="#475569" fontSize={11} axisLine={false} tickLine={false} />
                <YAxis yAxisId="right" orientation="right" stroke="#475569" fontSize={11} axisLine={false} tickLine={false} />
                <Tooltip 
                  cursor={{ fill: 'rgba(255,255,255,0.03)' }}
                  contentStyle={{ background: '#0f172a', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '16px', boxShadow: '0 10px 15px -3px rgba(0, 0, 0, 0.3)' }}
                />
                <Bar yAxisId="left" dataKey="rwa" fill="#3b82f6" name="RWA" radius={[6, 6, 0, 0]} barSize={24} />
                <Line yAxisId="right" type="monotone" dataKey="ratio" stroke="#4ade80" name="BIS Ratio" strokeWidth={3} dot={{ r: 4, fill: '#4ade80', stroke: '#0f172a', strokeWidth: 2 }} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </section>

      {/* 세부 내역 테이블 */}
      <section className="bg-white/5 border border-white/10 rounded-[32px] overflow-hidden">
        <div className="p-8 border-b border-white/5 flex justify-between items-center">
            <h3 className="text-lg font-bold text-white">세부 자산군별 RWA 산출 내역</h3>
            <span className="px-3 py-1 bg-blue-500/10 text-blue-400 text-[10px] font-black rounded-full border border-blue-500/20 tracking-tighter uppercase">As of 2026-04-23</span>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="text-slate-500 text-[10px] font-black uppercase tracking-widest bg-white/[0.02]">
                <th className="px-8 py-4">Asset Class</th>
                <th className="px-8 py-4">Exposure (EAD)</th>
                <th className="px-8 py-4">Risk Weight (RW)</th>
                <th className="px-8 py-4">Credit RWA</th>
                <th className="px-8 py-4 text-center">Risk Tier</th>
                <th className="px-8 py-4 text-right">Variance</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5">
              {[
                { class: '기업대출 (Corporate)', ead: '245.2B', rw: '75%', rwa: '183.9B', tier: 'MODERATE', tierColor: 'text-amber-400 bg-amber-400/10', var: '+12.4B', plus: true },
                { class: '소매대출 (Retail)', ead: '120.5B', rw: '45%', rwa: '54.2B', tier: 'STABLE', tierColor: 'text-emerald-400 bg-emerald-400/10', var: '-2.1B', plus: false },
                { class: '주택담보대출 (Mortgage)', ead: '185.7B', rw: '35%', rwa: '65.0B', tier: 'STABLE', tierColor: 'text-emerald-400 bg-emerald-400/10', var: '+4.8B', plus: true },
                { class: '고위험 자산 (High Risk)', ead: '45.8B', rw: '150%', rwa: '68.7B', tier: 'CRITICAL', tierColor: 'text-rose-400 bg-rose-400/10', var: '+8.5B', plus: true },
              ].map((row, idx) => (
                <tr key={idx} className="hover:bg-white/[0.03] transition-colors group">
                  <td className="px-8 py-5 text-sm font-bold text-slate-200 group-hover:text-blue-400">{row.class}</td>
                  <td className="px-8 py-5 text-sm font-mono text-slate-400 font-bold">{row.ead}</td>
                  <td className="px-8 py-5 text-sm font-mono text-slate-500 font-bold">{row.rw}</td>
                  <td className="px-8 py-5 text-sm font-black text-white italic">{row.rwa}</td>
                  <td className="px-8 py-5 text-center">
                    <span className={`px-2.5 py-1 rounded-md text-[10px] font-black ${row.tierColor} border border-white/5`}>{row.tier}</span>
                  </td>
                  <td className={`px-8 py-5 text-right font-bold text-sm ${row.plus ? 'text-rose-400' : 'text-emerald-400'}`}>
                    {row.plus ? '+' : ''}{row.var}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}

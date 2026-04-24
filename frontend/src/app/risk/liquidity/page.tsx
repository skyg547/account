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
    <div className="flex flex-col gap-8">
      <header className="flex justify-between items-center">
        <div>
          <h2 className="text-3xl font-black text-white italic tracking-tight">유동성 리스크 모니터링</h2>
          <p className="text-slate-500 mt-2 text-sm font-medium leading-none">LCR, NSFR 규제 대응 및 단기 유동성 과부족 현황 통합 분석</p>
        </div>
        <div className="flex gap-2">
          <button className="bg-slate-800 hover:bg-slate-700 text-slate-300 px-4 py-2 rounded-xl border border-white/5 transition-all flex items-center gap-2 text-sm font-bold shadow-lg">
            <Clock size={18} /> 실시간: 14:30:22
          </button>
          <button className="bg-blue-600 hover:bg-blue-500 text-white px-5 py-2 rounded-xl transition-all flex items-center gap-2 text-sm font-black shadow-lg shadow-blue-500/20">
            <FileText size={18} /> 규제 보고서 출력
          </button>
        </div>
      </header>

      <section className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* LCR Card */}
        <div className="bg-white/5 border border-white/10 p-10 rounded-[40px] backdrop-blur-xl relative overflow-hidden group">
          <div className="relative z-10">
            <h3 className="text-sm font-black text-slate-500 uppercase tracking-widest mb-2">LCR (Liquidity Coverage Ratio)</h3>
            <div className="flex items-baseline gap-2">
               <div className="text-6xl font-black italic text-white tracking-tighter">118.4%</div>
               <div className="text-emerald-500 text-xs font-bold bg-emerald-500/10 px-2 py-0.5 rounded-md border border-emerald-500/20 animate-pulse">STABLE</div>
            </div>
            <div className="mt-8 flex items-center gap-4">
               <div className="flex-1 h-3 bg-slate-900 rounded-full border border-white/5 overflow-hidden">
                  <div className="h-full bg-gradient-to-r from-blue-600 to-emerald-500 w-[78%] rounded-full shadow-[0_0_15px_rgba(16,185,129,0.3)] transition-all duration-1000" />
               </div>
               <span className="text-[10px] font-black text-slate-500 tracking-tighter">REGULATORY: 100% MIN</span>
            </div>
          </div>
          <ShieldCheck size={160} className="absolute -right-10 -bottom-10 text-white opacity-[0.03] group-hover:opacity-[0.05] transition-opacity duration-700" />
        </div>

        {/* NSFR Card */}
        <div className="bg-white/5 border border-white/10 p-10 rounded-[40px] backdrop-blur-xl relative overflow-hidden group">
          <div className="relative z-10">
            <h3 className="text-sm font-black text-slate-500 uppercase tracking-widest mb-2">NSFR (Net Stable Funding Ratio)</h3>
            <div className="flex items-baseline gap-2">
               <div className="text-6xl font-black italic text-white tracking-tighter">105.2%</div>
               <div className="text-amber-500 text-xs font-bold bg-amber-500/10 px-2 py-0.5 rounded-md border border-amber-500/20 line-clamp-1">NEAR LIMIT</div>
            </div>
            <div className="mt-8 flex items-center gap-4">
               <div className="flex-1 h-3 bg-slate-900 rounded-full border border-white/5 overflow-hidden">
                  <div className="h-full bg-gradient-to-r from-blue-600 to-amber-500 w-[62%] rounded-full shadow-[0_0_15px_rgba(245,158,11,0.3)] transition-all duration-1000" />
               </div>
               <span className="text-[10px] font-black text-slate-500 tracking-tighter">REGULATORY: 100% MIN</span>
            </div>
          </div>
          <AlertTriangle size={160} className="absolute -right-10 -bottom-10 text-white opacity-[0.03] group-hover:opacity-[0.05] transition-opacity duration-700" />
        </div>
      </section>

      <section className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-stretch">
        {/* 현금유출입 갭 분석 */}
        <div className="lg:col-span-7 bg-white/5 border border-white/10 rounded-[32px] p-8 backdrop-blur-xl">
          <div className="flex justify-between items-center mb-8">
            <h3 className="text-lg font-bold text-white flex items-center gap-3 leading-none tracking-tight">
              <TrendingUp size={20} className="text-emerald-400" /> 기간별 순현금유출입(Gap) 분석
            </h3>
            <span className="text-[10px] font-black text-slate-500 uppercase tracking-widest bg-slate-900 px-2 py-1 rounded-md border border-white/5">Forecast Mode</span>
          </div>
          <div style={{ width: '100%', height: 320 }}>
            <ResponsiveContainer>
              <BarChart data={maturityGapData}>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" vertical={false} />
                <XAxis dataKey="bucket" stroke="#475569" fontSize={11} fontWeight="black" axisLine={false} tickLine={false} />
                <YAxis stroke="#475569" fontSize={11} axisLine={false} tickLine={false} />
                <Tooltip 
                   cursor={{ fill: 'rgba(255,255,255,0.03)' }}
                   contentStyle={{ background: '#0f172a', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '16px' }}
                />
                <Bar dataKey="gap" name="순유입액" fill="#3b82f6" radius={[6, 6, 0, 0]} barSize={40}>
                  {maturityGapData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.gap > 0 ? '#3b82f6' : '#ef4444'} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* HQLA Composition */}
        <div className="lg:col-span-5 bg-white/5 border border-white/10 rounded-[32px] p-8 backdrop-blur-xl flex flex-col">
          <h3 className="text-lg font-bold text-white flex items-center gap-3 mb-8 leading-none tracking-tight">
            <Droplets size={20} className="text-blue-400" /> 고유동성자산(HQLA) 구성
          </h3>
          <div className="space-y-3 flex-1">
            {[
              { name: 'Level 1 자산 (현금, 국채)', value: '₩320.5B', weight: '100%', color: 'bg-emerald-500' },
              { name: 'Level 2A 자산 (공공기관채)', value: '₩85.2B', weight: '85%', color: 'bg-blue-500' },
              { name: 'Level 2B 자산 (우량회사채)', value: '₩44.3B', weight: '50%', color: 'bg-amber-500' },
            ].map((item, i) => (
              <div key={i} className="p-4 rounded-2xl bg-white/[0.03] border border-white/5 hover:border-white/10 hover:bg-white/[0.05] transition-all group">
                <div className="flex justify-between items-center mb-2">
                  <span className="text-xs font-bold text-slate-400 group-hover:text-slate-200 transition-colors">{item.name}</span>
                  <span className="text-sm font-black text-white italic tracking-tight">{item.value}</span>
                </div>
                <div className="flex items-center gap-3">
                   <div className="flex-1 h-1 bg-slate-800 rounded-full overflow-hidden">
                      <div className={`h-full ${item.color} w-[${item.weight}] opacity-50`} style={{ width: item.weight }} />
                   </div>
                   <span className="text-[9px] font-black text-slate-600">{item.weight} WGHT.</span>
                </div>
              </div>
            ))}
            
            <div className="mt-4 p-5 rounded-2xl bg-blue-600/10 border border-blue-600/20 flex justify-between items-center group cursor-default">
              <span className="text-sm font-black text-white italic group-hover:text-blue-400 transition-colors">총 가용 유동성 자산</span>
              <span className="text-2xl font-black italic text-blue-400 tracking-tighter transition-all">₩450.0B</span>
            </div>
          </div>

          <div className="mt-8 p-5 rounded-2xl bg-amber-500/5 border border-amber-500/10 flex gap-4 items-center">
             <div className="w-10 h-10 rounded-full bg-amber-500/10 flex items-center justify-center text-amber-500 shrink-0">
                <AlertTriangle size={20} />
             </div>
             <div>
               <p className="text-xs font-black text-amber-500 uppercase tracking-tighter">Early Warning Alert</p>
               <p className="mt-1 text-[11px] text-slate-500 font-medium leading-relaxed tracking-tighter">최근 3일간 외화 예수금 유출량이 임계치를 초과했습니다.</p>
             </div>
          </div>
        </div>
      </section>
    </div>
  );
}

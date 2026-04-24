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
    <div className="flex flex-col gap-8">
      <header className="flex justify-between items-center">
        <div>
          <h2 className="text-3xl font-black text-white italic tracking-tight">ALM / 금리 리스크 관리</h2>
          <p className="text-slate-500 mt-2 text-sm font-medium leading-none">자산 및 부채의 금리 민감도 분석과 전략적 기간별 갭 통합 관리</p>
        </div>
        <div className="flex gap-2">
          <button className="bg-slate-800 hover:bg-slate-700 text-slate-300 px-4 py-2 rounded-xl border border-white/5 transition-all flex items-center gap-2 text-sm font-bold shadow-lg">
            <Calendar size={18} /> 기준일자: 2026-04-23
          </button>
          <button className="bg-blue-600 hover:bg-blue-500 text-white px-5 py-2 rounded-xl transition-all flex items-center gap-2 text-sm font-black shadow-lg shadow-blue-500/20">
            <TrendingUp size={18} /> 시나리오 분석 실행
          </button>
        </div>
      </header>

      <section className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* 금리 갭 추이 그래프 */}
        <div className="lg:col-span-8 bg-white/5 border border-white/10 rounded-[32px] p-8 backdrop-blur-xl">
          <div className="flex justify-between items-center mb-8">
            <h3 className="text-lg font-bold text-white flex items-center gap-3 leading-none">
              <ArrowRightLeft size={20} className="text-blue-400" /> 기간별 자산-부채 금리 갭 추이
            </h3>
            <span className="text-[10px] font-black text-slate-500 uppercase tracking-widest bg-slate-900 px-2 py-1 rounded-md border border-white/5">UNIT: ₩ TRILLION</span>
          </div>
          <div style={{ width: '100%', height: 360 }}>
            <ResponsiveContainer>
              <ComposedChart data={gapData}>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" vertical={false} />
                <XAxis dataKey="period" stroke="#475569" fontSize={11} fontWeight="black" axisLine={false} tickLine={false} />
                <YAxis stroke="#475569" fontSize={11} axisLine={false} tickLine={false} />
                <Tooltip 
                  cursor={{ fill: 'rgba(255,255,255,0.03)' }}
                  contentStyle={{ background: '#0f172a', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '16px' }}
                />
                <Legend wrapperStyle={{ paddingTop: '20px', fontSize: '11px', fontWeight: 'bold' }} />
                <Bar dataKey="asset" name="자산(RSA)" fill="#3b82f6" opacity={0.8} radius={[4, 4, 0, 0]} barSize={34} />
                <Bar dataKey="liability" name="부채(RSL)" fill="#ef4444" opacity={0.8} radius={[4, 4, 0, 0]} barSize={34} />
                <Line type="monotone" dataKey="gap" name="Interest Gap" stroke="#fbbf24" strokeWidth={4} dot={{ r: 5, fill: '#fbbf24', stroke: '#0f172a', strokeWidth: 2 }} />
              </ComposedChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* 시나리오 설정 패널 */}
        <div className="lg:col-span-4 bg-white/5 border border-white/10 rounded-[32px] p-8 backdrop-blur-xl h-full shadow-2xl">
          <h3 className="text-lg font-bold text-white flex items-center gap-3 mb-8 leading-none tracking-tight">
            <Settings2 size={20} className="text-purple-400" /> 금리 충격 시나리오
          </h3>
          <div className="space-y-6">
            <div className="flex flex-col gap-2">
              <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">Parallel Shift (bp)</label>
              <input type="number" defaultValue="100" className="bg-slate-900 border border-white/10 rounded-xl p-3 text-white text-sm font-mono font-bold focus:border-blue-500 outline-none transition-all" />
            </div>
            <div className="flex flex-col gap-2">
              <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">시장금리 변동 전망</label>
              <select className="bg-slate-900 border border-white/10 rounded-xl p-3 text-white text-sm font-bold focus:border-blue-500 outline-none transition-all cursor-pointer">
                <option>급격한 금리 인상</option>
                <option>점진적 인상</option>
                <option>금리 동결</option>
              </select>
            </div>
            <div className="mt-8 p-6 rounded-[24px] bg-blue-500/5 border border-blue-500/20 group hover:bg-blue-500/10 transition-all cursor-default">
              <div className="text-[10px] font-black text-blue-500 uppercase tracking-widest mb-1.5 px-0.5">EST. NII VARIANCE</div>
              <div className="text-2xl font-black italic text-blue-400 tracking-tight">+₩125.4B</div>
              <p className="text-[10px] text-slate-500 font-bold mt-2 leading-relaxed tracking-tighter">100bp 금리 상승 시 자산 유리 포지션 보유 (이자이익 증가 기대)</p>
            </div>
            <div className="mt-4 p-6 rounded-[24px] bg-rose-500/5 border border-rose-500/20 group hover:bg-rose-500/10 transition-all cursor-default">
              <div className="text-[10px] font-black text-rose-500 uppercase tracking-widest mb-1.5 px-0.5">EST. EvE VARIANCE</div>
              <div className="text-2xl font-black italic text-rose-400 tracking-tight">-₩45.2B</div>
              <p className="text-[10px] text-slate-500 font-bold mt-2 leading-relaxed tracking-tighter">경제적 가치 변동 영향도 시뮬레이션 결과</p>
            </div>
          </div>
        </div>
      </section>

      {/* 금리 갭 분석 상세표 */}
      <section className="bg-white/5 border border-white/10 rounded-[32px] overflow-hidden">
        <div className="p-8 border-b border-white/5 bg-white/[0.01] flex justify-between items-center">
            <h3 className="text-lg font-bold text-white flex items-center gap-3 leading-none">
              <Scale size={20} className="text-amber-400" /> ALM 금리 갭 분석 상세 테이블
            </h3>
            <span className="text-[10px] font-black text-slate-500 italic uppercase">Interest Rate Sensitivity Analysis</span>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="text-slate-500 text-[10px] font-black uppercase tracking-widest bg-white/[0.02]">
                <th className="px-8 py-5">Classification</th>
                <th className="px-6 py-5">{"<"} 1 Month</th>
                <th className="px-6 py-5">1-3 Months</th>
                <th className="px-6 py-5">3-6 Months</th>
                <th className="px-6 py-5">6M-1 Year</th>
                <th className="px-6 py-5">1-3 Years</th>
                <th className="px-6 py-5">3-5 Years</th>
                <th className="px-8 py-5 text-right">Total</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5">
              {[
                { label: '금리민감자산 (RSA)', values: [15.2, 24.5, 42.8, 68.4, 120.5, 85.2], total: 356.6, bold: false },
                { label: '금리민감부채 (RSL)', values: [12.4, 28.1, 35.2, 72.5, 98.4, 110.2], total: 356.8, bold: false },
                { label: '금리 갭 (Interest Gap)', values: [2.8, -3.6, 7.6, -4.1, 22.1, -25.0], total: -0.2, bold: true, colored: true },
                { label: '누적 금리 갭 (Cumul.)', values: [2.8, -0.8, 6.8, 2.7, 24.8, -0.2], total: '-', bold: false, textSlate: true },
              ].map((row, idx) => (
                <tr key={idx} className={`hover:bg-white/[0.03] transition-colors group ${row.bold ? 'bg-blue-600/5' : ''}`}>
                  <td className={`px-8 py-5 text-sm font-bold ${row.bold ? 'text-white italic' : 'text-slate-300'}`}>{row.label}</td>
                  {row.values.map((v, i) => (
                    <td key={i} className={`px-6 py-5 text-sm font-mono font-bold ${row.colored ? (v >= 0 ? 'text-emerald-400' : 'text-rose-400') : (row.textSlate ? 'text-slate-500' : 'text-slate-400')}`}>
                      {v > 0 && row.colored ? '+' : ''}{v}
                    </td>
                  ))}
                  <td className={`px-8 py-5 text-sm text-right font-black italic tracking-tight ${row.bold ? 'text-white' : 'text-slate-500'}`}>{row.total}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}

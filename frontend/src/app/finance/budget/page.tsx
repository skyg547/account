"use client";

import React from 'react';
import { 
  Wallet, 
  TrendUp, 
  TrendDown, 
  Plus, 
  CheckCircle2, 
  AlertTriangle,
  BarChart,
  ArrowRight,
  Target,
  FileSpreadsheet
} from 'lucide-react';

/**
 * [예산 관리 화면]
 * 전사/부서별 예산을 편성하고 실제 집행액과 비교하여 통제합니다.
 */
export default function BudgetPage() {
  return (
    <div className="space-y-10">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
        <div className="space-y-2">
          <div className="flex items-center gap-3 text-purple-500 mb-2">
            <Wallet size={20} />
            <span className="text-xs font-black uppercase tracking-[0.3em]">Budgetary Control System</span>
          </div>
          <h2 className="text-4xl font-black text-white tracking-tighter italic">
            전사 예산 편성 및 집행 관리
          </h2>
          <p className="text-slate-500 font-medium max-w-2xl">
            사업부문별 가용 리소스를 정의하고, 실시간 지출 프로세스와 연동하여 예산 초과를 방지합니다.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button className="px-6 py-3 bg-white/5 hover:bg-white/10 border border-white/10 rounded-2xl text-slate-400 text-sm font-black transition-all flex items-center gap-2">
            <FileSpreadsheet size={18} /> 예산 일괄 업로드
          </button>
          <button className="px-6 py-3 bg-blue-600 hover:bg-blue-500 rounded-2xl text-white text-sm font-black transition-all shadow-lg shadow-blue-600/20 flex items-center gap-2">
            <Plus size={18} /> 예산 증액/조정 요청
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {[
          { label: 'Total Budget (Draft)', amount: '2,500,000,000', change: '+5.0%', type: 'total' },
          { label: 'Executed Amount', amount: '1,120,450,000', change: '44.8%', type: 'exec' },
          { label: 'Remaining Budget', amount: '1,379,550,000', change: '55.2%', type: 'remain' },
        ].map((stat, i) => (
          <div key={i} className="glass-panel p-8 rounded-[3rem] border border-white/10 bg-white/[0.01] flex flex-col justify-between">
             <span className="text-[10px] font-black text-slate-500 uppercase tracking-widest leading-none">{stat.label}</span>
             <div className="mt-4 flex items-end justify-between">
                <span className="text-3xl font-black text-white italic tracking-tighter">₩{stat.amount}</span>
                <span className={`text-[10px] font-black px-2 py-1 rounded-lg border ${
                  stat.type === 'total' ? 'text-blue-500 border-blue-500/20' :
                  stat.type === 'exec' ? 'text-rose-500 border-rose-500/20' :
                  'text-emerald-500 border-emerald-500/20'
                }`}>
                   {stat.change}
                </span>
             </div>
          </div>
        ))}
      </div>

      {/* Budget Execution by Department */}
      <div className="glass-panel p-10 rounded-[3rem] border border-white/10 bg-white/[0.01]">
         <div className="flex items-center justify-between mb-8">
            <h3 className="text-xl font-black text-white italic tracking-tight uppercase">Budget Items & Status</h3>
            <div className="flex gap-4">
               <div className="flex items-center gap-2"><div className="w-2 h-2 rounded-full bg-emerald-500" /><span className="text-[10px] font-black text-slate-600 uppercase">On Track</span></div>
               <div className="flex items-center gap-2"><div className="w-2 h-2 rounded-full bg-amber-500" /><span className="text-[10px] font-black text-slate-600 uppercase">Near Limit</span></div>
               <div className="flex items-center gap-2"><div className="w-2 h-2 rounded-full bg-rose-500" /><span className="text-[10px] font-black text-slate-600 uppercase">Over Budget</span></div>
            </div>
         </div>

         <div className="space-y-8">
            {[
              { dept: '디지털혁신실', item: '클라우드 인프라 운영비', budget: '500,000,000', exec: '320,000,000', percent: 64 },
              { dept: '재무회계본부', item: '외부 감사 수수료', budget: '200,000,000', exec: '185,000,000', percent: 92.5 },
              { dept: '인사노무팀', item: '사내 복지 기금', budget: '800,000,000', exec: '350,000,000', percent: 43.7 },
              { dept: '영업본부', item: '마케팅/프로모션', budget: '1,000,000,000', exec: '1,050,000,000', percent: 105 },
            ].map((row, idx) => (
              <div key={idx} className="space-y-3">
                 <div className="flex justify-between items-end">
                    <div className="flex flex-col">
                       <span className="text-[10px] font-black text-slate-600 uppercase tracking-widest">{row.dept}</span>
                       <span className="text-base font-black text-white tracking-tight">{row.item}</span>
                    </div>
                    <div className="text-right">
                       <span className="text-xs font-mono font-black text-slate-400">₩{row.exec} / </span>
                       <span className="text-xs font-mono font-black text-blue-500">₩{row.budget}</span>
                       <span className={`ml-4 text-sm font-black ${row.percent > 100 ? 'text-rose-500' : row.percent > 90 ? 'text-amber-500' : 'text-emerald-500'}`}>{row.percent}%</span>
                    </div>
                 </div>
                 <div className="h-1.5 w-full bg-slate-900 rounded-full overflow-hidden border border-white/5">
                    <div 
                      className={`h-full rounded-full transition-all duration-1000 ${
                        row.percent > 100 ? 'bg-rose-500 animate-pulse' : 
                        row.percent > 90 ? 'bg-amber-500' : 
                        'bg-emerald-500'
                      }`} 
                      style={{ width: `${Math.min(row.percent, 100)}%` }} 
                    />
                 </div>
              </div>
            ))}
         </div>
      </div>
    </div>
  );
}

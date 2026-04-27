"use client";

import React from 'react';
import { 
  Activity, 
  TrendingUp, 
  TrendingDown, 
  ArrowRight, 
  PieChart, 
  DollarSign,
  Calendar,
  AlertCircle,
  Clock,
  ArrowUpRight
} from 'lucide-react';

/**
 * [자금 수지 및 현금 흐름 계획 화면] 
 * 가용 현금을 모니터링하고 미래 지출/수입 일정을 통해 자금 과부족을 예측합니다.
 */
export default function CashflowPage() {
  return (
    <div className="space-y-10">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
        <div className="space-y-2">
          <div className="flex items-center gap-3 text-emerald-500 mb-2">
            <Activity size={20} />
            <span className="text-xs font-black uppercase tracking-[0.3em]">Cash Management System</span>
          </div>
          <h2 className="text-4xl font-black text-white tracking-tighter italic">
            실시간 자금 수지 모니터링
          </h2>
          <p className="text-slate-500 font-medium max-w-2xl">
            전사 가용 유동성을 파악하고 매일 발생하는 자금 유입/유출을 실시간으로 추적하여 과부족을 예방합니다.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button className="px-6 py-3 bg-white/5 hover:bg-white/10 border border-white/10 rounded-2xl text-slate-400 text-sm font-black transition-all flex items-center gap-2">
            <Calendar size={18} /> 자금 일계표 조회
          </button>
          <button className="px-6 py-3 bg-blue-600 hover:bg-blue-500 rounded-2xl text-white text-sm font-black transition-all shadow-lg shadow-blue-600/20 flex items-center gap-2 px-8">
             AI 자금 예측 실행
          </button>
        </div>
      </div>

      {/* Liquidity Stats */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
         {[
           { label: 'Total Liquidity', value: '₩42.5B', color: 'blue' },
           { label: 'Daily Inflow', value: '₩1.2B', color: 'emerald' },
           { label: 'Daily Outflow', value: '₩0.8B', color: 'rose' },
           { label: 'Net Cash Flow', value: '+₩0.4B', color: 'amber' },
         ].map((stat, i) => (
           <div key={i} className="glass-panel p-6 rounded-[2rem] border border-white/10 bg-white/[0.01]">
              <span className="text-[10px] font-black text-slate-600 uppercase tracking-widest">{stat.label}</span>
              <div className="text-2xl font-black text-white mt-2 italic tracking-tighter">{stat.value}</div>
              <div className="h-1 w-full bg-slate-900 rounded-full mt-4 overflow-hidden border border-white/5">
                 <div className={`h-full bg-${stat.color}-600 rounded-full`} style={{ width: '40%' }} />
              </div>
           </div>
         ))}
      </div>

      {/* Cash Flow Forecast Chart Placeholder & Schedule Table */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
         {/* Forecast Visualization */}
         <div className="lg:col-span-8 glass-panel p-10 rounded-[3rem] border border-white/10 bg-white/[0.01]">
            <div className="flex items-center justify-between mb-10">
               <h3 className="text-xl font-black text-white italic tracking-tight uppercase">Cash Balance Forecast (30 Days)</h3>
               <div className="flex items-center gap-2 text-rose-500 text-xs font-black bg-rose-500/10 px-3 py-1 rounded-full border border-rose-500/20 shadow-lg shadow-rose-900/10 animate-pulse">
                  <AlertCircle size={14} /> Potential Shortfall Warning (D+15)
               </div>
            </div>
            
            <div className="h-64 rounded-[2rem] bg-slate-950 border border-white/5 flex items-center justify-center italic text-slate-700 text-xs text-center border-dashed group hover:border-blue-500/30 transition-all cursor-pointer">
               <div>
                  <div className="mb-4 flex justify-center"><Activity size={40} className="text-slate-900 group-hover:text-blue-900 transition-colors" /></div>
                  [Interactive Cash Forecast Visualization Layer] <br /> 
                  <span className="text-slate-800 tracking-widest">Select time granularity to rebuild forecast model</span>
               </div>
            </div>
         </div>

         {/* Upcoming Schedule */}
         <div className="lg:col-span-4 glass-panel p-10 rounded-[3rem] border border-white/10 bg-slate-950">
            <h3 className="text-sm font-black text-white italic mb-8 uppercase tracking-widest">Major In/Out Schedule</h3>
            <div className="space-y-6">
               {[
                 { date: 'Apr 25', label: 'Partner A Payment', amount: '-₩120M', type: 'out' },
                 { date: 'Apr 26', label: 'Quarterly VAT Refund', amount: '+₩340M', type: 'in' },
                 { date: 'Apr 28', label: 'Monthly Payroll', amount: '-₩850M', type: 'out' },
                 { date: 'May 02', label: 'Treasury Bond Maturity', amount: '+₩1.2B', type: 'in' },
               ].map((item, i) => (
                 <div key={i} className="flex items-center justify-between group cursor-pointer active:scale-95 transition-all">
                    <div className="flex items-center gap-4">
                       <div className="text-[10px] font-black text-slate-600 bg-white/5 px-2 py-1 rounded border border-white/5">{item.date}</div>
                       <span className="text-xs font-bold text-slate-400 group-hover:text-white transition-colors">{item.label}</span>
                    </div>
                    <span className={`text-xs font-black font-mono ${item.type === 'in' ? 'text-emerald-500' : 'text-rose-500'}`}>{item.amount}</span>
                 </div>
               ))}
            </div>

            <button className="w-full mt-10 py-4 bg-white/5 hover:bg-white/10 border border-white/5 rounded-2xl text-[10px] font-black text-slate-500 uppercase tracking-[0.2em] transition-all flex items-center justify-center gap-2 group">
               View Full Settlement Schedule <ArrowUpRight size={14} className="group-hover:translate-x-1 group-hover:-translate-y-1 transition-transform" />
            </button>
         </div>
      </div>
    </div>
  );
}

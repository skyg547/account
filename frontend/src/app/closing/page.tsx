"use client";

import React, { useState } from 'react';
import { 
  CalendarDays, 
  ChevronRight, 
  PieChart, 
  BarChart, 
  FileText, 
  CheckCircle2, 
  Activity,
  ArrowUpRight,
  Stamp,
  Lock,
  Search
} from 'lucide-react';

/**
 * [결산 및 재무제표 조회 화면]
 * 월말/연말 결산 상태를 모니터링하고 표준 재무제표(BS, PL 등)를 생성합니다.
 */
export default function ClosingPage() {
  const [activeTab, setActiveTab] = useState('STATUS');

  return (
    <div className="space-y-10">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
        <div className="space-y-2">
          <div className="flex items-center gap-3 text-blue-500 mb-2">
            <CalendarDays size={20} />
            <span className="text-xs font-black uppercase tracking-[0.3em]">Accounting Closing Hub</span>
          </div>
          <h2 className="text-4xl font-black text-white tracking-tighter italic">
            결산 및 재무제표 관리
          </h2>
          <p className="text-slate-500 font-medium max-w-2xl">
            월간/연간 결산 프로세스를 수행하고 실시간 재무 데이터 기반의 정합성 검증 및 리포트를 생성합니다.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button className="px-6 py-3 bg-white/5 hover:bg-white/10 border border-white/10 rounded-2xl text-slate-400 text-sm font-black transition-all flex items-center gap-2">
            <Lock size={18} /> 계정 잠금 설정
          </button>
          <button className="px-6 py-3 bg-blue-600 hover:bg-blue-500 rounded-2xl text-white text-sm font-black transition-all shadow-lg shadow-blue-600/20 flex items-center gap-2">
            <Stamp size={18} /> 결산 승인 요청
          </button>
        </div>
      </div>

      {/* Main Content Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        {/* Closing Progress Bar Area */}
        <div className="lg:col-span-12 glass-panel p-8 rounded-[3rem] border border-white/10 bg-white/[0.01]">
           <div className="flex items-center justify-between mb-8">
              <h3 className="text-xl font-black text-white italic tracking-tight uppercase">Current Closing Progress (2026.04)</h3>
              <span className="text-xs font-black text-blue-500 bg-blue-500/10 px-3 py-1 rounded-full border border-blue-500/20">78% COMPLETED</span>
           </div>
           
           <div className="relative w-full h-4 bg-slate-900 rounded-full overflow-hidden border border-white/5 p-1 mb-10">
              <div className="h-full bg-gradient-to-r from-blue-600 to-emerald-500 rounded-full shadow-[0_0_15px_rgba(59,130,246,0.3)] transition-all duration-1000" style={{ width: '78%' }} />
           </div>

           <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
              {[
                { label: '전표 마감', status: 'SUCCESS', date: '2026-04-22' },
                { label: '외화 환평가', status: 'SUCCESS', date: '2026-04-23' },
                { label: '결산 분개 생성', status: 'IN_PROGRESS', date: 'Processing' },
                { label: '재무제표 확정', status: 'WAITING', date: 'Pending' },
              ].map((step, idx) => (
                <div key={idx} className="flex flex-col gap-3 p-5 rounded-2xl bg-white/5 border border-white/5 group hover:border-white/10 transition-all">
                   <div className="flex items-center justify-between">
                      <span className="text-[10px] font-black text-slate-500 uppercase tracking-widest">{`Step 0${idx + 1}`}</span>
                      {step.status === 'SUCCESS' ? <CheckCircle2 size={16} className="text-emerald-500" /> : <Activity size={16} className="text-slate-600 animate-pulse" />}
                   </div>
                   <span className="text-sm font-black text-white">{step.label}</span>
                   <span className="text-[10px] font-bold text-slate-600 italic tracking-tighter">{step.date}</span>
                </div>
              ))}
           </div>
        </div>

        {/* Financial Statements Preview */}
        <div className="lg:col-span-8 glass-panel p-8 rounded-[3rem] border border-white/10 bg-white/[0.01]">
           <div className="flex items-center justify-between mb-8">
              <h3 className="text-xl font-black text-white italic tracking-tight uppercase">Statement Preview</h3>
              <div className="flex gap-2">
                 {['BS', 'PL', 'CF'].map(tab => (
                   <button key={tab} className="px-4 py-2 text-[10px] font-black text-slate-500 border border-white/5 rounded-xl hover:bg-white/5 transition-all">{tab}</button>
                 ))}
              </div>
           </div>

           <div className="space-y-4">
              {[
                { acc: 'I. 유동자산', cur: '1,245,600,000', prev: '1,100,500,000', change: '+13.1%' },
                { acc: '   1. 현금 및 현금성자산', cur: '540,200,000', prev: '420,000,000', change: '+28.6%' },
                { acc: '   2. 단기금융상품', cur: '200,000,000', prev: '250,000,000', change: '-20.0%' },
                { acc: '   3. 매출채권', cur: '505,400,000', prev: '430,500,000', change: '+17.4%' },
                { acc: 'II. 비유동자산', cur: '2,800,000,000', prev: '2,750,000,000', change: '+1.8%' },
              ].map((row, idx) => (
                <div key={idx} className="flex items-center justify-between p-4 rounded-xl hover:bg-white/[0.02] transition-colors border border-transparent hover:border-white/5">
                   <span className={`text-sm font-bold ${row.acc.startsWith(' ') ? 'text-slate-500 ml-4' : 'text-slate-200'}`}>{row.acc}</span>
                   <div className="flex items-center gap-10">
                      <span className="text-sm font-mono font-black text-white w-32 text-right">{row.cur}</span>
                      <span className={`text-[10px] font-black w-14 text-right ${row.change.startsWith('+') ? 'text-emerald-500' : 'text-rose-500'}`}>{row.change}</span>
                   </div>
                </div>
              ))}
           </div>
        </div>

        {/* Sidebar Controls */}
        <div className="lg:col-span-4 space-y-8">
           <div className="glass-panel p-8 rounded-[3rem] border border-white/10 bg-emerald-500/[0.02]">
              <h3 className="text-lg font-black text-white italic mb-6">결산 정합성 체크</h3>
              <div className="space-y-4">
                 {[
                   '차/대 정합성 검증 (O)',
                   '미승인 전표 존재 유부 (None)',
                   '은행 잔액 대조 (Matched)',
                   '감가상각비 계산 (O)'
                 ].map((check, i) => (
                   <div key={i} className="flex items-center justify-between">
                      <span className="text-xs text-slate-500 font-medium">{check}</span>
                      <ArrowUpRight size={14} className="text-emerald-500" />
                   </div>
                 ))}
              </div>
           </div>

           <div className="glass-panel p-8 rounded-[3rem] border border-white/10 bg-slate-950">
              <h4 className="text-[10px] font-black text-slate-500 uppercase tracking-widest mb-6">Report Search</h4>
              <div className="relative mb-6">
                 <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-700" size={16} />
                 <input type="text" placeholder="Search by year/month..." className="w-full bg-slate-900 border border-white/5 rounded-xl py-3 pl-12 text-xs text-white outline-none" />
              </div>
              <div className="flex flex-col gap-2">
                 <button className="flex items-center justify-between p-4 rounded-2xl bg-white/5 border border-white/5 text-xs text-slate-300 font-black hover:bg-white/10 transition-all">
                    <span>2026.03 Financial Statements</span>
                    <FileText size={16} />
                 </button>
                 <button className="flex items-center justify-between p-4 rounded-2xl bg-white/5 border border-white/5 text-xs text-slate-300 font-black hover:bg-white/10 transition-all">
                    <span>2026.02 Financial Statements</span>
                    <FileText size={16} />
                 </button>
              </div>
           </div>
        </div>
      </div>
    </div>
  );
}

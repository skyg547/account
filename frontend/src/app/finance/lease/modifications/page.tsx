"use client";

import React, { useState } from 'react';
import { 
  FileText, 
  RefreshCcw, 
  Slash, 
  Info,
  Calendar,
  Layers,
  ArrowRightLeft
} from 'lucide-react';

export default function LeaseModificationPage() {
  const [modType, setModType] = useState('REMEASURE'); // REMEASURE, TERMINATE

  return (
    <div className="flex flex-col gap-8">
      <header className="flex justify-between items-center">
        <div>
          <h2 className="text-3xl font-black text-white italic tracking-tight">리스 계약 변경 및 해지 (Modification)</h2>
          <p className="text-slate-500 mt-2 text-sm font-medium leading-none">계약 조건 변동에 따른 리스부채 재측정과 사용권자산(ROU) 잔액 정밀 조정</p>
        </div>
        <div className="flex bg-slate-900 p-1.5 rounded-2xl border border-white/5 shadow-2xl">
           <button 
             onClick={() => setModType('REMEASURE')}
             className={`px-8 py-2.5 rounded-xl text-sm font-black transition-all ${modType === 'REMEASURE' ? 'bg-indigo-600 text-white shadow-[0_0_20px_rgba(79,70,229,0.4)]' : 'text-slate-500 hover:text-slate-300'}`}
           >
             리스 재측정
           </button>
           <button 
             onClick={() => setModType('TERMINATE')}
             className={`px-8 py-2.5 rounded-xl text-sm font-black transition-all ${modType === 'TERMINATE' ? 'bg-rose-600 text-white shadow-[0_0_20px_rgba(225,29,72,0.4)]' : 'text-slate-500 hover:text-slate-300'}`}
           >
             중도 해지
           </button>
        </div>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        {/* Contract Summary Column */}
        <div className="lg:col-span-4 flex flex-col gap-6">
           <div className="bg-white/5 border border-white/10 rounded-[40px] p-10 backdrop-blur-xl shadow-2xl relative overflow-hidden group">
              <h3 className="text-lg font-black text-white mb-10 flex items-center gap-3 tracking-tight">
                 <FileText size={22} className="text-indigo-400" /> 대상 계약 상세
              </h3>
              <div className="space-y-10 relative z-10">
                 <div>
                    <div className="text-[10px] text-slate-500 uppercase font-black tracking-widest mb-2 px-1">Contract Code</div>
                    <div className="text-indigo-400 font-mono font-black italic text-xl px-1">L-IFRS-2026-042</div>
                 </div>
                 <div>
                    <div className="text-[10px] text-slate-500 uppercase font-black tracking-widest mb-2 px-1">Description / Lessee</div>
                    <div className="text-white font-black text-xl tracking-tight px-1 leading-snug">판교 제2빌딩 입주자 지원센터</div>
                    <div className="text-xs text-slate-500 font-bold mt-2 px-1">대왕개발(주) (Strategic Real Estate)</div>
                 </div>
                 <div className="grid grid-cols-1 gap-6 pt-4 border-t border-white/5">
                    <div>
                       <div className="text-[10px] text-slate-500 uppercase font-black tracking-widest mb-2 px-1 italic">Current ROU Asset</div>
                       <div className="text-2xl font-black italic text-white tracking-tighter px-1">₩845,000,000</div>
                    </div>
                    <div>
                       <div className="text-[10px] text-slate-500 uppercase font-black tracking-widest mb-2 px-1 italic">Lease Liability</div>
                       <div className="text-2xl font-black italic text-white tracking-tighter px-1">₩790,250,000</div>
                    </div>
                 </div>
              </div>
              <div className="absolute -right-20 -bottom-20 opacity-[0.02] group-hover:opacity-[0.05] transition-opacity duration-1000 pointer-events-none">
                 <RefreshCcw size={300} className="text-white" />
              </div>
           </div>
        </div>

        {/* Action Column */}
        <div className="lg:col-span-8 bg-white/5 border border-white/10 rounded-[40px] p-12 backdrop-blur-3xl shadow-2xl">
           {modType === 'REMEASURE' ? (
             <div className="flex flex-col gap-10 animate-in fade-in slide-in-from-right-4 duration-700">
                <div className="flex items-center gap-4">
                   <div className="w-12 h-12 rounded-2xl bg-indigo-500/10 flex items-center justify-center text-indigo-400">
                      <Layers size={24} />
                   </div>
                   <div>
                      <h4 className="text-xl font-black text-white italic tracking-tight">재측정 조건 설정</h4>
                      <p className="text-xs font-bold text-slate-500 uppercase tracking-widest">RE-MEASUREMENT PARAMETERS</p>
                   </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-x-12 gap-y-8">
                   <div className="flex flex-col gap-3">
                      <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">변경 적용일 (Effective Date)</label>
                      <div className="relative">
                         <Calendar className="absolute left-5 top-5 text-indigo-500" size={18} />
                         <input type="date" className="w-full bg-slate-950 border border-white/5 rounded-2xl py-4 pl-14 pr-6 text-white font-bold focus:border-indigo-500 outline-none transition-all shadow-inner" />
                      </div>
                   </div>
                   <div className="flex flex-col gap-3">
                      <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">변경 후 리스료 (Monthly)</label>
                      <input type="number" className="w-full bg-slate-950 border border-white/5 rounded-2xl py-4 px-6 text-white text-lg font-mono font-black italic focus:border-indigo-500 outline-none transition-all shadow-inner" placeholder="₩ 0" />
                   </div>
                   <div className="flex flex-col gap-3">
                      <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">계약 만료일 변경 (End Date)</label>
                      <input type="date" className="w-full bg-slate-950 border border-white/5 rounded-2xl py-4 px-6 text-white font-bold focus:border-indigo-500 outline-none transition-all shadow-inner" />
                   </div>
                   <div className="flex flex-col gap-3">
                      <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">신규 증분차입이자율 (%)</label>
                      <input type="number" step="0.01" className="w-full bg-slate-950 border border-white/5 rounded-2xl py-4 px-6 text-white font-mono font-black italic focus:border-indigo-500 outline-none transition-all shadow-inner" placeholder="4.25" />
                   </div>
                </div>

                <div className="mt-4 p-8 bg-indigo-600/5 rounded-[32px] border border-indigo-600/20 flex flex-col gap-6 relative overflow-hidden group">
                   <div className="flex items-center gap-3 text-indigo-400 font-black text-sm uppercase tracking-widest px-1">
                      <Layers size={18} /> 예상 변경 효과 분석 (Impact)
                   </div>
                   <div className="space-y-4 px-1">
                      <div className="flex items-center justify-between">
                         <span className="text-slate-500 text-xs font-bold uppercase tracking-tighter">Current Liability</span>
                         <span className="font-mono text-slate-400 font-black text-lg italic">₩790,250,000</span>
                      </div>
                      <div className="flex items-center justify-between pb-6 border-b border-white/5">
                         <span className="text-slate-500 text-xs font-bold uppercase tracking-tighter">Adjusted Liability</span>
                         <span className="font-mono text-indigo-300 font-black text-lg italic">₩920,400,000</span>
                      </div>
                      <div className="flex items-center justify-between pt-2">
                         <span className="text-indigo-400 font-black text-sm tracking-tight italic">ROU 자산 조정액 (+/ -)</span>
                         <span className="text-3xl font-black text-white italic tracking-tighter shadow-indigo-500/50">+ ₩130,150,000</span>
                      </div>
                   </div>
                   <div className="absolute -right-5 -bottom-5 opacity-[0.03] group-hover:opacity-[0.05] transition-opacity duration-1000">
                      <ArrowRightLeft size={120} className="text-white" />
                   </div>
                </div>

                <button className="w-full bg-indigo-600 hover:bg-indigo-500 h-16 rounded-[24px] font-black text-lg transition-all shadow-xl shadow-indigo-900/30 active:scale-95 text-white italic tracking-tight">
                   리스 재측정 및 전표 바인딩
                </button>
             </div>
           ) : (
             <div className="flex flex-col gap-10 animate-in fade-in slide-in-from-right-4 duration-700">
                <div className="flex items-center gap-4">
                   <div className="w-12 h-12 rounded-2xl bg-rose-500/10 flex items-center justify-center text-rose-400">
                      <Slash size={24} />
                   </div>
                   <div>
                      <h4 className="text-xl font-black text-white italic tracking-tight">중도 해지 조건 확정</h4>
                      <p className="text-xs font-bold text-slate-500 uppercase tracking-widest">TERMINATION PARAMETERS</p>
                   </div>
                </div>
                
                <div className="grid grid-cols-1 md:grid-cols-2 gap-x-12 gap-y-8">
                   <div className="flex flex-col gap-3">
                      <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">해지 일자 (Termination Date)</label>
                      <input type="date" className="w-full bg-slate-950 border border-white/5 rounded-2xl py-4 px-6 text-white font-bold focus:border-rose-500 outline-none transition-all shadow-inner" />
                   </div>
                   <div className="flex flex-col gap-3">
                      <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">중도해지 수수료 (Penalty)</label>
                      <input type="number" className="w-full bg-slate-950 border border-white/5 rounded-2xl py-4 px-6 text-white text-lg font-mono font-black italic focus:border-rose-500 outline-none transition-all shadow-inner" placeholder="₩ 0" />
                   </div>
                </div>

                <div className="bg-rose-600/5 p-8 rounded-[32px] border border-rose-600/20 flex flex-col gap-6 relative overflow-hidden group">
                   <div className="flex items-center gap-3 text-rose-400 font-black text-sm uppercase tracking-widest px-1">
                      <ArrowRightLeft size={18} /> 해지 정산 효과 (P&L Impact)
                   </div>
                   <div className="space-y-4 px-1">
                      <div className="flex items-center justify-between">
                         <span className="text-slate-500 text-[10px] font-black uppercase tracking-tighter">Derecognized Liability</span>
                         <span className="font-mono text-slate-400 font-bold italic tracking-tight">₩790,250,000</span>
                      </div>
                      <div className="flex items-center justify-between pb-6 border-b border-white/5">
                         <span className="text-slate-500 text-[10px] font-black uppercase tracking-tighter">Derecognized ROU Asset</span>
                         <span className="font-mono text-slate-400 font-bold italic tracking-tight">- ₩845,000,000</span>
                      </div>
                      <div className="flex items-center justify-between pt-2">
                         <span className="text-rose-400 font-black tracking-tight italic">최종 리스해지손익</span>
                         <span className="text-3xl font-black text-white italic tracking-tighter shadow-rose-500/50">- ₩54,750,000</span>
                      </div>
                   </div>
                   <div className="absolute -right-5 -bottom-5 opacity-[0.03] group-hover:opacity-[0.05] transition-opacity duration-1000">
                      <Slash size={120} className="text-white" />
                   </div>
                </div>

                <button className="w-full bg-rose-600 hover:bg-rose-500 h-16 rounded-[24px] font-black text-lg transition-all shadow-xl shadow-rose-900/30 active:scale-95 text-white italic tracking-tight">
                   리스 계약 해지 처리 완료
                </button>
             </div>
           )}

           <div className="mt-12 flex gap-6 text-slate-500 bg-white/5 p-8 rounded-[32px] border border-white/5 items-start">
              <div className="w-10 h-10 rounded-full bg-white/5 flex items-center justify-center shrink-0">
                 <Info size={20} className="text-slate-400" />
              </div>
              <div>
                 <p className="text-[11px] font-bold leading-relaxed tracking-tighter transition-colors hover:text-slate-400">
                    리스 변경 시 최초 인식한 이자율이 아닌 변경 시점의 증분차입이자율을 적용해야 하는 경우를 명확히 구분하십시오. 
                    전용범위 확대나 기간 연장은 대부분 새로운 지수나 요율을 적용한 재측정이 필요하며, 해지 시 P&L 영향을 사전에 분석해야 합니다.
                 </p>
              </div>
           </div>
        </div>
      </div>
    </div>
  );
}

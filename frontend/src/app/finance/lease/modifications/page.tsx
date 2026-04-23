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
    <div className="p-8 flex flex-col gap-8 bg-[#020617] min-h-screen text-slate-200">
      <header className="flex justify-between items-center bg-slate-900/50 p-6 rounded-2xl border border-slate-800">
        <div>
          <h2 className="text-2xl font-black text-white flex items-center gap-3">
             <RefreshCcw size={28} className="text-indigo-500" /> 리스 계약 변경 및 해지 (Modification)
          </h2>
          <p className="text-sm text-slate-500 mt-1">계약 조건 변경에 따른 리스부채 재측정과 ROU 자산 조정을 수행합니다.</p>
        </div>
        <div className="flex bg-slate-950 p-1 rounded-xl border border-slate-800">
           <button 
             onClick={() => setModType('REMEASURE')}
             className={`px-6 py-2 rounded-lg text-sm font-bold transition-all ${modType === 'REMEASURE' ? 'bg-indigo-600 text-white shadow-lg' : 'text-slate-500 hover:text-slate-300'}`}
           >
             리스 재측정
           </button>
           <button 
             onClick={() => setModType('TERMINATE')}
             className={`px-6 py-2 rounded-lg text-sm font-bold transition-all ${modType === 'TERMINATE' ? 'bg-red-600 text-white shadow-lg' : 'text-slate-500 hover:text-slate-300'}`}
           >
             중도 해지
           </button>
        </div>
      </header>

      <div className="grid grid-cols-12 gap-8">
        {/* Contract Summary Column */}
        <div className="col-span-4 flex flex-col gap-6">
           <div className="bg-slate-900 border border-slate-800 rounded-3xl p-8 sticky top-8">
              <h3 className="text-lg font-bold mb-6 flex items-center gap-2 border-b border-slate-800 pb-4">
                 <FileText size={20} className="text-indigo-400" /> 대상 계약 정보
              </h3>
              <div className="space-y-6">
                 <div>
                    <div className="text-[10px] text-slate-500 uppercase font-black mb-1">계약 번호</div>
                    <div className="text-indigo-400 font-mono font-bold">L-IFRS-2026-042</div>
                 </div>
                 <div>
                    <div className="text-[10px] text-slate-500 uppercase font-black mb-1">계약명 / 리스 이용자</div>
                    <div className="text-slate-200 font-bold">판교 제2빌딩 입주자 지원센터</div>
                    <div className="text-xs text-slate-500">대왕개발(주)</div>
                 </div>
                 <div className="grid grid-cols-2 gap-4">
                    <div>
                       <div className="text-[10px] text-slate-500 uppercase font-black mb-1">사용권자산 잔액</div>
                       <div className="text-lg font-black italic">₩845,000,000</div>
                    </div>
                    <div>
                       <div className="text-[10px] text-slate-500 uppercase font-black mb-1">리스부채 잔액</div>
                       <div className="text-lg font-black italic">₩790,250,000</div>
                    </div>
                 </div>
              </div>
           </div>
        </div>

        {/* Action Column */}
        <div className="col-span-8 bg-slate-900/30 border border-slate-800 rounded-3xl p-10 backdrop-blur-3xl">
           {modType === 'REMEASURE' ? (
             <div className="flex flex-col gap-8 animate-in fade-in duration-500">
                <div className="flex items-center gap-4 border-l-4 border-indigo-500 pl-6 py-2">
                   <h4 className="text-xl font-bold">변경 조건 입력</h4>
                   <span className="text-xs bg-indigo-500/10 text-indigo-400 px-3 py-1 rounded-full border border-indigo-500/20">RE-MEASUREMENT</span>
                </div>

                <div className="grid grid-cols-2 gap-x-10 gap-y-8">
                   <div className="flex flex-col gap-2">
                      <label className="text-sm font-bold text-slate-400">변경 적용일 (Effective Date)</label>
                      <div className="relative">
                         <Calendar className="absolute left-4 top-3.5 text-slate-500" size={18} />
                         <input type="date" className="w-full bg-slate-950 border border-slate-800 rounded-xl py-3 pl-12 pr-4 text-white focus:border-indigo-500 outline-none" />
                      </div>
                   </div>
                   <div className="flex flex-col gap-2">
                      <label className="text-sm font-bold text-slate-400">변경 후 리스료 (Monthly)</label>
                      <input type="number" className="w-full bg-slate-950 border border-slate-800 rounded-xl py-3 px-4 text-white focus:border-indigo-500 outline-none font-bold" placeholder="₩ 0" />
                   </div>
                   <div className="flex flex-col gap-2">
                      <label className="text-sm font-bold text-slate-400">계약 만료일 변경</label>
                      <input type="date" className="w-full bg-slate-950 border border-slate-800 rounded-xl py-3 px-4 text-white focus:border-indigo-500 outline-none" />
                   </div>
                   <div className="flex flex-col gap-2">
                      <label className="text-sm font-bold text-slate-400">신규 증분차입이자율 (%)</label>
                      <input type="number" step="0.01" className="w-full bg-slate-950 border border-slate-800 rounded-xl py-3 px-4 text-white focus:border-indigo-500 outline-none" placeholder="4.25" />
                   </div>
                </div>

                <div className="mt-6 p-6 bg-indigo-500/5 rounded-2xl border border-indigo-500/10 flex flex-col gap-4">
                   <div className="flex items-center gap-2 text-indigo-400 font-bold text-sm">
                      <Layers size={18} /> 예상 변경 효과 (Impact Analysis)
                   </div>
                   <div className="flex items-center justify-between">
                      <span className="text-slate-400 text-sm">기존 리스부채 가액</span>
                      <span className="font-mono">₩790,250,000</span>
                   </div>
                   <div className="flex items-center justify-between border-b border-indigo-500/10 pb-4">
                      <span className="text-slate-400 text-sm">재측정 리스부채 가액</span>
                      <span className="font-bold text-indigo-300">₩920,400,000</span>
                   </div>
                   <div className="flex items-center justify-between">
                      <span className="text-indigo-400 font-black">ROU 자산 조정액 (+/ -)</span>
                      <span className="text-xl font-black text-indigo-400">+ ₩130,150,000</span>
                   </div>
                </div>

                <button className="w-full bg-indigo-600 hover:bg-indigo-500 h-14 rounded-2xl font-black text-lg transition-all shadow-xl shadow-indigo-500/20 active:scale-[0.98]">
                   리스 재측정 및 전표 발행
                </button>
             </div>
           ) : (
             <div className="flex flex-col gap-8 animate-in slide-in-from-right duration-500">
                <div className="flex items-center gap-4 border-l-4 border-red-500 pl-6 py-2">
                   <h4 className="text-xl font-bold">중도 해지 조건 입력</h4>
                   <span className="text-xs bg-red-500/10 text-red-400 px-3 py-1 rounded-full border border-red-500/20">CONTRACT TERMINATION</span>
                </div>
                
                <div className="space-y-6">
                   <div className="flex flex-col gap-2">
                      <label className="text-sm font-bold text-slate-400">해지 일자</label>
                      <input type="date" className="w-full bg-slate-950 border border-slate-800 rounded-xl py-3 px-4 text-white focus:border-red-500 outline-none" />
                   </div>
                   <div className="flex flex-col gap-2">
                      <label className="text-sm font-bold text-slate-400">중도해지 수수료 (Penalty)</label>
                      <input type="number" className="w-full bg-slate-950 border border-slate-800 rounded-xl py-3 px-4 text-white focus:border-red-500 outline-none" placeholder="₩ 0" />
                   </div>
                </div>

                <div className="bg-red-500/5 p-6 rounded-2xl border border-red-500/10 flex flex-col gap-4 mt-4">
                   <div className="flex items-center gap-2 text-red-400 font-bold text-sm">
                      <ArrowRightLeft size={18} /> 해지 정산 분석
                   </div>
                   <div className="flex items-center justify-between text-sm text-slate-500">
                      <span>제거되는 리스부채</span>
                      <span>₩790,250,000</span>
                   </div>
                   <div className="flex items-center justify-between text-sm text-slate-500">
                      <span>제거되는 ROU 자산</span>
                      <span>- ₩845,000,000</span>
                   </div>
                   <div className="flex items-center justify-between border-t border-red-500/10 pt-4">
                      <span className="text-red-400 font-bold">리스해지손익 (P&L)</span>
                      <span className="text-xl font-black text-red-400">- ₩54,750,000</span>
                   </div>
                </div>

                <button className="w-full bg-red-600 hover:bg-red-500 h-14 rounded-2xl font-black text-lg transition-all shadow-xl shadow-red-500/20">
                   리스 계약 해지 처리
                </button>
             </div>
           )}

           <div className="mt-10 flex gap-4 text-slate-500 bg-slate-800/20 p-6 rounded-2xl border border-slate-800/50">
              <Info size={24} className="shrink-0" />
              <p className="text-xs leading-relaxed">
                 리스 변경 시 최초 인식한 이자율이 아닌 변경 시점의 증분차입이자율을 적용해야 하는 경우를 명확히 구분하십시오. 
                 전용범위 확대나 기간 연장은 대부분 새로운 지수나 요율을 적용한 재측정이 필요합니다.
              </p>
           </div>
        </div>
      </div>
    </div>
  );
}

"use client";

import React, { useState } from 'react';
import { 
  TrendingUp, 
  TrendingDown, 
  AlertCircle, 
  CheckCircle2, 
  Calculator,
  Search,
  ArrowRight,
  FileText
} from 'lucide-react';

export default function AssetRevaluationPage() {
  const [selectedAsset, setSelectedAsset] = useState<any>(null);

  // Mock Assets for Revaluation
  const assetsForReval = [
    { id: 1, code: 'AST-BUILD-001', name: '강남사옥 (토지/건물)', bookValue: 1250000000, lastReval: '2025-12-31' },
    { id: 2, code: 'AST-MACH-042', name: '반도체 식각 장비', bookValue: 450000000, lastReval: '2026-01-10' },
  ];

  return (
    <div className="flex flex-col gap-8">
      <header className="flex justify-between items-center">
        <div>
          <h2 className="text-3xl font-black text-white italic tracking-tight">자산 재평가 및 손상차손 관리</h2>
          <p className="text-slate-500 mt-2 text-sm font-medium leading-none">공정가치 변동에 따른 자산가액 현실화 및 잠재적 손실 조기 실현 모니터링</p>
        </div>
        <div className="flex gap-2">
          <button className="bg-slate-800 hover:bg-slate-700 text-slate-300 px-4 py-2 rounded-xl border border-white/5 transition-all flex items-center gap-2 text-sm font-bold shadow-lg">
             <Calculator size={18} /> 상각 시뮬레이션
          </button>
        </div>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        {/* Left: Asset Selection List */}
        <div className="lg:col-span-4 bg-white/5 border border-white/10 rounded-[32px] p-8 backdrop-blur-xl">
           <div className="flex justify-between items-center mb-8 px-1">
              <h3 className="text-lg font-bold text-white leading-none flex items-center gap-2">
                 <Search size={20} className="text-blue-500" /> 검토 대상 자산
              </h3>
              <span className="text-[10px] font-black text-slate-500 uppercase tracking-widest">ACTIVE LIST</span>
           </div>
           <div className="space-y-4">
              {assetsForReval.map(asset => (
                <div 
                  key={asset.id}
                  onClick={() => setSelectedAsset(asset)}
                  className={`p-6 rounded-2xl cursor-pointer transition-all border ring-offset-4 ring-offset-[#0f172a] ${
                    selectedAsset?.id === asset.id 
                      ? 'bg-blue-600/10 border-blue-500/50 ring-2 ring-blue-500/20 shadow-xl shadow-blue-500/10' 
                      : 'bg-white/5 border-transparent hover:border-white/10 hover:bg-white/[0.08]'
                  }`}
                >
                  <div className="flex justify-between items-start mb-2">
                    <span className="text-[10px] text-blue-500 font-black font-mono tracking-tighter">{asset.code}</span>
                    {selectedAsset?.id === asset.id && <ArrowRight size={14} className="text-blue-400 animate-pulse" />}
                  </div>
                  <div className={`font-black text-lg tracking-tight ${selectedAsset?.id === asset.id ? 'text-white' : 'text-slate-300 transition-colors'}`}>{asset.name}</div>
                  <div className="mt-4 flex justify-between items-end">
                     <div className="text-[10px] text-slate-500 font-bold uppercase tracking-wider">Book Value</div>
                     <div className="text-sm font-mono text-slate-400 font-black italic">₩{asset.bookValue.toLocaleString()}</div>
                  </div>
                </div>
              ))}
           </div>
        </div>

        {/* Right: Revaluation Form */}
        <div className="lg:col-span-8 flex flex-col gap-6">
          {!selectedAsset ? (
            <div className="min-h-[600px] flex flex-col items-center justify-center bg-white/5 border border-dashed border-white/10 rounded-[40px] text-slate-600 group">
               <div className="w-20 h-20 rounded-full bg-slate-900 flex items-center justify-center mb-6 group-hover:scale-110 transition-transform">
                  <AlertCircle size={32} className="opacity-30" />
               </div>
               <p className="font-bold text-lg tracking-tight">분석을 진행할 자산을 좌측에서 선택해 주세요.</p>
               <p className="text-sm mt-2 font-medium opacity-50">실시간 공정가치 데이터가 연동 대기 중입니다.</p>
            </div>
          ) : (
            <div className="animate-in fade-in slide-in-from-right-4 duration-500 flex flex-col gap-8">
              {/* Asset Info Card */}
              <div className="bg-gradient-to-r from-blue-900/20 to-indigo-900/10 border border-blue-500/20 rounded-[32px] p-10 flex justify-between items-center relative overflow-hidden group">
                 <div className="relative z-10">
                    <div className="flex items-center gap-3 mb-2">
                       <div className="w-2 h-2 bg-blue-500 rounded-full animate-ping" />
                       <span className="text-[10px] font-black text-blue-400 uppercase tracking-widest">Currently Inspecting</span>
                    </div>
                    <h3 className="text-3xl font-black text-white tracking-tighter">{selectedAsset.name}</h3>
                    <p className="text-slate-500 text-sm font-bold mt-2">최종 평가 기록: {selectedAsset.lastReval} (K-IFRS 준수)</p>
                 </div>
                 <div className="text-right relative z-10">
                    <span className="text-[10px] text-slate-500 font-black uppercase tracking-widest block mb-1">Standard book value</span>
                    <span className="text-5xl font-black italic text-white tracking-tighter">₩{selectedAsset.bookValue.toLocaleString()}</span>
                 </div>
                 <div className="absolute right-0 top-0 p-10 opacity-[0.02] group-hover:opacity-[0.05] transition-opacity duration-1000 pointer-events-none">
                    <TrendingUp size={200} className="text-white" />
                 </div>
              </div>

              {/* Input Area */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                 {/* Revaluation Gain */}
                 <div className="bg-white/5 border border-white/10 rounded-[32px] p-8 hover:bg-white/[0.08] transition-all">
                    <div className="flex items-center gap-3 mb-8">
                       <div className="w-10 h-10 rounded-xl bg-emerald-500/10 flex items-center justify-center text-emerald-400">
                          <TrendingUp size={24} />
                       </div>
                       <h4 className="text-lg font-black text-white leading-none">재평가 증액 (Gain)</h4>
                    </div>
                    <div className="space-y-6">
                       <div className="flex flex-col gap-2">
                          <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">평가 공정가액</label>
                          <input type="number" className="w-full bg-slate-950 border border-white/10 rounded-2xl p-4 text-white text-lg font-mono font-black italic focus:border-emerald-500 outline-none transition-all shadow-inner" placeholder="0" />
                       </div>
                       <div className="flex flex-col gap-2">
                          <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">자본 임계치 계정</label>
                          <select className="w-full bg-slate-950 border border-white/10 rounded-2xl p-4 text-white text-sm font-bold focus:border-emerald-500 outline-none cursor-pointer">
                             <option>3201-01 기타포괄손익-재평가잉여금</option>
                             <option>9102-01 당기손익-재평가이익(기인식환입)</option>
                          </select>
                       </div>
                       <button className="w-full bg-emerald-600 hover:bg-emerald-500 text-white font-black py-4 rounded-2xl mt-4 transition-all shadow-xl shadow-emerald-900/20 active:scale-95">증액 전표 바인딩</button>
                    </div>
                 </div>

                 {/* Impairment Loss */}
                 <div className="bg-white/5 border border-white/10 rounded-[32px] p-8 hover:bg-white/[0.08] transition-all">
                    <div className="flex items-center gap-3 mb-8">
                        <div className="w-10 h-10 rounded-xl bg-rose-500/10 flex items-center justify-center text-rose-400">
                          <TrendingDown size={24} />
                       </div>
                       <h4 className="text-lg font-black text-white leading-none">손상차손 인식 (Loss)</h4>
                    </div>
                    <div className="space-y-6">
                       <div className="flex flex-col gap-2">
                          <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">회수가능가액 (NRV)</label>
                          <input type="number" className="w-full bg-slate-950 border border-white/10 rounded-2xl p-4 text-white text-lg font-mono font-black italic focus:border-rose-500 outline-none transition-all shadow-inner" placeholder="0" />
                       </div>
                       <div className="flex flex-col gap-2">
                          <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">손상 유발 이벤트</label>
                          <select className="w-full bg-slate-950 border border-white/10 rounded-2xl p-4 text-white text-sm font-bold focus:border-rose-500 outline-none cursor-pointer">
                             <option>시장가치의 중대한 하락</option>
                             <option>내용연수/기술적 진부화</option>
                             <option>물리적 파손 및 작동 중지</option>
                             <option>경제적 성과의 악화 증거</option>
                          </select>
                       </div>
                       <button className="w-full bg-rose-600 hover:bg-rose-500 text-white font-black py-4 rounded-2xl mt-4 transition-all shadow-xl shadow-rose-900/20 active:scale-95">손상 확정 및 전표발행</button>
                    </div>
                 </div>
              </div>

              {/* Advanced Controls & Note */}
              <div className="bg-white/5 border border-white/10 rounded-[32px] p-8 flex lg:flex-row flex-col gap-8 items-center">
                 <div className="flex-1">
                    <h5 className="flex items-center gap-2 text-amber-500 font-black text-xs uppercase tracking-widest mb-3 px-1">
                       <CheckCircle2 size={16} /> 회계 처리 가이드라인 (Compliance)
                    </h5>
                    <p className="text-slate-500 text-sm font-medium leading-relaxed px-1">
                       재평가 모형 선택 시, 해당 자산 분류군 내의 모든 유형자산을 일괄 재평가해야 합니다. 손상차손은 매 보고기간 말마다 손상 징후를 검토하며, 장부금액이 회수가능가액을 초과할 경우 즉시 비용으로 인식합니다.
                    </p>
                 </div>
                 <div className="shrink-0">
                    <button className="bg-slate-800 hover:bg-slate-700 text-slate-300 px-6 py-3 rounded-2xl border border-white/5 transition-all text-xs font-black uppercase tracking-widest flex items-center gap-3 group">
                       <FileText size={16} className="group-hover:text-amber-500" /> 세부 이력 조회
                    </button>
                 </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

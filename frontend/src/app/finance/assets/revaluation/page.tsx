"use client";

import React, { useState } from 'react';
import { 
  TrendingUp, 
  TrendingDown, 
  AlertCircle, 
  CheckCircle2, 
  Calculator,
  Search,
  ArrowRight
} from 'lucide-react';

export default function AssetRevaluationPage() {
  const [selectedAsset, setSelectedAsset] = useState<any>(null);

  // Mock Assets for Revaluation
  const assetsForReval = [
    { id: 1, code: 'AST-BUILD-001', name: '강남사옥 (토지/건물)', bookValue: 1250000000, lastReval: '2025-12-31' },
    { id: 2, code: 'AST-MACH-042', name: '반도체 식각 장비', bookValue: 450000000, lastReval: '2026-01-10' },
  ];

  return (
    <div className="p-8 flex flex-col gap-8 bg-[#0f172a] min-h-screen text-white">
      <header className="flex justify-between items-start">
        <div>
          <h2 className="text-3xl font-bold tracking-tight">자산 재평가 및 손상차손 관리</h2>
          <p className="text-slate-400 mt-2">보유 자산의 공정가치 변동에 따른 가액 조정 및 손상 징후를 검토합니다.</p>
        </div>
        <div className="flex gap-2">
          <button className="bg-slate-800 hover:bg-slate-700 px-4 py-2 rounded-xl border border-slate-700 transition-all flex items-center gap-2">
             <Calculator size={18} /> 재평가 상각 시뮬레이션
          </button>
        </div>
      </header>

      <div className="grid grid-cols-3 gap-8">
        {/* Left: Asset Selection List */}
        <div className="col-span-1 bg-white/5 border border-white/10 rounded-2xl p-6 backdrop-blur-xl">
           <div className="flex items-center gap-2 mb-6 text-slate-300 font-semibold border-b border-white/5 pb-4">
              <Search size={18} /> 검토 대상 자산 선택
           </div>
           <div className="flex flex-col gap-3">
              {assetsForReval.map(asset => (
                <div 
                  key={asset.id}
                  onClick={() => setSelectedAsset(asset)}
                  className={`p-4 rounded-xl cursor-pointer transition-all border ${
                    selectedAsset?.id === asset.id ? 'bg-blue-500/20 border-blue-500' : 'bg-white/5 border-transparent hover:border-white/20'
                  }`}
                >
                  <div className="text-xs text-blue-400 font-mono mb-1">{asset.code}</div>
                  <div className="font-bold text-slate-200">{asset.name}</div>
                  <div className="text-sm text-slate-500 mt-2">장부금액: ₩{asset.bookValue.toLocaleString()}</div>
                </div>
              ))}
           </div>
        </div>

        {/* Right: Revaluation Form */}
        <div className="col-span-2 flex flex-col gap-6">
          {!selectedAsset ? (
            <div className="h-full flex flex-col items-center justify-center bg-white/5 border border-dashed border-white/20 rounded-2xl text-slate-500">
               <AlertCircle size={48} className="mb-4 opacity-20" />
               <p>좌측에서 재평가를 진행할 자산을 선택해 주세요.</p>
            </div>
          ) : (
            <>
              {/* Asset Info Card */}
              <div className="bg-blue-600/10 border border-blue-500/20 rounded-2xl p-6 flex justify-between items-center">
                 <div>
                    <h3 className="text-xl font-bold text-blue-400">{selectedAsset.name}</h3>
                    <p className="text-sm text-slate-500 mt-1">최종 평가일: {selectedAsset.lastReval}</p>
                 </div>
                 <div className="text-right">
                    <span className="text-xs text-slate-500 block mb-1 uppercase tracking-wider">현재 장부가액</span>
                    <span className="text-2xl font-black italic">₩{selectedAsset.bookValue.toLocaleString()}</span>
                 </div>
              </div>

              {/* Input Area */}
              <div className="grid grid-cols-2 gap-6">
                 <div className="bg-white/5 border border-white/10 rounded-2xl p-6">
                    <div className="flex items-center gap-2 mb-6 text-green-400 font-bold">
                       <TrendingUp size={20} /> 재평가 증액 (Revaluation Gain)
                    </div>
                    <div className="flex flex-col gap-4">
                       <div>
                          <label className="text-xs text-slate-500 mb-2 block">평가 공정가액</label>
                          <input type="number" className="w-full bg-slate-900 border border-slate-700 rounded-lg p-3 text-white outline-none focus:border-green-500 transition-all" placeholder="평가 가액 입력" />
                       </div>
                       <div>
                          <label className="text-xs text-slate-500 mb-2 block">재평가잉여금 계정</label>
                          <select className="w-full bg-slate-900 border border-slate-700 rounded-lg p-3 text-white outline-none">
                             <option>3201-01 재평가잉여금</option>
                          </select>
                       </div>
                       <button className="w-full bg-green-600 hover:bg-green-500 text-white font-bold py-3 rounded-lg mt-4 transition-all">증액 전표 생성</button>
                    </div>
                 </div>

                 <div className="bg-white/5 border border-white/10 rounded-2xl p-6">
                    <div className="flex items-center gap-2 mb-6 text-red-400 font-bold">
                       <TrendingDown size={20} /> 손상차손 인식 (Impairment)
                    </div>
                    <div className="flex flex-col gap-4">
                       <div>
                          <label className="text-xs text-slate-500 mb-2 block">회수가능가액</label>
                          <input type="number" className="w-full bg-slate-900 border border-slate-700 rounded-lg p-3 text-white outline-none focus:border-red-500 transition-all" placeholder="회수가능가액 입력" />
                       </div>
                       <div>
                          <label className="text-xs text-slate-500 mb-2 block">손상 징후 사유</label>
                          <select className="w-full bg-slate-900 border border-slate-700 rounded-lg p-3 text-white outline-none">
                             <option>시장가치의 급격한 하락</option>
                             <option>기술적 진부화</option>
                             <option>물리적 손상</option>
                             <option>사용 범위의 중대한 변화</option>
                          </select>
                       </div>
                       <button className="w-full bg-red-600 hover:bg-red-500 text-white font-bold py-3 rounded-lg mt-4 transition-all">손상차손 전표 생성</button>
                    </div>
                 </div>
              </div>

              {/* Note */}
              <div className="bg-amber-500/10 border border-amber-500/20 rounded-2xl p-4 flex gap-3 text-amber-500 text-xs">
                 <AlertCircle size={20} />
                 <div>
                    <p className="font-bold">회계상 주의사항</p>
                    <p className="mt-1 opacity-80 leading-relaxed">자산 재평가는 매 결산기마다 공정가액 변동이 중요할 때 수행하며, 한 번 재평가 모형을 선택하면 해당 부류의 모든 자산에 동일하게 적용해야 합니다.</p>
                 </div>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

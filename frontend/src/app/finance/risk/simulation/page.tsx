'use client';

import React, { useState, useEffect } from 'react';
import { 
  Calculator, TrendingUp, AlertCircle, RefreshCcw, 
  ArrowRight, ShieldCheck, Zap, BarChart, PieChart,
  ArrowUpRight, ArrowDownRight, Info
} from 'lucide-react';
import { riskService, RiskWeightedAsset } from '@/services/riskService';

/**
 * [리스크 시뮬레이션 (What-If Analysis)]
 * 리스크 파라미터(PD, LGD, RW) 변경 시 RWA 및 BIS 비율에 미치는 영향을 시뮬레이션합니다.
 */
export default function RiskSimulationPage() {
  const [originalData, setOriginalData] = useState<RiskWeightedAsset[]>([]);
  const [simulatedData, setSimulatedData] = useState<RiskWeightedAsset[]>([]);
  const [loading, setLoading] = useState(true);
  
  // 시뮬레이션 파라미터 (조정 비율)
  const [pdMultiplier, setPdMultiplier] = useState(1.0); // PD 조정 (1.2 = 20% 상승)
  const [rwMultiplier, setRwMultiplier] = useState(1.0); // RW 조정
  
  const [capital, setCapital] = useState(5000000000); // 자기자본 (50억)

  useEffect(() => {
    async function loadData() {
      try {
        const data = await riskService.getRwaResults('2026-01-01', '2026-12-31');
        setOriginalData(data);
        setSimulatedData(data);
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    }
    loadData();
  }, []);

  const handleSimulate = () => {
    const updated = originalData.map(item => ({
      ...item,
      // 시뮬레이션 로직: RW에 조정 비율 적용 (실제로는 PD/LGD 변화가 RW에 복합적으로 영향을 미침)
      riskWeight: item.riskWeight * rwMultiplier,
      rwaAmount: item.eadAmount * (item.riskWeight * rwMultiplier)
    }));
    setSimulatedData(updated);
  };

  const totalOriginalRwa = originalData.reduce((sum, item) => sum + item.rwaAmount, 0);
  const totalSimulatedRwa = simulatedData.reduce((sum, item) => sum + item.rwaAmount, 0);
  
  const originalBis = totalOriginalRwa > 0 ? (capital / totalOriginalRwa) * 100 : 0;
  const simulatedBis = totalSimulatedRwa > 0 ? (capital / totalSimulatedRwa) * 100 : 0;
  
  const rwaDelta = totalSimulatedRwa - totalOriginalRwa;
  const bisDelta = simulatedBis - originalBis;

  return (
    <div className="flex flex-col gap-8 pb-20">
      <header className="flex justify-between items-center">
        <div>
          <div className="flex items-center gap-3">
            <div className="p-2 bg-indigo-500/10 rounded-lg">
              <Calculator className="text-indigo-400" size={24} />
            </div>
            <h2 className="text-3xl font-black text-white italic tracking-tight uppercase">Risk Simulation</h2>
          </div>
          <p className="text-slate-500 mt-2 text-sm font-medium leading-none">위험가중치 및 파라미터 변동에 따른 자본적정성 영향 분석</p>
        </div>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* 컨트롤 패널 */}
        <aside className="lg:col-span-4 space-y-6">
          <div className="bg-slate-900/60 border border-white/10 rounded-[32px] p-8 backdrop-blur-xl">
             <h3 className="text-sm font-black text-white uppercase tracking-widest mb-8 flex items-center gap-2 italic">
               <Zap size={18} className="text-amber-400" /> Simulation Parameters
             </h3>
             
             <div className="space-y-10">
                <div className="space-y-4">
                  <div className="flex justify-between items-end">
                    <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest">Global Risk Weight (RW)</label>
                    <span className="text-lg font-black text-indigo-400 italic">x{rwMultiplier.toFixed(2)}</span>
                  </div>
                  <input 
                    type="range" min="0.5" max="2.0" step="0.05" 
                    value={rwMultiplier}
                    onChange={(e) => setRwMultiplier(parseFloat(e.target.value))}
                    className="w-full h-1.5 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-indigo-500"
                  />
                  <div className="flex justify-between text-[8px] font-black text-slate-600 uppercase tracking-widest">
                    <span>-50% (Optimistic)</span>
                    <span>+100% (Stress)</span>
                  </div>
                </div>

                <div className="space-y-4">
                  <div className="flex justify-between items-end">
                    <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest">Tier 1 Capital (₩)</label>
                  </div>
                  <input 
                    type="number" 
                    value={capital}
                    onChange={(e) => setCapital(parseInt(e.target.value))}
                    className="w-full bg-slate-950 border border-white/5 rounded-2xl p-4 text-white font-mono text-sm focus:outline-none focus:border-indigo-500/50 transition-all"
                  />
                </div>

                <button 
                  onClick={handleSimulate}
                  className="w-full bg-indigo-600 hover:bg-indigo-500 text-white font-black py-5 rounded-2xl transition-all shadow-xl shadow-indigo-600/20 active:scale-[0.98] uppercase tracking-[0.2em] flex items-center justify-center gap-2"
                >
                  <RefreshCcw size={18} /> Run Simulation
                </button>
             </div>
          </div>

          <div className="bg-amber-500/10 border border-amber-500/20 rounded-[32px] p-8">
            <div className="flex items-start gap-4">
              <AlertCircle className="text-amber-500 shrink-0" size={24} />
              <div className="space-y-2">
                <h4 className="text-xs font-black text-amber-500 uppercase tracking-widest">Simulation Note</h4>
                <p className="text-[10px] text-slate-400 leading-relaxed font-medium">
                  본 시뮬레이션은 RW 수치에 대한 단순 가감 방식을 사용합니다. 실제 Basel III 산출 시에는 PD, LGD, 자산 분류 등이 복합적으로 작용하므로 정밀 산출 엔진 결과와 차이가 있을 수 있습니다.
                </p>
              </div>
            </div>
          </div>
        </aside>

        {/* 결과 분석 */}
        <main className="lg:col-span-8 space-y-8">
          {/* 비교 대시보드 */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
             <div className="bg-slate-900/40 border border-white/10 rounded-[32px] p-8 backdrop-blur-xl">
                <span className="text-[10px] font-black text-slate-500 uppercase tracking-widest block mb-4">RWA Impact</span>
                <div className="flex items-end gap-3">
                   <div className="text-4xl font-black text-white italic tracking-tighter">
                     ₩{(totalSimulatedRwa / 100000000).toFixed(1)}억
                   </div>
                   <div className={`flex items-center gap-1 text-sm font-black italic mb-1 ${rwaDelta >= 0 ? 'text-rose-400' : 'text-emerald-400'}`}>
                      {rwaDelta >= 0 ? <ArrowUpRight size={16} /> : <ArrowDownRight size={16} />}
                      {Math.abs(rwaDelta / 100000000).toFixed(1)}억
                   </div>
                </div>
                <div className="mt-6 h-2 bg-slate-800 rounded-full overflow-hidden flex">
                   <div className="h-full bg-slate-600" style={{width: `${(totalOriginalRwa / Math.max(totalOriginalRwa, totalSimulatedRwa)) * 100}%`}} />
                   {rwaDelta > 0 && <div className="h-full bg-rose-500 animate-pulse" style={{width: `${(rwaDelta / totalSimulatedRwa) * 100}%`}} />}
                </div>
             </div>

             <div className="bg-slate-900/40 border border-white/10 rounded-[32px] p-8 backdrop-blur-xl">
                <span className="text-[10px] font-black text-slate-500 uppercase tracking-widest block mb-4">BIS Ratio Impact</span>
                <div className="flex items-end gap-3">
                   <div className={`text-4xl font-black italic tracking-tighter ${simulatedBis >= 8 ? 'text-white' : 'text-rose-400'}`}>
                     {simulatedBis.toFixed(2)}%
                   </div>
                   <div className={`flex items-center gap-1 text-sm font-black italic mb-1 ${bisDelta >= 0 ? 'text-emerald-400' : 'text-rose-400'}`}>
                      {bisDelta >= 0 ? <ArrowUpRight size={16} /> : <ArrowDownRight size={16} />}
                      {Math.abs(bisDelta).toFixed(2)}%p
                   </div>
                </div>
                <div className="mt-6 flex justify-between items-center">
                   <span className="text-[10px] font-bold text-slate-600 uppercase">Current: {originalBis.toFixed(2)}%</span>
                   <div className={`px-3 py-1 rounded-full text-[10px] font-black uppercase tracking-widest ${simulatedBis >= 8 ? 'bg-emerald-500/10 text-emerald-500 border border-emerald-500/20' : 'bg-rose-500/10 text-rose-500 border border-rose-500/20'}`}>
                      {simulatedBis >= 8 ? 'Compliance' : 'Under Limit'}
                   </div>
                </div>
             </div>
          </div>

          {/* 세부 비교 테이블 */}
          <div className="bg-slate-900/40 border border-white/10 rounded-[32px] overflow-hidden">
            <div className="p-8 border-b border-white/5 bg-white/[0.02] flex justify-between items-center">
               <h3 className="text-sm font-black text-white italic uppercase tracking-widest flex items-center gap-2">
                 <BarChart size={18} className="text-indigo-400" /> Simulated Detail Analysis
               </h3>
               <span className="text-[10px] font-bold text-slate-500 uppercase tracking-widest">Unit: KRW</span>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-left">
                <thead>
                  <tr className="text-slate-500 text-[10px] font-black uppercase tracking-[0.15em] bg-white/[0.03]">
                    <th className="px-8 py-6">Counterparty</th>
                    <th className="px-6 py-6">EAD</th>
                    <th className="px-6 py-6">Orig RW</th>
                    <th className="px-6 py-6">Sim RW</th>
                    <th className="px-6 py-6 text-right">Sim RWA</th>
                    <th className="px-8 py-6 text-right">Change</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/5">
                  {simulatedData.map((item, idx) => {
                    const origItem = originalData[idx];
                    const change = item.rwaAmount - origItem.rwaAmount;
                    return (
                      <tr key={item.id} className="hover:bg-white/[0.02] transition-colors group">
                        <td className="px-8 py-6 font-bold text-white text-sm">{item.businessPartnerName}</td>
                        <td className="px-6 py-6 text-xs font-mono text-slate-500">₩{item.eadAmount.toLocaleString()}</td>
                        <td className="px-6 py-6 text-xs font-black text-slate-600">{(origItem.riskWeight * 100).toFixed(0)}%</td>
                        <td className="px-6 py-6 text-xs font-black text-indigo-400">{(item.riskWeight * 100).toFixed(0)}%</td>
                        <td className="px-6 py-6 text-right font-black text-white italic tracking-tighter">₩{item.rwaAmount.toLocaleString()}</td>
                        <td className={`px-8 py-6 text-right text-[10px] font-bold ${change >= 0 ? 'text-rose-400' : 'text-emerald-400'}`}>
                          {change > 0 ? '+' : ''}{change.toLocaleString()}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}

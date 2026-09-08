'use client';

import React, { useState, useSyncExternalStore } from 'react';
import {
  Cpu,
  Sliders,
  Play,
  RotateCcw,
  BarChart3,
  ArrowRight,
  Zap
} from 'lucide-react';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  Legend,
  CartesianGrid
} from 'recharts';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import AmountDisplay from '@/components/ui/AmountDisplay';
import { mockEadSimulations, EadSimulationDto } from '@/mocks/ecl';

const emptySubscribe = () => () => {};

export default function EclEadPage() {
  const mounted = useSyncExternalStore(emptySubscribe, () => true, () => false);

  // Simulation Form State
  const [productType, setProductType] = useState('기업 한도대출');
  const [committedAmount, setCommittedAmount] = useState<number>(1000000000); // 10억
  const [drawnAmount, setDrawnAmount] = useState<number>(600000000); // 6억
  const [undrawnAmount, setUndrawnAmount] = useState<number>(400000000); // 4억
  const [ccf, setCcf] = useState<number>(75); // 75%
  const [amortization, setAmortization] = useState('만기일시');
  const [remainingMonths] = useState<number>(36);

  // Dynamic Calculation
  const calculatedEad = drawnAmount + (undrawnAmount * (ccf / 100));
  const undrawnEadComponent = undrawnAmount * (ccf / 100);
  const effectiveEadRatio = committedAmount > 0 ? ((calculatedEad / committedAmount) * 100).toFixed(1) : '0';

  const [testScenarios, setTestScenarios] = useState<EadSimulationDto[]>(mockEadSimulations);

  // Chart Data for EAD Simulation Scenarios
  const chartData = testScenarios.map(s => ({
    name: s.contractNo,
    Drawn: s.drawnAmount / 100000000,
    UndrawnEAD: (s.undrawnAmount * (s.ccf / 100)) / 100000000,
    TotalEAD: s.calculatedEad / 100000000,
  }));

  if (!mounted) {
    return <div className="p-8 text-slate-400">Loading EAD 엔진 검증 Page...</div>;
  }

  return (
    <div className="space-y-8 pb-16">
      {/* Page Header */}
      <PageHeader
        title="EAD 엔진 검증"
        description="부도시 익스포저(Exposure at Default) 산출 공식 검증 및 파라미터 시뮬레이션 Engine"
        breadcrumbs={[
          { label: 'ECL' },
          { label: 'EAD 엔진 검증' }
        ]}
        icon={Cpu}
        actions={
          <div className="flex items-center gap-3">
            <button
              onClick={() => {
                setDrawnAmount(600000000);
                setUndrawnAmount(400000000);
                setCcf(75);
              }}
              className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 border border-white/10 font-bold text-sm transition-all"
            >
              <RotateCcw size={16} />
              시뮬레이터 초기화
            </button>
          </div>
        }
      />

      {/* Main Interactive Simulation Engine */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Input Parameters Form (2 cols) */}
        <div className="lg:col-span-2 bg-slate-900/50 backdrop-blur-md rounded-2xl border border-white/5 p-6 space-y-6">
          <div className="flex items-center justify-between pb-4 border-b border-white/5">
            <div className="space-y-1">
              <span className="text-xs font-bold text-blue-400 uppercase tracking-widest">REALTIME EAD SIMULATION</span>
              <h3 className="text-lg font-bold text-white flex items-center gap-2">
                <Sliders size={18} className="text-blue-400" />
                EAD 산출 변수 패널
              </h3>
            </div>
            <span className="text-xs text-slate-400 font-mono bg-blue-500/10 text-blue-400 px-3 py-1 rounded-full border border-blue-500/20">
              Formula: EAD = Drawn + (Undrawn × CCF)
            </span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
            <div>
              <label className="block text-xs font-bold text-slate-400 mb-1">상품 구분</label>
              <select
                value={productType}
                onChange={(e) => setProductType(e.target.value)}
                className="w-full px-4 py-2.5 bg-slate-950 border border-white/10 rounded-xl text-sm text-white focus:outline-none focus:border-blue-500 font-medium"
              >
                <option value="기업 한도대출">기업 한도대출 (Credit Line)</option>
                <option value="아파트 주택담보대출">아파트 주택담보대출</option>
                <option value="개인 직장인 신용대출">개인 직장인 신용대출</option>
                <option value="기업구매카드 한도">기업구매카드 한도</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-400 mb-1">상각 방식</label>
              <select
                value={amortization}
                onChange={(e) => setAmortization(e.target.value)}
                className="w-full px-4 py-2.5 bg-slate-950 border border-white/10 rounded-xl text-sm text-white focus:outline-none focus:border-blue-500 font-medium"
              >
                <option value="만기일시">만기일시상환</option>
                <option value="원리금균등">원리금균등 분할상환</option>
                <option value="원금균등">원금균등 분할상환</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-400 mb-1">총 약정 한도 (Committed Limit)</label>
              <input
                type="number"
                step="10000000"
                value={committedAmount}
                onChange={(e) => {
                  const val = parseFloat(e.target.value) || 0;
                  setCommittedAmount(val);
                  if (drawnAmount > val) setDrawnAmount(val);
                  setUndrawnAmount(Math.max(0, val - drawnAmount));
                }}
                className="w-full px-4 py-2.5 bg-slate-950 border border-white/10 rounded-xl text-base font-bold text-white font-mono focus:outline-none focus:border-blue-500"
              />
              <span className="text-[11px] text-slate-500 font-mono">
                ₩{(committedAmount / 100000000).toFixed(2)} 억원
              </span>
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-400 mb-1">현재 미상환 잔액 (Drawn Balance)</label>
              <input
                type="number"
                step="10000000"
                value={drawnAmount}
                onChange={(e) => {
                  const val = parseFloat(e.target.value) || 0;
                  setDrawnAmount(val);
                  setUndrawnAmount(Math.max(0, committedAmount - val));
                }}
                className="w-full px-4 py-2.5 bg-slate-950 border border-white/10 rounded-xl text-base font-bold text-emerald-400 font-mono focus:outline-none focus:border-blue-500"
              />
              <span className="text-[11px] text-slate-500 font-mono">
                ₩{(drawnAmount / 100000000).toFixed(2)} 억원
              </span>
            </div>
          </div>

          {/* CCF Slider Control */}
          <div className="p-4 rounded-xl bg-slate-950/60 border border-white/5 space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-slate-300">신용전환율 적용 (CCF %)</span>
              <span className="text-lg font-black text-amber-400 font-mono">{ccf}%</span>
            </div>
            <input
              type="range"
              min="0"
              max="100"
              step="5"
              value={ccf}
              onChange={(e) => setCcf(parseInt(e.target.value))}
              className="w-full h-2 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-amber-500"
            />
            <div className="flex justify-between text-[10px] text-slate-500 font-mono">
              <span>0% (미인출 전액 제외)</span>
              <span>50% (표준 CCF)</span>
              <span>100% (전액 인출 가정)</span>
            </div>
          </div>

          {/* Real-time Equation Breakdown Box */}
          <div className="p-4 rounded-xl bg-blue-500/10 border border-blue-500/20 text-xs font-mono space-y-2">
            <div className="text-blue-400 font-bold flex items-center gap-2">
              <Zap size={14} /> EAD 산출 방정식 연산 결과:
            </div>
            <div className="text-slate-300 text-sm flex items-center gap-2 flex-wrap">
              <span>₩{(drawnAmount / 100000000).toFixed(2)}억 (미상환)</span>
              <span className="text-blue-400 font-bold">+</span>
              <span>[ ₩{(undrawnAmount / 100000000).toFixed(2)}억 (미인출) × {ccf}% ]</span>
              <ArrowRight size={14} className="text-blue-400" />
              <span className="text-white font-black text-base">₩{(calculatedEad / 100000000).toFixed(2)}억원</span>
            </div>
          </div>
        </div>

        {/* Output KPI Card (1 col) */}
        <div className="bg-slate-900/50 backdrop-blur-md rounded-2xl border border-white/5 p-6 flex flex-col justify-between space-y-6">
          <div className="space-y-4">
            <div className="pb-3 border-b border-white/5">
              <span className="text-xs font-bold text-emerald-400 uppercase tracking-widest">CALCULATED OUTPUT</span>
              <h3 className="text-xl font-bold text-white">최종 EAD 산출액</h3>
            </div>

            <div className="space-y-1">
              <span className="text-xs text-slate-400">최종 EAD (Exposure at Default)</span>
              <div>
                <AmountDisplay amount={calculatedEad} className="text-3xl font-black text-emerald-400" />
              </div>
            </div>

            <div className="space-y-3 pt-2">
              <div className="p-3 rounded-xl bg-slate-950/60 border border-white/5 space-y-1">
                <span className="text-xs text-slate-400">미인출 한도 발생 EAD 가산분</span>
                <div>
                  <AmountDisplay amount={undrawnEadComponent} className="text-lg font-bold text-amber-400" />
                </div>
              </div>

              <div className="p-3 rounded-xl bg-slate-950/60 border border-white/5 space-y-1">
                <span className="text-xs text-slate-400">약정 한도 대비 EAD 비율</span>
                <div className="text-xl font-black text-white font-mono">{effectiveEadRatio}%</div>
              </div>
            </div>
          </div>

          <button
            onClick={() => {
              const newScenario: EadSimulationDto = {
                id: `ead-${Date.now()}`,
                contractNo: `SIM-USER-${Math.floor(Math.random() * 900 + 100)}`,
                customerName: '시뮬레이션 차주',
                productType: productType,
                committedAmount: committedAmount,
                drawnAmount: drawnAmount,
                undrawnAmount: undrawnAmount,
                ccf: ccf,
                calculatedEad: calculatedEad,
                amortizationMethod: amortization as EadSimulationDto['amortizationMethod'],
                remainingMonths: remainingMonths,
                status: 'PASS',
              };
              setTestScenarios(prev => [newScenario, ...prev]);
            }}
            className="w-full py-3 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-bold text-sm shadow-lg shadow-blue-600/30 transition-all flex items-center justify-center gap-2"
          >
            <Play size={16} />
            시나리오 결과에 추가
          </button>
        </div>
      </div>

      {/* Comparison Recharts Visual Chart */}
      <div className="bg-slate-900/50 backdrop-blur-md rounded-2xl border border-white/5 p-6 space-y-4">
        <div className="flex items-center justify-between pb-2 border-b border-white/5">
          <h3 className="font-bold text-white text-base flex items-center gap-2">
            <BarChart3 size={18} className="text-blue-400" />
            계약 시나리오별 EAD 분해 구성 (단위: 억원)
          </h3>
        </div>
        <div className="h-64 w-full">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={chartData} margin={{ top: 20, right: 30, left: 10, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#334155" opacity={0.5} />
              <XAxis dataKey="name" stroke="#94a3b8" tick={{ fill: '#94a3b8', fontSize: 12 }} />
              <YAxis stroke="#94a3b8" tick={{ fill: '#94a3b8', fontSize: 12 }} />
              <Tooltip
                contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '12px', color: '#fff' }}
                formatter={(val: unknown) => [`${Number(val || 0).toFixed(2)}억원`, '']}
              />
              <Legend wrapperStyle={{ paddingTop: '10px' }} />
              <Bar dataKey="Drawn" name="미상환 잔액 (Drawn)" stackId="a" fill="#10B981" />
              <Bar dataKey="UndrawnEAD" name="미인출 CCF 반영분" stackId="a" fill="#F59E0B" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Test Scenarios & Contract Verification Table */}
      <div className="bg-slate-900/50 backdrop-blur-md rounded-2xl border border-white/5 overflow-hidden">
        <div className="p-6 border-b border-white/5 flex items-center justify-between">
          <h3 className="font-bold text-white text-lg">EAD 산출 검증 시나리오 목록</h3>
          <span className="text-xs text-slate-400 font-mono">총 {testScenarios.length}개 계약 시뮬레이션</span>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-white/5 bg-slate-900/80 text-slate-400 text-xs font-black uppercase tracking-wider">
                <th className="px-6 py-4">계약 시뮬레이션 번호 / 차주명</th>
                <th className="px-6 py-4">상품종류</th>
                <th className="px-6 py-4 text-right">약정금액</th>
                <th className="px-6 py-4 text-right">미상환 잔액</th>
                <th className="px-6 py-4 text-right">미인출 잔액</th>
                <th className="px-6 py-4 text-center">CCF %</th>
                <th className="px-6 py-4 text-right">최종 EAD 산출액</th>
                <th className="px-6 py-4 text-center">검증 결과</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5 text-slate-300">
              {testScenarios.map((item) => (
                <tr key={item.id} className="hover:bg-white/5 transition-all">
                  <td className="px-6 py-4">
                    <div className="font-mono font-bold text-white">{item.contractNo}</div>
                    <div className="text-xs text-slate-400">{item.customerName}</div>
                  </td>
                  <td className="px-6 py-4 font-bold text-slate-200">{item.productType}</td>
                  <td className="px-6 py-4 text-right">
                    <AmountDisplay amount={item.committedAmount} className="font-bold" />
                  </td>
                  <td className="px-6 py-4 text-right text-emerald-400">
                    <AmountDisplay amount={item.drawnAmount} className="text-emerald-400" />
                  </td>
                  <td className="px-6 py-4 text-right text-amber-400">
                    <AmountDisplay amount={item.undrawnAmount} className="text-amber-400" />
                  </td>
                  <td className="px-6 py-4 text-center font-mono font-bold text-amber-400">
                    {item.ccf}%
                  </td>
                  <td className="px-6 py-4 text-right">
                    <AmountDisplay amount={item.calculatedEad} className="font-black text-blue-400" />
                  </td>
                  <td className="px-6 py-4 text-center">
                    <StatusBadge
                      status={item.status === 'PASS' ? '검증 통과' : '경고'}
                      variant={item.status === 'PASS' ? 'success' : 'warning'}
                    />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

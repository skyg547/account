'use client';

import React, { useState, useSyncExternalStore } from 'react';
import {
  PieChart as PieIcon,
  BarChart3,
  TrendingUp,
  FileSpreadsheet,
  CheckCircle2,
  AlertTriangle,
  FileCheck,
  Layers,
  ArrowUpRight,
  ArrowDownRight,
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
  CartesianGrid,
  PieChart,
  Pie,
  Cell,
  LineChart,
  Line
} from 'recharts';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import AmountDisplay from '@/components/ui/AmountDisplay';
import { mockResults, EclResultDto } from '@/mocks/ecl';

const emptySubscribe = () => () => {};

export default function EclResultsPage() {
  const mounted = useSyncExternalStore(emptySubscribe, () => true, () => false);
  const [results, setResults] = useState<EclResultDto[]>(mockResults);
  const [notification, setNotification] = useState<string | null>(null);

  const totalExposure = results.reduce((sum, item) => sum + item.exposureAmount, 0);
  const totalStage1 = results.reduce((sum, item) => sum + item.stage1, 0);
  const totalStage2 = results.reduce((sum, item) => sum + item.stage2, 0);
  const totalStage3 = results.reduce((sum, item) => sum + item.stage3, 0);
  const totalEcl = results.reduce((sum, item) => sum + item.totalEcl, 0);
  const avgCoverageRatio = totalExposure > 0 ? ((totalEcl / totalExposure) * 100).toFixed(2) : '0';

  // Chart 1 Data: Stacked Bar Chart for Portfolios
  const portfolioChartData = results.map(r => ({
    name: r.portfolio.split(' ')[0], // Short name
    Stage1: r.stage1 / 100000000, // 억원 단위
    Stage2: r.stage2 / 100000000,
    Stage3: r.stage3 / 100000000,
  }));

  // Chart 2 Data: Stage Distribution Pie Chart
  const pieChartData = [
    { name: 'Stage 1 (12M ECL)', value: totalStage1, color: '#10B981' },
    { name: 'Stage 2 (Lifetime ECL)', value: totalStage2, color: '#F59E0B' },
    { name: 'Stage 3 (Impaired ECL)', value: totalStage3, color: '#EF4444' },
  ];

  // Chart 3 Data: 6-Month Trend
  const trendData = [
    { month: '2026-01', ecl: 25.8, coverage: 1.82 },
    { month: '2026-02', ecl: 26.2, coverage: 1.85 },
    { month: '2026-03', ecl: 26.9, coverage: 1.90 },
    { month: '2026-04', ecl: 27.1, coverage: 1.93 },
    { month: '2026-05', ecl: 27.0, coverage: 1.91 },
    { month: '2026-06', ecl: 27.45, coverage: 1.96 },
  ];

  const handleIssueJournal = () => {
    setNotification('IFRS9 대손충당금 분개 전표가 재무회계 시스템으로 발행되었습니다. (전표번호: JV-20260630-ECL01)');
    setTimeout(() => setNotification(null), 5000);
  };

  if (!mounted) {
    return <div className="p-8 text-slate-400">Loading ECL 산출 결과 Page...</div>;
  }

  return (
    <div className="space-y-8 pb-16">
      {/* Page Header */}
      <PageHeader
        title="ECL 산출 결과"
        description="포트폴리오 및 Stage별 대손충당금 산출 결과 시각화 및 손상 차손 영향 분석"
        breadcrumbs={[
          { label: 'ECL' },
          { label: '산출 결과' }
        ]}
        icon={PieIcon}
        actions={
          <div className="flex items-center gap-3">
            <button className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 border border-white/10 font-bold text-sm transition-all">
              <FileSpreadsheet size={16} />
              결과 엑셀 다운로드
            </button>
            <button
              onClick={handleIssueJournal}
              className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-bold text-sm shadow-lg shadow-blue-600/30 transition-all"
            >
              <FileCheck size={16} />
              회계 전표 발행
            </button>
          </div>
        }
      />

      {/* Toast Notification */}
      {notification && (
        <div className="p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 font-medium flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Zap size={18} />
            <span>{notification}</span>
          </div>
          <button onClick={() => setNotification(null)} className="text-slate-400 hover:text-white text-xs">닫기</button>
        </div>
      )}

      {/* KPI Stat Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <div className="bg-slate-900/50 backdrop-blur-md p-6 rounded-2xl border border-white/5 space-y-2">
          <div className="flex items-center justify-between text-slate-400 text-xs font-bold uppercase tracking-wider">
            <span>총 여신 익스포저 (EAD)</span>
            <Layers size={18} className="text-blue-400" />
          </div>
          <div>
            <AmountDisplay amount={totalExposure} className="text-2xl font-black" />
          </div>
          <p className="text-xs text-slate-400 pt-1">5개 주요 포트폴리오 합계</p>
        </div>

        <div className="bg-slate-900/50 backdrop-blur-md p-6 rounded-2xl border border-white/5 space-y-2">
          <div className="flex items-center justify-between text-slate-400 text-xs font-bold uppercase tracking-wider">
            <span>총 대손충당금 (ECL)</span>
            <PieIcon size={18} className="text-rose-400" />
          </div>
          <div>
            <AmountDisplay amount={totalEcl} className="text-2xl font-black text-rose-400" />
          </div>
          <div className="flex items-center gap-1 text-xs text-emerald-400 pt-1 font-bold">
            <ArrowUpRight size={14} />
            <span>전월 대비 +2.4% 증가</span>
          </div>
        </div>

        <div className="bg-slate-900/50 backdrop-blur-md p-6 rounded-2xl border border-white/5 space-y-2">
          <div className="flex items-center justify-between text-slate-400 text-xs font-bold uppercase tracking-wider">
            <span>평균 충당금 적립률</span>
            <TrendingUp size={18} className="text-emerald-400" />
          </div>
          <div className="text-2xl font-black text-white font-mono">
            {avgCoverageRatio} <span className="text-sm font-normal text-slate-400">%</span>
          </div>
          <p className="text-xs text-slate-400 pt-1">IFRS 9 기준 적립 충족율 100%</p>
        </div>

        <div className="bg-slate-900/50 backdrop-blur-md p-6 rounded-2xl border border-white/5 space-y-2">
          <div className="flex items-center justify-between text-slate-400 text-xs font-bold uppercase tracking-wider">
            <span>Stage 3 비중 및 충당금</span>
            <AlertTriangle size={18} className="text-amber-400" />
          </div>
          <div>
            <AmountDisplay amount={totalStage3} className="text-2xl font-black text-amber-400" />
          </div>
          <p className="text-xs text-slate-400 pt-1">전체 충당금의 {((totalStage3 / totalEcl) * 100).toFixed(1)}% 차지</p>
        </div>
      </div>

      {/* Visualizations Section */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Stacked Bar Chart (2 cols) */}
        <div className="lg:col-span-2 bg-slate-900/50 backdrop-blur-md p-6 rounded-2xl border border-white/5 space-y-4">
          <div className="flex items-center justify-between pb-2 border-b border-white/5">
            <div className="space-y-1">
              <h3 className="font-bold text-white text-base flex items-center gap-2">
                <BarChart3 size={18} className="text-blue-400" />
                포트폴리오별 Stage 1/2/3 충당금 구성 (단위: 억원)
              </h3>
            </div>
          </div>
          <div className="h-72 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={portfolioChartData} margin={{ top: 20, right: 30, left: 10, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#334155" opacity={0.5} />
                <XAxis dataKey="name" stroke="#94a3b8" tick={{ fill: '#94a3b8', fontSize: 12 }} />
                <YAxis stroke="#94a3b8" tick={{ fill: '#94a3b8', fontSize: 12 }} />
                <Tooltip
                  contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '12px', color: '#fff' }}
                  formatter={(val: any) => [`${Number(val || 0).toFixed(1)}억원`, '']}
                />
                <Legend wrapperStyle={{ paddingTop: '10px' }} />
                <Bar dataKey="Stage1" name="Stage 1 (12M)" stackId="a" fill="#10B981" radius={[0, 0, 0, 0]} />
                <Bar dataKey="Stage2" name="Stage 2 (생애)" stackId="a" fill="#F59E0B" radius={[0, 0, 0, 0]} />
                <Bar dataKey="Stage3" name="Stage 3 (손상)" stackId="a" fill="#EF4444" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Stage Donut Pie Chart (1 col) */}
        <div className="bg-slate-900/50 backdrop-blur-md p-6 rounded-2xl border border-white/5 space-y-4">
          <div className="pb-2 border-b border-white/5">
            <h3 className="font-bold text-white text-base flex items-center gap-2">
              <PieIcon size={18} className="text-purple-400" />
              Stage별 충당금 적립 비율
            </h3>
          </div>
          <div className="h-56 w-full flex items-center justify-center">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={pieChartData}
                  cx="50%"
                  cy="50%"
                  innerRadius={55}
                  outerRadius={80}
                  paddingAngle={5}
                  dataKey="value"
                >
                  {pieChartData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip
                  contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '12px', color: '#fff' }}
                  formatter={(val: any) => [`₩${(Number(val || 0) / 100000000).toFixed(1)}억원`, '충당금']}
                />
              </PieChart>
            </ResponsiveContainer>
          </div>
          <div className="space-y-2 text-xs font-medium pt-2">
            {pieChartData.map((item, idx) => (
              <div key={idx} className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <div className="w-3 h-3 rounded-full" style={{ backgroundColor: item.color }} />
                  <span className="text-slate-300">{item.name}</span>
                </div>
                <span className="font-mono text-white font-bold">{((item.value / totalEcl) * 100).toFixed(1)}%</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Results Detailed Table */}
      <div className="bg-slate-900/50 backdrop-blur-md rounded-2xl border border-white/5 overflow-hidden">
        <div className="p-6 border-b border-white/5 flex items-center justify-between">
          <h3 className="font-bold text-white text-lg">포트폴리오별 ECL 산출 상세 명세</h3>
          <span className="text-xs text-slate-400 font-mono">기준일: 2026-06-30</span>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-white/5 bg-slate-900/80 text-slate-400 text-xs font-black uppercase tracking-wider">
                <th className="px-6 py-4">포트폴리오</th>
                <th className="px-6 py-4 text-right">여신 익스포저 (EAD)</th>
                <th className="px-6 py-4 text-right">Stage 1 ECL</th>
                <th className="px-6 py-4 text-right">Stage 2 ECL</th>
                <th className="px-6 py-4 text-right">Stage 3 ECL</th>
                <th className="px-6 py-4 text-right">총 산출 ECL</th>
                <th className="px-6 py-4 text-center">충당금 적립률</th>
                <th className="px-6 py-4 text-center">전월 대비 변동</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5 text-slate-300">
              {results.map((r) => (
                <tr key={r.id} className="hover:bg-white/5 transition-all">
                  <td className="px-6 py-4 font-bold text-white">
                    {r.portfolio}
                  </td>
                  <td className="px-6 py-4 text-right">
                    <AmountDisplay amount={r.exposureAmount} className="font-bold" />
                  </td>
                  <td className="px-6 py-4 text-right text-emerald-400 font-mono">
                    <AmountDisplay amount={r.stage1} className="text-emerald-400" />
                  </td>
                  <td className="px-6 py-4 text-right text-amber-400 font-mono">
                    <AmountDisplay amount={r.stage2} className="text-amber-400" />
                  </td>
                  <td className="px-6 py-4 text-right text-rose-400 font-mono">
                    <AmountDisplay amount={r.stage3} className="text-rose-400" />
                  </td>
                  <td className="px-6 py-4 text-right font-black text-rose-400">
                    <AmountDisplay amount={r.totalEcl} className="text-rose-400 font-black" />
                  </td>
                  <td className="px-6 py-4 text-center font-mono font-bold text-white">
                    {r.coverageRatio}%
                  </td>
                  <td className="px-6 py-4 text-center font-mono text-xs">
                    {r.changeRate !== undefined && r.changeRate > 0 ? (
                      <span className="text-rose-400 flex items-center justify-center gap-1">
                        <ArrowUpRight size={14} /> +{r.changeRate}%
                      </span>
                    ) : (
                      <span className="text-emerald-400 flex items-center justify-center gap-1">
                        <ArrowDownRight size={14} /> {r.changeRate}%
                      </span>
                    )}
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

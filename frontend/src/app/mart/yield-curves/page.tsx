'use client';

import React, { useState, useSyncExternalStore } from 'react';
import {
  LineChart as LineChartIcon,
  TrendingUp,
  Download,
  RefreshCw,
  Calendar,
  Zap,
  Sliders
} from 'lucide-react';
import { 
  ResponsiveContainer, 
  LineChart, 
  Line, 
  XAxis, 
  YAxis, 
  Tooltip, 
  Legend, 
  CartesianGrid 
} from 'recharts';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import Tabs, { TabItem } from '@/components/ui/Tabs';import { mockYieldCurves, YieldCurveDto } from '@/mocks/mart';

const emptySubscribe = () => () => {};

export default function YieldCurvesPage() {
  const mounted = useSyncExternalStore(emptySubscribe, () => true, () => false);
  const [curves] = useState<YieldCurveDto[]>(mockYieldCurves);
  const [selectedCurveId, setSelectedCurveId] = useState<string>('ALL');
  const [interpolator, setInterpolator] = useState<'CUBIC_SPLINE' | 'NELSON_SIEGEL' | 'LINEAR'>('CUBIC_SPLINE');
  const [isRecalculating, setIsRecalculating] = useState(false);

  const curveTabs: TabItem[] = [
    { id: 'ALL', label: '전체 수익률곡선 비교' },
    { id: 'YC-KRW-GOV', label: '국고채 Curve' },
    { id: 'YC-KRW-CORP-AA', label: '회사채 AA- Curve' },
    { id: 'YC-USD-TREASURY', label: '미 국채 Curve' },
  ];

  const govCurve = curves.find(c => c.curveId === 'YC-KRW-GOV');
  const corpCurve = curves.find(c => c.curveId === 'YC-KRW-CORP-AA');
  const usCurve = curves.find(c => c.curveId === 'YC-USD-TREASURY');

  // Build merged chart data array for tenors
  const tenors = ['1M', '3M', '6M', '9M', '1Y', '2Y', '3Y', '5Y', '10Y', '20Y', '30Y'];
  const chartData = tenors.map(t => {
    const govPt = govCurve?.points.find(p => p.tenor === t);
    const corpPt = corpCurve?.points.find(p => p.tenor === t);
    const usPt = usCurve?.points.find(p => p.tenor === t);
    return {
      tenor: t,
      gov: govPt?.rate || 0,
      corp: corpPt?.rate || 0,
      us: usPt?.rate || 0,
      govChange: govPt?.changeBp || 0,
    };
  });

  const handleRecalculate = () => {
    setIsRecalculating(true);
    setTimeout(() => {
      setIsRecalculating(false);
      alert(`[${interpolator}] 보간 알고리즘으로 수익률곡선 재산출이 완료되었습니다.`);
    }, 1000);
  };

  if (!mounted) return null;

  return (
    <div className="space-y-8 pb-12">
      {/* Page Header */}
      <PageHeader
        title="수익률곡선"
        description="국고채 및 회사채 만기별 수익률곡선 (Yield Curve) 시각화, 보간 알고리즘 관리 및 텐너별 금리 파악"
        breadcrumbs={[
          { label: '홈', href: '/' },
          { label: '기준정보 / 마트' },
          { label: '수익률곡선' },
        ]}
        icon={LineChartIcon}
        actions={
          <div className="flex items-center gap-3">
            <button
              onClick={handleRecalculate}
              disabled={isRecalculating}
              className="px-4 py-2.5 rounded-xl text-xs font-black bg-white/5 text-slate-300 hover:text-white border border-white/10 hover:bg-white/10 transition-all flex items-center gap-2 disabled:opacity-50"
            >
              <RefreshCw size={14} className={isRecalculating ? 'animate-spin text-blue-400' : 'text-blue-400'} />
              {isRecalculating ? '보간 계산 중...' : '곡선 재산출'}
            </button>
            <button
              onClick={() => alert('Yield Curve 데이터셋(JSON/CSV)을 익스포트합니다.')}
              className="px-5 py-2.5 rounded-xl text-xs font-black bg-blue-600 hover:bg-blue-500 text-white shadow-lg shadow-blue-600/20 transition-all flex items-center gap-2"
            >
              <Download size={14} />
              Curve Data Export
            </button>
          </div>
        }
      />

      {/* Top Stat Bar & Interpolation Settings */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-5">
        <div className="p-5 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="text-xs font-bold text-slate-400 uppercase tracking-wider">평가 일자 (Valuation Date)</div>
          <div className="text-xl font-black text-white font-mono flex items-center gap-2">
            <Calendar size={18} className="text-blue-400" />
            2026-07-28
          </div>
          <div className="text-xs text-slate-500">당일 최종 채권평가사 마감</div>
        </div>

        <div className="p-5 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="text-xs font-bold text-slate-400 uppercase tracking-wider">보간법 (Interpolator)</div>
          <div className="flex items-center gap-2">
            <select
              value={interpolator}
              onChange={e => setInterpolator(e.target.value as 'CUBIC_SPLINE' | 'NELSON_SIEGEL' | 'LINEAR')}
              className="w-full bg-white/5 border border-white/10 rounded-xl px-3 py-1.5 text-xs text-white font-mono focus:outline-none focus:border-blue-500"
            >
              <option value="CUBIC_SPLINE" className="bg-slate-900">CUBIC SPLINE (3차 스플라인)</option>
              <option value="NELSON_SIEGEL" className="bg-slate-900">NELSON-SIEGEL 모델</option>
              <option value="LINEAR" className="bg-slate-900">LINEAR (선형 보간)</option>
            </select>
          </div>
          <div className="text-xs text-emerald-400 flex items-center gap-1">
            <Zap size={12} /> 실시간 곡선 보간 정밀도 상
          </div>
        </div>

        <div className="p-5 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="text-xs font-bold text-slate-400 uppercase tracking-wider">국고 3Y vs 10Y 장단기 스프레드</div>
          <div className="text-xl font-black text-white font-mono">
            -6.0 <span className="text-sm font-normal text-slate-400">bp (역전)</span>
          </div>
          <div className="text-xs text-amber-400">3Y: 3.38% / 10Y: 3.32%</div>
        </div>

        <div className="p-5 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="text-xs font-bold text-slate-400 uppercase tracking-wider">신용 스프레드 (AA- 3Y - 국고 3Y)</div>
          <div className="text-xl font-black text-white font-mono">
            +80.0 <span className="text-sm font-normal text-slate-400">bp</span>
          </div>
          <div className="text-xs text-slate-400">회사채 AA- 3Y: 4.18%</div>
        </div>
      </div>

      {/* Tabs */}
      <Tabs tabs={curveTabs} activeTab={selectedCurveId} onChange={setSelectedCurveId} />

      {/* Recharts Visualization */}
      <div className="p-6 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-base font-black text-white flex items-center gap-2">
              <TrendingUp className="text-blue-400" size={18} />
              만기구간(Tenor)별 Yield Curve 시각화
            </h3>
            <p className="text-xs text-slate-400 mt-1">1개월(1M)부터 30년(30Y)까지 수익률 곡선 추이</p>
          </div>
          <StatusBadge status={`보간: ${interpolator}`} variant="info" />
        </div>

        <div className="h-80 w-full pt-4">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={chartData} margin={{ top: 10, right: 30, left: 10, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
              <XAxis dataKey="tenor" stroke="#64748b" fontSize={11} tickLine={false} />
              <YAxis domain={[3.0, 5.5]} stroke="#64748b" fontSize={11} tickLine={false} unit="%" />
              <Tooltip 
                contentStyle={{ 
                  backgroundColor: '#0f172a', 
                  borderColor: 'rgba(255,255,255,0.1)', 
                  borderRadius: '12px',
                  color: '#fff',
                  fontSize: '12px'
                }} 
              />
              <Legend wrapperStyle={{ fontSize: '11px', paddingTop: '10px' }} />
              {(selectedCurveId === 'ALL' || selectedCurveId === 'YC-KRW-GOV') && (
                <Line type="monotone" dataKey="gov" name="국고채 (KTB)" stroke="#3b82f6" strokeWidth={3} dot={{ r: 4 }} />
              )}
              {(selectedCurveId === 'ALL' || selectedCurveId === 'YC-KRW-CORP-AA') && (
                <Line type="monotone" dataKey="corp" name="회사채 AA-" stroke="#10b981" strokeWidth={3} dot={{ r: 4 }} />
              )}
              {(selectedCurveId === 'ALL' || selectedCurveId === 'YC-USD-TREASURY') && (
                <Line type="monotone" dataKey="us" name="미 국채 (Treasury)" stroke="#8b5cf6" strokeWidth={3} dot={{ r: 4 }} />
              )}
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Tenor Rates Data Table */}
      <div className="p-6 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-6">
        <div className="flex items-center justify-between">
          <h3 className="text-base font-black text-white flex items-center gap-2">
            <Sliders className="text-purple-400" size={18} />
            만기구간(Tenor) 금리 세부 내역
          </h3>
          <span className="text-xs text-slate-500 font-mono">단위: % (변동: bp)</span>
        </div>

        <div className="overflow-x-auto rounded-xl border border-white/5">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="bg-white/5 border-b border-white/10 text-slate-400 font-black uppercase tracking-wider">
                <th className="py-3.5 px-4">Tenor (만기구간)</th>
                <th className="py-3.5 px-4 text-right">국고채 금리 (%)</th>
                <th className="py-3.5 px-4 text-right">국고채 전일변동 (bp)</th>
                <th className="py-3.5 px-4 text-right">회사채 AA- (%)</th>
                <th className="py-3.5 px-4 text-right">신용 스프레드 (bp)</th>
                <th className="py-3.5 px-4 text-right">미 국채 (%)</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5 text-slate-300">
              {chartData.map((row) => {
                const creditSpread = ((row.corp - row.gov) * 100).toFixed(1);
                return (
                  <tr key={row.tenor} className="hover:bg-white/[0.02] transition-colors">
                    <td className="py-3.5 px-4 font-mono font-black text-blue-400 text-sm">{row.tenor}</td>
                    <td className="py-3.5 px-4 text-right font-mono font-bold text-white text-sm">{row.gov.toFixed(2)} %</td>
                    <td className="py-3.5 px-4 text-right font-mono text-emerald-400 font-bold">{row.govChange} bp</td>
                    <td className="py-3.5 px-4 text-right font-mono font-bold text-emerald-400 text-sm">{row.corp.toFixed(2)} %</td>
                    <td className="py-3.5 px-4 text-right font-mono text-slate-300">+{creditSpread} bp</td>
                    <td className="py-3.5 px-4 text-right font-mono font-bold text-purple-400 text-sm">{row.us.toFixed(2)} %</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

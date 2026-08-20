'use client';

import React, { useState, useSyncExternalStore } from 'react';
import { 
  ShieldCheck, 
  AlertTriangle, 
  CheckCircle2, 
  RefreshCw, 
  Download, 
  Search, 
  BarChart2, 
  Activity,
  Layers,
  FileCheck2
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
import Tabs, { TabItem } from '@/components/ui/Tabs';
import EmptyState from '@/components/ui/EmptyState';
import { mockDqMetrics, DqMetricDto } from '@/mocks/mart';

const emptySubscribe = () => () => {};

export default function DqAuditPage() {
  const mounted = useSyncExternalStore(emptySubscribe, () => true, () => false);
  const [metrics, setMetrics] = useState<DqMetricDto[]>(mockDqMetrics);
  const [selectedStatus, setSelectedStatus] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [isAuditing, setIsAuditing] = useState(false);

  const totalRecordsSum = metrics.reduce((acc, m) => acc + m.totalRecords, 0);
  const totalErrorsSum = metrics.reduce((acc, m) => acc + m.errorRecords, 0);
  const avgDqScore = (metrics.reduce((acc, m) => acc + m.dqScore, 0) / metrics.length).toFixed(1);
  const avgTimeliness = (metrics.reduce((acc, m) => acc + m.timeliness, 0) / metrics.length).toFixed(1);

  const filterTabs: TabItem[] = [
    { id: 'ALL', label: '전체 테이블' },
    { id: 'EXCELLENT', label: '우수 (95%+)' },
    { id: 'WARNING', label: '주의 필요' },
  ];

  const filteredMetrics = metrics.filter(item => {
    const matchesStatus = 
      selectedStatus === 'ALL' || 
      (selectedStatus === 'EXCELLENT' && item.dqScore >= 95) ||
      (selectedStatus === 'WARNING' && item.dqScore < 95);

    const matchesSearch = 
      item.tableName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      item.domain.toLowerCase().includes(searchQuery.toLowerCase());

    return matchesStatus && matchesSearch;
  });

  const chartData = metrics.map(m => ({
    name: m.tableName.replace('DM_', ''),
    completeness: m.completeness,
    validity: m.validity,
    uniqueness: m.uniqueness,
    timeliness: m.timeliness,
  }));

  const handleRunAudit = () => {
    setIsAuditing(true);
    setTimeout(() => {
      setIsAuditing(false);
      alert('전체 마트 테이블 DQ 재감사 배치가 완료되었습니다. (오류 0건 검출)');
    }, 1200);
  };

  if (!mounted) return null;

  return (
    <div className="space-y-8 pb-12">
      {/* Page Header */}
      <PageHeader
        title="DQ 감사"
        description="데이터 품질 (Data Quality) 모니터링, 완전성/유효성/유일성/적시성 4대 지표 감사"
        breadcrumbs={[
          { label: '홈', href: '/' },
          { label: '데이터 마트' },
          { label: 'DQ 감사' },
        ]}
        icon={ShieldCheck}
        actions={
          <div className="flex items-center gap-3">
            <button
              onClick={handleRunAudit}
              disabled={isAuditing}
              className="px-4 py-2.5 rounded-xl text-xs font-black bg-white/5 text-slate-300 hover:text-white border border-white/10 hover:bg-white/10 transition-all flex items-center gap-2 disabled:opacity-50"
            >
              <RefreshCw size={14} className={isAuditing ? 'animate-spin text-blue-400' : 'text-blue-400'} />
              {isAuditing ? '감사 진행 중...' : 'DQ 재감사 실행'}
            </button>
            <button
              onClick={() => alert('DQ 품질 감사 종합보고서(PDF) 다운로드를 시작합니다.')}
              className="px-5 py-2.5 rounded-xl text-xs font-black bg-blue-600 hover:bg-blue-500 text-white shadow-lg shadow-blue-600/20 transition-all flex items-center gap-2"
            >
              <Download size={14} />
              감사 보고서 다운로드
            </button>
          </div>
        }
      />

      {/* Metric Stat Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-5">
        <div className="p-5 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5">
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">종합 DQ 품질 점수</span>
            <ShieldCheck className="text-emerald-400" size={18} />
          </div>
          <div className="text-3xl font-black text-white">{avgDqScore} <span className="text-sm font-normal text-slate-400">%</span></div>
          <div className="text-xs text-emerald-400 mt-1 flex items-center gap-1">
            <CheckCircle2 size={12} /> 목표 품질 레벨(95%) 상회
          </div>
        </div>

        <div className="p-5 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5">
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">총 레코드 / 검증 데이터</span>
            <Layers className="text-blue-400" size={18} />
          </div>
          <div className="text-2xl font-black text-white">{(totalRecordsSum / 10000).toFixed(1)} <span className="text-sm font-normal text-slate-400">만 건</span></div>
          <div className="text-xs text-slate-500 mt-1">
            5개 핵심 데이터마트 테이블 전체
          </div>
        </div>

        <div className="p-5 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5">
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">검출된 오류 레코드</span>
            <AlertTriangle className="text-amber-400" size={18} />
          </div>
          <div className="text-2xl font-black text-white">{totalErrorsSum.toLocaleString()} <span className="text-sm font-normal text-slate-400">건</span></div>
          <div className="text-xs text-amber-400 mt-1">
            오류율 약 {((totalErrorsSum / totalRecordsSum) * 100).toFixed(2)}%
          </div>
        </div>

        <div className="p-5 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5">
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">적시성 (Timeliness)</span>
            <Activity className="text-purple-400" size={18} />
          </div>
          <div className="text-2xl font-black text-white">{avgTimeliness} <span className="text-sm font-normal text-slate-400">%</span></div>
          <div className="text-xs text-slate-500 mt-1">
            일별 배치 마감 04:00 준수율
          </div>
        </div>
      </div>

      {/* Chart Section */}
      <div className="p-6 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-base font-black text-white flex items-center gap-2">
              <BarChart2 className="text-blue-400" size={18} />
              테이블별 DQ 4대 지표 비교 차트
            </h3>
            <p className="text-xs text-slate-400 mt-1">완전성(Completeness), 유효성(Validity), 유일성(Uniqueness), 적시성(Timeliness)</p>
          </div>
        </div>

        <div className="h-72 w-full pt-4">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={chartData} margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
              <XAxis dataKey="name" stroke="#64748b" fontSize={11} tickLine={false} />
              <YAxis domain={[80, 100]} stroke="#64748b" fontSize={11} tickLine={false} />
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
              <Bar dataKey="completeness" name="완전성 (%)" fill="#3b82f6" radius={[4, 4, 0, 0]} />
              <Bar dataKey="validity" name="유효성 (%)" fill="#10b981" radius={[4, 4, 0, 0]} />
              <Bar dataKey="uniqueness" name="유일성 (%)" fill="#8b5cf6" radius={[4, 4, 0, 0]} />
              <Bar dataKey="timeliness" name="적시성 (%)" fill="#f59e0b" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Table Section */}
      <div className="p-6 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-6">
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
          <Tabs tabs={filterTabs} activeTab={selectedStatus} onChange={setSelectedStatus} />

          <div className="relative w-full sm:w-72">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500" size={16} />
            <input
              type="text"
              placeholder="테이블명 / 도메인 검색..."
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 rounded-xl bg-white/5 border border-white/10 text-white text-xs placeholder:text-slate-600 focus:outline-none focus:border-blue-500 transition-all"
            />
          </div>
        </div>

        {filteredMetrics.length === 0 ? (
          <EmptyState
            icon={FileCheck2}
            title="조건에 해당하는 테이블이 없습니다"
            description="검색 조건 또는 상태 필터를 조정하여 감사 결과를 확인하세요."
          />
        ) : (
          <div className="overflow-x-auto rounded-xl border border-white/5">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="bg-white/5 border-b border-white/10 text-slate-400 font-black uppercase tracking-wider">
                  <th className="py-3.5 px-4">테이블 ID</th>
                  <th className="py-3.5 px-4">테이블명</th>
                  <th className="py-3.5 px-4">도메인</th>
                  <th className="py-3.5 px-4 text-right">총 레코드</th>
                  <th className="py-3.5 px-4 text-right">오류 건수</th>
                  <th className="py-3.5 px-4 text-center">완전성</th>
                  <th className="py-3.5 px-4 text-center">유효성</th>
                  <th className="py-3.5 px-4 text-center">유일성</th>
                  <th className="py-3.5 px-4 text-center">적시성</th>
                  <th className="py-3.5 px-4 text-center">종합 점수</th>
                  <th className="py-3.5 px-4">최종 감사시각</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5 text-slate-300">
                {filteredMetrics.map((item) => (
                  <tr key={item.tableId} className="hover:bg-white/[0.02] transition-colors">
                    <td className="py-3.5 px-4 font-mono text-slate-500">{item.tableId}</td>
                    <td className="py-3.5 px-4 font-mono font-bold text-blue-400">{item.tableName}</td>
                    <td className="py-3.5 px-4">
                      <span className="px-2 py-0.5 rounded bg-white/5 text-slate-300 font-medium">
                        {item.domain}
                      </span>
                    </td>
                    <td className="py-3.5 px-4 text-right font-mono">{item.totalRecords.toLocaleString()}</td>
                    <td className="py-3.5 px-4 text-right font-mono text-amber-400">{item.errorRecords.toLocaleString()}</td>
                    <td className="py-3.5 px-4 text-center font-mono text-slate-300">{item.completeness}%</td>
                    <td className="py-3.5 px-4 text-center font-mono text-slate-300">{item.validity}%</td>
                    <td className="py-3.5 px-4 text-center font-mono text-slate-300">{item.uniqueness}%</td>
                    <td className="py-3.5 px-4 text-center font-mono text-slate-300">{item.timeliness}%</td>
                    <td className="py-3.5 px-4 text-center">
                      <StatusBadge 
                        status={`${item.dqScore}%`} 
                        variant={item.dqScore >= 95 ? 'success' : 'warning'} 
                      />
                    </td>
                    <td className="py-3.5 px-4 font-mono text-slate-500 text-[11px]">{item.lastAudited}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

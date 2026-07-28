'use client';

import React, { useState, useSyncExternalStore } from 'react';
import { 
  PlayCircle, 
  RefreshCw, 
  CheckCircle2, 
  AlertTriangle, 
  XCircle, 
  Clock, 
  Calendar, 
  Filter, 
  Search,
  Zap,
  Sliders,
  Layers
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import Tabs, { TabItem } from '@/components/ui/Tabs';
import EmptyState from '@/components/ui/EmptyState';
import { 
  mockReconciliationRuns, 
  mockReconciliationRules, 
  ReconciliationRunDto 
} from '@/mocks/mart';

const emptySubscribe = () => () => {};

export default function ReconciliationRunPage() {
  const mounted = useSyncExternalStore(emptySubscribe, () => true, () => false);
  const [runs, setRuns] = useState<ReconciliationRunDto[]>(mockReconciliationRuns);
  const [selectedRuleId, setSelectedRuleId] = useState<string>(mockReconciliationRules[0].id);
  const [targetDate, setTargetDate] = useState<string>('2026-07-28');
  const [runMode, setRunMode] = useState<'FULL' | 'INCREMENTAL'>('INCREMENTAL');
  const [isExecuting, setIsExecuting] = useState(false);
  const [progress, setProgress] = useState(0);

  const [selectedStatus, setSelectedStatus] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  const filterTabs: TabItem[] = [
    { id: 'ALL', label: '전체 실행 이력' },
    { id: 'SUCCESS', label: '성공' },
    { id: 'WARNING', label: '차이 발생 (경고)' },
    { id: 'FAILED', label: '오류/실패' },
  ];

  const filteredRuns = runs.filter(run => {
    const matchesStatus = selectedStatus === 'ALL' || run.status === selectedStatus;
    const matchesSearch = 
      run.ruleName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      run.runId.toLowerCase().includes(searchQuery.toLowerCase()) ||
      run.executedBy.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesStatus && matchesSearch;
  });

  const handleStartRun = () => {
    const targetRule = mockReconciliationRules.find(r => r.id === selectedRuleId);
    if (!targetRule) return;

    setIsExecuting(true);
    setProgress(10);

    const interval = setInterval(() => {
      setProgress(prev => {
        if (prev >= 100) {
          clearInterval(interval);
          setIsExecuting(false);

          const newRun: ReconciliationRunDto = {
            runId: `RUN-20260728-0${runs.length + 1}`,
            ruleId: targetRule.id,
            ruleName: targetRule.ruleName,
            executedAt: '2026-07-28 09:45:00',
            status: 'SUCCESS',
            totalRows: 15400,
            matchedRows: 15400,
            diffRows: 0,
            durationSec: 2.8,
            executedBy: '강팀장 (수동배치)',
          };
          setRuns([newRun, ...runs]);
          alert(`[${targetRule.ruleName}] 대사 배치가 성공적으로 완수되었습니다.`);
          return 0;
        }
        return prev + 30;
      });
    }, 400);
  };

  const getStatusVariant = (status: string) => {
    switch (status) {
      case 'SUCCESS': return 'success';
      case 'WARNING': return 'warning';
      case 'FAILED': return 'error';
      case 'RUNNING': return 'info';
      default: return 'neutral';
    }
  };

  const getStatusLabel = (status: string) => {
    switch (status) {
      case 'SUCCESS': return '성공 (완료)';
      case 'WARNING': return '차이 검출';
      case 'FAILED': return '실패';
      case 'RUNNING': return '실행 중';
      default: return status;
    }
  };

  if (!mounted) return null;

  return (
    <div className="space-y-8 pb-12">
      {/* Page Header */}
      <PageHeader
        title="대사 실행"
        description="실시간 및 정기 대사 배치 수동 실행, 모니터링 및 결과 모니터링"
        breadcrumbs={[
          { label: '홈', href: '/' },
          { label: '대사 관리' },
          { label: '대사 실행' },
        ]}
        icon={PlayCircle}
      />

      {/* Overview Stat Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-5">
        <div className="p-5 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="text-xs font-bold text-slate-400 uppercase tracking-wider">오늘 실행된 배치</div>
          <div className="text-3xl font-black text-white">{runs.length} <span className="text-sm font-normal text-slate-400">건</span></div>
          <div className="text-xs text-slate-500">배치 실행 주서 일별 5회 수동/자동</div>
        </div>

        <div className="p-5 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="text-xs font-bold text-slate-400 uppercase tracking-wider">대사 성공률</div>
          <div className="text-3xl font-black text-white">
            {((runs.filter(r => r.status === 'SUCCESS').length / runs.length) * 100).toFixed(0)} <span className="text-sm font-normal text-slate-400">%</span>
          </div>
          <div className="text-xs text-emerald-400 flex items-center gap-1">
            <CheckCircle2 size={12} /> 정상 일치 완료
          </div>
        </div>

        <div className="p-5 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="text-xs font-bold text-slate-400 uppercase tracking-wider">총 매칭 레코드</div>
          <div className="text-2xl font-black text-white">
            {(runs.reduce((sum, r) => sum + r.matchedRows, 0) / 10000).toFixed(1)} <span className="text-sm font-normal text-slate-400">만 건</span>
          </div>
          <div className="text-xs text-slate-500">검증된 레코드 수</div>
        </div>

        <div className="p-5 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="text-xs font-bold text-slate-400 uppercase tracking-wider">검출된 차이 레코드</div>
          <div className="text-2xl font-black text-amber-400 font-mono">
            {runs.reduce((sum, r) => sum + r.diffRows, 0)} <span className="text-sm font-normal text-slate-400">건</span>
          </div>
          <div className="text-xs text-amber-400">차이 해소 화면으로 이동하여 처리</div>
        </div>
      </div>

      {/* Execution Trigger Box */}
      <div className="p-6 rounded-2xl bg-gradient-to-r from-slate-900/80 to-blue-950/40 backdrop-blur-md border border-blue-500/20 space-y-6">
        <div className="flex items-center justify-between border-b border-white/5 pb-4">
          <div>
            <h3 className="text-base font-black text-white flex items-center gap-2">
              <Zap className="text-blue-400" size={18} />
              수동 대사 배치 트리거 (Reconciliation Execution Trigger)
            </h3>
            <p className="text-xs text-slate-400 mt-1">대사 대상 규칙과 일자를 지정하여 원장 vs 마트 재검증을 즉시 실행합니다.</p>
          </div>
          <StatusBadge status={isExecuting ? '대사 실행 중...' : '대기 상태'} variant={isExecuting ? 'info' : 'success'} />
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-5 text-xs">
          <div>
            <label className="block text-slate-400 font-bold mb-1.5">대상 대사 규칙 선택</label>
            <select
              value={selectedRuleId}
              onChange={e => setSelectedRuleId(e.target.value)}
              className="w-full bg-slate-950 border border-white/10 rounded-xl px-3.5 py-2.5 text-white font-medium focus:outline-none focus:border-blue-500"
            >
              {mockReconciliationRules.map(r => (
                <option key={r.id} value={r.id}>
                  [{r.id}] {r.ruleName}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-slate-400 font-bold mb-1.5">대사 기준 일자</label>
            <input
              type="date"
              value={targetDate}
              onChange={e => setTargetDate(e.target.value)}
              className="w-full bg-slate-950 border border-white/10 rounded-xl px-3.5 py-2 text-white font-mono focus:outline-none focus:border-blue-500"
            />
          </div>

          <div>
            <label className="block text-slate-400 font-bold mb-1.5">실행 모드 (Execution Mode)</label>
            <div className="flex items-center gap-2 p-1 bg-slate-950 rounded-xl border border-white/10">
              <button
                type="button"
                onClick={() => setRunMode('INCREMENTAL')}
                className={`flex-1 py-1.5 rounded-lg text-xs font-bold transition-all ${
                  runMode === 'INCREMENTAL' ? 'bg-blue-600 text-white' : 'text-slate-400 hover:text-white'
                }`}
              >
                증분 대사 (Incremental)
              </button>
              <button
                type="button"
                onClick={() => setRunMode('FULL')}
                className={`flex-1 py-1.5 rounded-lg text-xs font-bold transition-all ${
                  runMode === 'FULL' ? 'bg-purple-600 text-white' : 'text-slate-400 hover:text-white'
                }`}
              >
                전체 대사 (Full)
              </button>
            </div>
          </div>
        </div>

        {/* Progress bar when executing */}
        {isExecuting && (
          <div className="space-y-2 pt-2">
            <div className="flex items-center justify-between text-xs text-blue-300 font-bold">
              <span>대사 데이터 추출 및 해시 매칭 진행 중...</span>
              <span>{progress}%</span>
            </div>
            <div className="w-full h-2 rounded-full bg-slate-800 overflow-hidden">
              <div 
                className="h-full bg-blue-500 transition-all duration-300 rounded-full"
                style={{ width: `${progress}%` }}
              />
            </div>
          </div>
        )}

        <div className="flex items-center justify-end pt-2">
          <button
            onClick={handleStartRun}
            disabled={isExecuting}
            className="px-6 py-3 rounded-xl text-xs font-black bg-blue-600 hover:bg-blue-500 text-white shadow-xl shadow-blue-600/20 transition-all flex items-center gap-2 disabled:opacity-50"
          >
            <PlayCircle size={16} />
            {isExecuting ? '대사 실행 중...' : '대사 배치 실행 시작'}
          </button>
        </div>
      </div>

      {/* Execution History Section */}
      <div className="p-6 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-6">
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
          <Tabs tabs={filterTabs} activeTab={selectedStatus} onChange={setSelectedStatus} />

          <div className="relative w-full sm:w-72">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500" size={16} />
            <input
              type="text"
              placeholder="규칙명 / 실행자 검색..."
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 rounded-xl bg-white/5 border border-white/10 text-white text-xs placeholder:text-slate-600 focus:outline-none focus:border-blue-500 transition-all"
            />
          </div>
        </div>

        {filteredRuns.length === 0 ? (
          <EmptyState
            icon={Clock}
            title="실행 이력이 존재하지 않습니다"
            description="선택한 상태 필터에 부합하는 대사 실행 이력이 없습니다."
          />
        ) : (
          <div className="overflow-x-auto rounded-xl border border-white/5">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="bg-white/5 border-b border-white/10 text-slate-400 font-black uppercase tracking-wider">
                  <th className="py-3.5 px-4">실행 ID</th>
                  <th className="py-3.5 px-4">대사 규칙명</th>
                  <th className="py-3.5 px-4">실행 일시</th>
                  <th className="py-3.5 px-4 text-center">상태</th>
                  <th className="py-3.5 px-4 text-right">총 대상 (Rows)</th>
                  <th className="py-3.5 px-4 text-right">일치 건수</th>
                  <th className="py-3.5 px-4 text-right">차이 건수</th>
                  <th className="py-3.5 px-4 text-right">소요시간</th>
                  <th className="py-3.5 px-4">실행 주체</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5 text-slate-300">
                {filteredRuns.map((run) => (
                  <tr key={run.runId} className="hover:bg-white/[0.02] transition-colors">
                    <td className="py-3.5 px-4 font-mono font-bold text-blue-400">{run.runId}</td>
                    <td className="py-3.5 px-4 font-bold text-white max-w-xs">{run.ruleName}</td>
                    <td className="py-3.5 px-4 font-mono text-slate-400">{run.executedAt}</td>
                    <td className="py-3.5 px-4 text-center">
                      <StatusBadge 
                        status={getStatusLabel(run.status)} 
                        variant={getStatusVariant(run.status)} 
                      />
                    </td>
                    <td className="py-3.5 px-4 text-right font-mono">{run.totalRows.toLocaleString()}</td>
                    <td className="py-3.5 px-4 text-right font-mono text-emerald-400 font-bold">{run.matchedRows.toLocaleString()}</td>
                    <td className={`py-3.5 px-4 text-right font-mono font-bold ${
                      run.diffRows > 0 ? 'text-amber-400' : 'text-slate-500'
                    }`}>
                      {run.diffRows}
                    </td>
                    <td className="py-3.5 px-4 text-right font-mono text-slate-400">{run.durationSec}s</td>
                    <td className="py-3.5 px-4 text-slate-300">{run.executedBy}</td>
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

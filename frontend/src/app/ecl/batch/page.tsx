'use client';

import React, { useState, useSyncExternalStore } from 'react';
import {
  Activity,
  Play,
  RotateCcw,
  Terminal,
  CheckCircle2,
  AlertTriangle,
  Clock,
  Database,
  Cpu,
  FileText,
  Pause,
  Download,
  Filter,
  RefreshCw,
  Search,
  CheckCircle,
  XCircle,
  Zap,
  Server
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import { mockBatches, mockBatchLogs, EclBatchDto, EclBatchLogDto } from '@/mocks/ecl';

const emptySubscribe = () => () => {};

export default function EclBatchPage() {
  const mounted = useSyncExternalStore(emptySubscribe, () => true, () => false);
  const [batches, setBatches] = useState<EclBatchDto[]>(mockBatches);
  const [logs, setLogs] = useState<EclBatchLogDto[]>(mockBatchLogs);
  const [logFilter, setLogFilter] = useState<'ALL' | 'INFO' | 'WARN' | 'ERROR'>('ALL');
  const [logSearch, setLogSearch] = useState('');
  const [isExecuting, setIsExecuting] = useState(false);
  const [notification, setNotification] = useState<string | null>(null);

  const currentActiveBatch = batches.find(b => b.status === 'RUNNING') || batches[0];

  const handleRunBatch = () => {
    setIsExecuting(true);
    setNotification('ECL 산출 배치가 새로 트리거되었습니다.');
    setTimeout(() => {
      setIsExecuting(false);
      setNotification('배치 프로세스가 성공적으로 구동 중입니다.');
      setTimeout(() => setNotification(null), 4000);
    }, 1500);
  };

  const handleRetryFailed = () => {
    setNotification('실패된 배치를 다시 트리거하는 중입니다...');
    setTimeout(() => {
      setBatches(prev => prev.map(b => b.status === 'FAILED' ? { ...b, status: 'RUNNING', progressPercent: 50, currentStep: '재시도 수행 중...' } : b));
      setNotification('배치 재시도가 시작되었습니다.');
      setTimeout(() => setNotification(null), 3000);
    }, 1000);
  };

  const filteredLogs = logs.filter(log => {
    const matchesFilter = logFilter === 'ALL' || log.level === logFilter;
    const matchesSearch = log.message.toLowerCase().includes(logSearch.toLowerCase()) || log.step.toLowerCase().includes(logSearch.toLowerCase());
    return matchesFilter && matchesSearch;
  });

  const getStatusVariant = (status: EclBatchDto['status']) => {
    switch (status) {
      case 'COMPLETED': return 'success';
      case 'RUNNING': return 'info';
      case 'FAILED': return 'error';
      case 'WAITING': return 'warning';
      default: return 'neutral';
    }
  };

  const getStatusLabel = (status: EclBatchDto['status']) => {
    switch (status) {
      case 'COMPLETED': return '완료';
      case 'RUNNING': return '실행 중';
      case 'FAILED': return '오류 발생';
      case 'WAITING': return '대기 중';
      default: return status;
    }
  };

  const pipelineSteps = [
    { name: '1. 원장 추출', status: 'COMPLETED', desc: '145,000건 추출 완료' },
    { name: '2. Stage 분류', status: 'COMPLETED', desc: 'Stage 1/2/3 분류' },
    { name: '3. 파라미터 매핑', status: 'COMPLETED', desc: 'PD/LGD/CCF 매핑' },
    { name: '4. ECL 연산 엔진', status: 'RUNNING', desc: 'ECL 산출 진행 중' },
    { name: '5. GL 전표 반영', status: 'WAITING', desc: '연산 완료 후 반영' },
  ];

  if (!mounted) {
    return <div className="p-8 text-slate-400">Loading ECL Batch Control Tower...</div>;
  }

  return (
    <div className="space-y-8 pb-16">
      {/* Page Header */}
      <PageHeader
        title="ECL 배치 관제탑"
        description="IFRS9 대손충당금 산출 배치 프로세스 실시간 관제 및 대용량 연산 모니터링"
        breadcrumbs={[
          { label: 'ECL' },
          { label: '배치 관제탑' }
        ]}
        icon={Activity}
        actions={
          <div className="flex items-center gap-3">
            <button
              onClick={handleRetryFailed}
              className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 border border-white/10 font-bold text-sm transition-all"
            >
              <RotateCcw size={16} />
              실패 건 재시도
            </button>
            <button
              onClick={handleRunBatch}
              disabled={isExecuting}
              className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-bold text-sm shadow-lg shadow-blue-600/30 transition-all disabled:opacity-50"
            >
              <Play size={16} className={isExecuting ? 'animate-spin' : ''} />
              {isExecuting ? '배치 실행 중...' : 'ECL 배치 수동 실행'}
            </button>
          </div>
        }
      />

      {/* Toast Notification */}
      {notification && (
        <div className="p-4 rounded-2xl bg-blue-500/10 border border-blue-500/30 text-blue-400 font-medium flex items-center justify-between animate-fade-in">
          <div className="flex items-center gap-3">
            <Zap size={18} className="animate-bounce" />
            <span>{notification}</span>
          </div>
          <button onClick={() => setNotification(null)} className="text-slate-400 hover:text-white text-xs">닫기</button>
        </div>
      )}

      {/* KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <div className="bg-slate-900/50 backdrop-blur-md p-6 rounded-2xl border border-white/5 space-y-2">
          <div className="flex items-center justify-between text-slate-400 text-xs font-bold uppercase tracking-wider">
            <span>배치 구동 상태</span>
            <Activity size={18} className="text-blue-400" />
          </div>
          <div className="flex items-center justify-between pt-1">
            <span className="text-2xl font-black text-white">
              {currentActiveBatch.status === 'RUNNING' ? '연산 처리 중' : '대기 / 완료'}
            </span>
            <StatusBadge
              status={getStatusLabel(currentActiveBatch.status)}
              variant={getStatusVariant(currentActiveBatch.status)}
            />
          </div>
          <p className="text-xs text-slate-400 pt-1">기준월: {currentActiveBatch.periodYearMonth}</p>
        </div>

        <div className="bg-slate-900/50 backdrop-blur-md p-6 rounded-2xl border border-white/5 space-y-2">
          <div className="flex items-center justify-between text-slate-400 text-xs font-bold uppercase tracking-wider">
            <span>처리 레코드 수</span>
            <Database size={18} className="text-emerald-400" />
          </div>
          <div className="text-2xl font-black text-white font-mono">
            {currentActiveBatch.processedRecords.toLocaleString()} <span className="text-sm font-normal text-slate-400">/ {currentActiveBatch.totalRecords.toLocaleString()}</span>
          </div>
          <div className="w-full bg-slate-800 rounded-full h-1.5 overflow-hidden">
            <div
              className="bg-emerald-500 h-1.5 rounded-full transition-all duration-500"
              style={{ width: `${currentActiveBatch.progressPercent}%` }}
            />
          </div>
        </div>

        <div className="bg-slate-900/50 backdrop-blur-md p-6 rounded-2xl border border-white/5 space-y-2">
          <div className="flex items-center justify-between text-slate-400 text-xs font-bold uppercase tracking-wider">
            <span>평균 연산 속도</span>
            <Cpu size={18} className="text-purple-400" />
          </div>
          <div className="text-2xl font-black text-white font-mono">
            1,240 <span className="text-xs font-normal text-slate-400">rec/sec</span>
          </div>
          <p className="text-xs text-emerald-400 flex items-center gap-1 pt-1">
            <Zap size={12} /> 분산 병렬 엔진 가동 중
          </p>
        </div>

        <div className="bg-slate-900/50 backdrop-blur-md p-6 rounded-2xl border border-white/5 space-y-2">
          <div className="flex items-center justify-between text-slate-400 text-xs font-bold uppercase tracking-wider">
            <span>검증 및 오류 건수</span>
            <AlertTriangle size={18} className="text-amber-400" />
          </div>
          <div className="text-2xl font-black text-white font-mono">
            {currentActiveBatch.errorRecords} <span className="text-xs font-normal text-slate-400">건</span>
          </div>
          <p className="text-xs text-slate-400 pt-1">데이터 정합성 이상 무</p>
        </div>
      </div>

      {/* Active Batch Progress Panel */}
      <div className="bg-slate-900/50 backdrop-blur-md rounded-2xl border border-white/5 p-6 space-y-6">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-4 border-b border-white/5">
          <div className="space-y-1">
            <div className="flex items-center gap-3">
              <span className="text-xs font-black px-2.5 py-1 rounded-md bg-blue-500/20 text-blue-400 uppercase tracking-widest">
                ACTIVE BATCH
              </span>
              <h3 className="text-xl font-bold text-white">{currentActiveBatch.batchName}</h3>
            </div>
            <p className="text-sm text-slate-400">{currentActiveBatch.currentStep}</p>
          </div>

          <div className="flex items-center gap-4 text-xs text-slate-400 font-mono">
            <div className="flex items-center gap-1.5">
              <Clock size={14} className="text-blue-400" />
              <span>시작: {currentActiveBatch.startTime}</span>
            </div>
            <div className="flex items-center gap-1.5">
              <Server size={14} className="text-purple-400" />
              <span>실행자: {currentActiveBatch.executor}</span>
            </div>
          </div>
        </div>

        {/* Progress Bar & Details */}
        <div className="space-y-3">
          <div className="flex items-center justify-between text-sm font-bold">
            <span className="text-slate-300">배치 전체 진행률</span>
            <span className="text-blue-400 font-mono text-base">{currentActiveBatch.progressPercent}%</span>
          </div>
          <div className="w-full bg-slate-800 rounded-full h-3 p-0.5 overflow-hidden border border-white/5">
            <div
              className="bg-gradient-to-r from-blue-500 via-indigo-500 to-purple-500 h-full rounded-full transition-all duration-700 shadow-lg shadow-blue-500/50"
              style={{ width: `${currentActiveBatch.progressPercent}%` }}
            />
          </div>
        </div>

        {/* Pipeline Step Cards */}
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-5 gap-4 pt-2">
          {pipelineSteps.map((step, idx) => (
            <div
              key={idx}
              className={`p-4 rounded-xl border transition-all ${
                step.status === 'COMPLETED'
                  ? 'bg-emerald-500/5 border-emerald-500/20 text-emerald-400'
                  : step.status === 'RUNNING'
                  ? 'bg-blue-500/10 border-blue-500/30 text-blue-400 shadow-lg shadow-blue-500/10 animate-pulse'
                  : 'bg-slate-900/30 border-white/5 text-slate-500'
              }`}
            >
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs font-bold">{step.name}</span>
                {step.status === 'COMPLETED' && <CheckCircle size={14} />}
                {step.status === 'RUNNING' && <Activity size={14} className="animate-spin" />}
                {step.status === 'WAITING' && <Clock size={14} />}
              </div>
              <p className="text-[11px] text-slate-400">{step.desc}</p>
            </div>
          ))}
        </div>
      </div>

      {/* Real-time Log Console & History Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Terminal Log Console (2 cols) */}
        <div className="lg:col-span-2 bg-slate-950/80 backdrop-blur-md rounded-2xl border border-white/10 p-6 space-y-4 font-mono">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-white/10">
            <div className="flex items-center gap-2">
              <Terminal size={18} className="text-emerald-400" />
              <h3 className="font-bold text-white text-base">실시간 산출 로그 (Live Console)</h3>
            </div>

            <div className="flex items-center gap-3">
              {/* Filter tabs */}
              <div className="flex items-center bg-slate-900 p-1 rounded-lg border border-white/5 text-xs">
                {(['ALL', 'INFO', 'WARN', 'ERROR'] as const).map(f => (
                  <button
                    key={f}
                    onClick={() => setLogFilter(f)}
                    className={`px-2.5 py-1 rounded-md transition-all ${
                      logFilter === f ? 'bg-white/10 text-white font-bold' : 'text-slate-400 hover:text-slate-200'
                    }`}
                  >
                    {f}
                  </button>
                ))}
              </div>

              {/* Search log */}
              <div className="relative">
                <Search size={14} className="absolute left-2.5 top-2.5 text-slate-500" />
                <input
                  type="text"
                  placeholder="로그 검색..."
                  value={logSearch}
                  onChange={(e) => setLogSearch(e.target.value)}
                  className="pl-8 pr-3 py-1 bg-slate-900 border border-white/10 rounded-lg text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500"
                />
              </div>
            </div>
          </div>

          {/* Console Viewport */}
          <div className="h-64 overflow-y-auto space-y-2 text-xs pr-2 scrollbar-thin scrollbar-thumb-slate-800">
            {filteredLogs.length === 0 ? (
              <div className="text-center py-12 text-slate-500">검색 조건에 해당되는 로그가 없습니다.</div>
            ) : (
              filteredLogs.map(log => (
                <div key={log.id} className="flex items-start gap-3 py-1 hover:bg-white/5 px-2 rounded">
                  <span className="text-slate-500 select-none">[{log.timestamp}]</span>
                  <span className={`px-1.5 py-0.5 rounded text-[10px] font-bold uppercase ${
                    log.level === 'INFO' ? 'bg-blue-500/20 text-blue-400' :
                    log.level === 'WARN' ? 'bg-amber-500/20 text-amber-400' :
                    log.level === 'ERROR' ? 'bg-rose-500/20 text-rose-400' : 'bg-emerald-500/20 text-emerald-400'
                  }`}>
                    {log.level}
                  </span>
                  <span className="text-slate-400">[{log.step}]</span>
                  <span className="text-slate-200">{log.message}</span>
                </div>
              ))
            )}
          </div>
        </div>

        {/* History Table (1 col) */}
        <div className="bg-slate-900/50 backdrop-blur-md rounded-2xl border border-white/5 p-6 space-y-4">
          <div className="flex items-center justify-between pb-4 border-b border-white/5">
            <h3 className="font-bold text-white text-base flex items-center gap-2">
              <FileText size={18} className="text-blue-400" />
              배치 이력 (Batch History)
            </h3>
            <button className="text-xs text-blue-400 hover:underline flex items-center gap-1">
              전체보기
            </button>
          </div>

          <div className="space-y-3">
            {batches.map(batch => (
              <div key={batch.id} className="p-3.5 rounded-xl bg-slate-900/80 border border-white/5 space-y-2 hover:border-white/10 transition-all">
                <div className="flex items-center justify-between">
                  <span className="font-bold text-sm text-white">{batch.periodYearMonth} 배치</span>
                  <StatusBadge
                    status={getStatusLabel(batch.status)}
                    variant={getStatusVariant(batch.status)}
                  />
                </div>
                <div className="text-xs text-slate-400 flex items-center justify-between font-mono">
                  <span>건수: {batch.totalRecords.toLocaleString()}건</span>
                  <span>오류: {batch.errorRecords}건</span>
                </div>
                <div className="text-[11px] text-slate-500 flex items-center justify-between">
                  <span>시작: {batch.startTime.substring(5, 16)}</span>
                  <span>{batch.executor}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

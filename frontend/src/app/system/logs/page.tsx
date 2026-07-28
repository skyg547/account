"use client";

import React, { useState } from 'react';
import { 
  Terminal, 
  AlertTriangle, 
  Search, 
  Play, 
  Pause, 
  Trash2, 
  Download, 
  RefreshCw,
  Server,
  Activity,
  Zap
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';

interface LogEntry {
  id: string;
  timestamp: string;
  level: 'INFO' | 'WARN' | 'ERROR' | 'DEBUG';
  service: string;
  message: string;
}

const mockLogs: LogEntry[] = [
  {
    id: 'L-1001',
    timestamp: '2026-07-28 09:14:02.129',
    level: 'ERROR',
    service: 'batch-closing-job',
    message: 'Exception in thread "main" java.lang.NullPointerException: Account code \'11100\' not found in General Ledger balance map during monthly closing step 4.'
  },
  {
    id: 'L-1002',
    timestamp: '2026-07-28 09:12:45.004',
    level: 'WARN',
    service: 'ifrs9-ecl-engine',
    message: 'ECL Model LGD parameter convergence threshold exceeded standard margin (0.005). Retrying iterations (2/5)...'
  },
  {
    id: 'L-1003',
    timestamp: '2026-07-28 09:10:11.883',
    level: 'INFO',
    service: 'auth-service',
    message: 'User USR-001 (김회계) successfully authenticated via SAML 2.0 SSO from IP 192.168.1.10.'
  },
  {
    id: 'L-1004',
    timestamp: '2026-07-28 09:08:50.771',
    level: 'ERROR',
    service: 'journal-post-queue',
    message: 'DBDeadlockDetectedException: Transaction (Process ID 54) was deadlocked on lock resources with another process and has been chosen as deadlock victim.'
  },
  {
    id: 'L-1005',
    timestamp: '2026-07-28 09:05:32.410',
    level: 'INFO',
    service: 'asset-depreciation-service',
    message: 'Straight-line depreciation calculation completed for 1,420 asset items. Total amount: ₩45,200,000.'
  },
  {
    id: 'L-1006',
    timestamp: '2026-07-28 08:59:01.012',
    level: 'DEBUG',
    service: 'redis-cache',
    message: 'Flushing cache key prefix: \'cache:master:accounts:*\'. Invalidation triggered by user USR-003.'
  },
  {
    id: 'L-1007',
    timestamp: '2026-07-28 08:45:22.990',
    level: 'WARN',
    service: 'fx-rate-sync',
    message: 'Bank of Korea API rate limit warning: 85% capacity reached for today.'
  }
];

export default function SystemLogsPage() {
  const [logs, setLogs] = useState<LogEntry[]>(mockLogs);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedLevel, setSelectedLevel] = useState<string>('ALL');
  const [isStreaming, setIsStreaming] = useState(true);

  const filteredLogs = logs.filter(log => {
    const matchesSearch = 
      log.message.toLowerCase().includes(searchTerm.toLowerCase()) ||
      log.service.toLowerCase().includes(searchTerm.toLowerCase()) ||
      log.id.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesLevel = selectedLevel === 'ALL' || log.level === selectedLevel;
    return matchesSearch && matchesLevel;
  });

  const getLevelBadgeClass = (level: LogEntry['level']) => {
    switch (level) {
      case 'ERROR': return 'text-rose-400 bg-rose-500/20 border-rose-500/30';
      case 'WARN': return 'text-amber-400 bg-amber-500/20 border-amber-500/30';
      case 'INFO': return 'text-emerald-400 bg-emerald-500/20 border-emerald-500/30';
      case 'DEBUG': return 'text-blue-400 bg-blue-500/20 border-blue-500/30';
    }
  };

  const handleClear = () => {
    setLogs([]);
  };

  const errorCount = logs.filter(l => l.level === 'ERROR').length;
  const warnCount = logs.filter(l => l.level === 'WARN').length;

  return (
    <div className="space-y-8 p-2">
      <PageHeader
        title="시스템 로그"
        description="서버 에러 및 배치 실행 예외 로그"
        breadcrumbs={[
          { label: 'System', href: '/system/logs' },
          { label: '시스템 로그' }
        ]}
        icon={Terminal}
        actions={
          <div className="flex items-center gap-3">
            <button
              onClick={() => setIsStreaming(!isStreaming)}
              className={`px-4 py-2.5 rounded-2xl text-sm font-bold transition-all flex items-center gap-2 border ${
                isStreaming
                  ? 'bg-amber-500/10 text-amber-400 border-amber-500/20 hover:bg-amber-500/20'
                  : 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20 hover:bg-emerald-500/20'
              }`}
            >
              {isStreaming ? <Pause size={16} /> : <Play size={16} />}
              {isStreaming ? '스트리밍 일시정지' : '실시간 스트리밍 시작'}
            </button>
            <button
              onClick={() => setLogs(mockLogs)}
              className="px-4 py-2.5 bg-white/5 hover:bg-white/10 border border-white/10 rounded-2xl text-slate-300 text-sm font-bold transition-all flex items-center gap-2"
            >
              <RefreshCw size={16} /> 새로고침
            </button>
          </div>
        }
      />

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-black text-slate-400 uppercase tracking-wider">누적 로그 수</p>
              <h3 className="text-3xl font-black text-white mt-2 tracking-tight">{logs.length}건</h3>
            </div>
            <div className="w-12 h-12 rounded-2xl bg-blue-500/10 border border-blue-500/20 flex items-center justify-center text-blue-400">
              <Server size={22} />
            </div>
          </div>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-black text-slate-400 uppercase tracking-wider">심각한 에러 (ERROR)</p>
              <h3 className="text-3xl font-black text-rose-400 mt-2 tracking-tight">{errorCount}건</h3>
            </div>
            <div className="w-12 h-12 rounded-2xl bg-rose-500/10 border border-rose-500/20 flex items-center justify-center text-rose-400">
              <AlertTriangle size={22} />
            </div>
          </div>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-black text-slate-400 uppercase tracking-wider">경고 (WARN)</p>
              <h3 className="text-3xl font-black text-amber-400 mt-2 tracking-tight">{warnCount}건</h3>
            </div>
            <div className="w-12 h-12 rounded-2xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-center text-amber-400">
              <Zap size={22} />
            </div>
          </div>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-black text-slate-400 uppercase tracking-wider">시스템 헬스 상태</p>
              <h3 className="text-xl font-black text-emerald-400 mt-2 tracking-tight">Normal (99.8%)</h3>
            </div>
            <div className="w-12 h-12 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
              <Activity size={22} />
            </div>
          </div>
        </div>
      </div>

      {/* Terminal View Container */}
      <div className="rounded-2xl bg-slate-950 border border-white/10 overflow-hidden shadow-2xl space-y-0">
        {/* Terminal Top Control Bar */}
        <div className="flex items-center justify-between px-6 py-4 bg-slate-900/80 border-b border-white/10">
          <div className="flex items-center gap-3">
            {/* macOS Style Traffic Lights */}
            <div className="flex items-center gap-2">
              <span className="w-3 h-3 rounded-full bg-rose-500/80 inline-block" />
              <span className="w-3 h-3 rounded-full bg-amber-500/80 inline-block" />
              <span className="w-3 h-3 rounded-full bg-emerald-500/80 inline-block" />
            </div>
            <span className="text-xs font-mono font-bold text-slate-400 ml-2">
              bash - system-events.log (Live tail)
            </span>
            {isStreaming && (
              <span className="flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-[11px] font-mono animate-pulse">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-400" /> LIVE
              </span>
            )}
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={handleClear}
              className="p-2 rounded-xl text-slate-400 hover:text-rose-400 hover:bg-white/5 transition-colors text-xs font-bold flex items-center gap-1"
            >
              <Trash2 size={14} /> Clear
            </button>
            <button className="p-2 rounded-xl text-slate-400 hover:text-blue-400 hover:bg-white/5 transition-colors text-xs font-bold flex items-center gap-1">
              <Download size={14} /> Export
            </button>
          </div>
        </div>

        {/* Filter Bar */}
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4 p-4 bg-slate-900/40 border-b border-white/5">
          <div className="relative w-full sm:w-80">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500" size={16} />
            <input
              type="text"
              placeholder="로그 메시지 또는 서비스 검색..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full bg-slate-900 border border-white/10 rounded-xl py-2 pl-10 pr-4 text-white text-xs font-mono placeholder-slate-500 focus:outline-none focus:border-blue-500/50"
            />
          </div>

          <div className="flex items-center gap-2 w-full sm:w-auto">
            {['ALL', 'ERROR', 'WARN', 'INFO', 'DEBUG'].map((lvl) => (
              <button
                key={lvl}
                onClick={() => setSelectedLevel(lvl)}
                className={`px-3 py-1 rounded-lg font-mono text-xs transition-all ${
                  selectedLevel === lvl
                    ? 'bg-blue-600 text-white font-bold'
                    : 'bg-white/5 text-slate-400 hover:bg-white/10 hover:text-white'
                }`}
              >
                {lvl}
              </button>
            ))}
          </div>
        </div>

        {/* Console Terminal Body */}
        <div className="p-6 font-mono text-xs space-y-3 min-h-[400px] max-h-[600px] overflow-y-auto bg-slate-950">
          {filteredLogs.map((log) => (
            <div
              key={log.id}
              className="flex flex-col sm:flex-row items-start gap-3 p-2.5 rounded-xl hover:bg-white/[0.03] transition-colors border border-transparent hover:border-white/5"
            >
              <div className="flex items-center gap-2 shrink-0">
                <span className="text-slate-500 text-[11px]">{log.timestamp}</span>
                <span className={`px-2 py-0.5 rounded text-[10px] font-bold border ${getLevelBadgeClass(log.level)}`}>
                  {log.level}
                </span>
                <span className="px-2 py-0.5 rounded text-[10px] bg-slate-800 text-blue-400 border border-white/5">
                  [{log.service}]
                </span>
              </div>
              <div className="text-slate-300 break-all leading-relaxed pl-1 sm:pl-0">
                {log.message}
              </div>
            </div>
          ))}

          {filteredLogs.length === 0 && (
            <div className="py-20 text-center text-slate-600 font-mono">
              -- 표시할 시스템 로그 데이터가 없습니다 --
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

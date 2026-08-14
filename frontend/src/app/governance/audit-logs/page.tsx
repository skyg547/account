"use client";

import React, { useState } from 'react';
import { 
  FileSearch, 
  ShieldCheck, 
  AlertOctagon, 
  User, 
  Clock, 
  Globe, 
  Search, 
  Filter, 
  Download, 
  RefreshCw,
  Activity,
  CheckCircle,
  Database
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import { mockAuditLogs, AuditLogDto } from '@/mocks/system';

const actionDescriptions: Record<string, { label: string; category: string }> = {
  APPROVE_JOURNAL: { label: '전표 승인 (Approve Journal)', category: 'FINANCE' },
  UPDATE_ECL_PARAM: { label: 'ECL 파라미터 변경', category: 'RISK' },
  LOGIN_ATTEMPT: { label: '시스템 로그인 시도', category: 'SECURITY' },
  UPDATE_ROLE: { label: '사용자 권한 변경', category: 'SECURITY' },
  DELETE_RECORD: { label: '마스터 데이터 삭제', category: 'SYSTEM' },
};

export default function AuditLogsPage() {
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [logList, setLogList] = useState<AuditLogDto[]>(mockAuditLogs);

  const filteredLogs = logList.filter(log => {
    const matchesSearch = 
      log.actorId.toLowerCase().includes(searchTerm.toLowerCase()) ||
      log.action.toLowerCase().includes(searchTerm.toLowerCase()) ||
      log.resource.toLowerCase().includes(searchTerm.toLowerCase()) ||
      log.logId.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesStatus = statusFilter === 'ALL' || log.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  const successCount = logList.filter(l => l.status === 'SUCCESS').length;
  const failureCount = logList.filter(l => l.status === 'FAILURE').length;

  return (
    <div className="space-y-8 p-2">
      <PageHeader
        title="감사 로그 탐색기"
        description="재무 데이터 및 권한 변경 추적"
        breadcrumbs={[
          { label: 'Governance', href: '/governance/audit-logs' },
          { label: '감사 로그 탐색기' }
        ]}
        icon={FileSearch}
        actions={
          <div className="flex items-center gap-3">
            <button
              onClick={() => setLogList(mockAuditLogs)}
              className="px-4 py-2.5 bg-white/5 hover:bg-white/10 border border-white/10 rounded-2xl text-slate-300 text-sm font-bold transition-all flex items-center gap-2"
            >
              <RefreshCw size={16} /> 새로고침
            </button>
            <button className="px-5 py-2.5 bg-blue-600 hover:bg-blue-500 text-white rounded-2xl text-sm font-bold shadow-lg shadow-blue-600/20 transition-all flex items-center gap-2">
              <Download size={16} /> 감사 보고서 내보내기
            </button>
          </div>
        }
      />

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-black text-slate-400 uppercase tracking-wider">감사 이력 수</p>
              <h3 className="text-3xl font-black text-white mt-2 tracking-tight">{logList.length}건</h3>
            </div>
            <div className="w-12 h-12 rounded-2xl bg-blue-500/10 border border-blue-500/20 flex items-center justify-center text-blue-400">
              <Activity size={22} />
            </div>
          </div>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-black text-slate-400 uppercase tracking-wider">정상 수행 이력</p>
              <h3 className="text-3xl font-black text-emerald-400 mt-2 tracking-tight">{successCount}건</h3>
            </div>
            <div className="w-12 h-12 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
              <CheckCircle size={22} />
            </div>
          </div>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-black text-slate-400 uppercase tracking-wider">이상/실패 이력</p>
              <h3 className="text-3xl font-black text-rose-400 mt-2 tracking-tight">{failureCount}건</h3>
            </div>
            <div className="w-12 h-12 rounded-2xl bg-rose-500/10 border border-rose-500/20 flex items-center justify-center text-rose-400">
              <AlertOctagon size={22} />
            </div>
          </div>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-black text-slate-400 uppercase tracking-wider">무결성 검증</p>
              <h3 className="text-xl font-black text-blue-400 mt-2 tracking-tight">Verified (SHA-256)</h3>
            </div>
            <div className="w-12 h-12 rounded-2xl bg-purple-500/10 border border-purple-500/20 flex items-center justify-center text-purple-400">
              <ShieldCheck size={22} />
            </div>
          </div>
        </div>
      </div>

      {/* Main Table and Filter Panel */}
      <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md space-y-6">
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="relative w-full sm:w-80">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500" size={18} />
            <input
              type="text"
              placeholder="작업자 ID, 행위, 리소스 검색..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full bg-white/5 border border-white/10 rounded-2xl py-2.5 pl-11 pr-4 text-white placeholder-slate-500 text-sm focus:outline-none focus:border-blue-500/50 transition-all"
            />
          </div>

          <div className="flex items-center gap-2 w-full sm:w-auto">
            <span className="text-xs font-bold text-slate-400 mr-2 flex items-center gap-1">
              <Filter size={14} /> 상태:
            </span>
            {['ALL', 'SUCCESS', 'FAILURE'].map((st) => (
              <button
                key={st}
                onClick={() => setStatusFilter(st)}
                className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition-all ${
                  statusFilter === st
                    ? 'bg-blue-600 text-white shadow-md'
                    : 'bg-white/5 text-slate-400 hover:bg-white/10 hover:text-white'
                }`}
              >
                {st === 'ALL' ? '전체' : st === 'SUCCESS' ? '성공' : '실패'}
              </button>
            ))}
          </div>
        </div>

        {/* Audit Log Table */}
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-white/10 text-xs font-black text-slate-400 uppercase tracking-wider">
                <th className="py-4 px-4">감사 ID / 발생일시</th>
                <th className="py-4 px-4">작업자 (Actor)</th>
                <th className="py-4 px-4">행위 (Action)</th>
                <th className="py-4 px-4">대상 리소스 (Resource)</th>
                <th className="py-4 px-4">처리 결과</th>
                <th className="py-4 px-4">접속 IP</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5 text-sm">
              {filteredLogs.map((log) => {
                const actionInfo = actionDescriptions[log.action] || { label: log.action, category: 'GENERAL' };
                return (
                  <tr 
                    key={log.logId} 
                    className="hover:bg-white/5 transition-colors group"
                  >
                    <td className="py-4 px-4">
                      <div className="font-bold text-white group-hover:text-blue-400 transition-colors font-mono">
                        {log.logId}
                      </div>
                      <div className="text-xs text-slate-400 flex items-center gap-1 mt-0.5">
                        <Clock size={12} className="text-slate-500" /> {log.timestamp}
                      </div>
                    </td>
                    <td className="py-4 px-4">
                      <div className="flex items-center gap-2">
                        <div className="w-7 h-7 rounded-lg bg-white/10 flex items-center justify-center text-slate-300 text-xs font-bold">
                          <User size={14} />
                        </div>
                        <span className="font-bold text-slate-200">{log.actorId}</span>
                      </div>
                    </td>
                    <td className="py-4 px-4">
                      <div className="font-bold text-white">{actionInfo.label}</div>
                      <span className="text-[11px] font-mono text-slate-400">{log.action}</span>
                    </td>
                    <td className="py-4 px-4">
                      <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-xl bg-slate-800 border border-white/5 text-xs font-mono text-blue-300">
                        <Database size={12} /> {log.resource}
                      </div>
                    </td>
                    <td className="py-4 px-4">
                      <StatusBadge 
                        status={log.status === 'SUCCESS' ? '성공' : '실패'} 
                        variant={log.status === 'SUCCESS' ? 'success' : 'error'} 
                      />
                    </td>
                    <td className="py-4 px-4 font-mono text-xs text-slate-400">
                      <div className="flex items-center gap-1.5">
                        <Globe size={14} className="text-slate-500" />
                        {log.ipAddress}
                      </div>
                    </td>
                  </tr>
                );
              })}
              {filteredLogs.length === 0 && (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-slate-500 font-medium">
                    조건에 일치하는 감사 로그가 없습니다.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

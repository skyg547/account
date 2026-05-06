"use client";

import React, { useState } from 'react';
import { 
  History, 
  Search, 
  Filter, 
  ChevronRight, 
  User, 
  Clock, 
  AlertCircle, 
  CheckCircle2, 
  RotateCcw,
  ArrowLeft
} from 'lucide-react';
import Link from 'next/link';

/**
 * [결산 감사 로그 데이터 타입]
 */
interface AuditLog {
  id: number;
  timestamp: string;
  action: string;
  actionDisplay: string;
  actor: string;
  status: 'SUCCESS' | 'FAILED' | 'WARNING';
  message: string;
  remarks?: string;
}

const MOCK_LOGS: AuditLog[] = [
  {
    id: 1,
    timestamp: '2026-04-20 09:00:12',
    action: 'CLOSING_OPENED',
    actionDisplay: '결산 오픈',
    actor: '시스템 관리자 (admin)',
    status: 'SUCCESS',
    message: '2026년 04월 정기 결산 프로세스가 활성화되었습니다.'
  },
  {
    id: 2,
    timestamp: '2026-04-22 14:30:45',
    action: 'JOURNAL_CLOSED',
    actionDisplay: '전표 마감',
    actor: '회계 1팀 (kim_account)',
    status: 'SUCCESS',
    message: '전표 입력 및 수정 권한이 제한되었습니다.'
  },
  {
    id: 3,
    timestamp: '2026-04-23 11:20:00',
    action: 'FX_REVALUATION',
    actionDisplay: '외화 환평가',
    actor: '자금팀 (lee_treasury)',
    status: 'SUCCESS',
    message: '당월 말 기준 환율을 적용한 외화 자산/부채 평가 완료.'
  },
  {
    id: 4,
    timestamp: '2026-04-24 10:15:33',
    action: 'REOPEN_REQUESTED',
    actionDisplay: '재오픈 요청',
    actor: '회계 1팀 (kim_account)',
    status: 'WARNING',
    message: '외환 환평가 오기재로 인한 재오픈 요청 발생',
    remarks: '누락된 해외 송금 내역 3건 발견으로 인한 재작업 필요.'
  },
  {
    id: 5,
    timestamp: '2026-04-24 13:00:05',
    action: 'CLOSING_REOPENED',
    actionDisplay: '결산 재오픈',
    actor: '시스템 관리자 (admin)',
    status: 'SUCCESS',
    message: '결산 상태가 [전표 마감] 이전으로 롤백되었습니다.'
  },
  {
    id: 6,
    timestamp: '2026-04-25 17:45:12',
    action: 'CLOSING_FINALIZED',
    actionDisplay: '결산 확정',
    actor: 'CFO (choi_cfo)',
    status: 'SUCCESS',
    message: '2026년 04월 결산 데이터가 승인 및 확정되었습니다.'
  }
];

/**
 * [결산 감사 로그 조회 화면]
 * 결산 프로세스의 모든 변경 이력을 타임라인 형태로 제공합니다.
 */
export default function ClosingAuditPage() {
  const [searchTerm, setSearchTerm] = useState('');

  const filteredLogs = MOCK_LOGS.filter(log => 
    log.message.includes(searchTerm) || 
    log.actor.includes(searchTerm) || 
    log.actionDisplay.includes(searchTerm)
  );

  return (
    <div className="space-y-8 animate-in fade-in duration-700">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
        <div className="space-y-2">
          <Link href="/closing" className="flex items-center gap-2 text-slate-500 hover:text-blue-500 transition-colors mb-4 group">
            <ArrowLeft size={16} className="group-hover:-translate-x-1 transition-transform" />
            <span className="text-xs font-bold uppercase tracking-widest">Back to Closing Hub</span>
          </Link>
          <div className="flex items-center gap-3 text-blue-500 mb-2">
            <History size={20} />
            <span className="text-xs font-black uppercase tracking-[0.3em]">Audit Trail</span>
          </div>
          <h2 className="text-4xl font-black text-white tracking-tighter italic">
            결산 감사 로그
          </h2>
          <p className="text-slate-500 font-medium max-w-2xl">
            결산 프로세스의 모든 수행 이력과 상태 변경을 투명하게 관리합니다. 재오픈 사유 및 승인 이력을 확인할 수 있습니다.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <div className="relative">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-600" size={16} />
            <input 
              type="text" 
              placeholder="이력 검색 (작업자, 내용...)" 
              className="bg-white/5 border border-white/10 rounded-2xl py-3 pl-12 pr-6 text-sm text-white outline-none focus:border-blue-500/50 transition-all w-64 md:w-80"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
          <button className="p-3 bg-white/5 border border-white/10 rounded-2xl text-slate-400 hover:bg-white/10 transition-all">
            <Filter size={20} />
          </button>
        </div>
      </div>

      {/* Audit Timeline */}
      <div className="glass-panel p-10 rounded-[3.5rem] border border-white/10 bg-white/[0.01] relative overflow-hidden">
        {/* Background Accent */}
        <div className="absolute top-0 right-0 w-96 h-96 bg-blue-600/5 blur-[100px] -z-10" />
        <div className="absolute bottom-0 left-0 w-96 h-96 bg-emerald-600/5 blur-[100px] -z-10" />

        <div className="relative space-y-12">
          {/* Vertical Line */}
          <div className="absolute left-8 top-2 bottom-2 w-0.5 bg-gradient-to-b from-blue-500/50 via-slate-800 to-emerald-500/50" />

          {filteredLogs.length === 0 ? (
            <div className="text-center py-20 space-y-4">
              <AlertCircle size={48} className="text-slate-700 mx-auto" />
              <p className="text-slate-500 font-bold">검색 결과와 일치하는 감사 로그가 없습니다.</p>
            </div>
          ) : (
            filteredLogs.map((log, index) => (
              <div key={log.id} className="relative pl-24 group">
                {/* Timeline Dot */}
                <div className={`absolute left-[1.625rem] top-1.5 w-4 h-4 rounded-full border-4 border-slate-950 z-10 transition-transform group-hover:scale-125 ${
                  log.status === 'SUCCESS' ? 'bg-emerald-500 shadow-[0_0_10px_rgba(16,185,129,0.5)]' : 
                  log.status === 'WARNING' ? 'bg-amber-500 shadow-[0_0_10px_rgba(245,158,11,0.5)]' : 'bg-rose-500 shadow-[0_0_10px_rgba(244,63,94,0.5)]'
                }`} />

                {/* Log Card */}
                <div className="glass-panel p-6 rounded-3xl bg-white/[0.02] border border-white/5 hover:border-white/10 transition-all group-hover:bg-white/[0.04]">
                  <div className="flex flex-col md:flex-row justify-between gap-4 mb-4">
                    <div className="space-y-1">
                      <div className="flex items-center gap-3">
                        <span className={`text-[10px] font-black px-2 py-0.5 rounded-md tracking-tighter ${
                          log.action === 'CLOSING_REOPENED' || log.action === 'REOPEN_REQUESTED' 
                          ? 'bg-rose-500/10 text-rose-500' : 'bg-blue-500/10 text-blue-500'
                        }`}>
                          {log.action}
                        </span>
                        <h4 className="text-lg font-black text-white italic tracking-tight">{log.actionDisplay}</h4>
                      </div>
                      <div className="flex items-center gap-4 text-slate-500">
                        <div className="flex items-center gap-1.5">
                          <User size={14} />
                          <span className="text-xs font-bold">{log.actor}</span>
                        </div>
                        <div className="flex items-center gap-1.5">
                          <Clock size={14} />
                          <span className="text-xs font-medium font-mono">{log.timestamp}</span>
                        </div>
                      </div>
                    </div>
                    
                    <div className="flex items-center gap-2 self-start md:self-auto">
                      {log.status === 'SUCCESS' ? (
                        <div className="flex items-center gap-1.5 text-emerald-500 bg-emerald-500/10 px-3 py-1 rounded-full border border-emerald-500/20">
                          <CheckCircle2 size={14} />
                          <span className="text-[10px] font-black">COMPLETED</span>
                        </div>
                      ) : (
                        <div className="flex items-center gap-1.5 text-amber-500 bg-amber-500/10 px-3 py-1 rounded-full border border-amber-500/20">
                          <RotateCcw size={14} />
                          <span className="text-[10px] font-black">ACTION REQUIRED</span>
                        </div>
                      )}
                    </div>
                  </div>

                  <p className="text-sm text-slate-300 font-medium leading-relaxed">
                    {log.message}
                  </p>

                  {log.remarks && (
                    <div className="mt-4 p-4 rounded-xl bg-rose-500/5 border border-rose-500/10 text-rose-200/70 text-xs italic">
                      <span className="font-black text-rose-500 not-italic mr-2">REMARKS:</span>
                      {log.remarks}
                    </div>
                  )}
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      {/* Footer Info */}
      <div className="flex items-center justify-between px-6 text-slate-600">
        <p className="text-[10px] font-bold tracking-widest uppercase italic">Generated at {new Date().toLocaleString()}</p>
        <div className="flex items-center gap-6">
           <div className="flex items-center gap-2">
             <div className="w-2 h-2 bg-blue-500 rounded-full" />
             <span className="text-[10px] font-black">STANDARD ACTION</span>
           </div>
           <div className="flex items-center gap-2">
             <div className="w-2 h-2 bg-rose-500 rounded-full" />
             <span className="text-[10px] font-black">CRITICAL CHANGE</span>
           </div>
        </div>
      </div>
    </div>
  );
}

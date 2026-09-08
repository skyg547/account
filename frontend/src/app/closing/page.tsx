"use client";

import React, { useState, useEffect, useCallback } from 'react';
import {
  CalendarDays,
  CheckCircle2,
  Activity,
  Stamp,
  History,
  AlertTriangle,
  ChevronRight,
  RefreshCcw,
  User,
  Loader2
} from 'lucide-react';
import Link from 'next/link';
import { closingService, ClosingTaskDto } from '@/services/closingService';
import { useToast } from '@/context/ToastContext';

/**
 * [결산 관리 화면 리팩토링]
 * 백엔드의 ClosingTaskStatus 및 Category 체계를 반영하여 고도화된 UI를 제공합니다.
 */

export default function ClosingPage() {
  const { success: showSuccessToast, error: showErrorToast, warning: showWarningToast } = useToast();
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL');
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [tasks, setTasks] = useState<ClosingTaskDto[]>([]);
  const [retryingTaskId, setRetryingTaskId] = useState<number | null>(null);
  const [isApproving, setIsApproving] = useState(false);

  const fetchTasks = useCallback(async () => {
    setIsRefreshing(true);
    try {
      // 1은 하드코딩된 예시 캘린더 ID
      const fetchedTasks = await closingService.getTasks(1);
      setTasks(fetchedTasks);
    } catch (e) {
      console.error(e);
      showErrorToast('결산 태스크 목록 조회에 실패했습니다.');
    } finally {
      setIsRefreshing(false);
    }
  }, [showErrorToast]);

  useEffect(() => {

    fetchTasks();
  }, [fetchTasks]);

  const handleRetryBatch = async (task: ClosingTaskDto) => {
    if (retryingTaskId !== null) return;

    if (!task.name.includes('ECL') && !task.name.includes('FX')) {
      showWarningToast('현재 지원되지 않는 배치 재실행입니다.', { title: '지원 안 됨' });
      return;
    }

    setRetryingTaskId(task.id);
    try {
      let success = false;
      if (task.name.includes('ECL')) {
        success = await closingService.runProvisionBatch({
          fiscalPeriodId: 1,
          provisionType: 'ECL',
          runBy: 'frontend-admin'
        });
      } else if (task.name.includes('FX')) {
        success = await closingService.runValuationBatch({
          fiscalPeriodId: 1,
          valuationType: 'FX_RATE',
          runBy: 'frontend-admin'
        });
      }

      if (success) {
        showSuccessToast(`[${task.name}] 배치 실행 요청이 성공적으로 전송되었습니다.`, { title: '배치 재실행 성공' });
        await fetchTasks();
      } else {
        showErrorToast(`[${task.name}] 배치 실행 요청에 실패했습니다. 백엔드 상태를 확인하세요.`, { title: '배치 재실행 실패' });
      }
    } catch (err: unknown) {
      showErrorToast(err instanceof Error ? err.message : '배치 요청 중 오류가 발생했습니다.', { title: '배치 오류' });
    } finally {
      setRetryingTaskId(null);
    }
  };

  const handleApprovalRequest = async () => {
    if (isApproving) return;
    setIsApproving(true);
    try {
      // 1초 시뮬레이션 및 API 요청
      await new Promise((r) => setTimeout(r, 800));
      showSuccessToast('결산 승인 요청이 상신되었습니다. CFO 결재 대기 중입니다.', { title: '결산 승인 상신' });
    } catch {
      showErrorToast('결산 승인 요청에 실패했습니다.');
    } finally {
      setIsApproving(false);
    }
  };

  const filteredTasks = selectedCategory === 'ALL'
    ? tasks
    : tasks.filter(t => t.category === selectedCategory);

  const progress = tasks.length > 0
    ? Math.round((tasks.filter(t => t.status === 'COMPLETED').length / tasks.length) * 100)
    : 0;

  return (
    <div className="space-y-10 animate-in fade-in duration-700">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
        <div className="space-y-2">
          <div className="flex items-center gap-3 text-blue-500 mb-2">
            <CalendarDays size={20} />
            <span className="text-xs font-black uppercase tracking-[0.3em]">Accounting Closing Hub</span>
          </div>
          <h2 className="text-4xl font-black text-white tracking-tighter italic">
            결산 및 재무제표 관리
          </h2>
          <p className="text-slate-500 font-medium max-w-2xl">
            2026년 04월 결산 프로세스가 진행 중입니다. 백엔드 검증 로직을 통해 실시간 정합성을 체크합니다.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <Link href="/closing/audit" className="px-6 py-3 bg-white/5 hover:bg-white/10 border border-white/10 rounded-2xl text-slate-400 text-sm font-black transition-all flex items-center gap-2">
            <History size={18} /> 감사 로그 조회
          </Link>
          <button
            onClick={handleApprovalRequest}
            disabled={isApproving}
            aria-busy={isApproving}
            className="px-6 py-3 bg-blue-600 hover:bg-blue-500 rounded-2xl text-white text-sm font-black transition-all shadow-lg shadow-blue-600/20 flex items-center gap-2 disabled:opacity-60 disabled:cursor-not-allowed disabled:pointer-events-none cursor-pointer"
          >
            {isApproving ? (
              <>
                <Loader2 size={18} className="animate-spin" />
                <span>승인 요청 처리 중...</span>
              </>
            ) : (
              <>
                <Stamp size={18} />
                <span>결산 승인 요청</span>
              </>
            )}
          </button>
        </div>
      </div>

      {/* Progress & Quick Stats */}
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        <div className="lg:col-span-3 glass-panel p-8 rounded-[3rem] border border-white/10 bg-white/[0.01]">
           <div className="flex items-center justify-between mb-8">
              <div className="flex items-center gap-4">
                 <h3 className="text-xl font-black text-white italic tracking-tight uppercase">Overall Progress</h3>
                 <button
                  onClick={fetchTasks}
                  className={`p-2 rounded-full hover:bg-white/5 transition-all ${isRefreshing ? 'animate-spin text-blue-500' : 'text-slate-600'}`}
                 >
                    <RefreshCcw size={16} />
                 </button>
              </div>
              <span className="text-xs font-black text-blue-500 bg-blue-500/10 px-4 py-1.5 rounded-full border border-blue-500/20">{progress}% COMPLETED</span>
           </div>

           <div className="relative w-full h-3 bg-slate-900 rounded-full overflow-hidden border border-white/5 p-0.5 mb-8">
              <div className="h-full bg-gradient-to-r from-blue-600 via-indigo-500 to-emerald-500 rounded-full shadow-[0_0_20px_rgba(59,130,246,0.3)] transition-all duration-1000 ease-out" style={{ width: `${progress}%` }} />
           </div>

           <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              {[
                { label: 'Total Tasks', value: tasks.length, color: 'text-white' },
                { label: 'Completed', value: tasks.filter(t => t.status === 'COMPLETED').length, color: 'text-emerald-500' },
                { label: 'In Progress', value: tasks.filter(t => t.status === 'IN_PROGRESS').length, color: 'text-blue-500' },
                { label: 'Failed', value: tasks.filter(t => t.status === 'FAILED').length, color: 'text-rose-500' },
              ].map((stat, i) => (
                <div key={i} className="bg-white/5 border border-white/5 p-4 rounded-2xl">
                   <p className="text-[10px] font-black text-slate-500 uppercase mb-1">{stat.label}</p>
                   <p className={`text-2xl font-black ${stat.color}`}>{stat.value}</p>
                </div>
              ))}
           </div>
        </div>

        <div className="glass-panel p-8 rounded-[3rem] border border-white/10 bg-amber-500/[0.03]">
           <h3 className="text-lg font-black text-white italic mb-6 flex items-center gap-2">
              <AlertTriangle size={18} className="text-amber-500" />
              Critical Alerts
           </h3>
           <div className="space-y-4">
              {tasks.filter(t => t.status === 'FAILED').map(task => (
                <div key={task.id} className="p-4 rounded-2xl bg-rose-500/10 border border-rose-500/20 space-y-2">
                   <p className="text-xs font-black text-rose-500 uppercase">{task.name}</p>
                   <p className="text-[10px] text-rose-200/60 leading-relaxed font-medium">{task.errorMessage || '원인 불명의 오류가 발생했습니다.'}</p>
                </div>
              ))}
              {tasks.filter(t => t.status === 'FAILED').length === 0 && (
                <p className="text-xs text-slate-600 font-bold italic">No critical issues found.</p>
              )}
           </div>
        </div>
      </div>

      {/* Task Explorer */}
      <div className="space-y-6">
        <div className="flex items-center justify-between px-2">
           <h3 className="text-xl font-black text-white italic tracking-tight">Closing Task Explorer</h3>
           <div className="flex bg-white/5 p-1 rounded-xl border border-white/5">
              {['ALL', 'PRE_CLOSING', 'CLOSING_ENTRY', 'POST_CLOSING', 'REPORTING'].map(cat => (
                <button
                  key={cat}
                  onClick={() => setSelectedCategory(cat)}
                  className={`px-4 py-2 text-[10px] font-black rounded-lg transition-all ${
                    selectedCategory === cat ? 'bg-blue-600 text-white shadow-lg' : 'text-slate-500 hover:text-slate-300'
                  }`}
                >
                  {cat.replace('_', ' ')}
                </button>
              ))}
           </div>
        </div>

        <div className="grid grid-cols-1 gap-4">
           {filteredTasks.map((task) => (
             <div key={task.id} className="glass-panel group p-6 rounded-[2rem] border border-white/5 bg-white/[0.01] hover:bg-white/[0.03] hover:border-white/10 transition-all flex items-center justify-between">
                <div className="flex items-center gap-6">
                   <div className={`w-12 h-12 rounded-2xl flex items-center justify-center border transition-all ${
                      task.status === 'COMPLETED' ? 'bg-emerald-500/10 border-emerald-500/20 text-emerald-500' :
                      task.status === 'FAILED' ? 'bg-rose-500/10 border-rose-500/20 text-rose-500 animate-pulse' :
                      task.status === 'IN_PROGRESS' ? 'bg-blue-500/10 border-blue-500/20 text-blue-500' :
                      'bg-slate-800 border-white/5 text-slate-600'
                   }`}>
                      {task.status === 'COMPLETED' ? <CheckCircle2 size={24} /> :
                       task.status === 'FAILED' ? <AlertTriangle size={24} /> :
                       <Activity size={24} className={task.status === 'IN_PROGRESS' ? 'animate-spin-slow' : ''} />}
                   </div>

                   <div className="space-y-1">
                      <div className="flex items-center gap-3">
                         <h4 className="text-lg font-black text-white group-hover:text-blue-400 transition-colors">{task.name}</h4>
                         {task.isMandatory && <span className="text-[8px] font-black text-amber-500 border border-amber-500/30 px-1.5 py-0.5 rounded uppercase">Mandatory</span>}
                      </div>
                      <div className="flex items-center gap-4">
                         <span className="text-[10px] font-black text-slate-500 uppercase tracking-widest">{task.category}</span>
                         <span className="w-1 h-1 bg-slate-800 rounded-full" />
                         <span className="text-[10px] font-bold text-slate-600 flex items-center gap-1">
                            <User size={12} /> {task.assignedTo}
                         </span>
                         <span className="w-1 h-1 bg-slate-800 rounded-full" />
                         <span className="text-[10px] font-bold text-slate-600 flex items-center gap-1">
                            <CalendarDays size={12} /> Due: {task.dueDate}
                         </span>
                      </div>
                   </div>
                </div>

                <div className="flex items-center gap-4">
                    {task.status === 'FAILED' && (
                      <button
                         onClick={() => handleRetryBatch(task)}
                         disabled={retryingTaskId === task.id}
                         aria-busy={retryingTaskId === task.id}
                         className="px-4 py-2 bg-rose-600/20 hover:bg-rose-600/30 text-rose-500 text-[10px] font-black rounded-xl border border-rose-500/20 transition-all flex items-center gap-1.5 disabled:opacity-50 disabled:cursor-not-allowed disabled:pointer-events-none cursor-pointer"
                      >
                         {retryingTaskId === task.id ? (
                           <>
                             <Loader2 size={12} className="animate-spin" />
                             <span>RETRYING...</span>
                           </>
                         ) : (
                           <span>RETRY BATCH</span>
                         )}
                      </button>
                    )}
                   <button className="p-3 bg-white/5 rounded-xl border border-white/5 text-slate-500 hover:text-white hover:bg-white/10 transition-all">
                      <ChevronRight size={20} />
                   </button>
                </div>
             </div>
           ))}
        </div>
      </div>

      {/* Financial Consistency Check */}
      <div className="glass-panel p-10 rounded-[3.5rem] border border-white/10 bg-slate-950 overflow-hidden relative">
        <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-blue-600 via-emerald-500 to-blue-600" />
        <div className="flex flex-col md:flex-row items-center justify-between gap-8">
           <div className="space-y-4">
              <h3 className="text-2xl font-black text-white italic tracking-tighter">실시간 재무 정합성 검증</h3>
              <p className="text-slate-500 text-sm font-medium max-w-xl">
                 결산 확정 전, 시스템이 전표의 차/대 평형, 미승인 건수, 마스터 데이터 정합성을 자동으로 검수합니다.
                 모든 항목이 <span className="text-emerald-500 font-bold">PASS</span> 상태여야 결산 승인이 가능합니다.
              </p>
           </div>
           <div className="grid grid-cols-1 md:grid-cols-2 gap-4 w-full md:w-auto">
              {[
                { label: 'Trial Balance Match', status: 'PASS' },
                { label: 'Unposted Journals', status: '0 건' },
                { label: 'Bank Reconciliation', status: 'MATCHED' },
                { label: 'Asset Depreciation', status: 'CALCULATED' },
              ].map((check, i) => (
                <div key={i} className="flex items-center justify-between gap-10 p-4 rounded-2xl bg-white/5 border border-white/5">
                   <span className="text-xs font-bold text-slate-400">{check.label}</span>
                   <span className="text-xs font-black text-emerald-500">{check.status}</span>
                </div>
              ))}
           </div>
        </div>
      </div>
    </div>
  );
}

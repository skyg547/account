"use client";

import React, { useState } from 'react';
import { 
  Lock, 
  Unlock, 
  ShieldAlert, 
  Calendar, 
  User, 
  CheckCircle2, 
  AlertCircle, 
  Send
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import { mockPeriods, PeriodDto } from '@/mocks/closing';

export default function PeriodLockPage() {
  const [periods, setPeriods] = useState<PeriodDto[]>(mockPeriods);
  const [selectedPeriodId, setSelectedPeriodId] = useState<string>(mockPeriods[0].id);
  const [actionType, setActionType] = useState<'LOCK' | 'REOPEN'>('LOCK');
  const [reason, setReason] = useState('');
  const [requester, setRequester] = useState('강팀장 (회계팀)');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const activePeriod = periods.find(p => p.id === selectedPeriodId) || periods[0];

  const handleActionSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!reason.trim()) return;

    setIsSubmitting(true);
    setTimeout(() => {
      setPeriods(prev =>
        prev.map(p => {
          if (p.id === selectedPeriodId) {
            if (actionType === 'LOCK') {
              return {
                ...p,
                status: 'LOCKED',
                lockedAt: new Date().toISOString().slice(0, 16).replace('T', ' '),
                lockedBy: requester,
                canReopen: true,
                note: `[잠금완료] ${reason}`,
              };
            } else {
              return {
                ...p,
                status: 'CLOSING_IN_PROGRESS',
                lockedAt: undefined,
                lockedBy: undefined,
                canReopen: false,
                note: `[재개완료] ${reason}`,
              };
            }
          }
          return p;
        })
      );

      setIsSubmitting(false);
      setSuccessMessage(
        actionType === 'LOCK'
          ? `${activePeriod.periodName} 기간이 성공적으로 잠금 마감되었습니다.`
          : `${activePeriod.periodName} 기간 결산 재개가 승인 처리되었습니다.`
      );
      setReason('');

      setTimeout(() => setSuccessMessage(null), 5000);
    }, 1000);
  };

  const getStatusVariant = (status: PeriodDto['status']) => {
    switch (status) {
      case 'LOCKED': return 'success';
      case 'CLOSING_IN_PROGRESS': return 'warning';
      case 'PENDING_APPROVAL': return 'info';
      case 'OPEN': return 'neutral';
      default: return 'neutral';
    }
  };

  const getStatusLabel = (status: PeriodDto['status']) => {
    switch (status) {
      case 'LOCKED': return '기간 잠금 완료';
      case 'CLOSING_IN_PROGRESS': return '결산 진행 중';
      case 'PENDING_APPROVAL': return '승인 대기';
      case 'OPEN': return '일반 오픈';
      default: return status;
    }
  };

  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      {/* Page Header */}
      <PageHeader
        title="기간 잠금/재개"
        description="월별 회계 기간의 전표 입력 권한을 잠그거나 예외 발생 시 마감 재개를 신청 및 관리합니다."
        breadcrumbs={[
          { label: '결산 관리', href: '/closing' },
          { label: '기간 잠금/재개' },
        ]}
        icon={Lock}
      />

      {/* Success Banner */}
      {successMessage && (
        <div className="p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-300 text-sm font-bold flex items-center gap-3 animate-in fade-in slide-in-from-top-2">
          <CheckCircle2 size={20} className="text-emerald-400 shrink-0" />
          <span>{successMessage}</span>
        </div>
      )}

      {/* Grid Layout: Table & Request Form */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left 2 Cols: Period Table */}
        <div className="lg:col-span-2 space-y-6">
          <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-lg font-black text-white italic">회계 기간 현황 목록</h3>
                <p className="text-xs text-slate-400">기간별 마감 처리 및 전표 수량</p>
              </div>
              <span className="text-xs font-mono font-bold text-slate-400 bg-white/5 px-3 py-1 rounded-xl border border-white/5">
                총 {periods.length}개 기간
              </span>
            </div>

            {/* Table */}
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-white/10 text-[11px] font-black uppercase tracking-wider text-slate-400">
                    <th className="py-3 px-4">회계 기간</th>
                    <th className="py-3 px-4">마감 상태</th>
                    <th className="py-3 px-4">전표 진행률</th>
                    <th className="py-3 px-4">잠금 처리 정보</th>
                    <th className="py-3 px-4 text-right">선택</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/5 text-xs font-medium">
                  {periods.map((period) => {
                    const isSelected = period.id === selectedPeriodId;
                    const closedPct = period.totalJournals 
                      ? Math.round(((period.closedJournals || 0) / period.totalJournals) * 100) 
                      : 0;

                    return (
                      <tr 
                        key={period.id} 
                        className={`transition-all hover:bg-white/5 ${
                          isSelected ? 'bg-blue-600/10 font-bold' : ''
                        }`}
                      >
                        <td className="py-4 px-4 font-bold text-white">
                          <div className="flex items-center gap-2">
                            <Calendar size={14} className="text-blue-400" />
                            <span>{period.periodName}</span>
                          </div>
                        </td>
                        <td className="py-4 px-4">
                          <StatusBadge
                            status={getStatusLabel(period.status)}
                            variant={getStatusVariant(period.status)}
                          />
                        </td>
                        <td className="py-4 px-4">
                          <div className="space-y-1 w-32">
                            <div className="flex justify-between text-[10px] text-slate-400 font-mono">
                              <span>{period.closedJournals}/{period.totalJournals}</span>
                              <span>{closedPct}%</span>
                            </div>
                            <div className="w-full h-1.5 bg-slate-800 rounded-full overflow-hidden">
                              <div 
                                className="h-full bg-blue-500 rounded-full" 
                                style={{ width: `${closedPct}%` }}
                              />
                            </div>
                          </div>
                        </td>
                        <td className="py-4 px-4 text-slate-400">
                          {period.lockedAt ? (
                            <div className="space-y-0.5 text-[11px]">
                              <p className="text-slate-300">{period.lockedBy}</p>
                              <p className="font-mono text-slate-500">{period.lockedAt}</p>
                            </div>
                          ) : (
                            <span className="text-slate-600 italic">미잠금</span>
                          )}
                        </td>
                        <td className="py-4 px-4 text-right">
                          <button
                            onClick={() => {
                              setSelectedPeriodId(period.id);
                              setActionType(period.status === 'LOCKED' ? 'REOPEN' : 'LOCK');
                            }}
                            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all ${
                              isSelected
                                ? 'bg-blue-600 text-white shadow-md'
                                : 'bg-white/5 text-slate-300 hover:bg-white/10 hover:text-white'
                            }`}
                          >
                            선택
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        </div>

        {/* Right 1 Col: Request Form */}
        <div className="space-y-6">
          <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-6">
            <div className="flex items-center gap-3 pb-4 border-b border-white/5">
              <div className="w-10 h-10 rounded-2xl bg-blue-600/10 border border-blue-500/20 flex items-center justify-center text-blue-400">
                {actionType === 'LOCK' ? <Lock size={20} /> : <Unlock size={20} />}
              </div>
              <div>
                <h3 className="text-base font-black text-white italic">기간 잠금 / 재개 신청</h3>
                <p className="text-xs text-slate-400">선택된 기간의 마감 상태 변경</p>
              </div>
            </div>

            <form onSubmit={handleActionSubmit} className="space-y-4">
              <div className="p-4 rounded-2xl bg-white/5 border border-white/5 space-y-2">
                <span className="text-[10px] font-black uppercase text-slate-400 tracking-wider">대상 회계 기간</span>
                <div className="flex items-center justify-between">
                  <span className="text-lg font-black text-white">{activePeriod.periodName}</span>
                  <StatusBadge
                    status={getStatusLabel(activePeriod.status)}
                    variant={getStatusVariant(activePeriod.status)}
                  />
                </div>
                {activePeriod.note && (
                  <p className="text-xs text-slate-400 border-t border-white/5 pt-2 mt-2">
                    {activePeriod.note}
                  </p>
                )}
              </div>

              <div className="space-y-2">
                <label className="text-xs font-bold text-slate-400">요청 처리 구분</label>
                <div className="grid grid-cols-2 gap-2 p-1 bg-slate-950/50 rounded-2xl border border-white/5">
                  <button
                    type="button"
                    onClick={() => setActionType('LOCK')}
                    disabled={activePeriod.status === 'LOCKED'}
                    className={`py-2.5 rounded-xl text-xs font-black transition-all flex items-center justify-center gap-1.5 ${
                      actionType === 'LOCK'
                        ? 'bg-blue-600 text-white shadow-md'
                        : 'text-slate-400 hover:text-white disabled:opacity-30'
                    }`}
                  >
                    <Lock size={14} /> 기간 잠금
                  </button>
                  <button
                    type="button"
                    onClick={() => setActionType('REOPEN')}
                    disabled={!activePeriod.canReopen && activePeriod.status !== 'LOCKED'}
                    className={`py-2.5 rounded-xl text-xs font-black transition-all flex items-center justify-center gap-1.5 ${
                      actionType === 'REOPEN'
                        ? 'bg-amber-600 text-white shadow-md'
                        : 'text-slate-400 hover:text-white disabled:opacity-30'
                    }`}
                  >
                    <Unlock size={14} /> 결산 재개
                  </button>
                </div>
              </div>

              <div className="space-y-2">
                <label className="text-xs font-bold text-slate-400">요청자 정보</label>
                <div className="relative">
                  <User size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500" />
                  <input
                    type="text"
                    value={requester}
                    onChange={(e) => setRequester(e.target.value)}
                    className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-white/5 border border-white/10 text-xs text-white outline-none focus:border-blue-500/50"
                  />
                </div>
              </div>

              <div className="space-y-2">
                <label className="text-xs font-bold text-slate-400">사유 및 상세 승인 요청 메모 *</label>
                <textarea
                  rows={4}
                  required
                  placeholder={
                    actionType === 'LOCK'
                      ? '당월 정기 결산 검증 완료 및 CFO 승인에 따른 잠금 실행사유 입력...'
                      : '누락 전표 보완 또는 수정 요청에 따른 재오픈 사유 명시...'
                  }
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  className="w-full p-3.5 rounded-xl bg-white/5 border border-white/10 text-xs text-white placeholder-slate-500 outline-none focus:border-blue-500/50 resize-none"
                />
              </div>

              {actionType === 'LOCK' ? (
                <div className="p-3 rounded-xl bg-blue-500/10 border border-blue-500/20 text-blue-300 text-[11px] font-medium flex items-start gap-2">
                  <ShieldAlert size={16} className="shrink-0 mt-0.5" />
                  <span>기간이 잠금되면 해당 회계 기간의 신규 전표 등록 및 기존 전표 수정/삭제가 전면 차단됩니다.</span>
                </div>
              ) : (
                <div className="p-3 rounded-xl bg-amber-500/10 border border-amber-500/20 text-amber-300 text-[11px] font-medium flex items-start gap-2">
                  <AlertCircle size={16} className="shrink-0 mt-0.5" />
                  <span>결산 재개 승인 시 해당 기간 전표 수정이 임시 허용되며 감사 로그에 이력이 기록됩니다.</span>
                </div>
              )}

              <button
                type="submit"
                disabled={isSubmitting || !reason.trim()}
                className={`w-full py-3.5 rounded-2xl font-black text-sm flex items-center justify-center gap-2 shadow-lg transition-all disabled:opacity-50 ${
                  actionType === 'LOCK'
                    ? 'bg-blue-600 hover:bg-blue-500 text-white shadow-blue-600/20'
                    : 'bg-amber-600 hover:bg-amber-500 text-white shadow-amber-600/20'
                }`}
              >
                <Send size={16} />
                <span>{isSubmitting ? '처리 요청 중...' : actionType === 'LOCK' ? '기간 잠금 확정' : '결산 재개 신청'}</span>
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}

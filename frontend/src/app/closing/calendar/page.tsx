"use client";

import React, { useState } from 'react';
import { 
  Calendar as CalendarIcon, 
  ChevronLeft, 
  ChevronRight, 
  Clock, 
  CheckCircle2, 
  Flag
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';

interface Milestone {
  id: string;
  date: string;
  title: string;
  category: string;
  status: 'COMPLETED' | 'IN_PROGRESS' | 'PENDING' | 'OVERDUE';
  owner: string;
}

export default function ClosingCalendarPage() {
  const [selectedYear, setSelectedYear] = useState<number>(2026);
  const [selectedMonth, setSelectedMonth] = useState<number>(7);
  const [viewMode, setViewMode] = useState<'MONTH' | 'TIMELINE'>('MONTH');

  const milestones: Milestone[] = [
    { id: 'M1', date: '2026-07-05', title: '일반 전표 수집 마감', category: '전표', status: 'COMPLETED', owner: '회계1팀' },
    { id: 'M2', date: '2026-07-15', title: '은행 예금 및 차입금 잔액대사', category: '자금', status: 'COMPLETED', owner: '자금팀' },
    { id: 'M3', date: '2026-07-24', title: '거래처 미수/미지급 잔액 검증', category: '채권채무', status: 'COMPLETED', owner: '회계1팀' },
    { id: 'M4', date: '2026-07-25', title: '외화 통화별 기말 평가 배치', category: '배치', status: 'COMPLETED', owner: '자금팀' },
    { id: 'M5', date: '2026-07-27', title: '유무형 자산 감가상각 반영', category: '자산', status: 'IN_PROGRESS', owner: '자산팀' },
    { id: 'M6', date: '2026-07-28', title: '선급/미지급 비용 기간 안분', category: '조정', status: 'IN_PROGRESS', owner: '회계2팀' },
    { id: 'M7', date: '2026-07-29', title: '법인세 추산 계상 & 당기순이익 산출', category: '세무', status: 'PENDING', owner: '세무팀' },
    { id: 'M8', date: '2026-07-31', title: '월간 재무상태표/손익계산서 확정 및 기간 잠금', category: '최종결산', status: 'PENDING', owner: 'CFO/팀장' },
  ];

  const daysInMonth = 31;
  const startDayOfWeek = 3;

  const monthNames = ['1월', '2월', '3월', '4월', '5월', '6월', '7월', '8월', '9월', '10월', '11월', '12월'];

  const prevMonth = () => {
    if (selectedMonth === 1) {
      setSelectedMonth(12);
      setSelectedYear(selectedYear - 1);
    } else {
      setSelectedMonth(selectedMonth - 1);
    }
  };

  const nextMonth = () => {
    if (selectedMonth === 12) {
      setSelectedMonth(1);
      setSelectedYear(selectedYear + 1);
    } else {
      setSelectedMonth(selectedMonth + 1);
    }
  };

  const getMilestonesForDay = (day: number) => {
    const formattedDate = `${selectedYear}-${String(selectedMonth).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
    return milestones.filter(m => m.date === formattedDate);
  };

  const completedCount = milestones.filter(m => m.status === 'COMPLETED').length;
  const progressPercent = Math.round((completedCount / milestones.length) * 100);

  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      {/* Page Header */}
      <PageHeader
        title="결산 캘린더"
        description="월별 및 연간 결산 일정을 한눈에 파악하고 핵심 마일스톤과 마감 진행률을 모니터링합니다."
        breadcrumbs={[
          { label: '결산 관리', href: '/closing' },
          { label: '결산 캘린더' },
        ]}
        icon={CalendarIcon}
        actions={
          <div className="flex items-center gap-3">
            <div className="flex items-center p-1 bg-slate-900/50 backdrop-blur-md rounded-2xl border border-white/5">
              <button
                onClick={() => setViewMode('MONTH')}
                className={`px-4 py-2 rounded-xl text-xs font-black transition-all ${
                  viewMode === 'MONTH' 
                    ? 'bg-blue-600 text-white shadow-lg' 
                    : 'text-slate-400 hover:text-white'
                }`}
              >
                달력 뷰
              </button>
              <button
                onClick={() => setViewMode('TIMELINE')}
                className={`px-4 py-2 rounded-xl text-xs font-black transition-all ${
                  viewMode === 'TIMELINE' 
                    ? 'bg-blue-600 text-white shadow-lg' 
                    : 'text-slate-400 hover:text-white'
                }`}
              >
                타임라인 뷰
              </button>
            </div>
          </div>
        }
      />

      {/* Progress & Overview Card */}
      <div className="p-8 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 relative overflow-hidden">
        <div className="absolute top-0 right-0 w-96 h-96 bg-blue-600/10 rounded-full blur-[100px] pointer-events-none" />
        <div className="relative z-10 grid grid-cols-1 md:grid-cols-4 gap-6 items-center">
          <div className="md:col-span-2 space-y-3">
            <div className="flex items-center gap-3">
              <span className="text-xs font-black tracking-widest text-blue-400 uppercase bg-blue-500/10 px-3 py-1 rounded-full border border-blue-500/20">
                {selectedYear}년 {monthNames[selectedMonth - 1]} 결산 공정율
              </span>
              <StatusBadge status="진행중" variant="warning" />
            </div>
            <div className="flex items-baseline gap-4">
              <span className="text-5xl font-black text-white italic tracking-tighter">{progressPercent}%</span>
              <span className="text-sm font-medium text-slate-400">
                총 {milestones.length}개 마일스톤 중 {completedCount}개 완료
              </span>
            </div>
            <div className="w-full h-3 bg-slate-800 rounded-full overflow-hidden border border-white/5 p-0.5">
              <div 
                className="h-full bg-gradient-to-r from-blue-500 via-indigo-500 to-emerald-400 rounded-full transition-all duration-1000 shadow-[0_0_12px_rgba(59,130,246,0.6)]"
                style={{ width: `${progressPercent}%` }}
              />
            </div>
          </div>

          <div className="p-4 rounded-2xl bg-white/5 border border-white/5 space-y-2">
            <div className="flex items-center justify-between text-xs text-slate-400 font-bold">
              <span>완료된 마일스톤</span>
              <CheckCircle2 size={16} className="text-emerald-400" />
            </div>
            <p className="text-2xl font-black text-white tracking-tight">{completedCount} <span className="text-xs font-normal text-slate-500">건</span></p>
            <p className="text-[11px] text-emerald-400 font-medium">정상 스케줄 진행 완료</p>
          </div>

          <div className="p-4 rounded-2xl bg-white/5 border border-white/5 space-y-2">
            <div className="flex items-center justify-between text-xs text-slate-400 font-bold">
              <span>남은 핵심 과제</span>
              <Clock size={16} className="text-amber-400" />
            </div>
            <p className="text-2xl font-black text-white tracking-tight">{milestones.length - completedCount} <span className="text-xs font-normal text-slate-500">건</span></p>
            <p className="text-[11px] text-amber-400 font-medium">기말 잠금 D-3일 남음</p>
          </div>
        </div>
      </div>

      {/* Calendar Controls */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <button
            onClick={prevMonth}
            className="p-3 rounded-2xl bg-slate-900/50 border border-white/5 text-slate-300 hover:text-white hover:bg-white/10 transition-all"
          >
            <ChevronLeft size={20} />
          </button>
          <div className="flex items-center gap-3">
            <h3 className="text-2xl font-black text-white italic tracking-tight">
              {selectedYear}년 {monthNames[selectedMonth - 1]}
            </h3>
            <span className="text-xs font-mono px-2.5 py-1 rounded-lg bg-blue-500/10 text-blue-400 border border-blue-500/20 font-bold">
              JULY 2026
            </span>
          </div>
          <button
            onClick={nextMonth}
            className="p-3 rounded-2xl bg-slate-900/50 border border-white/5 text-slate-300 hover:text-white hover:bg-white/10 transition-all"
          >
            <ChevronRight size={20} />
          </button>
        </div>

        {/* Legend */}
        <div className="hidden md:flex items-center gap-4 text-xs font-bold text-slate-400">
          <div className="flex items-center gap-2">
            <span className="w-2.5 h-2.5 rounded-full bg-emerald-400 shadow-[0_0_8px_rgba(52,211,153,0.5)]" />
            <span>완료</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="w-2.5 h-2.5 rounded-full bg-blue-400 shadow-[0_0_8px_rgba(96,165,250,0.5)] animate-pulse" />
            <span>진행중</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="w-2.5 h-2.5 rounded-full bg-slate-600" />
            <span>예정</span>
          </div>
        </div>
      </div>

      {/* Main View Mode Content */}
      {viewMode === 'MONTH' ? (
        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5">
          <div className="grid grid-cols-7 gap-2 mb-4 text-center">
            {['일', '월', '화', '수', '목', '금', '토'].map((day, idx) => (
              <div key={day} className={`text-xs font-black uppercase tracking-wider py-2 ${idx === 0 ? 'text-rose-400' : idx === 6 ? 'text-blue-400' : 'text-slate-400'}`}>
                {day}
              </div>
            ))}
          </div>

          <div className="grid grid-cols-7 gap-2">
            {Array.from({ length: startDayOfWeek }).map((_, idx) => (
              <div key={`empty-${idx}`} className="h-32 rounded-2xl bg-white/[0.01] border border-white/[0.02]" />
            ))}

            {Array.from({ length: daysInMonth }).map((_, idx) => {
              const dayNum = idx + 1;
              const dayMilestones = getMilestonesForDay(dayNum);
              const isToday = dayNum === 28;

              return (
                <div
                  key={dayNum}
                  className={`h-32 p-3 rounded-2xl border transition-all flex flex-col justify-between group ${
                    isToday
                      ? 'bg-blue-600/10 border-blue-500/40 ring-1 ring-blue-500/50'
                      : 'bg-white/5 border-white/5 hover:border-white/20 hover:bg-white/[0.07]'
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <span className={`text-sm font-black ${isToday ? 'text-blue-400 font-mono text-base' : 'text-slate-300'}`}>
                      {dayNum}
                    </span>
                    {isToday && (
                      <span className="text-[9px] font-black uppercase px-2 py-0.5 rounded-full bg-blue-500 text-white">
                        TODAY
                      </span>
                    )}
                  </div>

                  <div className="space-y-1.5 overflow-y-auto max-h-20 scrollbar-none">
                    {dayMilestones.map((m) => (
                      <div
                        key={m.id}
                        className={`p-1.5 rounded-xl text-[11px] font-bold border flex items-center gap-1.5 truncate ${
                          m.status === 'COMPLETED'
                            ? 'bg-emerald-500/10 text-emerald-300 border-emerald-500/20'
                            : m.status === 'IN_PROGRESS'
                            ? 'bg-blue-500/10 text-blue-300 border-blue-500/20'
                            : 'bg-slate-800 text-slate-300 border-white/10'
                        }`}
                      >
                        <Flag size={12} className="shrink-0" />
                        <span className="truncate">{m.title}</span>
                      </div>
                    ))}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      ) : (
        <div className="p-8 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-6">
          <h3 className="text-xl font-black text-white italic">마일스톤 상세 타임라인</h3>
          <div className="relative border-l border-white/10 pl-6 space-y-8 ml-4">
            {milestones.map((m) => (
              <div key={m.id} className="relative group">
                <div className={`absolute -left-[31px] top-1 w-4 h-4 rounded-full border-2 border-slate-900 transition-all group-hover:scale-125 ${
                  m.status === 'COMPLETED'
                    ? 'bg-emerald-400 shadow-[0_0_10px_rgba(52,211,153,0.8)]'
                    : m.status === 'IN_PROGRESS'
                    ? 'bg-blue-500 animate-ping'
                    : 'bg-slate-700'
                }`} />

                <div className="p-5 rounded-2xl bg-white/5 border border-white/5 group-hover:border-white/10 transition-all space-y-3">
                  <div className="flex flex-col md:flex-row md:items-center justify-between gap-2">
                    <div className="flex items-center gap-3">
                      <span className="text-xs font-mono font-bold text-blue-400 bg-blue-500/10 px-2.5 py-1 rounded-lg border border-blue-500/20">
                        {m.date}
                      </span>
                      <h4 className="text-base font-bold text-white">{m.title}</h4>
                    </div>
                    <StatusBadge
                      status={m.status === 'COMPLETED' ? '완료' : m.status === 'IN_PROGRESS' ? '진행중' : '대기'}
                      variant={m.status === 'COMPLETED' ? 'success' : m.status === 'IN_PROGRESS' ? 'info' : 'neutral'}
                    />
                  </div>

                  <div className="flex items-center justify-between text-xs text-slate-400 border-t border-white/5 pt-3">
                    <span className="font-medium">담당: <strong className="text-slate-200">{m.owner}</strong></span>
                    <span className="text-slate-500 uppercase tracking-widest text-[10px] font-bold">카테고리: {m.category}</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

"use client";

import React, { useState } from 'react';
import { 
  CheckSquare, 
  Search, 
  Clock, 
  User, 
  AlertCircle, 
  CheckCircle2, 
  Plus, 
  Layers
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import EmptyState from '@/components/ui/EmptyState';
import { mockTasks, ClosingTaskDto } from '@/mocks/closing';

export default function ClosingTasksPage() {
  const [tasks, setTasks] = useState<ClosingTaskDto[]>(mockTasks);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL');
  const [selectedStatus, setSelectedStatus] = useState<string>('ALL');

  const categories = ['ALL', '수익/비용', '자산/부채', '전표마감', '세무/검증', '보고서'];

  const handleToggleTask = (taskId: string) => {
    setTasks(prev =>
      prev.map(t => {
        if (t.id === taskId) {
          const isDone = t.status === 'COMPLETED';
          return {
            ...t,
            status: isDone ? 'IN_PROGRESS' : 'COMPLETED',
            progress: isDone ? 50 : 100,
            completedAt: isDone ? undefined : new Date().toISOString().slice(0, 16).replace('T', ' '),
          };
        }
        return t;
      })
    );
  };

  const filteredTasks = tasks.filter(t => {
    const matchesSearch = t.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
                          t.assignee.toLowerCase().includes(searchTerm.toLowerCase()) ||
                          t.id.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesCat = selectedCategory === 'ALL' || t.category === selectedCategory;
    const matchesStatus = selectedStatus === 'ALL' || t.status === selectedStatus;
    return matchesSearch && matchesCat && matchesStatus;
  });

  const totalCount = tasks.length;
  const completedCount = tasks.filter(t => t.status === 'COMPLETED').length;
  const inProgressCount = tasks.filter(t => t.status === 'IN_PROGRESS').length;
  const overdueCount = tasks.filter(t => t.status === 'OVERDUE').length;

  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      {/* Page Header */}
      <PageHeader
        title="결산 태스크"
        description="월간 결산 체크리스트 및 개별 작업 항목들의 담당자, 만료일, 수행 현황을 체계적으로 관리합니다."
        breadcrumbs={[
          { label: '결산 관리', href: '/closing' },
          { label: '결산 태스크' },
        ]}
        icon={CheckSquare}
        actions={
          <button className="flex items-center gap-2 px-5 py-3 rounded-2xl bg-blue-600 hover:bg-blue-500 text-white font-bold text-sm shadow-lg shadow-blue-600/20 transition-all">
            <Plus size={18} />
            <span>신규 태스크 생성</span>
          </button>
        }
      />

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="flex items-center justify-between text-xs font-bold text-slate-400">
            <span>전체 태스크</span>
            <Layers size={16} className="text-slate-500" />
          </div>
          <p className="text-3xl font-black text-white">{totalCount} <span className="text-xs font-normal text-slate-500">개</span></p>
          <p className="text-xs text-slate-400 font-medium">당월 결산 등록 항목</p>
        </div>

        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="flex items-center justify-between text-xs font-bold text-emerald-400">
            <span>완료</span>
            <CheckCircle2 size={16} className="text-emerald-400" />
          </div>
          <p className="text-3xl font-black text-emerald-400">{completedCount} <span className="text-xs font-normal text-slate-500">개</span></p>
          <p className="text-xs text-emerald-400/80 font-medium">달성률 {Math.round((completedCount / totalCount) * 100)}%</p>
        </div>

        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="flex items-center justify-between text-xs font-bold text-blue-400">
            <span>진행중</span>
            <Clock size={16} className="text-blue-400" />
          </div>
          <p className="text-3xl font-black text-blue-400">{inProgressCount} <span className="text-xs font-normal text-slate-500">개</span></p>
          <p className="text-xs text-blue-400/80 font-medium">작업 처리 중</p>
        </div>

        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="flex items-center justify-between text-xs font-bold text-rose-400">
            <span>지연/지연위험</span>
            <AlertCircle size={16} className="text-rose-400" />
          </div>
          <p className="text-3xl font-black text-rose-400">{overdueCount} <span className="text-xs font-normal text-slate-500">개</span></p>
          <p className="text-xs text-rose-400/80 font-medium">우선 조치 필요</p>
        </div>
      </div>

      {/* Filters and Controls */}
      <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-4">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="flex items-center gap-1.5 overflow-x-auto pb-2 md:pb-0 scrollbar-none">
            {categories.map((cat) => (
              <button
                key={cat}
                onClick={() => setSelectedCategory(cat)}
                className={`px-4 py-2 rounded-xl text-xs font-black transition-all shrink-0 ${
                  selectedCategory === cat
                    ? 'bg-blue-600 text-white shadow-md'
                    : 'bg-white/5 text-slate-400 hover:text-white hover:bg-white/10'
                }`}
              >
                {cat === 'ALL' ? '전체 카테고리' : cat}
              </button>
            ))}
          </div>

          <div className="flex items-center gap-3">
            <div className="relative flex-1 md:w-64">
              <Search size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500" />
              <input
                type="text"
                placeholder="태스크명, 담당자 검색..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-white/5 border border-white/10 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500/50"
              />
            </div>
            <select
              value={selectedStatus}
              onChange={(e) => setSelectedStatus(e.target.value)}
              className="px-4 py-2.5 rounded-xl bg-white/5 border border-white/10 text-xs text-white font-bold outline-none focus:border-blue-500/50"
            >
              <option value="ALL" className="bg-slate-900 text-white">전체 상태</option>
              <option value="COMPLETED" className="bg-slate-900 text-white">완료</option>
              <option value="IN_PROGRESS" className="bg-slate-900 text-white">진행중</option>
              <option value="PENDING" className="bg-slate-900 text-white">대기</option>
              <option value="OVERDUE" className="bg-slate-900 text-white">지연</option>
            </select>
          </div>
        </div>
      </div>

      {/* Task List */}
      {filteredTasks.length === 0 ? (
        <EmptyState
          icon={AlertCircle}
          title="검색된 결산 태스크가 없습니다"
          description="필터 조건을 변경하거나 검색어를 다르게 입력해 보세요."
        />
      ) : (
        <div className="space-y-3">
          {filteredTasks.map((task) => {
            const isCompleted = task.status === 'COMPLETED';

            const getVariant = (status: ClosingTaskDto['status']) => {
              switch (status) {
                case 'COMPLETED': return 'success';
                case 'IN_PROGRESS': return 'info';
                case 'OVERDUE': return 'error';
                default: return 'neutral';
              }
            };

            const getPriorityStyle = (priority: ClosingTaskDto['priority']) => {
              switch (priority) {
                case 'HIGH': return 'text-rose-400 bg-rose-500/10 border-rose-500/20';
                case 'MEDIUM': return 'text-amber-400 bg-amber-500/10 border-amber-500/20';
                default: return 'text-slate-400 bg-slate-500/10 border-slate-500/20';
              }
            };

            return (
              <div
                key={task.id}
                className={`p-6 rounded-3xl border transition-all duration-300 ${
                  isCompleted 
                    ? 'bg-slate-900/30 border-white/5 opacity-80' 
                    : 'bg-slate-900/60 border-white/10 hover:border-white/20'
                }`}
              >
                <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
                  <div className="flex items-start gap-4 flex-1">
                    <button
                      onClick={() => handleToggleTask(task.id)}
                      className={`mt-1 w-6 h-6 rounded-lg border flex items-center justify-center transition-all ${
                        isCompleted
                          ? 'bg-emerald-500 border-emerald-500 text-slate-950'
                          : 'border-white/20 hover:border-blue-400 bg-white/5'
                      }`}
                    >
                      {isCompleted && <CheckCircle2 size={16} className="text-slate-950 font-black" />}
                    </button>

                    <div className="space-y-2 flex-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="text-[10px] font-mono text-slate-500 font-bold">{task.id}</span>
                        <span className="text-xs font-bold text-slate-400 bg-white/5 px-2.5 py-0.5 rounded-lg border border-white/5">
                          {task.category}
                        </span>
                        <span className={`text-[10px] font-black px-2 py-0.5 rounded-md border uppercase ${getPriorityStyle(task.priority)}`}>
                          {task.priority}
                        </span>
                        <StatusBadge
                          status={
                            task.status === 'COMPLETED' ? '완료' :
                            task.status === 'IN_PROGRESS' ? '진행중' :
                            task.status === 'OVERDUE' ? '지연' : '대기'
                          }
                          variant={getVariant(task.status)}
                        />
                      </div>

                      <h4 className={`text-base font-bold ${isCompleted ? 'line-through text-slate-500' : 'text-white'}`}>
                        {task.title}
                      </h4>

                      {task.description && (
                        <p className="text-xs text-slate-400 font-medium">
                          {task.description}
                        </p>
                      )}

                      <div className="flex flex-wrap items-center gap-4 text-xs text-slate-400 pt-1">
                        <div className="flex items-center gap-1.5">
                          <User size={14} className="text-slate-500" />
                          <span>{task.assignee}</span>
                        </div>
                        <div className="flex items-center gap-1.5">
                          <Clock size={14} className="text-slate-500" />
                          <span>마감일: <strong className="text-slate-300 font-mono">{task.dueDate}</strong></span>
                        </div>
                        {task.completedAt && (
                          <span className="text-[11px] text-emerald-400 font-mono">
                            완료시각: {task.completedAt}
                          </span>
                        )}
                      </div>
                    </div>
                  </div>

                  <div className="w-full lg:w-48 space-y-2 border-t lg:border-t-0 border-white/5 pt-3 lg:pt-0">
                    <div className="flex items-center justify-between text-xs font-bold">
                      <span className="text-slate-400">진행도</span>
                      <span className="text-white font-mono">{task.progress}%</span>
                    </div>
                    <div className="w-full h-2 bg-slate-800 rounded-full overflow-hidden">
                      <div
                        className={`h-full rounded-full transition-all duration-500 ${
                          isCompleted ? 'bg-emerald-400' : task.status === 'OVERDUE' ? 'bg-rose-500' : 'bg-blue-500'
                        }`}
                        style={{ width: `${task.progress}%` }}
                      />
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

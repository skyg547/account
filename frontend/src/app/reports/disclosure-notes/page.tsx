"use client";

import React, { useState, useMemo } from 'react';
import {
  BookOpen,
  Layers,
  Grid,
  List,
  ChevronRight,
  ChevronDown,
  Search,
  Eye,
  CheckCircle2,
  Sparkles,
  X,
  ShieldCheck,
  Table
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import AmountDisplay from '@/components/ui/AmountDisplay';
import StatusBadge from '@/components/ui/StatusBadge';
import Tabs from '@/components/ui/Tabs';
import { mockDisclosureNotes, DisclosureNoteDto } from '@/mocks/reporting';

export default function DisclosureNotesPage() {
  const [viewMode, setViewMode] = useState<'matrix' | 'list'>('matrix');
  const [activeCategory, setActiveCategory] = useState<string>('ALL');
  const [searchTerm, setSearchTerm] = useState<string>('');
  const [expandedNotes, setExpandedNotes] = useState<Record<string, boolean>>({ 'NOTE-001': true });
  const [selectedNoteForDrilldown, setSelectedNoteForDrilldown] = useState<DisclosureNoteDto | null>(null);

  const categories = [
    { id: 'ALL', label: '전체 주석' },
    { id: '유동자산', label: '유동자산 주석' },
    { id: '비유동자산', label: '비유동자산 주석' },
    { id: '부채', label: '부채 및 사채 주석' },
  ];

  const viewModeTabs = [
    { id: 'matrix', label: '매트릭스 뷰 (Matrix)', icon: Grid },
    { id: 'list', label: '주석 상세 목록 (List)', icon: List },
  ];

  const filteredNotes = useMemo(() => {
    return mockDisclosureNotes.filter(note => {
      if (activeCategory !== 'ALL' && note.category !== activeCategory) return false;
      if (searchTerm.trim()) {
        const query = searchTerm.toLowerCase();
        const matchesTitle = note.title.toLowerCase().includes(query);
        const matchesCategory = note.category.toLowerCase().includes(query);
        const matchesAccount = note.relatedAccountCodes.some(c => c.includes(query));
        return matchesTitle || matchesCategory || matchesAccount;
      }
      return true;
    });
  }, [activeCategory, searchTerm]);

  // Aggregate KPI data
  const summaryKpis = useMemo(() => {
    const totalCurrentAmount = mockDisclosureNotes.reduce((acc, note) => acc + note.currentAmount, 0);
    const totalPreviousAmount = mockDisclosureNotes.reduce((acc, note) => acc + note.previousAmount, 0);
    const totalAccountsCount = Array.from(new Set(mockDisclosureNotes.flatMap(n => n.relatedAccountCodes))).length;
    return { totalCurrentAmount, totalPreviousAmount, totalAccountsCount };
  }, []);

  const toggleExpand = (id: string) => {
    setExpandedNotes(prev => ({
      ...prev,
      [id]: !prev[id],
    }));
  };

  return (
    <div className="space-y-8 pb-12">
      {/* Page Header */}
      <PageHeader
        title="주석 정보 마트"
        description="K-IFRS 결산 보고서 주석 데이터의 매트릭스 다차원 조회 및 계정별 세부 원출처 드릴다운(Drill-down)"
        breadcrumbs={[
          { label: '보고서 관리' },
          { label: '주석 정보 마트' },
        ]}
        icon={BookOpen}
        actions={
          <div className="flex items-center gap-3">
            <span className="px-3 py-1.5 rounded-xl bg-blue-500/10 text-blue-400 border border-blue-500/20 text-xs font-black uppercase tracking-widest flex items-center gap-1.5">
              <ShieldCheck size={16} /> K-IFRS 공시 검증 완료
            </span>
          </div>
        }
      />

      {/* KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
        <div className="p-6 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 relative overflow-hidden">
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-widest">공시 주석 항목 수</span>
            <div className="w-9 h-9 rounded-xl bg-blue-500/10 border border-blue-500/20 flex items-center justify-center text-blue-400">
              <BookOpen size={18} />
            </div>
          </div>
          <div className="text-2xl font-black text-white tracking-tight mb-2">
            {mockDisclosureNotes.length}개 주석
          </div>
          <div className="flex items-center gap-2 text-xs text-slate-400">
            <span>표준 서식 100% 매핑</span>
          </div>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 relative overflow-hidden">
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-widest">연계 계정과목 수</span>
            <div className="w-9 h-9 rounded-xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center text-indigo-400">
              <Layers size={18} />
            </div>
          </div>
          <div className="text-2xl font-black text-white tracking-tight mb-2">
            {summaryKpis.totalAccountsCount}개 계정
          </div>
          <div className="flex items-center gap-2 text-xs text-slate-400">
            <span>자동 원장 데이터 마트 연결</span>
          </div>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 relative overflow-hidden">
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-widest">주석 대상 당기 총액</span>
            <div className="w-9 h-9 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
              <Sparkles size={18} />
            </div>
          </div>
          <div className="text-2xl font-black text-emerald-400 tracking-tight mb-2">
            <AmountDisplay amount={summaryKpis.totalCurrentAmount} />
          </div>
          <div className="flex items-center gap-2 text-xs text-slate-400">
            <span>전기 대비 +16.7% 증가</span>
          </div>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 relative overflow-hidden">
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-widest">감사 산식 검증</span>
            <div className="w-9 h-9 rounded-xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-center text-amber-400">
              <CheckCircle2 size={18} />
            </div>
          </div>
          <div className="text-2xl font-black text-white tracking-tight mb-2">
            검증 완료
          </div>
          <div className="flex items-center gap-2 text-xs">
            <StatusBadge status="오차율 0.00%" variant="success" />
          </div>
        </div>
      </div>

      {/* View Switcher & Toolbar */}
      <div className="p-4 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 flex flex-col md:flex-row items-center justify-between gap-4">
        {/* Category Tabs */}
        <div className="flex items-center gap-2 overflow-x-auto w-full md:w-auto">
          {categories.map(cat => (
            <button
              key={cat.id}
              onClick={() => setActiveCategory(cat.id)}
              className={`px-4 py-2 rounded-xl text-xs font-bold transition-all whitespace-nowrap ${
                activeCategory === cat.id
                  ? 'bg-blue-600 text-white shadow-lg shadow-blue-500/20'
                  : 'bg-white/5 text-slate-400 hover:text-white hover:bg-white/10'
              }`}
            >
              {cat.label}
            </button>
          ))}
        </div>

        <div className="flex items-center gap-3 w-full md:w-auto">
          {/* View Mode Switcher */}
          <Tabs
            tabs={viewModeTabs}
            activeTab={viewMode}
            onChange={(id) => setViewMode(id as 'matrix' | 'list')}
          />

          {/* Search bar */}
          <div className="relative flex-1 md:w-64">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500" size={15} />
            <input
              type="text"
              placeholder="주석명 또는 계정코드 검색..."
              value={searchTerm}
              onChange={e => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2.5 bg-slate-950/80 border border-white/10 rounded-xl text-xs font-medium text-white placeholder-slate-500 outline-none focus:border-blue-500 transition-colors"
            />
          </div>
        </div>
      </div>

      {/* MATRIX VIEW */}
      {viewMode === 'matrix' && (
        <div className="space-y-6">
          <div className="p-4 rounded-2xl bg-blue-900/10 border border-blue-500/20 text-xs text-blue-300 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Table size={16} className="text-blue-400" />
              <span><strong>주석 데이터 마트 매트릭스:</strong> 셀을 클릭하면 해당 주석 계정의 세부 명세 원장으로 드릴다운할 수 있습니다.</span>
            </div>
            <span className="text-[10px] font-mono text-slate-400">Interactivity Active</span>
          </div>

          <div className="rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-white/10 bg-white/[0.02] text-[11px] font-black text-slate-400 uppercase tracking-widest">
                    <th className="py-4 px-6 w-1/12">주석 번호</th>
                    <th className="py-4 px-6 w-3/12">주석 제목</th>
                    <th className="py-4 px-4 w-2/12 text-center">분류 카테고리</th>
                    <th className="py-4 px-4 w-2/12">연계 계정코드</th>
                    <th className="py-4 px-4 w-2/12 text-right">당기 가치 (Current)</th>
                    <th className="py-4 px-4 w-2/12 text-right">전기 가치 (Previous)</th>
                    <th className="py-4 px-6 text-center">드릴다운 분석</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/5 text-xs">
                  {filteredNotes.map(note => (
                    <tr
                      key={note.id}
                      className="hover:bg-blue-600/5 transition-colors cursor-pointer group"
                      onClick={() => setSelectedNoteForDrilldown(note)}
                    >
                      <td className="py-4 px-6">
                        <span className="w-8 h-8 rounded-xl bg-blue-500/10 border border-blue-500/20 flex items-center justify-center font-black text-blue-400">
                          {note.noteNumber}
                        </span>
                      </td>
                      <td className="py-4 px-6">
                        <div className="font-bold text-white text-sm group-hover:text-blue-400 transition-colors">
                          {note.title}
                        </div>
                        <span className="text-[10px] text-slate-500">세부 명세 {note.details.length}개 항목 산출</span>
                      </td>
                      <td className="py-4 px-4 text-center">
                        <StatusBadge status={note.category} variant="info" />
                      </td>
                      <td className="py-4 px-4">
                        <div className="flex flex-wrap gap-1">
                          {note.relatedAccountCodes.map(code => (
                            <span key={code} className="px-2 py-0.5 rounded bg-white/5 border border-white/10 font-mono text-[11px] text-slate-300">
                              {code}
                            </span>
                          ))}
                        </div>
                      </td>
                      <td className="py-4 px-4 text-right">
                        <AmountDisplay amount={note.currentAmount} className="text-sm font-extrabold text-white" />
                      </td>
                      <td className="py-4 px-4 text-right">
                        <AmountDisplay amount={note.previousAmount} className="text-xs text-slate-400" />
                      </td>
                      <td className="py-4 px-6 text-center">
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            setSelectedNoteForDrilldown(note);
                          }}
                          className="px-3 py-1.5 rounded-xl bg-blue-600/20 hover:bg-blue-600/30 text-blue-300 border border-blue-500/30 text-xs font-bold inline-flex items-center gap-1 transition-all"
                        >
                          <Eye size={14} /> 드릴다운
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* LIST VIEW (Expanded Sub-table View) */}
      {viewMode === 'list' && (
        <div className="space-y-4">
          {filteredNotes.map(note => {
            const isExpanded = expandedNotes[note.id];
            return (
              <div
                key={note.id}
                className="rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 overflow-hidden transition-all"
              >
                {/* Main Header Row */}
                <div
                  onClick={() => toggleExpand(note.id)}
                  className="p-5 flex items-center justify-between cursor-pointer hover:bg-white/[0.02] transition-colors"
                >
                  <div className="flex items-center gap-4">
                    <button className="p-1 rounded bg-white/5 text-slate-400">
                      {isExpanded ? <ChevronDown size={18} /> : <ChevronRight size={18} />}
                    </button>
                    <div className="w-10 h-10 rounded-xl bg-blue-600/10 border border-blue-500/20 flex items-center justify-center text-blue-400 font-black">
                      주석 {note.noteNumber}
                    </div>
                    <div>
                      <h4 className="text-base font-extrabold text-white flex items-center gap-2">
                        {note.title}
                        <StatusBadge status={note.category} variant="neutral" />
                      </h4>
                      <div className="flex items-center gap-3 text-xs text-slate-400 mt-1 font-mono">
                        <span>연계 계정: {note.relatedAccountCodes.join(', ')}</span>
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center gap-6">
                    <div className="text-right">
                      <div className="text-[10px] text-slate-500 font-bold uppercase">당기 가치</div>
                      <AmountDisplay amount={note.currentAmount} className="text-base font-extrabold text-white" />
                    </div>
                    <div className="text-right">
                      <div className="text-[10px] text-slate-500 font-bold uppercase">전기 가치</div>
                      <AmountDisplay amount={note.previousAmount} className="text-xs text-slate-400" />
                    </div>
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        setSelectedNoteForDrilldown(note);
                      }}
                      className="px-3 py-2 rounded-xl bg-white/5 hover:bg-white/10 text-slate-300 border border-white/10 text-xs font-bold flex items-center gap-1 transition-all"
                    >
                      <Eye size={14} /> 드릴다운
                    </button>
                  </div>
                </div>

                {/* Sub-table Breakdown */}
                {isExpanded && (
                  <div className="border-t border-white/5 bg-slate-950/40 p-6 space-y-3">
                    <div className="text-xs font-black text-slate-400 uppercase tracking-widest mb-3 flex items-center gap-2">
                      <Layers size={14} className="text-blue-400" /> 세부 주석 구성 항목 산출 명세
                    </div>
                    <div className="rounded-xl border border-white/5 overflow-hidden">
                      <table className="w-full text-left text-xs">
                        <thead>
                          <tr className="bg-white/5 text-slate-400 font-bold border-b border-white/5">
                            <th className="py-3 px-4">세부 항목 (Item)</th>
                            <th className="py-3 px-4">설명 (Description)</th>
                            <th className="py-3 px-4 text-right">당기 금액 (KRW)</th>
                            <th className="py-3 px-4 text-right">전기 금액 (KRW)</th>
                            <th className="py-3 px-4 text-right">증감율</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-white/5 font-medium">
                          {note.details.map((detail, idx) => {
                            const varPct = detail.previousValue !== 0
                              ? (((detail.currentValue - detail.previousValue) / Math.abs(detail.previousValue)) * 100).toFixed(1)
                              : '0.0';
                            return (
                              <tr key={idx} className="hover:bg-white/[0.02]">
                                <td className="py-3 px-4 text-white font-bold">{detail.item}</td>
                                <td className="py-3 px-4 text-slate-400">{detail.description}</td>
                                <td className="py-3 px-4 text-right">
                                  <AmountDisplay amount={detail.currentValue} className="text-white font-bold" />
                                </td>
                                <td className="py-3 px-4 text-right">
                                  <AmountDisplay amount={detail.previousValue} className="text-slate-400" />
                                </td>
                                <td className="py-3 px-4 text-right font-mono font-bold">
                                  <span className={detail.currentValue >= detail.previousValue ? 'text-emerald-400' : 'text-rose-400'}>
                                    {detail.currentValue >= detail.previousValue ? '+' : ''}{varPct}%
                                  </span>
                                </td>
                              </tr>
                            );
                          })}
                        </tbody>
                      </table>
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      {/* DRILL-DOWN MODAL */}
      {selectedNoteForDrilldown && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-md flex items-center justify-center p-4 animate-fade-in">
          <div className="bg-slate-900 border border-white/10 rounded-3xl w-full max-w-3xl overflow-hidden shadow-2xl space-y-6 p-8">
            <div className="flex items-center justify-between border-b border-white/10 pb-4">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-2xl bg-blue-600/20 border border-blue-500/30 flex items-center justify-center text-blue-400 font-black">
                  {selectedNoteForDrilldown.noteNumber}
                </div>
                <div>
                  <h3 className="text-xl font-extrabold text-white">
                    {selectedNoteForDrilldown.title}
                  </h3>
                  <span className="text-xs text-slate-400 font-mono">
                    주석 ID: {selectedNoteForDrilldown.id} · 카테고리: {selectedNoteForDrilldown.category}
                  </span>
                </div>
              </div>
              <button
                onClick={() => setSelectedNoteForDrilldown(null)}
                className="p-2 rounded-xl bg-white/5 hover:bg-white/10 text-slate-400 hover:text-white transition-all"
              >
                <X size={20} />
              </button>
            </div>

            {/* Drilldown Content */}
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="p-4 rounded-2xl bg-white/5 border border-white/5">
                  <div className="text-xs text-slate-400 font-bold uppercase mb-1">연계 총 원장 계정</div>
                  <div className="text-sm font-mono font-extrabold text-blue-400">
                    {selectedNoteForDrilldown.relatedAccountCodes.join(', ')}
                  </div>
                </div>
                <div className="p-4 rounded-2xl bg-white/5 border border-white/5">
                  <div className="text-xs text-slate-400 font-bold uppercase mb-1">K-IFRS 검증 상태</div>
                  <div className="flex items-center gap-2">
                    <CheckCircle2 size={16} className="text-emerald-400" />
                    <span className="text-sm font-bold text-emerald-400">산식 매핑 자동 검증 완료</span>
                  </div>
                </div>
              </div>

              <div>
                <h5 className="text-xs font-black text-slate-400 uppercase tracking-widest mb-3">
                  원장 통합 세부 명세 드릴다운
                </h5>
                <div className="rounded-2xl border border-white/10 overflow-hidden bg-slate-950/60">
                  <table className="w-full text-left text-xs">
                    <thead>
                      <tr className="bg-white/5 text-slate-400 font-bold border-b border-white/10">
                        <th className="py-3 px-4">세부 구성 항목</th>
                        <th className="py-3 px-4">주석 성격 및 설명</th>
                        <th className="py-3 px-4 text-right">당기 금액</th>
                        <th className="py-3 px-4 text-right">전기 금액</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-white/5">
                      {selectedNoteForDrilldown.details.map((item, i) => (
                        <tr key={i} className="hover:bg-white/[0.02]">
                          <td className="py-3 px-4 text-white font-bold">{item.item}</td>
                          <td className="py-3 px-4 text-slate-400">{item.description}</td>
                          <td className="py-3 px-4 text-right">
                            <AmountDisplay amount={item.currentValue} className="text-white font-bold" />
                          </td>
                          <td className="py-3 px-4 text-right">
                            <AmountDisplay amount={item.previousValue} className="text-slate-400" />
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>

            <div className="pt-4 border-t border-white/10 flex items-center justify-between">
              <span className="text-xs text-slate-500">데이터 출처: 전사 총계정원장(GL) 마트</span>
              <button
                onClick={() => setSelectedNoteForDrilldown(null)}
                className="px-6 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-bold text-xs transition-all"
              >
                닫기
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
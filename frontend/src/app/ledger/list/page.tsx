'use client';

import React, { useState } from 'react';
import { 
  ListFilter, 
  Search, 
  Download, 
  XCircle,
  ChevronDown,
  ChevronUp,
  FileSpreadsheet
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import AmountDisplay from '@/components/ui/AmountDisplay';
import Tabs, { TabItem } from '@/components/ui/Tabs';
import EmptyState from '@/components/ui/EmptyState';
import { mockJournals } from '@/mocks/ledger';
import { mockAccounts, mockPartners } from '@/mocks/master';

const statusTabs: TabItem[] = [
  { id: 'ALL', label: '전체 전표' },
  { id: 'POSTED', label: '확정' },
  { id: 'APPROVED', label: '승인완료' },
  { id: 'PENDING', label: '검토대기' },
  { id: 'DRAFT', label: '작성중' },
  { id: 'REJECTED', label: '반려' },
];

const getStatusVariant = (status: string) => {
  switch (status) {
    case 'POSTED':
      return { label: '확정', variant: 'success' as const };
    case 'APPROVED':
      return { label: '승인완료', variant: 'info' as const };
    case 'PENDING':
      return { label: '검토대기', variant: 'warning' as const };
    case 'DRAFT':
      return { label: '작성중', variant: 'neutral' as const };
    case 'REJECTED':
      return { label: '반려', variant: 'error' as const };
    default:
      return { label: status, variant: 'neutral' as const };
  }
};

export default function JournalListPage() {
  const [activeTab, setActiveTab] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [startDate, setStartDate] = useState<string>('');
  const [endDate, setEndDate] = useState<string>('');
  const [expandedId, setExpandedId] = useState<string | null>(null);

  const filteredJournals = mockJournals.filter((journal) => {
    // Tab filter
    if (activeTab !== 'ALL' && journal.status !== activeTab) {
      return false;
    }
    // Search filter
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      const matchId = journal.id.toLowerCase().includes(q);
      const matchLineDesc = journal.lines.some((l) => l.desc.toLowerCase().includes(q));
      if (!matchId && !matchLineDesc) return false;
    }
    // Date filter
    if (startDate && journal.date < startDate) return false;
    if (endDate && journal.date > endDate) return false;

    return true;
  });

  const toggleExpand = (id: string) => {
    setExpandedId(expandedId === id ? null : id);
  };

  const getAccountName = (code: string) => {
    const acc = mockAccounts.find((a) => a.code === code);
    return acc ? acc.name : code;
  };

  const getPartnerName = (partnerId?: string) => {
    if (!partnerId) return '-';
    const pt = mockPartners.find((p) => p.id === partnerId);
    return pt ? pt.name : partnerId;
  };

  return (
    <div className="space-y-8 pb-12">
      {/* Page Header */}
      <PageHeader
        title="전표 조회"
        description="등록된 전표 내역을 검색, 필터링 및 라인별 분개 내역을 검토합니다."
        breadcrumbs={[
          { label: '재무/회계' },
          { label: '전표관리' },
          { label: '전표 조회' }
        ]}
        icon={ListFilter}
        actions={
          <button className="flex items-center gap-2 px-4 py-2.5 rounded-2xl bg-emerald-600/20 hover:bg-emerald-600/30 text-emerald-400 border border-emerald-500/30 font-black text-xs transition-all shadow-lg">
            <Download size={14} /> 엑셀 내보내기
          </button>
        }
      />

      {/* Tabs */}
      <Tabs tabs={statusTabs} activeTab={activeTab} onChange={setActiveTab} />

      {/* Search & Filter Bar */}
      <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-6 shadow-xl space-y-4">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div className="md:col-span-2 relative">
            <Search className="absolute left-4 top-3.5 text-slate-500" size={18} />
            <input
              type="text"
              placeholder="전표 번호 또는 적요 키워드 검색..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full bg-slate-950/60 border border-white/10 rounded-2xl pl-11 pr-4 py-3 text-white text-sm font-medium focus:outline-none focus:border-blue-500 transition-colors placeholder:text-slate-600"
            />
            {searchQuery && (
              <button
                onClick={() => setSearchQuery('')}
                className="absolute right-3 top-3 text-slate-500 hover:text-white"
              >
                <XCircle size={18} />
              </button>
            )}
          </div>

          <div>
            <input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="w-full bg-slate-950/60 border border-white/10 rounded-2xl px-4 py-3 text-white text-sm font-medium focus:outline-none focus:border-blue-500 transition-colors"
            />
          </div>

          <div>
            <input
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              className="w-full bg-slate-950/60 border border-white/10 rounded-2xl px-4 py-3 text-white text-sm font-medium focus:outline-none focus:border-blue-500 transition-colors"
            />
          </div>
        </div>
      </div>

      {/* Data Table / Empty State */}
      <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-7 shadow-xl overflow-hidden">
        {filteredJournals.length === 0 ? (
          <EmptyState
            icon={FileSpreadsheet}
            title="조건에 맞는 전표가 없습니다"
            description="검색어 또는 기간 필터 조건을 변경하여 다시 시도해 주세요."
            action={
              <button
                onClick={() => {
                  setActiveTab('ALL');
                  setSearchQuery('');
                  setStartDate('');
                  setEndDate('');
                }}
                className="px-4 py-2 rounded-xl bg-blue-600/20 text-blue-400 border border-blue-500/30 text-xs font-bold"
              >
                필터 초기화
              </button>
            }
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-white/10 text-[11px] font-black uppercase tracking-wider text-slate-400">
                  <th className="py-3 px-4 min-w-[140px]">전표 번호</th>
                  <th className="py-3 px-4 min-w-[110px]">전표 일자</th>
                  <th className="py-3 px-4 min-w-[110px]">상태</th>
                  <th className="py-3 px-4 min-w-[240px]">주요 적요</th>
                  <th className="py-3 px-4 min-w-[140px] text-right">차변 합계</th>
                  <th className="py-3 px-4 min-w-[140px] text-right">대변 합계</th>
                  <th className="py-3 px-4 min-w-[90px] text-center">라인수</th>
                  <th className="py-3 px-4 min-w-[100px] text-center">상세보기</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {filteredJournals.map((journal) => {
                  const statusInfo = getStatusVariant(journal.status);
                  const totalDebit = journal.lines.reduce((s, l) => s + l.debit, 0);
                  const totalCredit = journal.lines.reduce((s, l) => s + l.credit, 0);
                  const isExpanded = expandedId === journal.id;

                  return (
                    <React.Fragment key={journal.id}>
                      <tr className="group hover:bg-white/[0.03] transition-colors cursor-pointer" onClick={() => toggleExpand(journal.id)}>
                        <td className="py-4 px-4 font-mono text-xs font-black text-blue-400">
                          {journal.id}
                        </td>
                        <td className="py-4 px-4 text-xs font-medium text-slate-300">
                          {journal.date}
                        </td>
                        <td className="py-4 px-4">
                          <StatusBadge status={statusInfo.label} variant={statusInfo.variant} />
                        </td>
                        <td className="py-4 px-4 text-xs font-bold text-white max-w-xs truncate">
                          {journal.lines[0]?.desc || '-'}
                        </td>
                        <td className="py-4 px-4 text-right">
                          <AmountDisplay amount={totalDebit} className="text-emerald-400 text-sm" />
                        </td>
                        <td className="py-4 px-4 text-right">
                          <AmountDisplay amount={totalCredit} className="text-blue-400 text-sm" />
                        </td>
                        <td className="py-4 px-4 text-center">
                          <span className="inline-block px-2 py-0.5 rounded-full bg-white/5 text-xs font-mono font-bold text-slate-400 border border-white/5">
                            {journal.lines.length}건
                          </span>
                        </td>
                        <td className="py-4 px-4 text-center">
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              toggleExpand(journal.id);
                            }}
                            className="p-1.5 rounded-xl bg-white/5 hover:bg-white/10 text-slate-300 transition-colors"
                          >
                            {isExpanded ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
                          </button>
                        </td>
                      </tr>

                      {/* Expanded Line Items Detail */}
                      {isExpanded && (
                        <tr className="bg-slate-950/70 border-y border-blue-500/20">
                          <td colSpan={8} className="p-6">
                            <div className="space-y-3">
                              <div className="flex items-center justify-between text-xs font-black text-slate-400 uppercase tracking-widest pb-2 border-b border-white/10">
                                <span>분개 세부 라인 내역 ({journal.id})</span>
                                <span>{journal.lines.length} Line Items</span>
                              </div>
                              <div className="overflow-x-auto">
                                <table className="w-full text-left text-xs">
                                  <thead>
                                    <tr className="text-slate-500 border-b border-white/5">
                                      <th className="py-2 px-3">계정코드</th>
                                      <th className="py-2 px-3">계정과목명</th>
                                      <th className="py-2 px-3">거래처</th>
                                      <th className="py-2 px-3">적요</th>
                                      <th className="py-2 px-3 text-right">차변 (Debit)</th>
                                      <th className="py-2 px-3 text-right">대변 (Credit)</th>
                                    </tr>
                                  </thead>
                                  <tbody className="divide-y divide-white/5">
                                    {journal.lines.map((line, idx) => (
                                      <tr key={idx} className="hover:bg-white/5">
                                        <td className="py-2.5 px-3 font-mono text-blue-400">{line.accountCode}</td>
                                        <td className="py-2.5 px-3 font-bold text-white">{getAccountName(line.accountCode)}</td>
                                        <td className="py-2.5 px-3 text-slate-300">{getPartnerName(line.partnerId)}</td>
                                        <td className="py-2.5 px-3 text-slate-300">{line.desc}</td>
                                        <td className="py-2.5 px-3 text-right text-emerald-400 font-mono font-bold">
                                          {line.debit > 0 ? `₩${line.debit.toLocaleString()}` : '-'}
                                        </td>
                                        <td className="py-2.5 px-3 text-right text-blue-400 font-mono font-bold">
                                          {line.credit > 0 ? `₩${line.credit.toLocaleString()}` : '-'}
                                        </td>
                                      </tr>
                                    ))}
                                  </tbody>
                                </table>
                              </div>
                            </div>
                          </td>
                        </tr>
                      )}
                    </React.Fragment>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

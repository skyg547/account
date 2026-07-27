"use client";

import React, { useState } from 'react';
import { 
  FolderTree, 
  Search, 
  ChevronRight, 
  ChevronDown, 
  Folder, 
  FileText, 
  Plus, 
  Edit3, 
  CheckCircle2, 
  XCircle, 
  Layers, 
  Scale, 
  Info,
  ShieldCheck
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import EmptyState from '@/components/ui/EmptyState';
import { mockAccounts } from '@/mocks/master';

export default function AccountTreePage() {
  const [selectedCode, setSelectedCode] = useState<string | null>('1110100');
  const [searchTerm, setSearchTerm] = useState('');
  const [expandedCodes, setExpandedCodes] = useState<Record<string, boolean>>({
    '1000000': true,
    '1100000': true,
    '1110000': true,
    '2000000': true,
    '2100000': true,
  });

  const toggleExpand = (code: string) => {
    setExpandedCodes((prev) => ({
      ...prev,
      [code]: !prev[code],
    }));
  };

  const expandAll = () => {
    const all: Record<string, boolean> = {};
    mockAccounts.forEach((acc) => {
      all[acc.code] = true;
    });
    setExpandedCodes(all);
  };

  const collapseAll = () => {
    setExpandedCodes({});
  };

  const filteredAccounts = mockAccounts.filter(
    (acc) =>
      acc.name.includes(searchTerm) || acc.code.includes(searchTerm)
  );

  const selectedAccount = mockAccounts.find((acc) => acc.code === selectedCode);

  return (
    <div className="space-y-8">
      <PageHeader
        title="계정과목 체계"
        description="전사 표준 계정과목 트리"
        breadcrumbs={[
          { label: '기준정보 마스터' },
          { label: '계정과목 관리' },
          { label: '계정과목 체계' },
        ]}
        icon={FolderTree}
        actions={
          <div className="flex items-center gap-3">
            <button
              onClick={expandAll}
              className="px-4 py-2.5 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl text-xs font-bold text-slate-300 transition-all"
            >
              전체 펼치기
            </button>
            <button
              onClick={collapseAll}
              className="px-4 py-2.5 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl text-xs font-bold text-slate-300 transition-all"
            >
              전체 접기
            </button>
          </div>
        }
      />

      {/* Split View Container */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* Left Side: Account Tree */}
        <div className="lg:col-span-5 glass-panel p-6 rounded-[2.5rem] border border-white/10 flex flex-col h-[700px]">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-black text-white italic tracking-tight flex items-center gap-2">
              <Layers size={18} className="text-blue-400" />
              계정과목 트리지도
            </h3>
            <span className="text-xs font-bold text-slate-500 bg-white/5 px-2.5 py-1 rounded-full border border-white/5">
              총 {mockAccounts.length}개 계정
            </span>
          </div>

          {/* Search Box */}
          <div className="relative mb-4">
            <Search
              size={16}
              className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500"
            />
            <input
              type="text"
              placeholder="계정 코드 또는 계정명 검색..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full bg-slate-950/80 border border-white/10 focus:border-blue-500/50 rounded-xl py-2.5 pl-11 pr-4 text-xs font-bold text-white outline-none transition-all placeholder:text-slate-600"
            />
          </div>

          {/* Hierarchical Tree List */}
          <div className="flex-1 overflow-y-auto space-y-1 pr-2 custom-scrollbar">
            {filteredAccounts.map((account) => {
              const isSelected = selectedCode === account.code;
              const isGroup = account.category === 'GROUP';
              const isExpanded = !!expandedCodes[account.code];
              const paddingLeft = (account.level - 1) * 20 + 12;

              return (
                <div
                  key={account.code}
                  style={{ paddingLeft: `${paddingLeft}px` }}
                  onClick={() => setSelectedCode(account.code)}
                  className={`group flex items-center justify-between p-2.5 rounded-xl cursor-pointer transition-all duration-200 ${
                    isSelected
                      ? 'bg-blue-600/20 border border-blue-500/40 text-white shadow-lg shadow-blue-500/10'
                      : 'hover:bg-white/5 border border-transparent text-slate-300'
                  }`}
                >
                  <div className="flex items-center gap-2 min-w-0">
                    {isGroup ? (
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          toggleExpand(account.code);
                        }}
                        className="p-1 text-slate-400 hover:text-white transition-colors"
                      >
                        {isExpanded ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
                      </button>
                    ) : (
                      <span className="w-3.5 h-3.5 inline-block" />
                    )}

                    {isGroup ? (
                      <Folder
                        size={16}
                        className={isSelected ? 'text-blue-400' : 'text-amber-400/80'}
                      />
                    ) : (
                      <FileText
                        size={15}
                        className={isSelected ? 'text-blue-400' : 'text-slate-500'}
                      />
                    )}

                    <span className="font-mono text-xs font-bold text-slate-400">
                      [{account.code}]
                    </span>
                    <span
                      className={`text-xs font-bold truncate ${
                        isSelected ? 'text-white' : isGroup ? 'text-slate-200' : 'text-slate-400'
                      }`}
                    >
                      {account.name}
                    </span>
                  </div>

                  <div className="flex items-center gap-1.5 shrink-0 ml-2">
                    <span
                      className={`px-1.5 py-0.5 rounded text-[10px] font-black ${
                        account.balanceType === 'DEBIT'
                          ? 'bg-blue-500/10 text-blue-400 border border-blue-500/20'
                          : 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                      }`}
                    >
                      {account.balanceType === 'DEBIT' ? '차변' : '대변'}
                    </span>
                    {isGroup && (
                      <span className="text-[10px] font-bold text-slate-500 bg-white/5 px-1.5 py-0.5 rounded">
                        그룹
                      </span>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Right Side: Account Details or Empty State */}
        <div className="lg:col-span-7 glass-panel p-8 rounded-[2.5rem] border border-white/10 min-h-[700px] flex flex-col justify-between">
          {selectedAccount ? (
            <div className="space-y-8">
              {/* Header card for selected account */}
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-6 rounded-2xl bg-white/[0.02] border border-white/5">
                <div className="flex items-center gap-4">
                  <div className="w-14 h-14 rounded-2xl bg-blue-600/10 border border-blue-500/20 flex items-center justify-center text-blue-400">
                    {selectedAccount.category === 'GROUP' ? <Folder size={28} /> : <FileText size={28} />}
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-mono text-sm font-black text-blue-400">
                        {selectedAccount.code}
                      </span>
                      <StatusBadge
                        status={selectedAccount.status}
                        variant={selectedAccount.status === 'ACTIVE' ? 'success' : 'neutral'}
                      />
                    </div>
                    <h2 className="text-2xl font-black text-white italic tracking-tight mt-1">
                      {selectedAccount.name}
                    </h2>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <button className="px-4 py-2 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl text-xs font-bold text-white transition-all flex items-center gap-1.5">
                    <Edit3 size={14} /> 속성 편집
                  </button>
                  <button className="px-4 py-2 bg-blue-600 hover:bg-blue-500 rounded-xl text-xs font-bold text-white transition-all shadow-lg shadow-blue-600/20 flex items-center gap-1.5">
                    <Plus size={14} /> 하위 계정 추가
                  </button>
                </div>
              </div>

              {/* Grid of Key Attributes */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="p-5 rounded-2xl bg-slate-900/60 border border-white/5 space-y-1">
                  <span className="text-[11px] font-black text-slate-500 uppercase tracking-widest">
                    계정 구분 (Category)
                  </span>
                  <div className="text-base font-bold text-white flex items-center gap-2">
                    <Layers size={16} className="text-blue-400" />
                    {selectedAccount.category === 'GROUP' ? '그룹계정 (분류)' : '세목계정 (전표입력)'}
                  </div>
                </div>

                <div className="p-5 rounded-2xl bg-slate-900/60 border border-white/5 space-y-1">
                  <span className="text-[11px] font-black text-slate-500 uppercase tracking-widest">
                    잔액 본차변 (Balance Type)
                  </span>
                  <div className="text-base font-bold text-white flex items-center gap-2">
                    <Scale size={16} className="text-emerald-400" />
                    {selectedAccount.balanceType === 'DEBIT' ? '차변 (Debit)' : '대변 (Credit)'}
                  </div>
                </div>

                <div className="p-5 rounded-2xl bg-slate-900/60 border border-white/5 space-y-1">
                  <span className="text-[11px] font-black text-slate-500 uppercase tracking-widest">
                    상위 계정 코드 (Parent Code)
                  </span>
                  <div className="text-base font-bold text-white font-mono">
                    {selectedAccount.parentCode ? selectedAccount.parentCode : '최상위 계정 (Root)'}
                  </div>
                </div>

                <div className="p-5 rounded-2xl bg-slate-900/60 border border-white/5 space-y-1">
                  <span className="text-[11px] font-black text-slate-500 uppercase tracking-widest">
                    계층 레벨 (Hierarchy Level)
                  </span>
                  <div className="text-base font-bold text-white">
                    Level {selectedAccount.level}
                  </div>
                </div>
              </div>

              {/* Extended Policy Details */}
              <div className="p-6 rounded-2xl bg-slate-900/40 border border-white/5 space-y-4">
                <h4 className="text-xs font-black uppercase tracking-widest text-slate-400 flex items-center gap-2">
                  <ShieldCheck size={14} className="text-blue-400" />
                  계정과목 통제 및 이월 설정
                </h4>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                  <div className="p-4 rounded-xl bg-white/[0.02] border border-white/5">
                    <span className="text-xs text-slate-500 font-bold block mb-1">전표 입력 가능</span>
                    <div className="flex items-center gap-1.5 text-xs font-black">
                      {selectedAccount.category === 'SUBJECT' ? (
                        <>
                          <CheckCircle2 size={14} className="text-emerald-400" />
                          <span className="text-emerald-400">허용됨</span>
                        </>
                      ) : (
                        <>
                          <XCircle size={14} className="text-rose-400" />
                          <span className="text-rose-400 font-bold">불가 (그룹계정)</span>
                        </>
                      )}
                    </div>
                  </div>

                  <div className="p-4 rounded-xl bg-white/[0.02] border border-white/5">
                    <span className="text-xs text-slate-500 font-bold block mb-1">거래처 필수 입력</span>
                    <div className="flex items-center gap-1.5 text-xs font-black text-amber-400">
                      <CheckCircle2 size={14} /> 선택 관리
                    </div>
                  </div>

                  <div className="p-4 rounded-xl bg-white/[0.02] border border-white/5">
                    <span className="text-xs text-slate-500 font-bold block mb-1">차기 이월 대상</span>
                    <div className="flex items-center gap-1.5 text-xs font-black text-emerald-400">
                      <CheckCircle2 size={14} /> 차기이월 적용
                    </div>
                  </div>
                </div>
              </div>

              {/* Quick Info Box */}
              <div className="p-4 rounded-xl bg-blue-500/10 border border-blue-500/20 text-xs font-medium text-blue-300 flex items-start gap-3">
                <Info size={16} className="text-blue-400 shrink-0 mt-0.5" />
                <span>
                  계정과목의 속성 변경 시 기존 매핑된 전표 및 관련 금융상품에 영향이 발생할 수 있습니다.
                  중요 속성 변경 시 <strong>기준정보 변경 승인 워크플로</strong>를 거치게 됩니다.
                </span>
              </div>
            </div>
          ) : (
            <EmptyState
              icon={FolderTree}
              title="계정과목을 선택하세요"
              description="좌측 트리에서 상세 정보를 조회할 계정과목을 선택해주세요."
            />
          )}

          <div className="pt-6 border-t border-white/5 flex items-center justify-between text-xs text-slate-500 font-medium">
            <span>마지막 동기화 시각: 2026-07-28 00:00:00</span>
            <span>시스템 마스터 v2.4</span>
          </div>
        </div>
      </div>
    </div>
  );
}

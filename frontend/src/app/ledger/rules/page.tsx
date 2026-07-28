'use client';

import React, { useState } from 'react';
import { 
  GitMerge, 
  Plus, 
  Search, 
  Check, 
  X, 
  Trash2, 
  Sparkles,
  Zap,
  Sliders
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import EmptyState from '@/components/ui/EmptyState';
import { mockAccounts } from '@/mocks/master';

interface AutoRule {
  id: string;
  name: string;
  module: 'BANKING' | 'CARD' | 'TAX_INVOICE';
  keyword: string;
  debitAccountCode: string;
  creditAccountCode: string;
  priority: number;
  isActive: boolean;
}

const mockRules: AutoRule[] = [
  {
    id: 'RUL-001',
    name: '커피빈 및 음료 지출 자동 분개',
    module: 'CARD',
    keyword: '커피빈',
    debitAccountCode: '5000000',
    creditAccountCode: '1110200',
    priority: 1,
    isActive: true,
  },
  {
    id: 'RUL-002',
    name: '클라우드 서버 사용료 분개',
    module: 'CARD',
    keyword: 'AWS / CLOUD',
    debitAccountCode: '5000000',
    creditAccountCode: '1110200',
    priority: 2,
    isActive: true,
  },
  {
    id: 'RUL-003',
    name: '법인 이자수입 자동 계상',
    module: 'BANKING',
    keyword: '예금이치이자',
    debitAccountCode: '1110200',
    creditAccountCode: '4000000',
    priority: 3,
    isActive: true,
  },
  {
    id: 'RUL-004',
    name: '전자세금계산서 매출 대금 매핑',
    module: 'TAX_INVOICE',
    keyword: '제품공급매출',
    debitAccountCode: '1110200',
    creditAccountCode: '4000000',
    priority: 4,
    isActive: false,
  },
];

export default function AutoJournalRulesPage() {
  const [rules, setRules] = useState<AutoRule[]>(mockRules);
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [showAddModal, setShowAddModal] = useState<boolean>(false);

  // New Rule State
  const [newRuleName, setNewRuleName] = useState('');
  const [newModule, setNewModule] = useState<'BANKING' | 'CARD' | 'TAX_INVOICE'>('CARD');
  const [newKeyword, setNewKeyword] = useState('');
  const [newDebitAcc, setNewDebitAcc] = useState('5000000');
  const [newCreditAcc, setNewCreditAcc] = useState('1110200');

  const handleToggleRule = (id: string) => {
    setRules((prev) =>
      prev.map((r) => (r.id === id ? { ...r, isActive: !r.isActive } : r))
    );
  };

  const handleDeleteRule = (id: string) => {
    if (confirm('해당 자동 분개 규칙을 삭제하시겠습니까?')) {
      setRules((prev) => prev.filter((r) => r.id !== id));
    }
  };

  const handleAddRule = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newRuleName || !newKeyword) {
      alert('규칙명과 키워드를 입력해 주세요.');
      return;
    }
    const created: AutoRule = {
      id: `RUL-00${rules.length + 1}`,
      name: newRuleName,
      module: newModule,
      keyword: newKeyword,
      debitAccountCode: newDebitAcc,
      creditAccountCode: newCreditAcc,
      priority: rules.length + 1,
      isActive: true,
    };
    setRules([created, ...rules]);
    setShowAddModal(false);
    setNewRuleName('');
    setNewKeyword('');
  };

  const getAccountName = (code: string) => {
    const acc = mockAccounts.find((a) => a.code === code);
    return acc ? `[${acc.code}] ${acc.name}` : code;
  };

  const filteredRules = rules.filter((r) => {
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      return (
        r.name.toLowerCase().includes(q) ||
        r.keyword.toLowerCase().includes(q) ||
        r.id.toLowerCase().includes(q)
      );
    }
    return true;
  });

  return (
    <div className="space-y-8 pb-12">
      {/* Page Header */}
      <PageHeader
        title="자동 분개 규칙"
        description="뱅킹, 카드, 전자세금계산서 등 거래 키워드에 맞춰 차/대변 계정과목을 자동 분개하는 규칙 관리"
        breadcrumbs={[
          { label: '재무/회계' },
          { label: '설정' },
          { label: '자동 분개 규칙' }
        ]}
        icon={GitMerge}
        actions={
          <button
            onClick={() => setShowAddModal(true)}
            className="flex items-center gap-2 px-5 py-2.5 rounded-2xl bg-blue-600 hover:bg-blue-500 text-white font-black text-xs transition-all shadow-lg shadow-blue-600/30"
          >
            <Plus size={16} /> 신규 규칙 등록
          </button>
        }
      />

      {/* Filter Bar */}
      <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-6 shadow-xl flex flex-col md:flex-row items-center justify-between gap-4">
        <div className="relative w-full md:w-96">
          <Search className="absolute left-4 top-3.5 text-slate-500" size={18} />
          <input
            type="text"
            placeholder="규칙명, 키워드 검색..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full bg-slate-950/60 border border-white/10 rounded-2xl pl-11 pr-4 py-3 text-white text-sm font-medium focus:outline-none focus:border-blue-500 transition-colors"
          />
        </div>

        <div className="flex items-center gap-3 text-xs font-bold text-slate-400">
          <Zap size={16} className="text-amber-400" />
          <span>활성화된 자동 규칙: <strong className="text-white font-black">{rules.filter(r => r.isActive).length}개</strong></span>
        </div>
      </div>

      {/* Rules Data Table */}
      <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-7 shadow-xl overflow-hidden">
        {filteredRules.length === 0 ? (
          <EmptyState
            icon={Sliders}
            title="등록된 자동 분개 규칙이 없습니다"
            description="새로운 매핑 키워드 및 계정 자동 분개 규칙을 추가해 보세요."
            action={
              <button
                onClick={() => setShowAddModal(true)}
                className="px-4 py-2 rounded-xl bg-blue-600 text-white font-bold text-xs"
              >
                규칙 등록하기
              </button>
            }
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-white/10 text-[11px] font-black uppercase text-slate-400">
                  <th className="py-3 px-4 min-w-[90px]">Rule ID</th>
                  <th className="py-3 px-4 min-w-[200px]">규칙명</th>
                  <th className="py-3 px-4 min-w-[120px]">적용 모듈</th>
                  <th className="py-3 px-4 min-w-[150px]">매핑 키워드</th>
                  <th className="py-3 px-4 min-w-[180px]">차변 계정과목</th>
                  <th className="py-3 px-4 min-w-[180px]">대변 계정과목</th>
                  <th className="py-3 px-4 min-w-[100px] text-center">우선순위</th>
                  <th className="py-3 px-4 min-w-[100px] text-center">상태</th>
                  <th className="py-3 px-4 min-w-[100px] text-center">관리</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {filteredRules.map((rule) => (
                  <tr key={rule.id} className="hover:bg-white/[0.03] transition-colors">
                    <td className="py-4 px-4 font-mono text-xs font-black text-blue-400">
                      {rule.id}
                    </td>
                    <td className="py-4 px-4 font-bold text-white text-sm">
                      {rule.name}
                    </td>
                    <td className="py-4 px-4">
                      <span className="inline-block px-2.5 py-1 rounded-full text-[10px] font-black uppercase bg-white/5 text-slate-300 border border-white/10">
                        {rule.module === 'CARD' ? '카드결제' : rule.module === 'BANKING' ? '금융계좌' : '세금계산서'}
                      </span>
                    </td>
                    <td className="py-4 px-4">
                      <span className="font-mono text-xs font-bold text-amber-400 bg-amber-400/10 px-2.5 py-1 rounded-lg border border-amber-400/20">
                        {rule.keyword}
                      </span>
                    </td>
                    <td className="py-4 px-4 text-xs font-mono font-bold text-emerald-400">
                      {getAccountName(rule.debitAccountCode)}
                    </td>
                    <td className="py-4 px-4 text-xs font-mono font-bold text-blue-400">
                      {getAccountName(rule.creditAccountCode)}
                    </td>
                    <td className="py-4 px-4 text-center font-mono text-xs text-slate-400 font-bold">
                      P-{rule.priority}
                    </td>
                    <td className="py-4 px-4 text-center">
                      <StatusBadge
                        status={rule.isActive ? '활성' : '비활성'}
                        variant={rule.isActive ? 'success' : 'neutral'}
                      />
                    </td>
                    <td className="py-4 px-4 text-center">
                      <div className="flex items-center justify-center gap-2">
                        <button
                          onClick={() => handleToggleRule(rule.id)}
                          className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-white/10 transition-colors"
                          title="토글 상태"
                        >
                          {rule.isActive ? <Check size={16} className="text-emerald-400" /> : <X size={16} />}
                        </button>
                        <button
                          onClick={() => handleDeleteRule(rule.id)}
                          className="p-1.5 rounded-lg text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 transition-colors"
                          title="삭제"
                        >
                          <Trash2 size={16} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Add Rule Modal Overlay */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-in fade-in">
          <div className="bg-slate-900 border border-white/10 rounded-3xl p-8 max-w-md w-full shadow-2xl space-y-6">
            <div className="flex justify-between items-center pb-4 border-b border-white/10">
              <h3 className="text-lg font-black text-white flex items-center gap-2">
                <Sparkles size={20} className="text-blue-400" />
                신규 자동 분개 규칙 등록
              </h3>
              <button
                onClick={() => setShowAddModal(false)}
                className="text-slate-400 hover:text-white"
              >
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleAddRule} className="space-y-4">
              <div>
                <label className="block text-xs font-black text-slate-400 uppercase tracking-wider mb-2">
                  규칙명
                </label>
                <input
                  type="text"
                  placeholder="예: 법인카드 접대비 지출 자동분개"
                  value={newRuleName}
                  onChange={(e) => setNewRuleName(e.target.value)}
                  className="w-full bg-slate-950 border border-white/10 rounded-xl px-4 py-2.5 text-white text-sm font-medium focus:outline-none focus:border-blue-500"
                />
              </div>

              <div>
                <label className="block text-xs font-black text-slate-400 uppercase tracking-wider mb-2">
                  적용 모듈
                </label>
                <select
                  value={newModule}
                  onChange={(e: React.ChangeEvent<HTMLSelectElement>) => setNewModule(e.target.value as 'BANKING' | 'CARD' | 'TAX_INVOICE')}
                  className="w-full bg-slate-950 border border-white/10 rounded-xl px-4 py-2.5 text-white text-sm font-medium focus:outline-none focus:border-blue-500"
                >
                  <option value="CARD">카드 결제</option>
                  <option value="BANKING">금융 계좌 (입출금)</option>
                  <option value="TAX_INVOICE">전자 세금계산서</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-black text-slate-400 uppercase tracking-wider mb-2">
                  매핑 검색 키워드
                </label>
                <input
                  type="text"
                  placeholder="예: 스타벅스, 택시비, 네이버페이"
                  value={newKeyword}
                  onChange={(e) => setNewKeyword(e.target.value)}
                  className="w-full bg-slate-950 border border-white/10 rounded-xl px-4 py-2.5 text-amber-400 font-mono text-sm font-bold focus:outline-none focus:border-blue-500"
                />
              </div>

              <div>
                <label className="block text-xs font-black text-slate-400 uppercase tracking-wider mb-2">
                  차변 자동 매핑 계정과목
                </label>
                <select
                  value={newDebitAcc}
                  onChange={(e) => setNewDebitAcc(e.target.value)}
                  className="w-full bg-slate-950 border border-white/10 rounded-xl px-4 py-2.5 text-white text-sm font-medium focus:outline-none focus:border-blue-500"
                >
                  {mockAccounts.map((a) => (
                    <option key={a.code} value={a.code}>
                      [{a.code}] {a.name}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-black text-slate-400 uppercase tracking-wider mb-2">
                  대변 자동 매핑 계정과목
                </label>
                <select
                  value={newCreditAcc}
                  onChange={(e) => setNewCreditAcc(e.target.value)}
                  className="w-full bg-slate-950 border border-white/10 rounded-xl px-4 py-2.5 text-white text-sm font-medium focus:outline-none focus:border-blue-500"
                >
                  {mockAccounts.map((a) => (
                    <option key={a.code} value={a.code}>
                      [{a.code}] {a.name}
                    </option>
                  ))}
                </select>
              </div>

              <div className="pt-4 flex justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="px-4 py-2.5 rounded-xl bg-white/5 text-slate-300 font-bold text-xs"
                >
                  취소
                </button>
                <button
                  type="submit"
                  className="px-5 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-bold text-xs"
                >
                  규칙 추가 저장
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

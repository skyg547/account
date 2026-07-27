'use client';

import React, { useState } from 'react';
import { 
  CheckSquare, 
  Search, 
  ArrowRightLeft, 
  CheckCircle2, 
  X, 
  WalletCards
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import AmountDisplay from '@/components/ui/AmountDisplay';
import Tabs, { TabItem } from '@/components/ui/Tabs';
import EmptyState from '@/components/ui/EmptyState';
import { mockAccounts, mockPartners } from '@/mocks/master';

interface UnsettledItem {
  id: string;
  type: 'SUSPENSE' | 'RECEIVABLE' | 'PAYABLE'; // 가수금/가지급금, 채권, 채무
  occurredDate: string;
  accountCode: string;
  partnerId?: string;
  originalAmount: number;
  remainingAmount: number;
  status: 'UNSETTLED' | 'PARTIAL' | 'CLEARED';
  memo: string;
}

const initialUnsettledItems: UnsettledItem[] = [
  {
    id: 'UNS-20250601-01',
    type: 'SUSPENSE',
    occurredDate: '2025-06-01',
    accountCode: '1110200',
    partnerId: 'PT-001',
    originalAmount: 5000000,
    remainingAmount: 5000000,
    status: 'UNSETTLED',
    memo: '입처불명 보통예금 입금건 (가수금 정산 대상)',
  },
  {
    id: 'UNS-20250603-02',
    type: 'SUSPENSE',
    occurredDate: '2025-06-03',
    accountCode: '1110100',
    partnerId: undefined,
    originalAmount: 1200000,
    remainingAmount: 400000,
    status: 'PARTIAL',
    memo: '출장비 가지급금 부분 정산',
  },
  {
    id: 'UNS-20250610-03',
    type: 'RECEIVABLE',
    occurredDate: '2025-06-10',
    accountCode: '1110200',
    partnerId: 'PT-002',
    originalAmount: 15000000,
    remainingAmount: 15000000,
    status: 'UNSETTLED',
    memo: '글로벌테크 외상매출금 수금 미결',
  },
  {
    id: 'UNS-20250612-04',
    type: 'PAYABLE',
    occurredDate: '2025-06-12',
    accountCode: '2110000',
    partnerId: 'PT-003',
    originalAmount: 8500000,
    remainingAmount: 8500000,
    status: 'UNSETTLED',
    memo: '홍길동 외상매입금 지급 미결',
  },
];

const categoryTabs: TabItem[] = [
  { id: 'ALL', label: '전체 미결항목' },
  { id: 'SUSPENSE', label: '가수금/가지급금' },
  { id: 'RECEIVABLE', label: '미결 채권' },
  { id: 'PAYABLE', label: '미결 채무' },
];

const getStatusVariant = (status: UnsettledItem['status']) => {
  switch (status) {
    case 'UNSETTLED':
      return { label: '미결', variant: 'error' as const };
    case 'PARTIAL':
      return { label: '부분정산', variant: 'warning' as const };
    case 'CLEARED':
      return { label: '반제완료', variant: 'success' as const };
    default:
      return { label: status, variant: 'neutral' as const };
  }
};

export default function UnsettledClearingPage() {
  const [items, setItems] = useState<UnsettledItem[]>(initialUnsettledItems);
  const [activeTab, setActiveTab] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');
  
  // Clearing Modal state
  const [selectedItem, setSelectedItem] = useState<UnsettledItem | null>(null);
  const [clearAmount, setClearAmount] = useState<number>(0);
  const [clearingAccount, setClearingAccount] = useState<string>('1110200');
  const [clearingNote, setClearingNote] = useState<string>('');
  const [successToast, setSuccessToast] = useState<string | null>(null);

  const filteredItems = items.filter((item) => {
    if (activeTab !== 'ALL' && item.type !== activeTab) {
      return false;
    }
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      return (
        item.id.toLowerCase().includes(q) ||
        item.memo.toLowerCase().includes(q) ||
        (item.partnerId && item.partnerId.toLowerCase().includes(q))
      );
    }
    return true;
  });

  const getAccountName = (code: string) => {
    const acc = mockAccounts.find((a) => a.code === code);
    return acc ? acc.name : code;
  };

  const getPartnerName = (partnerId?: string) => {
    if (!partnerId) return '-';
    const pt = mockPartners.find((p) => p.id === partnerId);
    return pt ? pt.name : partnerId;
  };

  const handleOpenClearModal = (item: UnsettledItem) => {
    setSelectedItem(item);
    setClearAmount(item.remainingAmount);
    setClearingNote(`${item.id} 반제 및 정산 처리`);
  };

  const handleExecuteClearing = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedItem || clearAmount <= 0) {
      alert('올바른 반제 금액을 입력해 주세요.');
      return;
    }
    if (clearAmount > selectedItem.remainingAmount) {
      alert('반제 금액이 남은 미결 잔액보다 클 수 없습니다.');
      return;
    }

    setItems((prev) =>
      prev.map((item) => {
        if (item.id === selectedItem.id) {
          const newRemaining = item.remainingAmount - clearAmount;
          const newStatus = newRemaining === 0 ? 'CLEARED' : 'PARTIAL';
          return {
            ...item,
            remainingAmount: newRemaining,
            status: newStatus,
          };
        }
        return item;
      })
    );

    setSuccessToast(`${selectedItem.id} 반제 전표가 정상 처리되었습니다.`);
    setSelectedItem(null);
    setTimeout(() => setSuccessToast(null), 4000);
  };

  const totalUnsettledAmount = items.reduce((sum, item) => sum + item.remainingAmount, 0);

  return (
    <div className="space-y-8 pb-12">
      {/* Page Header */}
      <PageHeader
        title="미결항목 정리"
        description="가수금, 가지급금 및 미결제 채권/채무 항목을 정산 및 반제 전표로 대체 처리합니다."
        breadcrumbs={[
          { label: '재무/회계' },
          { label: '정산' },
          { label: '미결항목 정리' }
        ]}
        icon={CheckSquare}
        actions={
          <div className="flex items-center gap-3 bg-slate-900/50 border border-white/5 px-4 py-2 rounded-2xl">
            <span className="text-xs text-slate-400 font-bold">총 미결 잔액:</span>
            <AmountDisplay amount={totalUnsettledAmount} className="text-rose-400 font-black text-base" />
          </div>
        }
      />

      {successToast && (
        <div className="p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-sm font-bold flex items-center gap-3 animate-in fade-in">
          <CheckCircle2 size={18} />
          <span>{successToast}</span>
        </div>
      )}

      {/* Tabs */}
      <Tabs tabs={categoryTabs} activeTab={activeTab} onChange={setActiveTab} />

      {/* Search Filter */}
      <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-6 shadow-xl">
        <div className="relative w-full md:w-96">
          <Search className="absolute left-4 top-3.5 text-slate-500" size={18} />
          <input
            type="text"
            placeholder="미결 ID, 메모, 거래처 검색..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full bg-slate-950/60 border border-white/10 rounded-2xl pl-11 pr-4 py-3 text-white text-sm font-medium focus:outline-none focus:border-blue-500 transition-colors"
          />
        </div>
      </div>

      {/* Unsettled Items Table */}
      <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-7 shadow-xl overflow-hidden">
        {filteredItems.length === 0 ? (
          <EmptyState
            icon={WalletCards}
            title="미결 항목이 없습니다"
            description="선택한 분류 조건에 해당하는 미결 항목이 모두 정산되었거나 존재하지 않습니다."
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-white/10 text-[11px] font-black uppercase text-slate-400">
                  <th className="py-3 px-4 min-w-[140px]">미결 ID</th>
                  <th className="py-3 px-4 min-w-[110px]">발생일</th>
                  <th className="py-3 px-4 min-w-[150px]">계정과목</th>
                  <th className="py-3 px-4 min-w-[140px]">거래처</th>
                  <th className="py-3 px-4 min-w-[200px]">메모 / 사유</th>
                  <th className="py-3 px-4 min-w-[140px] text-right">최초 발생 금액</th>
                  <th className="py-3 px-4 min-w-[140px] text-right">미결 잔액</th>
                  <th className="py-3 px-4 min-w-[100px] text-center">상태</th>
                  <th className="py-3 px-4 min-w-[110px] text-center">반제/정산</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {filteredItems.map((item) => {
                  const statusInfo = getStatusVariant(item.status);
                  return (
                    <tr key={item.id} className="hover:bg-white/[0.03] transition-colors">
                      <td className="py-4 px-4 font-mono text-xs font-black text-blue-400">
                        {item.id}
                      </td>
                      <td className="py-4 px-4 text-xs text-slate-300 font-mono">
                        {item.occurredDate}
                      </td>
                      <td className="py-4 px-4 text-xs font-bold text-white">
                        [{item.accountCode}] {getAccountName(item.accountCode)}
                      </td>
                      <td className="py-4 px-4 text-xs font-medium text-slate-300">
                        {getPartnerName(item.partnerId)}
                      </td>
                      <td className="py-4 px-4 text-xs text-slate-300">
                        {item.memo}
                      </td>
                      <td className="py-4 px-4 text-right">
                        <AmountDisplay amount={item.originalAmount} className="text-slate-300 text-sm" />
                      </td>
                      <td className="py-4 px-4 text-right">
                        <AmountDisplay amount={item.remainingAmount} className="text-rose-400 text-sm font-black" />
                      </td>
                      <td className="py-4 px-4 text-center">
                        <StatusBadge status={statusInfo.label} variant={statusInfo.variant} />
                      </td>
                      <td className="py-4 px-4 text-center">
                        {item.status !== 'CLEARED' ? (
                          <button
                            onClick={() => handleOpenClearModal(item)}
                            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-blue-600/20 hover:bg-blue-600/30 text-blue-400 border border-blue-500/30 text-xs font-black transition-all mx-auto"
                          >
                            <ArrowRightLeft size={14} /> 반제 처리
                          </button>
                        ) : (
                          <span className="text-xs text-slate-500 font-bold">완료됨</span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Clearing Action Modal */}
      {selectedItem && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-in fade-in">
          <div className="bg-slate-900 border border-white/10 rounded-3xl p-8 max-w-lg w-full shadow-2xl space-y-6">
            <div className="flex justify-between items-center pb-4 border-b border-white/10">
              <div>
                <h3 className="text-lg font-black text-white flex items-center gap-2">
                  <ArrowRightLeft size={20} className="text-blue-400" />
                  미결 항목 반제 및 정산 처리
                </h3>
                <span className="text-xs font-mono font-bold text-blue-400">{selectedItem.id}</span>
              </div>
              <button
                onClick={() => setSelectedItem(null)}
                className="text-slate-400 hover:text-white"
              >
                <X size={20} />
              </button>
            </div>

            <div className="p-4 rounded-2xl bg-slate-950/60 border border-white/5 text-xs space-y-2">
              <div className="flex justify-between">
                <span className="text-slate-400 font-bold">원본 계정과목:</span>
                <span className="text-white">[{selectedItem.accountCode}] {getAccountName(selectedItem.accountCode)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400 font-bold">거래처:</span>
                <span className="text-white">{getPartnerName(selectedItem.partnerId)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400 font-bold">현재 남은 미결 잔액:</span>
                <AmountDisplay amount={selectedItem.remainingAmount} className="text-rose-400 font-bold" />
              </div>
            </div>

            <form onSubmit={handleExecuteClearing} className="space-y-4">
              <div>
                <label className="block text-xs font-black text-slate-400 uppercase tracking-wider mb-2">
                  이번 차수 반제 금액 (원)
                </label>
                <input
                  type="number"
                  max={selectedItem.remainingAmount}
                  min={1}
                  value={clearAmount}
                  onChange={(e) => setClearAmount(Number(e.target.value))}
                  className="w-full bg-slate-950 border border-white/10 rounded-xl px-4 py-2.5 text-emerald-400 font-mono text-base font-black focus:outline-none focus:border-blue-500"
                />
              </div>

              <div>
                <label className="block text-xs font-black text-slate-400 uppercase tracking-wider mb-2">
                  상대 정산 계정과목
                </label>
                <select
                  value={clearingAccount}
                  onChange={(e) => setClearingAccount(e.target.value)}
                  className="w-full bg-slate-950 border border-white/10 rounded-xl px-4 py-2.5 text-white text-sm font-medium focus:outline-none focus:border-blue-500"
                >
                  {mockAccounts.map((acc) => (
                    <option key={acc.code} value={acc.code}>
                      [{acc.code}] {acc.name}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-black text-slate-400 uppercase tracking-wider mb-2">
                  반제 전표 적요
                </label>
                <input
                  type="text"
                  value={clearingNote}
                  onChange={(e) => setClearingNote(e.target.value)}
                  className="w-full bg-slate-950 border border-white/10 rounded-xl px-4 py-2.5 text-white text-sm font-medium focus:outline-none focus:border-blue-500"
                />
              </div>

              <div className="pt-4 flex justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setSelectedItem(null)}
                  className="px-4 py-2.5 rounded-xl bg-white/5 text-slate-300 font-bold text-xs"
                >
                  취소
                </button>
                <button
                  type="submit"
                  className="px-5 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-bold text-xs"
                >
                  반제 전표 생성 및 정산 완료
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

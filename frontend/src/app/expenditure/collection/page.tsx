'use client';

import React, { useState } from 'react';
import {
  ArrowDownToLine,
  Plus,
  CheckCircle2,
  Clock,
  Search,
  FileCheck,
  Link as LinkIcon
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import AmountDisplay from '@/components/ui/AmountDisplay';
import Tabs, { TabItem } from '@/components/ui/Tabs';
import { mockReceivables } from '@/mocks/expenditure';

interface CollectionRecord {
  id: string;
  colNumber: string;
  customerName: string;
  depositDate: string;
  depositBank: string;
  amount: number;
  matchedAmount: number;
  unmatchedAmount: number;
  status: 'FULL_MATCHED' | 'PARTIAL_MATCHED' | 'UNMATCHED';
  matchedArNo?: string;
  depositType: 'BANK_TRANSFER' | 'VIRTUAL_ACCOUNT' | 'BILL';
}

export default function CollectionPage() {
  const [collections, setCollections] = useState<CollectionRecord[]>([
    {
      id: 'col-001',
      colNumber: 'COL-2026-0701',
      customerName: '(주)엔씨소프트',
      depositDate: '2026-07-28',
      depositBank: '신한은행 (110-123-456789)',
      amount: 20000000,
      matchedAmount: 20000000,
      unmatchedAmount: 0,
      status: 'FULL_MATCHED',
      matchedArNo: 'AR-2026-0702',
      depositType: 'BANK_TRANSFER'
    },
    {
      id: 'col-002',
      colNumber: 'COL-2026-0702',
      customerName: '(주)LG유플러스',
      depositDate: '2026-07-22',
      depositBank: '하나은행 (298-910034-11004)',
      amount: 43000000,
      matchedAmount: 43000000,
      unmatchedAmount: 0,
      status: 'FULL_MATCHED',
      matchedArNo: 'AR-2026-0704',
      depositType: 'VIRTUAL_ACCOUNT'
    },
    {
      id: 'col-003',
      colNumber: 'COL-2026-0703',
      customerName: '현대자동차(주)',
      depositDate: '2026-07-25',
      depositBank: '신한은행 (110-123-456789)',
      amount: 50000000,
      matchedAmount: 0,
      unmatchedAmount: 50000000,
      status: 'UNMATCHED',
      depositType: 'BANK_TRANSFER'
    }
  ]);

  const [activeTab, setActiveTab] = useState('LIST');
  const [selectedCol, setSelectedCol] = useState<CollectionRecord | null>(null);
  const [selectedArId, setSelectedArId] = useState<string>('ar-006');
  const [searchQuery, setSearchQuery] = useState('');

  // New Collection Form
  const [newCustomer, setNewCustomer] = useState('');
  const [newAmount, setNewAmount] = useState<number>(0);
  const [newDate, setNewDate] = useState('2026-07-28');
  const [newBank, setNewBank] = useState('신한은행');
  const [newType] = useState<'BANK_TRANSFER' | 'VIRTUAL_ACCOUNT' | 'BILL'>('BANK_TRANSFER');

  const mainTabs: TabItem[] = [
    { id: 'LIST', label: '수금 입금 내역', icon: ArrowDownToLine },
    { id: 'NEW', label: '신규 입금 등록', icon: Plus },
  ];

  const totalCollected = collections.reduce((sum, c) => sum + c.amount, 0);
  const totalMatched = collections.reduce((sum, c) => sum + c.matchedAmount, 0);
  const totalUnmatched = collections.reduce((sum, c) => sum + c.unmatchedAmount, 0);

  const handleRegisterCollection = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newCustomer || newAmount <= 0) {
      alert('고객사명과 입금액을 정확히 입력해 주세요.');
      return;
    }

    const newRecord: CollectionRecord = {
      id: `col-${Date.now()}`,
      colNumber: `COL-2026-0${collections.length + 1}`,
      customerName: newCustomer,
      depositDate: newDate,
      depositBank: newBank,
      amount: Number(newAmount),
      matchedAmount: 0,
      unmatchedAmount: Number(newAmount),
      status: 'UNMATCHED',
      depositType: newType
    };

    setCollections([newRecord, ...collections]);
    alert(`[수금 입금 등록 완료] ${newCustomer} 대상 ₩${Number(newAmount).toLocaleString()} 수금이 정상 수신 등록되었습니다.`);
    setNewCustomer('');
    setNewAmount(0);
    setActiveTab('LIST');
  };

  const handleExecuteMatching = () => {
    if (!selectedCol) return;
    const targetAr = mockReceivables.find(r => r.id === selectedArId);
    if (!targetAr) return;

    setCollections(collections.map(c => {
      if (c.id !== selectedCol.id) return c;
      return {
        ...c,
        matchedAmount: c.amount,
        unmatchedAmount: 0,
        status: 'FULL_MATCHED',
        matchedArNo: targetAr.arNumber
      };
    }));

    alert(`[AR 수금 매칭 완료] 수금 ${selectedCol.colNumber} 건이 매출채권 ${targetAr.arNumber} (${targetAr.customerName}) 과 성공적으로 반제 매칭되었습니다.`);
    setSelectedCol(null);
  };

  return (
    <div className="space-y-8 pb-16">
      <PageHeader
        title="수금 관리"
        description="고객사 입금 내역을 등록하고 매출채권(AR) 세금계산서와 일대일 또는 일대다로 반제 매칭 정산합니다."
        breadcrumbs={[
          { label: '지출/수금관리' },
          { label: '수금 관리' }
        ]}
        icon={ArrowDownToLine}
      />

      {/* KPI Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-6 space-y-3 shadow-xl">
          <div className="flex items-center justify-between text-slate-400 text-xs font-black uppercase tracking-wider">
            <span>이번 달 총 수금액</span>
            <ArrowDownToLine size={18} className="text-emerald-400" />
          </div>
          <div className="text-3xl font-black text-emerald-400 italic tracking-tight font-mono">
            <AmountDisplay amount={totalCollected} />
          </div>
          <div className="text-xs text-slate-400 font-medium">
            전체 입금 수신 건수: <span className="text-white font-black">{collections.length}건</span>
          </div>
        </div>

        <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-6 space-y-3 shadow-xl">
          <div className="flex items-center justify-between text-slate-400 text-xs font-black uppercase tracking-wider">
            <span>AR 반제 매칭 완료</span>
            <CheckCircle2 size={18} className="text-blue-400" />
          </div>
          <div className="text-3xl font-black text-blue-400 italic tracking-tight font-mono">
            <AmountDisplay amount={totalMatched} />
          </div>
          <div className="text-xs text-slate-400 font-medium">
            정상 전표 대체 처리 금액
          </div>
        </div>

        <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-6 space-y-3 shadow-xl">
          <div className="flex items-center justify-between text-slate-400 text-xs font-black uppercase tracking-wider">
            <span>미매칭 입금 잔액</span>
            <Clock size={18} className="text-amber-400" />
          </div>
          <div className="text-3xl font-black text-amber-400 italic tracking-tight font-mono">
            <AmountDisplay amount={totalUnmatched} />
          </div>
          <div className="text-xs text-amber-400/80 font-medium">
            매출채권 매칭 대기 중인 입금액
          </div>
        </div>
      </div>

      <Tabs tabs={mainTabs} activeTab={activeTab} onChange={setActiveTab} />

      {activeTab === 'LIST' && (
        <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-7 shadow-xl space-y-4">
          <div className="flex items-center justify-between border-b border-white/5 pb-4">
            <h3 className="text-lg font-black text-white tracking-tight flex items-center gap-2">
              <FileCheck className="text-emerald-400" size={18} />
              수금 입금 내역 및 AR 반제 매칭
            </h3>

            <div className="relative w-72">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" size={14} />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="고객사명, 수금번호 검색"
                className="w-full bg-slate-950 border border-white/10 rounded-xl py-2 pl-9 pr-3 text-xs text-white"
              />
            </div>
          </div>

          <table className="w-full text-left text-xs">
            <thead>
              <tr className="border-b border-white/10 text-slate-400 font-black uppercase tracking-wider">
                <th className="py-3.5 px-4">수금 번호</th>
                <th className="py-3.5 px-4">고객사명</th>
                <th className="py-3.5 px-4">입금일자</th>
                <th className="py-3.5 px-4">입금 계좌 / 수단</th>
                <th className="py-3.5 px-4 text-right">총 입금액</th>
                <th className="py-3.5 px-4 text-right">AR 매칭액</th>
                <th className="py-3.5 px-4 text-center">매칭 AR 번호</th>
                <th className="py-3.5 px-4 text-center">매칭 상태</th>
                <th className="py-3.5 px-4 text-center">작업</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5">
              {collections.map((col) => (
                <tr key={col.id} className="hover:bg-white/[0.02] transition-colors">
                  <td className="py-4 px-4 font-mono font-bold text-emerald-400">{col.colNumber}</td>
                  <td className="py-4 px-4 font-black text-white">{col.customerName}</td>
                  <td className="py-4 px-4 font-mono text-slate-400">{col.depositDate}</td>
                  <td className="py-4 px-4 text-slate-300">
                    <div>{col.depositBank}</div>
                    <div className="text-[10px] text-slate-500">{col.depositType === 'BANK_TRANSFER' ? '계좌이체' : '가상계좌'}</div>
                  </td>
                  <td className="py-4 px-4 text-right font-mono font-bold text-white"><AmountDisplay amount={col.amount} /></td>
                  <td className="py-4 px-4 text-right font-mono text-blue-400"><AmountDisplay amount={col.matchedAmount} /></td>
                  <td className="py-4 px-4 text-center font-mono text-slate-400">
                    {col.matchedArNo || '-'}
                  </td>
                  <td className="py-4 px-4 text-center">
                    <StatusBadge 
                      status={col.status === 'FULL_MATCHED' ? '완료' : '미매칭'}
                      variant={col.status === 'FULL_MATCHED' ? 'success' : 'warning'}
                    />
                  </td>
                  <td className="py-4 px-4 text-center">
                    {col.status === 'UNMATCHED' ? (
                      <button
                        onClick={() => setSelectedCol(col)}
                        className="px-3 py-1.5 bg-blue-600/20 hover:bg-blue-600 text-blue-400 hover:text-white border border-blue-500/30 rounded-xl text-xs font-black transition-all flex items-center gap-1 mx-auto"
                      >
                        <LinkIcon size={12} /> AR 매칭
                      </button>
                    ) : (
                      <span className="text-[11px] text-slate-500 font-bold">매칭 완료 ✓</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {activeTab === 'NEW' && (
        <form onSubmit={handleRegisterCollection} className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-7 shadow-xl space-y-6">
          <div className="border-b border-white/5 pb-4">
            <h3 className="text-lg font-black text-white tracking-tight flex items-center gap-2">
              <Plus className="text-emerald-400" size={18} />
              신규 수금 입금 내역 등록
            </h3>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-2">
              <label className="text-xs font-black uppercase text-slate-400">입금 고객사명</label>
              <input
                type="text"
                value={newCustomer}
                onChange={(e) => setNewCustomer(e.target.value)}
                placeholder="예: 현대자동차(주)"
                className="w-full bg-slate-950 border border-white/10 rounded-2xl px-4 py-3 text-sm text-white font-medium"
                required
              />
            </div>

            <div className="space-y-2">
              <label className="text-xs font-black uppercase text-slate-400">입금 금액 (원)</label>
              <input
                type="number"
                value={newAmount || ''}
                onChange={(e) => setNewAmount(Number(e.target.value))}
                placeholder="0"
                className="w-full bg-slate-950 border border-white/10 rounded-2xl px-4 py-3 text-sm font-mono text-emerald-400 font-bold"
                required
              />
            </div>

            <div className="space-y-2">
              <label className="text-xs font-black uppercase text-slate-400">입금 수신 일자</label>
              <input
                type="date"
                value={newDate}
                onChange={(e) => setNewDate(e.target.value)}
                className="w-full bg-slate-950 border border-white/10 rounded-2xl px-4 py-3 text-sm text-white font-medium"
              />
            </div>

            <div className="space-y-2">
              <label className="text-xs font-black uppercase text-slate-400">입금 은행 계좌</label>
              <select
                value={newBank}
                onChange={(e) => setNewBank(e.target.value)}
                className="w-full bg-slate-950 border border-white/10 rounded-2xl px-4 py-3 text-sm text-white font-medium"
              >
                <option value="신한은행">신한은행 당좌 (110-123-456789)</option>
                <option value="하나은행">하나은행 법인 (298-910034-11004)</option>
                <option value="국민은행">국민은행 사업자 (817-21-0922-811)</option>
              </select>
            </div>
          </div>

          <div className="flex justify-end pt-4">
            <button
              type="submit"
              className="px-8 py-3 bg-emerald-600 hover:bg-emerald-500 rounded-xl text-white text-xs font-black transition-all shadow-lg shadow-emerald-600/20"
            >
              수금 입금 등록
            </button>
          </div>
        </form>
      )}

      {/* AR Matching Dialog */}
      {selectedCol && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-md">
          <div className="w-full max-w-2xl rounded-3xl bg-slate-900 border border-white/10 p-7 shadow-2xl space-y-6">
            <div className="border-b border-white/10 pb-4">
              <h3 className="text-lg font-black text-white italic">매출채권(AR) 반제 매칭</h3>
              <p className="text-xs text-slate-400 mt-1">수금 입금 건 [{selectedCol.colNumber}] ₩{selectedCol.amount.toLocaleString()} 과 반제할 AR 매출 채권을 선택합니다.</p>
            </div>

            <div className="space-y-3">
              <label className="text-xs font-black uppercase text-slate-400">대상 고객사 미수 채권 목록</label>
              <select
                value={selectedArId}
                onChange={(e) => setSelectedArId(e.target.value)}
                className="w-full bg-slate-950 border border-white/10 rounded-2xl p-3 text-xs text-white"
              >
                {mockReceivables.filter(r => r.balance > 0).map(r => (
                  <option key={r.id} value={r.id}>
                    {r.arNumber} - {r.customerName} : {r.description} (미수잔액: ₩{r.balance.toLocaleString()})
                  </option>
                ))}
              </select>
            </div>

            <div className="flex items-center justify-end gap-3 pt-4">
              <button
                onClick={() => setSelectedCol(null)}
                className="px-5 py-2.5 bg-white/5 hover:bg-white/10 rounded-xl text-xs font-black text-slate-400"
              >
                취소
              </button>
              <button
                onClick={handleExecuteMatching}
                className="px-6 py-2.5 bg-blue-600 hover:bg-blue-500 rounded-xl text-xs font-black text-white shadow-lg shadow-blue-600/20"
              >
                반제 매칭 확정
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

'use client';

import React, { useState } from 'react';
import { 
  Building2, 
  Search, 
  Download, 
  FileCheck2,
  Users
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import AmountDisplay from '@/components/ui/AmountDisplay';
import { mockPartners, PartnerDto, mockAccounts } from '@/mocks/master';
import { mockJournals } from '@/mocks/ledger';

const getPartnerStatusVariant = (status: PartnerDto['status']) => {
  switch (status) {
    case 'ACTIVE':
      return { label: '정상', variant: 'success' as const };
    case 'PENDING':
      return { label: '승인대기', variant: 'warning' as const };
    case 'BLOCKED':
      return { label: '거래정지', variant: 'error' as const };
    default:
      return { label: status, variant: 'neutral' as const };
  }
};

const getAccountName = (code: string) => {
  const acc = mockAccounts.find((a) => a.code === code);
  return acc ? acc.name : code;
};

export default function SubLedgerPage() {
  const [selectedPartnerId, setSelectedPartnerId] = useState<string>('PT-001');
  const [searchQuery, setSearchQuery] = useState<string>('');

  const selectedPartner = mockPartners.find((p) => p.id === selectedPartnerId) || mockPartners[0];

  const filteredPartners = mockPartners.filter((p) => {
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      return (
        p.name.toLowerCase().includes(q) ||
        p.id.toLowerCase().includes(q) ||
        p.businessNumber.includes(q)
      );
    }
    return true;
  });

  // Calculate transactions associated with the selected partner
  const partnerTransactions = mockJournals.flatMap((j) =>
    j.lines
      .filter((l) => l.partnerId === selectedPartner.id)
      .map((l) => ({
        journalId: j.id,
        date: j.date,
        status: j.status,
        accountCode: l.accountCode,
        desc: l.desc,
        debit: l.debit,
        credit: l.credit,
      }))
  );

  const totalDebit = partnerTransactions.reduce((sum, t) => sum + t.debit, 0);
  const totalCredit = partnerTransactions.reduce((sum, t) => sum + t.credit, 0);
  const netBalance = totalDebit - totalCredit;

  return (
    <div className="space-y-8 pb-12">
      {/* Page Header */}
      <PageHeader
        title="보조원장 (SL)"
        description="거래처별 및 사업자 관리항목별 세부 보조원장 원장 거래 내역 및 잔액 현황"
        breadcrumbs={[
          { label: '재무/회계' },
          { label: '장부관리' },
          { label: '보조원장' }
        ]}
        icon={Building2}
        actions={
          <button className="flex items-center gap-2 px-4 py-2.5 rounded-2xl bg-emerald-600/20 hover:bg-emerald-600/30 text-emerald-400 border border-emerald-500/30 font-black text-xs transition-all shadow-lg">
            <Download size={14} /> 보조원장 출력 (Excel)
          </button>
        }
      />

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        
        {/* Left: Partner Selection List (4 cols) */}
        <div className="lg:col-span-4 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-6 shadow-xl space-y-5">
          <div className="flex items-center justify-between">
            <h3 className="text-base font-black text-white tracking-tight flex items-center gap-2">
              <Users size={18} className="text-blue-400" />
              거래처 선택
            </h3>
            <span className="text-xs font-mono font-bold text-slate-500">
              {filteredPartners.length}개 거래처
            </span>
          </div>

          <div className="relative">
            <Search className="absolute left-3.5 top-3 text-slate-500" size={16} />
            <input
              type="text"
              placeholder="거래처명 또는 사업자번호 검색..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full bg-slate-950/60 border border-white/10 rounded-xl pl-10 pr-3 py-2.5 text-white text-xs font-medium focus:outline-none focus:border-blue-500"
            />
          </div>

          <div className="space-y-2.5 max-h-[560px] overflow-y-auto pr-1">
            {filteredPartners.map((pt) => {
              const isSelected = pt.id === selectedPartnerId;
              const statusInfo = getPartnerStatusVariant(pt.status);
              return (
                <div
                  key={pt.id}
                  onClick={() => setSelectedPartnerId(pt.id)}
                  className={`p-4 rounded-2xl border transition-all cursor-pointer ${
                    isSelected
                      ? 'bg-blue-600/15 border-blue-500/40 shadow-lg'
                      : 'bg-white/[0.02] border-white/5 hover:bg-white/[0.06]'
                  }`}
                >
                  <div className="flex items-center justify-between mb-1">
                    <span className="font-mono text-xs font-black text-blue-400">{pt.id}</span>
                    <StatusBadge status={statusInfo.label} variant={statusInfo.variant} />
                  </div>
                  <h4 className="text-sm font-bold text-white mb-1">{pt.name}</h4>
                  <div className="text-[11px] text-slate-400 font-mono">
                    사업자: {pt.businessNumber}
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Right: Partner Detail & Sub-Ledger Grid (8 cols) */}
        <div className="lg:col-span-8 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-7 shadow-xl space-y-6">
          
          {/* Partner Info Summary Card */}
          <div className="p-6 rounded-2xl bg-white/[0.03] border border-white/10 space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div>
                <div className="flex items-center gap-3">
                  <span className="font-mono text-xs font-black text-blue-400">{selectedPartner.id}</span>
                  <StatusBadge
                    status={getPartnerStatusVariant(selectedPartner.status).label}
                    variant={getPartnerStatusVariant(selectedPartner.status).variant}
                  />
                  <span className="text-xs text-slate-400 font-medium">
                    {selectedPartner.type === 'CORPORATE' ? '법인사업자' : '개인사업자'}
                  </span>
                </div>
                <h2 className="text-2xl font-black text-white tracking-tight mt-1">
                  {selectedPartner.name}
                </h2>
              </div>

              <div className="flex items-center gap-4 text-right">
                <div>
                  <span className="text-[11px] font-black uppercase text-slate-500 block">순 잔액 (차액)</span>
                  <AmountDisplay amount={netBalance} className="text-xl font-black text-emerald-400" />
                </div>
              </div>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-3 gap-4 pt-4 border-t border-white/10 text-xs">
              <div>
                <span className="text-slate-500 block mb-0.5 font-bold">사업자 등록번호</span>
                <span className="text-white font-mono font-medium">{selectedPartner.businessNumber}</span>
              </div>
              <div>
                <span className="text-slate-500 block mb-0.5 font-bold">대표자명</span>
                <span className="text-white font-medium">{selectedPartner.ceoName}</span>
              </div>
              <div>
                <span className="text-slate-500 block mb-0.5 font-bold">등록일자</span>
                <span className="text-white font-mono">{selectedPartner.registeredAt}</span>
              </div>
            </div>
          </div>

          {/* Quick Totals */}
          <div className="grid grid-cols-2 gap-4">
            <div className="p-4 rounded-2xl bg-emerald-500/5 border border-emerald-500/20">
              <span className="text-xs font-bold text-slate-400 block mb-1">총 차변 발생액 (채권/지출)</span>
              <AmountDisplay amount={totalDebit} className="text-xl font-black text-emerald-400" />
            </div>
            <div className="p-4 rounded-2xl bg-blue-500/5 border border-blue-500/20">
              <span className="text-xs font-bold text-slate-400 block mb-1">총 대변 발생액 (수익/채무)</span>
              <AmountDisplay amount={totalCredit} className="text-xl font-black text-blue-400" />
            </div>
          </div>

          {/* Partner Ledger Transactions Table */}
          <div>
            <h4 className="text-sm font-black text-white uppercase tracking-wider mb-4 flex items-center gap-2">
              <FileCheck2 size={16} className="text-blue-400" />
              거래처 원장 발생 내역
            </h4>

            {partnerTransactions.length === 0 ? (
              <div className="p-10 text-center text-slate-500 text-sm font-medium border border-dashed border-white/10 rounded-2xl">
                해당 거래처로 매핑된 전표 거래 내역이 없습니다.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="border-b border-white/10 text-[11px] font-black uppercase text-slate-400">
                      <th className="py-3 px-3">일자</th>
                      <th className="py-3 px-3">전표번호</th>
                      <th className="py-3 px-4">계정과목</th>
                      <th className="py-3 px-4">적요</th>
                      <th className="py-3 px-3 text-right">차변</th>
                      <th className="py-3 px-3 text-right">대변</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-white/5">
                    {partnerTransactions.map((tx, idx) => (
                      <tr key={idx} className="hover:bg-white/[0.03] transition-colors">
                        <td className="py-3 px-3 text-xs font-mono text-slate-300">{tx.date}</td>
                        <td className="py-3 px-3 text-xs font-mono font-bold text-blue-400">{tx.journalId}</td>
                        <td className="py-3 px-4 text-xs font-bold text-white">
                          [{tx.accountCode}] {getAccountName(tx.accountCode)}
                        </td>
                        <td className="py-3 px-4 text-xs text-slate-300">{tx.desc}</td>
                        <td className="py-3 px-3 text-right text-xs font-mono font-bold text-emerald-400">
                          {tx.debit > 0 ? `₩${tx.debit.toLocaleString()}` : '-'}
                        </td>
                        <td className="py-3 px-3 text-right text-xs font-mono font-bold text-blue-400">
                          {tx.credit > 0 ? `₩${tx.credit.toLocaleString()}` : '-'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>

      </div>
    </div>
  );
}

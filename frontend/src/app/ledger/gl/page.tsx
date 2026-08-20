'use client';

import React, { useState } from 'react';
import { 
  BookOpen, 
  Search, 
  Download, 
  ArrowUpDown, 
  Layers
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import AmountDisplay from '@/components/ui/AmountDisplay';
import { mockAccounts } from '@/mocks/master';
import { mockJournals } from '@/mocks/ledger';

interface AccountSummary {
  code: string;
  name: string;
  category: string;
  balanceType: 'DEBIT' | 'CREDIT';
  carriedOver: number;
  periodDebit: number;
  periodCredit: number;
  endingBalance: number;
}

export default function GeneralLedgerPage() {
  const [selectedAccountCode, setSelectedAccountCode] = useState<string>('1110200'); // Usually 보통예금
  const [searchQuery, setSearchQuery] = useState<string>('');

  // Compute summary for each account subject from mockJournals
  const accountSummaries: AccountSummary[] = mockAccounts
    .filter((acc) => acc.category === 'SUBJECT')
    .map((acc) => {
      // Calculate debit and credit totals from mockJournals
      let debitSum = 0;
      let creditSum = 0;

      mockJournals.forEach((j) => {
        j.lines.forEach((line) => {
          if (line.accountCode === acc.code) {
            debitSum += line.debit;
            creditSum += line.credit;
          }
        });
      });

      const carriedOver = 50000000; // Mock starting carried balance
      const endingBalance =
        acc.balanceType === 'DEBIT'
          ? carriedOver + debitSum - creditSum
          : carriedOver + creditSum - debitSum;

      return {
        code: acc.code,
        name: acc.name,
        category: acc.category,
        balanceType: acc.balanceType,
        carriedOver,
        periodDebit: debitSum,
        periodCredit: creditSum,
        endingBalance,
      };
    });

  const filteredSummaries = accountSummaries.filter((acc) => {
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      if (!acc.code.includes(q) && !acc.name.toLowerCase().includes(q)) {
        return false;
      }
    }
    return true;
  });

  const selectedSummary = accountSummaries.find((a) => a.code === selectedAccountCode) || accountSummaries[0];

  // Get line items for the selected account
  const selectedAccountTransactions = mockJournals
    .flatMap((j) =>
      j.lines
        .filter((l) => l.accountCode === selectedSummary.code)
        .map((l) => ({
          journalId: j.id,
          date: j.date,
          status: j.status,
          desc: l.desc,
          debit: l.debit,
          credit: l.credit,
          partnerId: l.partnerId,
        }))
    )
    .sort((a, b) => (a.date > b.date ? 1 : -1));

  return (
    <div className="space-y-8 pb-12">
      {/* Page Header */}
      <PageHeader
        title="총계정원장 (GL)"
        description="계정과목별 총잔액 및 당기 거래 내역을 실시간으로 확인하고 검토합니다."
        breadcrumbs={[
          { label: '재무/회계' },
          { label: '장부관리' },
          { label: '총계정원장' }
        ]}
        icon={BookOpen}
        actions={
          <button className="flex items-center gap-2 px-4 py-2.5 rounded-2xl bg-emerald-600/20 hover:bg-emerald-600/30 text-emerald-400 border border-emerald-500/30 font-black text-xs transition-all shadow-lg">
            <Download size={14} /> GL 원장 출력 (PDF/Excel)
          </button>
        }
      />

      {/* Grid Layout: Account List Left, GL Detail Right */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        
        {/* Left Column: Account Subjects Summary Table (4 cols) */}
        <div className="lg:col-span-5 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-6 shadow-xl space-y-5">
          <div className="flex items-center justify-between">
            <h3 className="text-base font-black text-white tracking-tight flex items-center gap-2">
              <Layers size={18} className="text-blue-400" />
              계정과목 선택
            </h3>
            <span className="text-xs font-mono font-bold text-slate-500">
              {filteredSummaries.length}개 과목
            </span>
          </div>

          <div className="relative">
            <Search className="absolute left-3.5 top-3 text-slate-500" size={16} />
            <input
              type="text"
              placeholder="계정코드 또는 계정명 검색..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full bg-slate-950/60 border border-white/10 rounded-xl pl-10 pr-3 py-2.5 text-white text-xs font-medium focus:outline-none focus:border-blue-500"
            />
          </div>

          <div className="space-y-2 max-h-[560px] overflow-y-auto pr-1">
            {filteredSummaries.map((acc) => {
              const isSelected = acc.code === selectedAccountCode;
              return (
                <div
                  key={acc.code}
                  onClick={() => setSelectedAccountCode(acc.code)}
                  className={`p-4 rounded-2xl border transition-all cursor-pointer flex items-center justify-between ${
                    isSelected
                      ? 'bg-blue-600/15 border-blue-500/40 shadow-lg'
                      : 'bg-white/[0.02] border-white/5 hover:bg-white/[0.06]'
                  }`}
                >
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <span className="font-mono text-xs font-black text-blue-400">
                        {acc.code}
                      </span>
                      <StatusBadge
                        status={acc.balanceType === 'DEBIT' ? '차변' : '대변'}
                        variant={acc.balanceType === 'DEBIT' ? 'info' : 'warning'}
                      />
                    </div>
                    <p className="text-sm font-bold text-white">{acc.name}</p>
                  </div>

                  <div className="text-right">
                    <div className="text-xs text-slate-400 font-bold mb-0.5">기말 잔액</div>
                    <AmountDisplay amount={acc.endingBalance} className="text-sm font-black" />
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Right Column: Selected GL Transaction Ledger (7 cols) */}
        <div className="lg:col-span-7 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-7 shadow-xl space-y-6">
          {/* Header Card for Selected Account */}
          <div className="p-6 rounded-2xl bg-white/[0.03] border border-white/10 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
            <div>
              <div className="flex items-center gap-2">
                <span className="font-mono text-sm font-black text-blue-400">
                  [{selectedSummary.code}]
                </span>
                <StatusBadge
                  status={selectedSummary.balanceType === 'DEBIT' ? '차변계정' : '대변계정'}
                  variant={selectedSummary.balanceType === 'DEBIT' ? 'info' : 'warning'}
                />
              </div>
              <h2 className="text-2xl font-black text-white tracking-tight mt-1">
                {selectedSummary.name}
              </h2>
            </div>

            <div className="flex items-center gap-6 text-right">
              <div>
                <span className="text-[11px] font-black uppercase text-slate-500 block">이월 잔액</span>
                <AmountDisplay amount={selectedSummary.carriedOver} className="text-sm font-bold" />
              </div>
              <div className="w-px h-8 bg-white/10" />
              <div>
                <span className="text-[11px] font-black uppercase text-blue-400 block">현재 총잔액</span>
                <AmountDisplay amount={selectedSummary.endingBalance} className="text-lg font-black text-emerald-400" />
              </div>
            </div>
          </div>

          {/* Quick Metrics: Period Debit & Period Credit */}
          <div className="grid grid-cols-2 gap-4">
            <div className="p-4 rounded-2xl bg-emerald-500/5 border border-emerald-500/20">
              <span className="text-xs font-bold text-slate-400 block mb-1">당기 차변 발생 합계</span>
              <AmountDisplay amount={selectedSummary.periodDebit} className="text-xl font-black text-emerald-400" />
            </div>
            <div className="p-4 rounded-2xl bg-blue-500/5 border border-blue-500/20">
              <span className="text-xs font-bold text-slate-400 block mb-1">당기 대변 발생 합계</span>
              <AmountDisplay amount={selectedSummary.periodCredit} className="text-xl font-black text-blue-400" />
            </div>
          </div>

          {/* Transactions Detail Table */}
          <div>
            <h4 className="text-sm font-black text-white uppercase tracking-wider mb-4 flex items-center gap-2">
              <ArrowUpDown size={16} className="text-blue-400" />
              세부 거래 원장 (Transactions)
            </h4>

            {selectedAccountTransactions.length === 0 ? (
              <div className="p-10 text-center text-slate-500 text-sm font-medium border border-dashed border-white/10 rounded-2xl">
                선택한 계정과목에 대한 당기 거래 내역이 존재하지 않습니다.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="border-b border-white/10 text-[11px] font-black uppercase text-slate-400">
                      <th className="py-3 px-3">일자</th>
                      <th className="py-3 px-3">전표번호</th>
                      <th className="py-3 px-4">적요</th>
                      <th className="py-3 px-3 text-right">차변</th>
                      <th className="py-3 px-3 text-right">대변</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-white/5">
                    {selectedAccountTransactions.map((tx, idx) => (
                      <tr key={idx} className="hover:bg-white/[0.03] transition-colors">
                        <td className="py-3 px-3 text-xs font-mono text-slate-300">{tx.date}</td>
                        <td className="py-3 px-3 text-xs font-mono font-bold text-blue-400">{tx.journalId}</td>
                        <td className="py-3 px-4 text-xs font-bold text-white">{tx.desc}</td>
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

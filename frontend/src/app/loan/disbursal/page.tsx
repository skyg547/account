'use client';

import React, { useState } from 'react';
import { 
  Coins, 
  Send, 
  CheckCircle2, 
  Clock, 
  AlertTriangle, 
  Building2, 
  ArrowRight, 
  CreditCard, 
  Search, 
  Plus, 
  FileCheck,
  ShieldAlert,
  ArrowUpRight
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import AmountDisplay from '@/components/ui/AmountDisplay';
import Tabs, { TabItem } from '@/components/ui/Tabs';
import EmptyState from '@/components/ui/EmptyState';
import { mockDisbursals, mockContracts, DisbursalDto } from '@/mocks/loan';

export default function LoanDisbursalPage() {
  const [disbursals, setDisbursals] = useState<DisbursalDto[]>(mockDisbursals);
  const [activeTab, setActiveTab] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  
  // Execution Form State
  const [selectedContractId, setSelectedContractId] = useState('');
  const [bankName, setBankName] = useState('하나은행');
  const [accountNumber, setAccountNumber] = useState('');
  const [disbursalAmount, setDisbursalAmount] = useState<number | ''>('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const tabs: TabItem[] = [
    { id: 'ALL', label: '전체 내역' },
    { id: 'PENDING', label: '실행 승인 대기' },
    { id: 'APPROVED', label: '실행 준비 완료' },
    { id: 'DISBURSED', label: '송금 완료' },
  ];

  const getBadgeProps = (status: DisbursalDto['status']) => {
    switch (status) {
      case 'PENDING':
        return { status: '승인 대기', variant: 'warning' as const };
      case 'APPROVED':
        return { status: '실행 준비 완료', variant: 'info' as const };
      case 'DISBURSED':
        return { status: '송금 완료', variant: 'success' as const };
      case 'REJECTED':
        return { status: '반려됨', variant: 'error' as const };
      default:
        return { status, variant: 'neutral' as const };
    }
  };

  const handleExecuteDisbursal = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedContractId || !accountNumber || !disbursalAmount) {
      alert('모든 필수 항목을 입력해주세요.');
      return;
    }

    setIsSubmitting(true);

    const contract = mockContracts.find(c => c.id === selectedContractId);
    setTimeout(() => {
      const newDisbursal: DisbursalDto = {
        disbursalId: `DISB-2026-00${disbursals.length + 1}`,
        contractId: selectedContractId,
        contractNo: contract ? contract.contractNo : 'LN-20260399-999',
        borrowerName: contract ? contract.borrowerName : '신규 차주',
        requestedAmount: Number(disbursalAmount),
        disbursedAmount: Number(disbursalAmount),
        bankName,
        accountNumber,
        requestDate: '2026-03-25',
        disbursalDate: '2026-03-25',
        status: 'DISBURSED',
        approvedBy: '직접 실행 (관리자)',
      };

      setDisbursals([newDisbursal, ...disbursals]);
      setIsSubmitting(false);
      setSuccessMessage(`${contract?.borrowerName || '차주'}에게 ₩${Number(disbursalAmount).toLocaleString()}원 대출 실행이 완료되었습니다.`);
      setSelectedContractId('');
      setAccountNumber('');
      setDisbursalAmount('');

      setTimeout(() => setSuccessMessage(null), 5000);
    }, 800);
  };

  const filteredDisbursals = disbursals.filter(d => {
    const matchesTab = activeTab === 'ALL' || d.status === activeTab;
    const matchesSearch = 
      d.contractNo.toLowerCase().includes(searchQuery.toLowerCase()) ||
      d.borrowerName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      d.bankName.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesTab && matchesSearch;
  });

  const totalDisbursed = disbursals
    .filter(d => d.status === 'DISBURSED')
    .reduce((acc, cur) => acc + cur.disbursedAmount, 0);

  const pendingCount = disbursals.filter(d => d.status === 'PENDING').length;

  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      <PageHeader
        title="대출 실행"
        description="승인된 대출 약정 건에 대하여 차주 지정 계좌로 자금을 실행(송금)하고 회계 분개를 생성합니다."
        breadcrumbs={[
          { label: '여신/대출 관리' },
          { label: '대출 실행' },
        ]}
        icon={Coins}
      />

      {/* Success Notification Banner */}
      {successMessage && (
        <div className="p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-sm font-bold flex items-center gap-3 animate-in slide-in-from-top-2">
          <CheckCircle2 size={20} />
          {successMessage}
        </div>
      )}

      {/* Summary KPI Bar */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md space-y-1">
          <span className="text-xs text-slate-400 font-bold uppercase tracking-wider">누적 총 대출 실행액</span>
          <div className="text-2xl font-black text-white">
            <AmountDisplay amount={totalDisbursed} />
          </div>
          <span className="text-xs text-slate-500 font-medium">실행 완료 건 누계 금액</span>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md space-y-1">
          <span className="text-xs text-slate-400 font-bold uppercase tracking-wider">승인 대기 건수</span>
          <div className="text-2xl font-black text-amber-400 font-mono">
            {pendingCount} <span className="text-sm font-normal text-slate-400">건</span>
          </div>
          <span className="text-xs text-slate-500 font-medium">검증 완료 후 즉시 송금 가능</span>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md space-y-1">
          <span className="text-xs text-slate-400 font-bold uppercase tracking-wider">자동 차/대 분개 연동</span>
          <div className="text-2xl font-black text-emerald-400 font-mono">
            100% 실시간
          </div>
          <span className="text-xs text-slate-500 font-medium">대출채권 계정 자동 생성</span>
        </div>
      </div>

      {/* Main Grid: Execution Form + List */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left Form: Disbursal Execution */}
        <div className="lg:col-span-1 p-6 rounded-3xl bg-slate-900/50 border border-white/10 backdrop-blur-md space-y-6 h-fit">
          <div className="flex items-center gap-3 border-b border-white/10 pb-4">
            <div className="p-2.5 rounded-xl bg-blue-500/10 text-blue-400 border border-blue-500/20">
              <Send size={20} />
            </div>
            <div>
              <h3 className="text-lg font-black text-white">대출 자금 실행 등록</h3>
              <p className="text-xs text-slate-400">약정 계좌로 자금을 송금 처리합니다.</p>
            </div>
          </div>

          <form onSubmit={handleExecuteDisbursal} className="space-y-4">
            <div>
              <label className="block text-xs font-bold text-slate-400 mb-2">대출 계약 선택 *</label>
              <select
                value={selectedContractId}
                onChange={(e) => {
                  setSelectedContractId(e.target.value);
                  const selected = mockContracts.find(c => c.id === e.target.value);
                  if (selected) {
                    setDisbursalAmount(selected.remainingBalance || selected.principalAmount);
                  }
                }}
                className="w-full px-4 py-2.5 bg-slate-950/80 border border-white/10 rounded-xl text-sm text-white focus:outline-none focus:border-blue-500/50"
                required
              >
                <option value="">-- 계약을 선택하세요 --</option>
                {mockContracts.map(c => (
                  <option key={c.id} value={c.id}>
                    [{c.contractNo}] {c.borrowerName} (₩{c.principalAmount.toLocaleString()})
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-400 mb-2">입금 은행 *</label>
              <select
                value={bankName}
                onChange={(e) => setBankName(e.target.value)}
                className="w-full px-4 py-2.5 bg-slate-950/80 border border-white/10 rounded-xl text-sm text-white focus:outline-none focus:border-blue-500/50"
              >
                <option value="하나은행">하나은행</option>
                <option value="신한은행">신한은행</option>
                <option value="KB국민은행">KB국민은행</option>
                <option value="우리은행">우리은행</option>
                <option value="기업은행">기업은행</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-400 mb-2">입금 계좌번호 *</label>
              <input
                type="text"
                placeholder="'-' 포함 또는 숫자만 입력"
                value={accountNumber}
                onChange={(e) => setAccountNumber(e.target.value)}
                className="w-full px-4 py-2.5 bg-slate-950/80 border border-white/10 rounded-xl text-sm text-white placeholder-slate-600 focus:outline-none focus:border-blue-500/50 font-mono"
                required
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-400 mb-2">실행금액 (KRW) *</label>
              <input
                type="number"
                placeholder="예: 500000000"
                value={disbursalAmount}
                onChange={(e) => setDisbursalAmount(e.target.value === '' ? '' : Number(e.target.value))}
                className="w-full px-4 py-2.5 bg-slate-950/80 border border-white/10 rounded-xl text-sm text-white font-mono focus:outline-none focus:border-blue-500/50"
                required
              />
            </div>

            <div className="p-3 rounded-xl bg-blue-500/5 border border-blue-500/10 text-xs text-blue-300 space-y-1">
              <span className="font-bold flex items-center gap-1"><ShieldAlert size={14} /> 자동 전표처리 안내</span>
              <p className="text-slate-400">실행 완료 즉시 (차) 대출채권 / (대) 보통예금 자동 회계 전표가 승인 처리됩니다.</p>
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full py-3 bg-blue-600 hover:bg-blue-500 disabled:opacity-50 text-white font-bold text-sm rounded-xl transition-all shadow-lg shadow-blue-600/20 flex items-center justify-center gap-2"
            >
              {isSubmitting ? '실행 처리 중...' : '대출 자금 실행 완료'}
              <ArrowRight size={16} />
            </button>
          </form>
        </div>

        {/* Right Section: History Table */}
        <div className="lg:col-span-2 space-y-4">
          <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
            <Tabs tabs={tabs} activeTab={activeTab} onChange={setActiveTab} />
            
            <div className="relative w-full sm:w-64">
              <Search size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500" />
              <input
                type="text"
                placeholder="차주명, 번호 검색..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-9 pr-3 py-2 bg-slate-900/50 border border-white/10 rounded-xl text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500/50"
              />
            </div>
          </div>

          {filteredDisbursals.length === 0 ? (
            <EmptyState
              icon={Coins}
              title="대출 실행 내역이 없습니다"
              description="해당하는 대출 자금 실행 기록이 없습니다."
            />
          ) : (
            <div className="rounded-2xl border border-white/5 bg-slate-900/50 backdrop-blur-md overflow-hidden">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm text-slate-300">
                  <thead className="bg-white/5 text-xs uppercase font-bold text-slate-400 border-b border-white/5">
                    <tr>
                      <th className="py-3.5 px-5">실행 ID / 계약번호</th>
                      <th className="py-3.5 px-5">차주명</th>
                      <th className="py-3.5 px-5">입금 은행 / 계좌</th>
                      <th className="py-3.5 px-5 text-right">실행 금액</th>
                      <th className="py-3.5 px-5">실행일자</th>
                      <th className="py-3.5 px-5 text-center">상태</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-white/5">
                    {filteredDisbursals.map((d) => {
                      const badge = getBadgeProps(d.status);
                      return (
                        <tr key={d.disbursalId} className="hover:bg-white/[0.02] transition-colors">
                          <td className="py-3.5 px-5">
                            <div className="font-mono text-xs text-blue-400 font-bold">{d.disbursalId}</div>
                            <div className="text-xs text-slate-400 font-mono">{d.contractNo}</div>
                          </td>
                          <td className="py-3.5 px-5 font-bold text-white">
                            {d.borrowerName}
                          </td>
                          <td className="py-3.5 px-5">
                            <div className="text-xs font-bold text-slate-200">{d.bankName}</div>
                            <div className="text-xs font-mono text-slate-500">{d.accountNumber}</div>
                          </td>
                          <td className="py-3.5 px-5 text-right font-bold text-white font-mono">
                            <AmountDisplay amount={d.requestedAmount} />
                          </td>
                          <td className="py-3.5 px-5 font-mono text-xs text-slate-400">
                            {d.disbursalDate}
                          </td>
                          <td className="py-3.5 px-5 text-center">
                            <StatusBadge status={badge.status} variant={badge.variant} />
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
      </div>
    </div>
  );
}

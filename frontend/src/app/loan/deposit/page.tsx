'use client';

import React, { useState, useMemo } from 'react';
import { 
  Landmark, 
  PiggyBank, 
  Percent, 
  TrendingUp, 
  Plus, 
  Search, 
  Building, 
  Coins, 
  Download, 
  Sparkles, 
  Calendar,
  CheckCircle2,
  Clock,
  ArrowUpRight,
  ShieldCheck
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import AmountDisplay from '@/components/ui/AmountDisplay';
import Tabs, { TabItem } from '@/components/ui/Tabs';
import EmptyState from '@/components/ui/EmptyState';
import { mockDepositAccounts, DepositAccountDto } from '@/mocks/loan';

export default function DepositAccountPage() {
  const [accounts, setAccounts] = useState<DepositAccountDto[]>(mockDepositAccounts);
  const [activeTab, setActiveTab] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [showModal, setShowModal] = useState(false);

  // Form State
  const [accountName, setAccountName] = useState('');
  const [bankName, setBankName] = useState('우리은행');
  const [accountNumber, setAccountNumber] = useState('');
  const [depositType, setDepositType] = useState<DepositAccountDto['depositType']>('TIME_DEPOSIT');
  const [principal, setPrincipal] = useState<number | ''>('');
  const [interestRate, setInterestRate] = useState<number | ''>('');

  const tabs: TabItem[] = [
    { id: 'ALL', label: '전체 계좌' },
    { id: 'TIME_DEPOSIT', label: '정기 예금' },
    { id: 'MONEY_MARKET', label: 'MMDA / 단기자금' },
    { id: 'SAVINGS', label: '자유 저축' },
    { id: 'NOTICE_DEPOSIT', label: '통지 예금' },
  ];

  const getDepositTypeLabel = (type: DepositAccountDto['depositType']) => {
    switch (type) {
      case 'TIME_DEPOSIT':
        return '정기 예금';
      case 'MONEY_MARKET':
        return 'MMDA / 단기';
      case 'SAVINGS':
        return '자유 저축';
      case 'NOTICE_DEPOSIT':
        return '통지 예금';
      default:
        return type;
    }
  };

  const getStatusBadgeProps = (status: DepositAccountDto['status']) => {
    switch (status) {
      case 'ACTIVE':
        return { status: '운용 중', variant: 'success' as const };
      case 'MATURED':
        return { status: '만기 도달', variant: 'info' as const };
      case 'CLOSED':
        return { status: '해지 완료', variant: 'neutral' as const };
      default:
        return { status, variant: 'neutral' as const };
    }
  };

  const filteredAccounts = useMemo(() => {
    return accounts.filter(acc => {
      const matchesTab = activeTab === 'ALL' || acc.depositType === activeTab;
      const matchesSearch = 
        acc.accountName.toLowerCase().includes(searchQuery.toLowerCase()) ||
        acc.accountNumber.toLowerCase().includes(searchQuery.toLowerCase()) ||
        acc.bankName.toLowerCase().includes(searchQuery.toLowerCase());
      return matchesTab && matchesSearch;
    });
  }, [accounts, activeTab, searchQuery]);

  const kpis = useMemo(() => {
    const totalPrincipal = accounts.reduce((acc, cur) => acc + cur.principal, 0);
    const totalAccruedInterest = accounts.reduce((acc, cur) => acc + cur.accruedInterest, 0);
    const totalNetInterest = accounts.reduce((acc, cur) => acc + cur.netInterest, 0);
    const avgRate = (accounts.reduce((acc, cur) => acc + cur.interestRate, 0) / accounts.length).toFixed(2);

    return { totalPrincipal, totalAccruedInterest, totalNetInterest, avgRate };
  }, [accounts]);

  const handleRegisterAccount = (e: React.FormEvent) => {
    e.preventDefault();
    if (!accountName || !accountNumber || !principal || !interestRate) {
      alert('필수 정보를 입력해 주세요.');
      return;
    }

    const principalNum = Number(principal);
    const rateNum = Number(interestRate);
    const accrued = Math.round(principalNum * (rateNum / 100) * 0.5); // 6개월 가상 경과
    const net = Math.round(accrued * (1 - 0.154));

    const newAccount: DepositAccountDto = {
      accountId: `DEP-2026-00${accounts.length + 1}`,
      accountNumber,
      accountName,
      bankName,
      depositType,
      principal: principalNum,
      currentBalance: principalNum,
      interestRate: rateNum,
      accruedInterest: accrued,
      taxRate: 15.4,
      netInterest: net,
      startDate: '2026-03-25',
      maturityDate: '2027-03-25',
      status: 'ACTIVE',
    };

    setAccounts([newAccount, ...accounts]);
    setShowModal(false);
    setAccountName('');
    setAccountNumber('');
    setPrincipal('');
    setInterestRate('');
    alert('신규 예금 계좌가 성공적으로 등록되었습니다.');
  };

  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      <PageHeader
        title="예금 계좌 관리"
        description="기업의 정기예금, MMDA, 단기 자금운용 계좌 현황 및 경과 이자수익(세전/세후)을 종합적으로 관리합니다."
        breadcrumbs={[
          { label: '여신/대출 관리' },
          { label: '예금 계좌 관리' },
        ]}
        icon={Landmark}
        actions={
          <div className="flex items-center gap-3">
            <button className="px-5 py-2.5 bg-white/5 hover:bg-white/10 text-slate-300 rounded-xl text-sm font-bold border border-white/10 transition-all flex items-center gap-2">
              <Download size={16} /> 이자조회 엑셀
            </button>
            <button 
              onClick={() => setShowModal(true)}
              className="px-5 py-2.5 bg-blue-600 hover:bg-blue-500 text-white rounded-xl text-sm font-bold shadow-lg shadow-blue-600/20 transition-all flex items-center gap-2"
            >
              <Plus size={16} /> 신규 예금 등록
            </button>
          </div>
        }
      />

      {/* Metric Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md space-y-2">
          <div className="flex items-center justify-between text-slate-400 text-xs font-bold uppercase tracking-wider">
            <span>총 예금 원금 합계</span>
            <PiggyBank size={18} className="text-blue-400" />
          </div>
          <div className="text-2xl font-black text-white">
            <AmountDisplay amount={kpis.totalPrincipal} />
          </div>
          <p className="text-xs text-slate-500 font-medium">전체 {accounts.length}개 예금 계좌 잔액</p>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md space-y-2">
          <div className="flex items-center justify-between text-slate-400 text-xs font-bold uppercase tracking-wider">
            <span>총 경과 이자 (세전)</span>
            <TrendingUp size={18} className="text-emerald-400" />
          </div>
          <div className="text-2xl font-black text-emerald-400">
            <AmountDisplay amount={kpis.totalAccruedInterest} />
          </div>
          <p className="text-xs text-slate-500 font-medium">당기 발생 누적 미수이자액</p>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md space-y-2">
          <div className="flex items-center justify-between text-slate-400 text-xs font-bold uppercase tracking-wider">
            <span>세후 순이자 수익 (15.4%)</span>
            <Sparkles size={18} className="text-amber-400" />
          </div>
          <div className="text-2xl font-black text-amber-400">
            <AmountDisplay amount={kpis.totalNetInterest} className="text-amber-400" />
          </div>
          <p className="text-xs text-slate-500 font-medium">이자소득세 공제 후 실 수령액</p>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md space-y-2">
          <div className="flex items-center justify-between text-slate-400 text-xs font-bold uppercase tracking-wider">
            <span>평균 예금 금리</span>
            <Percent size={18} className="text-indigo-400" />
          </div>
          <div className="text-2xl font-black text-white font-mono">
            {kpis.avgRate}%
          </div>
          <p className="text-xs text-slate-500 font-medium">운용 예금 평균 약정 이율</p>
        </div>
      </div>

      {/* Tabs and Search */}
      <div className="flex flex-col md:flex-row items-center justify-between gap-4">
        <Tabs tabs={tabs} activeTab={activeTab} onChange={setActiveTab} />

        <div className="relative w-full md:w-80">
          <Search size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500" />
          <input
            type="text"
            placeholder="계좌명, 은행, 계좌번호 검색..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-11 pr-4 py-2.5 bg-slate-900/50 border border-white/10 rounded-2xl text-sm text-white placeholder-slate-500 focus:outline-none focus:border-blue-500/50 transition-all"
          />
        </div>
      </div>

      {/* Account Table */}
      {filteredAccounts.length === 0 ? (
        <EmptyState
          icon={Landmark}
          title="등록된 예금 계좌가 없습니다"
          description="검색 조건에 맞는 예금 계좌 내역이 존재하지 않습니다."
        />
      ) : (
        <div className="rounded-2xl border border-white/5 bg-slate-900/50 backdrop-blur-md overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-300">
              <thead className="bg-white/5 text-xs uppercase font-bold tracking-wider text-slate-400 border-b border-white/5">
                <tr>
                  <th className="py-4 px-6">예금 상품명 / ID</th>
                  <th className="py-4 px-6">은행 / 계좌번호</th>
                  <th className="py-4 px-6 text-center">예금 유형</th>
                  <th className="py-4 px-6 text-right">예금 원금</th>
                  <th className="py-4 px-6 text-center">연 이율</th>
                  <th className="py-4 px-6 text-right text-emerald-400">경과 이자 (세전)</th>
                  <th className="py-4 px-6 text-right text-amber-400">세후 이자 (15.4%)</th>
                  <th className="py-4 px-6">만기일</th>
                  <th className="py-4 px-6 text-center">상태</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {filteredAccounts.map((acc) => {
                  const badge = getStatusBadgeProps(acc.status);
                  return (
                    <tr key={acc.accountId} className="hover:bg-white/[0.02] transition-colors">
                      <td className="py-4 px-6">
                        <div className="text-white font-bold">{acc.accountName}</div>
                        <div className="text-xs font-mono text-slate-500">{acc.accountId}</div>
                      </td>

                      <td className="py-4 px-6">
                        <div className="text-white font-bold">{acc.bankName}</div>
                        <div className="text-xs font-mono text-slate-400">{acc.accountNumber}</div>
                      </td>

                      <td className="py-4 px-6 text-center">
                        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold bg-white/5 border border-white/10 text-slate-300">
                          {getDepositTypeLabel(acc.depositType)}
                        </span>
                      </td>

                      <td className="py-4 px-6 text-right font-mono font-bold text-white">
                        <AmountDisplay amount={acc.principal} />
                      </td>

                      <td className="py-4 px-6 text-center font-mono font-bold text-amber-400">
                        {acc.interestRate}%
                      </td>

                      <td className="py-4 px-6 text-right font-mono font-bold text-emerald-400">
                        <AmountDisplay amount={acc.accruedInterest} className="text-emerald-400" />
                      </td>

                      <td className="py-4 px-6 text-right font-mono font-bold text-amber-400">
                        <AmountDisplay amount={acc.netInterest} className="text-amber-400" />
                      </td>

                      <td className="py-4 px-6 font-mono text-xs text-slate-400">
                        {acc.maturityDate}
                      </td>

                      <td className="py-4 px-6 text-center">
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

      {/* Modal for adding deposit account */}
      {showModal && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-white/10 rounded-3xl max-w-lg w-full p-6 space-y-6 shadow-2xl animate-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between border-b border-white/10 pb-4">
              <h3 className="text-xl font-black text-white flex items-center gap-2">
                <Landmark size={20} className="text-blue-400" /> 신규 예금 계좌 등록
              </h3>
              <button 
                onClick={() => setShowModal(false)}
                className="text-slate-400 hover:text-white text-sm font-bold bg-white/5 px-3 py-1.5 rounded-xl border border-white/10"
              >
                취소 ✕
              </button>
            </div>

            <form onSubmit={handleRegisterAccount} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-400 mb-2">예금 상품명 *</label>
                <input
                  type="text"
                  placeholder="예: 3년 만기 정기예금"
                  value={accountName}
                  onChange={(e) => setAccountName(e.target.value)}
                  className="w-full px-4 py-2.5 bg-slate-950/80 border border-white/10 rounded-xl text-sm text-white focus:outline-none focus:border-blue-500/50"
                  required
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-slate-400 mb-2">은행명 *</label>
                  <select
                    value={bankName}
                    onChange={(e) => setBankName(e.target.value)}
                    className="w-full px-4 py-2.5 bg-slate-950/80 border border-white/10 rounded-xl text-sm text-white focus:outline-none focus:border-blue-500/50"
                  >
                    <option value="우리은행">우리은행</option>
                    <option value="신한은행">신한은행</option>
                    <option value="하나은행">하나은행</option>
                    <option value="KB국민은행">KB국민은행</option>
                    <option value="NH농협은행">NH농협은행</option>
                    <option value="기업은행">기업은행</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-400 mb-2">예금 유형 *</label>
                  <select
                    value={depositType}
                    onChange={(e) => setDepositType(e.target.value as DepositAccountDto['depositType'])}
                    className="w-full px-4 py-2.5 bg-slate-950/80 border border-white/10 rounded-xl text-sm text-white focus:outline-none focus:border-blue-500/50"
                  >
                    <option value="TIME_DEPOSIT">정기 예금</option>
                    <option value="MONEY_MARKET">MMDA / 단기자금</option>
                    <option value="SAVINGS">자유 저축</option>
                    <option value="NOTICE_DEPOSIT">통지 예금</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-400 mb-2">계좌번호 *</label>
                <input
                  type="text"
                  placeholder="'-' 포함 입력"
                  value={accountNumber}
                  onChange={(e) => setAccountNumber(e.target.value)}
                  className="w-full px-4 py-2.5 bg-slate-950/80 border border-white/10 rounded-xl text-sm text-white font-mono focus:outline-none focus:border-blue-500/50"
                  required
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-slate-400 mb-2">예금 원금 (KRW) *</label>
                  <input
                    type="number"
                    placeholder="예: 1000000000"
                    value={principal}
                    onChange={(e) => setPrincipal(e.target.value === '' ? '' : Number(e.target.value))}
                    className="w-full px-4 py-2.5 bg-slate-950/80 border border-white/10 rounded-xl text-sm text-white font-mono focus:outline-none focus:border-blue-500/50"
                    required
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-400 mb-2">연 이율 (%) *</label>
                  <input
                    type="number"
                    step="0.01"
                    placeholder="예: 3.85"
                    value={interestRate}
                    onChange={(e) => setInterestRate(e.target.value === '' ? '' : Number(e.target.value))}
                    className="w-full px-4 py-2.5 bg-slate-950/80 border border-white/10 rounded-xl text-sm text-white font-mono focus:outline-none focus:border-blue-500/50"
                    required
                  />
                </div>
              </div>

              <div className="pt-4 flex items-center justify-end gap-3 border-t border-white/10">
                <button
                  type="submit"
                  className="px-6 py-2.5 bg-blue-600 hover:bg-blue-500 text-white font-bold text-sm rounded-xl transition-all shadow-lg shadow-blue-600/20"
                >
                  등록 등록 완료
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

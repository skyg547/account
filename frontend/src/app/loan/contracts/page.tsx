'use client';

import React, { useState, useMemo } from 'react';
import {
  FileText,
  Plus,
  Search,
  TrendingUp,
  Building2,
  Percent,
  ShieldCheck,
  Eye,
  Download,
  ArrowUpRight
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import AmountDisplay from '@/components/ui/AmountDisplay';
import Tabs, { TabItem } from '@/components/ui/Tabs';
import EmptyState from '@/components/ui/EmptyState';
import { mockContracts, LoanContractDto } from '@/mocks/loan';

export default function LoanContractsPage() {
  const [searchQuery, setSearchQuery] = useState('');
  const [activeTab, setActiveTab] = useState('ALL');
  const [selectedContract, setSelectedContract] = useState<LoanContractDto | null>(null);

  const statusTabs: TabItem[] = [
    { id: 'ALL', label: '전체 보기' },
    { id: 'ACTIVE', label: '정상 실행중' },
    { id: 'PENDING_DISBURSAL', label: '실행 대기' },
    { id: 'RESTRUCTURED', label: '조건 재조정' },
    { id: 'DEFAULTED', label: '연체/부실' },
    { id: 'COMPLETED', label: '상환 완료' },
  ];

  const getStatusBadgeProps = (status: LoanContractDto['status']) => {
    switch (status) {
      case 'ACTIVE':
        return { status: '정상 실행중', variant: 'success' as const };
      case 'PENDING_DISBURSAL':
        return { status: '실행 대기', variant: 'warning' as const };
      case 'RESTRUCTURED':
        return { status: '조건 재조정', variant: 'info' as const };
      case 'DEFAULTED':
        return { status: '연체/부실', variant: 'error' as const };
      case 'COMPLETED':
        return { status: '상환 완료', variant: 'neutral' as const };
      default:
        return { status, variant: 'neutral' as const };
    }
  };

  const filteredContracts = useMemo(() => {
    return mockContracts.filter(contract => {
      const matchesTab = activeTab === 'ALL' || contract.status === activeTab;
      const matchesSearch = 
        contract.contractNo.toLowerCase().includes(searchQuery.toLowerCase()) ||
        contract.borrowerName.toLowerCase().includes(searchQuery.toLowerCase()) ||
        contract.productName.toLowerCase().includes(searchQuery.toLowerCase());
      return matchesTab && matchesSearch;
    });
  }, [activeTab, searchQuery]);

  const kpis = useMemo(() => {
    const totalPrincipal = mockContracts.reduce((acc, cur) => acc + cur.principalAmount, 0);
    const activeBalance = mockContracts
      .filter(c => c.status === 'ACTIVE' || c.status === 'RESTRUCTURED')
      .reduce((acc, cur) => acc + cur.remainingBalance, 0);
    const avgEir = (mockContracts.reduce((acc, cur) => acc + cur.effectiveRate, 0) / mockContracts.length).toFixed(2);
    const activeCount = mockContracts.filter(c => c.status === 'ACTIVE').length;

    return { totalPrincipal, activeBalance, avgEir, activeCount };
  }, []);

  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      {/* Page Header */}
      <PageHeader
        title="대출 계약 관리"
        description="여신 대출 계약의 총괄 현황, 약정이율 및 유효이자율(EIR) 상각 기초 데이터를 관리합니다."
        breadcrumbs={[
          { label: '여신/대출 관리' },
          { label: '대출 계약 관리' },
        ]}
        icon={FileText}
        actions={
          <div className="flex items-center gap-3">
            <button className="px-5 py-2.5 bg-white/5 hover:bg-white/10 text-slate-300 rounded-xl text-sm font-bold border border-white/10 transition-all flex items-center gap-2">
              <Download size={16} /> 엑셀 내보내기
            </button>
            <button className="px-5 py-2.5 bg-blue-600 hover:bg-blue-500 text-white rounded-xl text-sm font-bold shadow-lg shadow-blue-600/20 transition-all flex items-center gap-2">
              <Plus size={16} /> 신규 대출 등록
            </button>
          </div>
        }
      />

      {/* Summary KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md space-y-2">
          <div className="flex items-center justify-between text-slate-400 text-xs font-bold uppercase tracking-wider">
            <span>총 약정 금액</span>
            <Building2 size={18} className="text-blue-400" />
          </div>
          <div className="text-2xl font-black text-white">
            <AmountDisplay amount={kpis.totalPrincipal} />
          </div>
          <p className="text-xs text-slate-500 font-medium">전체 {mockContracts.length}건 대출 약정 총합</p>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md space-y-2">
          <div className="flex items-center justify-between text-slate-400 text-xs font-bold uppercase tracking-wider">
            <span>현재 대출 잔액</span>
            <TrendingUp size={18} className="text-emerald-400" />
          </div>
          <div className="text-2xl font-black text-emerald-400">
            <AmountDisplay amount={kpis.activeBalance} />
          </div>
          <p className="text-xs text-slate-500 font-medium">운용 중인 정상/재조정 대출 잔액</p>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md space-y-2">
          <div className="flex items-center justify-between text-slate-400 text-xs font-bold uppercase tracking-wider">
            <span>평균 유효이자율 (EIR)</span>
            <Percent size={18} className="text-amber-400" />
          </div>
          <div className="text-2xl font-black text-white font-mono">
            {kpis.avgEir}%
          </div>
          <p className="text-xs text-slate-500 font-medium">부대수수료/원가 반영 가중평균</p>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md space-y-2">
          <div className="flex items-center justify-between text-slate-400 text-xs font-bold uppercase tracking-wider">
            <span>정상 실행 계약</span>
            <ShieldCheck size={18} className="text-indigo-400" />
          </div>
          <div className="text-2xl font-black text-white font-mono">
            {kpis.activeCount} <span className="text-sm font-normal text-slate-400">건</span>
          </div>
          <p className="text-xs text-slate-500 font-medium">전체 대비 정상 가동 비율 {(kpis.activeCount / mockContracts.length * 100).toFixed(0)}%</p>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col md:flex-row items-center justify-between gap-4">
        <Tabs tabs={statusTabs} activeTab={activeTab} onChange={setActiveTab} />

        <div className="relative w-full md:w-80">
          <Search size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500" />
          <input
            type="text"
            placeholder="차주명, 계약번호, 상품명 검색..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-11 pr-4 py-2.5 bg-slate-900/50 border border-white/10 rounded-2xl text-sm text-white placeholder-slate-500 focus:outline-none focus:border-blue-500/50 transition-all"
          />
        </div>
      </div>

      {/* Contracts Table */}
      {filteredContracts.length === 0 ? (
        <EmptyState
          icon={FileText}
          title="검색 결과가 없습니다"
          description="선택한 조건에 해당하는 대출 계약이 존재하지 않습니다. 다른 검색어나 필터를 선택해 보세요."
        />
      ) : (
        <div className="rounded-2xl border border-white/5 bg-slate-900/50 backdrop-blur-md overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-300">
              <thead className="bg-white/5 text-xs uppercase font-bold tracking-wider text-slate-400 border-b border-white/5">
                <tr>
                  <th className="py-4 px-6">계약 정보</th>
                  <th className="py-4 px-6">차주명 / ID</th>
                  <th className="py-4 px-6">약정 금액 / 잔액</th>
                  <th className="py-4 px-6">약정이율 / EIR</th>
                  <th className="py-4 px-6">상환 방식</th>
                  <th className="py-4 px-6">계약 기간</th>
                  <th className="py-4 px-6 text-center">상태</th>
                  <th className="py-4 px-6 text-right">상세</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {filteredContracts.map((contract) => {
                  const badgeProps = getStatusBadgeProps(contract.status);
                  return (
                    <tr 
                      key={contract.id}
                      className="hover:bg-white/[0.02] transition-colors group cursor-pointer"
                      onClick={() => setSelectedContract(contract)}
                    >
                      <td className="py-4 px-6 font-medium">
                        <div className="font-mono text-xs text-blue-400 font-bold">{contract.contractNo}</div>
                        <div className="text-white font-bold text-sm mt-0.5">{contract.productName}</div>
                      </td>

                      <td className="py-4 px-6">
                        <div className="text-white font-bold">{contract.borrowerName}</div>
                        <div className="text-xs text-slate-500 font-mono">{contract.borrowerId}</div>
                      </td>

                      <td className="py-4 px-6">
                        <div className="text-white font-bold">
                          <AmountDisplay amount={contract.principalAmount} />
                        </div>
                        <div className="text-xs text-slate-400 flex items-center gap-1 mt-0.5">
                          잔액: <AmountDisplay amount={contract.remainingBalance} className="text-slate-400" />
                        </div>
                      </td>

                      <td className="py-4 px-6 font-mono">
                        <div className="text-white font-bold">{contract.nominalRate}%</div>
                        <div className="text-xs text-amber-400 flex items-center gap-1 font-semibold">
                          EIR: {contract.effectiveRate}%
                        </div>
                      </td>

                      <td className="py-4 px-6 text-xs text-slate-300 font-medium">
                        {contract.repaymentType === 'BULLET' && '원금일시상환'}
                        {contract.repaymentType === 'EQUAL_PRINCIPAL' && '원금균등상환'}
                        {contract.repaymentType === 'EQUAL_INSTALLMENT' && '원리금균등상환'}
                      </td>

                      <td className="py-4 px-6 font-mono text-xs text-slate-400">
                        <div>{contract.startDate}</div>
                        <div className="text-slate-500">~ {contract.maturityDate} ({contract.termMonths}개월)</div>
                      </td>

                      <td className="py-4 px-6 text-center">
                        <StatusBadge status={badgeProps.status} variant={badgeProps.variant} />
                      </td>

                      <td className="py-4 px-6 text-right">
                        <button 
                          onClick={(e) => {
                            e.stopPropagation();
                            setSelectedContract(contract);
                          }}
                          className="p-2 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl text-slate-400 hover:text-white transition-all"
                        >
                          <Eye size={16} />
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Contract Detail Modal / Drawer */}
      {selectedContract && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-white/10 rounded-3xl max-w-2xl w-full p-6 space-y-6 shadow-2xl animate-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between border-b border-white/10 pb-4">
              <div>
                <div className="text-xs font-mono text-blue-400 font-bold">{selectedContract.contractNo}</div>
                <h3 className="text-xl font-black text-white">{selectedContract.borrowerName}</h3>
              </div>
              <button 
                onClick={() => setSelectedContract(null)}
                className="text-slate-400 hover:text-white text-sm font-bold bg-white/5 px-3 py-1.5 rounded-xl border border-white/10"
              >
                닫기 ✕
              </button>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="p-4 rounded-xl bg-white/5 border border-white/5 space-y-1">
                <span className="text-xs text-slate-400">대출 상품명</span>
                <p className="text-sm font-bold text-white">{selectedContract.productName}</p>
              </div>

              <div className="p-4 rounded-xl bg-white/5 border border-white/5 space-y-1">
                <span className="text-xs text-slate-400">담보 종류</span>
                <p className="text-sm font-bold text-slate-300">{selectedContract.collateralType || '신용대출'}</p>
              </div>

              <div className="p-4 rounded-xl bg-white/5 border border-white/5 space-y-1">
                <span className="text-xs text-slate-400">원금 / 잔액</span>
                <p className="text-sm font-bold text-white">
                  <AmountDisplay amount={selectedContract.principalAmount} /> / <AmountDisplay amount={selectedContract.remainingBalance} />
                </p>
              </div>

              <div className="p-4 rounded-xl bg-white/5 border border-white/5 space-y-1">
                <span className="text-xs text-slate-400">명목 금리 / EIR</span>
                <p className="text-sm font-mono font-bold text-amber-400">
                  {selectedContract.nominalRate}% / {selectedContract.effectiveRate}%
                </p>
              </div>

              <div className="p-4 rounded-xl bg-white/5 border border-white/5 space-y-1">
                <span className="text-xs text-slate-400">이연 대출 수수료</span>
                <p className="text-sm font-bold text-white">
                  <AmountDisplay amount={selectedContract.deferredFee} />
                </p>
              </div>

              <div className="p-4 rounded-xl bg-white/5 border border-white/5 space-y-1">
                <span className="text-xs text-slate-400">이연 대출 원가</span>
                <p className="text-sm font-bold text-white">
                  <AmountDisplay amount={selectedContract.deferredCost} />
                </p>
              </div>
            </div>

            <div className="flex items-center justify-end gap-3 pt-4 border-t border-white/10">
              <a
                href={`/loan/amortization?contractId=${selectedContract.id}`}
                className="px-5 py-2.5 bg-blue-600 hover:bg-blue-500 text-white rounded-xl text-sm font-bold transition-all flex items-center gap-2"
              >
                상각 스케줄 보기 <ArrowUpRight size={16} />
              </a>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

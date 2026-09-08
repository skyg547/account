'use client';

import React, { useState, useMemo } from 'react';
import {
  Layers,
  Plus,
  Search,
  TrendingDown,
  TrendingUp,
  PieChart,
  Sparkles,
  Download
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import AmountDisplay from '@/components/ui/AmountDisplay';
import Tabs, { TabItem } from '@/components/ui/Tabs';
import EmptyState from '@/components/ui/EmptyState';
import { mockDeferredItems, mockContracts, DeferredFeeCostDto } from '@/mocks/loan';

export default function DeferredFeeCostPage() {
  const [items, setItems] = useState<DeferredFeeCostDto[]>(mockDeferredItems);
  const [activeTab, setActiveTab] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [showAddModal, setShowAddModal] = useState(false);

  // Form state
  const [contractId, setContractId] = useState('');
  const [type, setType] = useState<'FEE' | 'COST'>('FEE');
  const [category, setCategory] = useState('');
  const [amount, setAmount] = useState<number | ''>('');
  const [amortizationMethod, setAmortizationMethod] = useState<'EIR' | 'STRAIGHT_LINE'>('EIR');

  const tabs: TabItem[] = [
    { id: 'ALL', label: '전체 항목' },
    { id: 'FEE', label: '이연 수수료 (Fee)' },
    { id: 'COST', label: '이연 부대원가 (Cost)' },
    { id: 'AMORTIZING', label: '상각 진행 중' },
    { id: 'FULLY_AMORTIZED', label: '상각 완료' },
  ];

  const filteredItems = useMemo(() => {
    return items.filter(item => {
      let matchesTab = true;
      if (activeTab === 'FEE') matchesTab = item.type === 'FEE';
      else if (activeTab === 'COST') matchesTab = item.type === 'COST';
      else if (activeTab === 'AMORTIZING') matchesTab = item.status === 'AMORTIZING';
      else if (activeTab === 'FULLY_AMORTIZED') matchesTab = item.status === 'FULLY_AMORTIZED';

      const matchesSearch = 
        item.contractNo.toLowerCase().includes(searchQuery.toLowerCase()) ||
        item.borrowerName.toLowerCase().includes(searchQuery.toLowerCase()) ||
        item.category.toLowerCase().includes(searchQuery.toLowerCase());

      return matchesTab && matchesSearch;
    });
  }, [items, activeTab, searchQuery]);

  const kpis = useMemo(() => {
    const totalFees = items.filter(i => i.type === 'FEE').reduce((acc, cur) => acc + cur.unamortizedBalance, 0);
    const totalCosts = items.filter(i => i.type === 'COST').reduce((acc, cur) => acc + cur.unamortizedBalance, 0);
    const netBalance = totalFees - totalCosts;
    const monthlyAmortization = items.reduce((acc, cur) => acc + (cur.amortizedAmount / 12), 0);

    return { totalFees, totalCosts, netBalance, monthlyAmortization };
  }, [items]);

  const handleAddItem = (e: React.FormEvent) => {
    e.preventDefault();
    if (!contractId || !category || !amount) {
      alert('모든 필수 항목을 입력해 주세요.');
      return;
    }

    const contract = mockContracts.find(c => c.id === contractId);
    const newItem: DeferredFeeCostDto = {
      id: `DEF-${type}-${items.length + 101}`,
      contractId,
      contractNo: contract ? contract.contractNo : 'LN-20260399-001',
      borrowerName: contract ? contract.borrowerName : '차주',
      type,
      category,
      amount: Number(amount),
      amortizedAmount: 0,
      unamortizedBalance: Number(amount),
      amortizationMethod,
      startDate: '2026-03-25',
      endDate: contract ? contract.maturityDate : '2029-03-25',
      status: 'AMORTIZING',
    };

    setItems([newItem, ...items]);
    setShowAddModal(false);
    setCategory('');
    setAmount('');
  };

  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      <PageHeader
        title="이연 수수료/원가"
        description="대출 발생 시 직접 관련된 부대수수료 및 부대원가를 이연하여 유효이자율법(EIR)에 따라 대출 기간 동안 상각 처리합니다."
        breadcrumbs={[
          { label: '여신/대출 관리' },
          { label: '이연 수수료/원가' },
        ]}
        icon={Layers}
        actions={
          <div className="flex items-center gap-3">
            <button className="px-5 py-2.5 bg-white/5 hover:bg-white/10 text-slate-300 rounded-xl text-sm font-bold border border-white/10 transition-all flex items-center gap-2">
              <Download size={16} /> 상각보고서 다운로드
            </button>
            <button 
              onClick={() => setShowAddModal(true)}
              className="px-5 py-2.5 bg-blue-600 hover:bg-blue-500 text-white rounded-xl text-sm font-bold shadow-lg shadow-blue-600/20 transition-all flex items-center gap-2"
            >
              <Plus size={16} /> 이연 항목 추가
            </button>
          </div>
        }
      />

      {/* Metric Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md space-y-2">
          <div className="flex items-center justify-between text-slate-400 text-xs font-bold uppercase tracking-wider">
            <span>미상각 이연수수료 (대)</span>
            <TrendingUp size={18} className="text-emerald-400" />
          </div>
          <div className="text-2xl font-black text-emerald-400">
            <AmountDisplay amount={kpis.totalFees} />
          </div>
          <p className="text-xs text-slate-500 font-medium">이자수익가산 처리 대상 부대수수료</p>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md space-y-2">
          <div className="flex items-center justify-between text-slate-400 text-xs font-bold uppercase tracking-wider">
            <span>미상각 이연부대원가 (차)</span>
            <TrendingDown size={18} className="text-rose-400" />
          </div>
          <div className="text-2xl font-black text-rose-400">
            <AmountDisplay amount={kpis.totalCosts} />
          </div>
          <p className="text-xs text-slate-500 font-medium">이자수익차감 처리 대상 부대비용</p>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md space-y-2">
          <div className="flex items-center justify-between text-slate-400 text-xs font-bold uppercase tracking-wider">
            <span>순 이연 차감/가산액</span>
            <PieChart size={18} className="text-blue-400" />
          </div>
          <div className="text-2xl font-black text-white">
            <AmountDisplay amount={kpis.netBalance} showSign />
          </div>
          <p className="text-xs text-slate-500 font-medium">장부가액 차감(대)/가산(차) 정산액</p>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md space-y-2">
          <div className="flex items-center justify-between text-slate-400 text-xs font-bold uppercase tracking-wider">
            <span>월 평균 상각인식액</span>
            <Sparkles size={18} className="text-amber-400" />
          </div>
          <div className="text-2xl font-black text-white">
            <AmountDisplay amount={Math.round(kpis.monthlyAmortization)} />
          </div>
          <p className="text-xs text-slate-500 font-medium">월 결산 시 실효이자 손익 조정</p>
        </div>
      </div>

      {/* Filter and Search */}
      <div className="flex flex-col md:flex-row items-center justify-between gap-4">
        <Tabs tabs={tabs} activeTab={activeTab} onChange={setActiveTab} />

        <div className="relative w-full md:w-80">
          <Search size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500" />
          <input
            type="text"
            placeholder="계약번호, 차주, 항목명 검색..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-11 pr-4 py-2.5 bg-slate-900/50 border border-white/10 rounded-2xl text-sm text-white placeholder-slate-500 focus:outline-none focus:border-blue-500/50 transition-all"
          />
        </div>
      </div>

      {/* Data Table */}
      {filteredItems.length === 0 ? (
        <EmptyState
          icon={Layers}
          title="이연 항목이 없습니다"
          description="선택한 조건에 해당하는 이연 수수료 또는 부대원가가 없습니다."
        />
      ) : (
        <div className="rounded-2xl border border-white/5 bg-slate-900/50 backdrop-blur-md overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-300">
              <thead className="bg-white/5 text-xs uppercase font-bold tracking-wider text-slate-400 border-b border-white/5">
                <tr>
                  <th className="py-4 px-6">이연 항목명 / ID</th>
                  <th className="py-4 px-6">계약번호 / 차주명</th>
                  <th className="py-4 px-6 text-center">구분</th>
                  <th className="py-4 px-6 text-right">최초 발생액</th>
                  <th className="py-4 px-6 text-right">누적 상각액</th>
                  <th className="py-4 px-6 text-right">미상각 잔액</th>
                  <th className="py-4 px-6 text-center">상각 방식</th>
                  <th className="py-4 px-6">상각 기간</th>
                  <th className="py-4 px-6 text-center">상태</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {filteredItems.map((item) => (
                  <tr key={item.id} className="hover:bg-white/[0.02] transition-colors">
                    <td className="py-4 px-6">
                      <div className="text-white font-bold">{item.category}</div>
                      <div className="text-xs font-mono text-slate-500">{item.id}</div>
                    </td>

                    <td className="py-4 px-6">
                      <div className="font-mono text-xs text-blue-400 font-bold">{item.contractNo}</div>
                      <div className="text-white font-bold text-xs mt-0.5">{item.borrowerName}</div>
                    </td>

                    <td className="py-4 px-6 text-center">
                      <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold ${
                        item.type === 'FEE' 
                          ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                          : 'bg-rose-500/10 text-rose-400 border border-rose-500/20'
                      }`}>
                        {item.type === 'FEE' ? '이연수수료' : '이연부대원가'}
                      </span>
                    </td>

                    <td className="py-4 px-6 text-right font-mono font-bold text-white">
                      <AmountDisplay amount={item.amount} />
                    </td>

                    <td className="py-4 px-6 text-right font-mono text-slate-400">
                      <AmountDisplay amount={item.amortizedAmount} className="text-slate-400" />
                    </td>

                    <td className="py-4 px-6 text-right font-mono font-bold text-amber-400">
                      <AmountDisplay amount={item.unamortizedBalance} className="text-amber-400" />
                    </td>

                    <td className="py-4 px-6 text-center font-mono text-xs text-slate-300">
                      {item.amortizationMethod === 'EIR' ? '유효이자율법(EIR)' : '직선법'}
                    </td>

                    <td className="py-4 px-6 font-mono text-xs text-slate-400">
                      {item.startDate} ~ {item.endDate}
                    </td>

                    <td className="py-4 px-6 text-center">
                      <StatusBadge 
                        status={item.status === 'AMORTIZING' ? '상각 중' : '상각 완료'} 
                        variant={item.status === 'AMORTIZING' ? 'info' : 'neutral'} 
                      />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Add Deferred Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-white/10 rounded-3xl max-w-lg w-full p-6 space-y-6 shadow-2xl animate-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between border-b border-white/10 pb-4">
              <h3 className="text-xl font-black text-white flex items-center gap-2">
                <Plus size={20} className="text-blue-400" /> 신규 이연 항목 등록
              </h3>
              <button 
                onClick={() => setShowAddModal(false)}
                className="text-slate-400 hover:text-white text-sm font-bold bg-white/5 px-3 py-1.5 rounded-xl border border-white/10"
              >
                취소 ✕
              </button>
            </div>

            <form onSubmit={handleAddItem} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-400 mb-2">대상 대출 계약 *</label>
                <select
                  value={contractId}
                  onChange={(e) => setContractId(e.target.value)}
                  className="w-full px-4 py-2.5 bg-slate-950/80 border border-white/10 rounded-xl text-sm text-white focus:outline-none focus:border-blue-500/50"
                  required
                >
                  <option value="">-- 계약 선택 --</option>
                  {mockContracts.map(c => (
                    <option key={c.id} value={c.id}>
                      [{c.contractNo}] {c.borrowerName}
                    </option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-slate-400 mb-2">이연 구분 *</label>
                  <select
                    value={type}
                    onChange={(e) => setType(e.target.value as 'FEE' | 'COST')}
                    className="w-full px-4 py-2.5 bg-slate-950/80 border border-white/10 rounded-xl text-sm text-white focus:outline-none focus:border-blue-500/50"
                  >
                    <option value="FEE">이연 수수료 (Fee)</option>
                    <option value="COST">이연 부대원가 (Cost)</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-400 mb-2">상각 방식 *</label>
                  <select
                    value={amortizationMethod}
                    onChange={(e) => setAmortizationMethod(e.target.value as 'EIR' | 'STRAIGHT_LINE')}
                    className="w-full px-4 py-2.5 bg-slate-950/80 border border-white/10 rounded-xl text-sm text-white focus:outline-none focus:border-blue-500/50"
                  >
                    <option value="EIR">유효이자율법 (EIR)</option>
                    <option value="STRAIGHT_LINE">직선법 (Straight Line)</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-400 mb-2">이연 항목명 *</label>
                <input
                  type="text"
                  placeholder="예: 대출취급 수수료, 법률자문료 등"
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  className="w-full px-4 py-2.5 bg-slate-950/80 border border-white/10 rounded-xl text-sm text-white placeholder-slate-600 focus:outline-none focus:border-blue-500/50"
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-400 mb-2">발생 금액 (KRW) *</label>
                <input
                  type="number"
                  placeholder="금액 입력"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value === '' ? '' : Number(e.target.value))}
                  className="w-full px-4 py-2.5 bg-slate-950/80 border border-white/10 rounded-xl text-sm text-white font-mono focus:outline-none focus:border-blue-500/50"
                  required
                />
              </div>

              <div className="pt-4 flex items-center justify-end gap-3 border-t border-white/10">
                <button
                  type="submit"
                  className="px-6 py-2.5 bg-blue-600 hover:bg-blue-500 text-white font-bold text-sm rounded-xl transition-all shadow-lg shadow-blue-600/20"
                >
                  등록 완료
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

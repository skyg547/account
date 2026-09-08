"use client";

import React, { useState, useMemo } from 'react';
import {
  FileText,
  Plus,
  Search,
  Building2,
  Percent,
  CheckCircle2,
  XCircle,
  Eye,
  CreditCard,
  Layers
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import AmountDisplay from '@/components/ui/AmountDisplay';
import Tabs, { TabItem } from '@/components/ui/Tabs';
import EmptyState from '@/components/ui/EmptyState';
import { mockLeases, LeaseContractDto } from '@/mocks/fair-value';

export default function LeaseContractsPage() {
  const [leases, setLeases] = useState<LeaseContractDto[]>(mockLeases);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedAssetType, setSelectedAssetType] = useState<string>('ALL');
  const [selectedLease, setSelectedLease] = useState<LeaseContractDto | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  // Form State
  const [newLease, setNewLease] = useState({
    contractName: '',
    lessor: '',
    lesseeDepartment: '경영지원본부',
    assetType: '부동산' as LeaseContractDto['assetType'],
    startDate: new Date().toISOString().split('T')[0],
    leaseTermMonths: 36,
    monthlyPayment: 10000000,
    discountRate: 4.5,
    underlyingAsset: '',
  });

  const typeTabs: TabItem[] = [
    { id: 'ALL', label: '전체 계약' },
    { id: '부동산', label: '부동산 (임대차)' },
    { id: '차량', label: '차량 렌트/리스' },
    { id: 'IT장비', label: 'IT/서버 장비' },
    { id: '기계설비', label: '기계/생산 설비' },
  ];

  const filteredLeases = useMemo(() => {
    return leases.filter(l => {
      const matchSearch = 
        l.contractName.toLowerCase().includes(searchTerm.toLowerCase()) ||
        l.contractNo.toLowerCase().includes(searchTerm.toLowerCase()) ||
        l.lessor.toLowerCase().includes(searchTerm.toLowerCase()) ||
        l.lesseeDepartment.toLowerCase().includes(searchTerm.toLowerCase());
      const matchType = selectedAssetType === 'ALL' || l.assetType === selectedAssetType;
      return matchSearch && matchType;
    });
  }, [leases, searchTerm, selectedAssetType]);

  // KPI Calculations
  const totalRouAsset = useMemo(() => leases.reduce((sum, l) => sum + l.rouAssetValue, 0), [leases]);
  const totalLeaseLiability = useMemo(() => leases.reduce((sum, l) => sum + l.leaseLiability, 0), [leases]);
  const activeCount = useMemo(() => leases.filter(l => l.status === 'ACTIVE' || l.status === 'MODIFIED').length, [leases]);
  const avgDiscountRate = useMemo(() => {
    if (leases.length === 0) return 0;
    const sum = leases.reduce((s, l) => s + l.discountRate, 0);
    return (sum / leases.length).toFixed(2);
  }, [leases]);

  const handleRegisterLease = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newLease.contractName || !newLease.lessor) return;

    // Approximate present value (ROU Asset & Liability) for UI creation
    const months = Number(newLease.leaseTermMonths);
    const monthlyRate = Number(newLease.discountRate) / 100 / 12;
    const payment = Number(newLease.monthlyPayment);
    // Simple PV formula: PMT * [(1 - (1+r)^-n) / r]
    const pv = Math.round(payment * ((1 - Math.pow(1 + monthlyRate, -months)) / monthlyRate));

    const startDateObj = new Date(newLease.startDate);
    const endDateObj = new Date(startDateObj);
    endDateObj.setMonth(endDateObj.getMonth() + months);
    const endDateStr = endDateObj.toISOString().split('T')[0];

    const created: LeaseContractDto = {
      id: `ls-${Date.now()}`,
      contractNo: `IFRS16-2026-${String(leases.length + 1).padStart(2, '0')}`,
      contractName: newLease.contractName,
      lessor: newLease.lessor,
      lesseeDepartment: newLease.lesseeDepartment,
      assetType: newLease.assetType,
      startDate: newLease.startDate,
      endDate: endDateStr,
      leaseTermMonths: months,
      monthlyPayment: payment,
      discountRate: Number(newLease.discountRate),
      rouAssetValue: pv,
      leaseLiability: pv,
      accumulatedDepreciation: 0,
      status: 'ACTIVE',
      underlyingAsset: newLease.underlyingAsset || newLease.contractName,
    };

    setLeases(prev => [created, ...prev]);
    setIsModalOpen(false);
    setToastMessage(`[${created.contractNo}] IFRS 16 리스 계약이 신규 등록되었습니다 (사용권자산 ₩${pv.toLocaleString()} 계상).`);
    setTimeout(() => setToastMessage(null), 5000);
  };

  const getStatusVariant = (status: LeaseContractDto['status']) => {
    switch (status) {
      case 'ACTIVE': return 'success';
      case 'MODIFIED': return 'warning';
      case 'TERMINATED': return 'error';
      case 'EXPIRED': return 'neutral';
      default: return 'neutral';
    }
  };

  const getStatusLabel = (status: LeaseContractDto['status']) => {
    switch (status) {
      case 'ACTIVE': return '진행 중';
      case 'MODIFIED': return '재측정/변경';
      case 'TERMINATED': return '중도 해지';
      case 'EXPIRED': return '만료 종결';
      default: return status;
    }
  };

  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      {/* Toast Notification */}
      {toastMessage && (
        <div className="p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-300 text-sm font-bold flex items-center gap-3 animate-in fade-in slide-in-from-top-2">
          <CheckCircle2 size={20} className="text-emerald-400 shrink-0" />
          <span>{toastMessage}</span>
        </div>
      )}

      {/* Page Header */}
      <PageHeader
        title="IFRS 16 리스 계약"
        description="K-IFRS 1116호(리스) 기준에 따른 임차 계약의 사용권자산(ROU Asset) 및 리스부채(Lease Liability) 통합 관리."
        breadcrumbs={[
          { label: '공정가치 / 자산회계', href: '/fair-value/assets' },
          { label: 'IFRS 16 리스 계약' },
        ]}
        icon={FileText}
        actions={
          <button
            onClick={() => setIsModalOpen(true)}
            className="px-5 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-xs font-black transition-all shadow-lg shadow-blue-600/20 flex items-center gap-2"
          >
            <Plus size={16} />
            <span>신규 리스계약 등록</span>
          </button>
        }
      />

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-black uppercase tracking-wider text-slate-400">총 사용권자산 (ROU Asset)</span>
            <div className="w-9 h-9 rounded-xl bg-blue-500/10 border border-blue-500/20 flex items-center justify-center text-blue-400">
              <Building2 size={18} />
            </div>
          </div>
          <div className="text-2xl font-black text-white">
            <AmountDisplay amount={totalRouAsset} />
          </div>
          <p className="text-[11px] text-slate-500 font-medium">최초 계상 사용권자산 총액</p>
        </div>

        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-black uppercase tracking-wider text-slate-400">총 리스부채 잔액</span>
            <div className="w-9 h-9 rounded-xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-center text-amber-400">
              <CreditCard size={18} />
            </div>
          </div>
          <div className="text-2xl font-black text-amber-400">
            <AmountDisplay amount={totalLeaseLiability} />
          </div>
          <p className="text-[11px] text-slate-500 font-medium">미지급 리스료 현재가치 잔액</p>
        </div>

        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-black uppercase tracking-wider text-slate-400">진행 중인 리스 계약</span>
            <div className="w-9 h-9 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
              <Layers size={18} />
            </div>
          </div>
          <div className="text-2xl font-black text-white font-mono">{activeCount} <span className="text-xs text-slate-500">/ 총 {leases.length}건</span></div>
          <p className="text-[11px] text-slate-500 font-medium">현재 상각 및 이자 인식 대상</p>
        </div>

        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-black uppercase tracking-wider text-slate-400">평균 증분차입할인율</span>
            <div className="w-9 h-9 rounded-xl bg-purple-500/10 border border-purple-500/20 flex items-center justify-center text-purple-400">
              <Percent size={18} />
            </div>
          </div>
          <div className="text-2xl font-black text-purple-400 font-mono">{avgDiscountRate}%</div>
          <p className="text-[11px] text-slate-500 font-medium">계약별 현재가치 평가 적용율</p>
        </div>
      </div>

      {/* Filter & Main Table */}
      <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-4">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <Tabs tabs={typeTabs} activeTab={selectedAssetType} onChange={setSelectedAssetType} />

          <div className="relative min-w-[240px]">
            <Search size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500" />
            <input
              type="text"
              placeholder="계약번호, 계약명, 임대인..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-white/5 border border-white/10 text-xs text-white placeholder-slate-500 outline-none focus:border-blue-500/50 transition-all"
            />
          </div>
        </div>

        <div className="overflow-x-auto">
          {filteredLeases.length === 0 ? (
            <EmptyState
              icon={FileText}
              title="리스 계약이 존재하지 않습니다."
              description="선택한 조건에 해당하는 IFRS 16 리스 계약이 없습니다."
            />
          ) : (
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-white/10 text-[11px] font-black uppercase tracking-wider text-slate-400">
                  <th className="py-3 px-4">계약 번호</th>
                  <th className="py-3 px-4">계약명 / 임대인</th>
                  <th className="py-3 px-4">자산 유형</th>
                  <th className="py-3 px-4">리스 기간</th>
                  <th className="py-3 px-4 text-right">월 리스료</th>
                  <th className="py-3 px-4 text-right">사용권자산 원가</th>
                  <th className="py-3 px-4 text-right">리스부채 잔액</th>
                  <th className="py-3 px-4 text-center">할인율</th>
                  <th className="py-3 px-4">상태</th>
                  <th className="py-3 px-4 text-center">상세</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5 text-xs font-medium">
                {filteredLeases.map((lease) => (
                  <tr key={lease.id} className="hover:bg-white/5 transition-all group">
                    <td className="py-4 px-4 font-mono font-bold text-blue-400">{lease.contractNo}</td>
                    <td className="py-4 px-4">
                      <p className="font-bold text-white group-hover:text-blue-300 transition-colors">{lease.contractName}</p>
                      <p className="text-[11px] text-slate-500">{lease.lessor} ({lease.lesseeDepartment})</p>
                    </td>
                    <td className="py-4 px-4">
                      <span className="px-2.5 py-1 rounded-lg bg-white/5 border border-white/10 text-slate-300 font-bold">
                        {lease.assetType}
                      </span>
                    </td>
                    <td className="py-4 px-4 font-mono text-slate-300">
                      <p>{lease.startDate} ~ {lease.endDate}</p>
                      <p className="text-[11px] text-slate-500">({lease.leaseTermMonths} 개월)</p>
                    </td>
                    <td className="py-4 px-4 text-right font-bold text-white">
                      <AmountDisplay amount={lease.monthlyPayment} />
                    </td>
                    <td className="py-4 px-4 text-right">
                      <AmountDisplay amount={lease.rouAssetValue} />
                    </td>
                    <td className="py-4 px-4 text-right">
                      <AmountDisplay amount={lease.leaseLiability} className="text-amber-400 font-bold" />
                    </td>
                    <td className="py-4 px-4 text-center font-mono font-bold text-purple-400">
                      {lease.discountRate}%
                    </td>
                    <td className="py-4 px-4">
                      <StatusBadge
                        status={getStatusLabel(lease.status)}
                        variant={getStatusVariant(lease.status)}
                      />
                    </td>
                    <td className="py-4 px-4 text-center">
                      <button
                        onClick={() => setSelectedLease(lease)}
                        className="p-1.5 rounded-lg bg-white/5 hover:bg-white/10 text-slate-400 hover:text-white transition-all"
                      >
                        <Eye size={16} />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {/* Contract Detail Drawer/Modal */}
      {selectedLease && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4 animate-in fade-in">
          <div className="bg-slate-900 border border-white/10 rounded-3xl max-w-xl w-full p-6 space-y-6 shadow-2xl">
            <div className="flex items-center justify-between border-b border-white/10 pb-4">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-2xl bg-blue-600/10 border border-blue-500/20 flex items-center justify-center text-blue-400">
                  <FileText size={20} />
                </div>
                <div>
                  <h3 className="text-lg font-black text-white">{selectedLease.contractName}</h3>
                  <p className="text-xs font-mono text-blue-400">{selectedLease.contractNo}</p>
                </div>
              </div>
              <button onClick={() => setSelectedLease(null)} className="p-2 text-slate-400 hover:text-white rounded-xl hover:bg-white/10">
                <XCircle size={20} />
              </button>
            </div>

            <div className="grid grid-cols-2 gap-4 text-xs">
              <div className="p-3.5 rounded-2xl bg-white/5 border border-white/5 space-y-1">
                <span className="text-[10px] text-slate-500 font-bold uppercase block">임대인 (Lessor)</span>
                <p className="font-bold text-white">{selectedLease.lessor}</p>
              </div>
              <div className="p-3.5 rounded-2xl bg-white/5 border border-white/5 space-y-1">
                <span className="text-[10px] text-slate-500 font-bold uppercase block">사용 부서</span>
                <p className="font-bold text-white">{selectedLease.lesseeDepartment}</p>
              </div>
              <div className="p-3.5 rounded-2xl bg-white/5 border border-white/5 space-y-1">
                <span className="text-[10px] text-slate-500 font-bold uppercase block">사용권자산 원가</span>
                <p className="font-bold text-white"><AmountDisplay amount={selectedLease.rouAssetValue} /></p>
              </div>
              <div className="p-3.5 rounded-2xl bg-white/5 border border-white/5 space-y-1">
                <span className="text-[10px] text-slate-500 font-bold uppercase block">현재 리스부채 잔액</span>
                <p className="font-bold text-amber-400"><AmountDisplay amount={selectedLease.leaseLiability} /></p>
              </div>
              <div className="p-3.5 rounded-2xl bg-white/5 border border-white/5 space-y-1">
                <span className="text-[10px] text-slate-500 font-bold uppercase block">월 지출 리스료</span>
                <p className="font-bold text-white"><AmountDisplay amount={selectedLease.monthlyPayment} /></p>
              </div>
              <div className="p-3.5 rounded-2xl bg-white/5 border border-white/5 space-y-1">
                <span className="text-[10px] text-slate-500 font-bold uppercase block">적용 할인율</span>
                <p className="font-bold text-purple-400 font-mono">{selectedLease.discountRate}%</p>
              </div>
            </div>

            <div className="p-3.5 rounded-2xl bg-white/5 border border-white/5 text-xs space-y-1">
              <span className="text-[10px] text-slate-500 font-bold uppercase block">기초자산 상세 내역</span>
              <p className="text-slate-300">{selectedLease.underlyingAsset}</p>
            </div>

            <div className="flex justify-end pt-2">
              <button
                onClick={() => setSelectedLease(null)}
                className="px-5 py-2.5 rounded-xl bg-white/10 hover:bg-white/20 text-white font-bold text-xs transition-all"
              >
                닫기
              </button>
            </div>
          </div>
        </div>
      )}

      {/* New Lease Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4 animate-in fade-in">
          <div className="bg-slate-900 border border-white/10 rounded-3xl max-w-xl w-full p-6 space-y-6 shadow-2xl">
            <div className="flex items-center justify-between border-b border-white/10 pb-4">
              <h3 className="text-lg font-black text-white italic">신규 IFRS 16 리스계약 등록</h3>
              <button onClick={() => setIsModalOpen(false)} className="p-2 text-slate-400 hover:text-white rounded-xl hover:bg-white/10">
                <XCircle size={20} />
              </button>
            </div>

            <form onSubmit={handleRegisterLease} className="space-y-4 text-xs">
              <div className="space-y-1">
                <label className="font-bold text-slate-400">계약명 *</label>
                <input
                  type="text"
                  required
                  placeholder="예: 서울 강남 사무실 10층 임대차 계약"
                  value={newLease.contractName}
                  onChange={e => setNewLease({ ...newLease, contractName: e.target.value })}
                  className="w-full p-3 rounded-xl bg-white/5 border border-white/10 text-white outline-none focus:border-blue-500"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1">
                  <label className="font-bold text-slate-400">임대인 (Lessor) *</label>
                  <input
                    type="text"
                    required
                    placeholder="예: (주)한국자산신탁"
                    value={newLease.lessor}
                    onChange={e => setNewLease({ ...newLease, lessor: e.target.value })}
                    className="w-full p-3 rounded-xl bg-white/5 border border-white/10 text-white outline-none focus:border-blue-500"
                  />
                </div>

                <div className="space-y-1">
                  <label className="font-bold text-slate-400">자산 유형</label>
                  <select
                    value={newLease.assetType}
                    onChange={e => setNewLease({ ...newLease, assetType: e.target.value as LeaseContractDto['assetType'] })}
                    className="w-full p-3 rounded-xl bg-slate-900 border border-white/10 text-white outline-none focus:border-blue-500"
                  >
                    <option value="부동산">부동산 (임대차)</option>
                    <option value="차량">차량 렌트/리스</option>
                    <option value="IT장비">IT/서버 장비</option>
                    <option value="기계설비">기계/생산 설비</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1">
                  <label className="font-bold text-slate-400">리스 개시일자</label>
                  <input
                    type="date"
                    value={newLease.startDate}
                    onChange={e => setNewLease({ ...newLease, startDate: e.target.value })}
                    className="w-full p-3 rounded-xl bg-white/5 border border-white/10 text-white outline-none focus:border-blue-500"
                  />
                </div>

                <div className="space-y-1">
                  <label className="font-bold text-slate-400">리스 기간 (개월)</label>
                  <input
                    type="number"
                    min={1}
                    value={newLease.leaseTermMonths}
                    onChange={e => setNewLease({ ...newLease, leaseTermMonths: Number(e.target.value) })}
                    className="w-full p-3 rounded-xl bg-white/5 border border-white/10 text-white outline-none focus:border-blue-500 font-mono"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1">
                  <label className="font-bold text-slate-400">월 지정 리스료 (원)</label>
                  <input
                    type="number"
                    min={1}
                    value={newLease.monthlyPayment}
                    onChange={e => setNewLease({ ...newLease, monthlyPayment: Number(e.target.value) })}
                    className="w-full p-3 rounded-xl bg-white/5 border border-white/10 text-white outline-none focus:border-blue-500 font-mono"
                  />
                </div>

                <div className="space-y-1">
                  <label className="font-bold text-slate-400">증분차입할인율 (%)</label>
                  <input
                    type="number"
                    step="0.1"
                    min={0}
                    value={newLease.discountRate}
                    onChange={e => setNewLease({ ...newLease, discountRate: Number(e.target.value) })}
                    className="w-full p-3 rounded-xl bg-white/5 border border-white/10 text-white outline-none focus:border-blue-500 font-mono"
                  />
                </div>
              </div>

              <div className="space-y-1">
                <label className="font-bold text-slate-400">기초자산 상세 정보</label>
                <textarea
                  rows={2}
                  placeholder="예: 서울시 강남구 테헤란로 123 10층 (전용면적 450m²)"
                  value={newLease.underlyingAsset}
                  onChange={e => setNewLease({ ...newLease, underlyingAsset: e.target.value })}
                  className="w-full p-3 rounded-xl bg-white/5 border border-white/10 text-white outline-none focus:border-blue-500 resize-none"
                />
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-white/10">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-5 py-2.5 rounded-xl bg-white/5 hover:bg-white/10 text-slate-300 font-bold"
                >
                  취소
                </button>
                <button
                  type="submit"
                  className="px-6 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-bold shadow-lg shadow-blue-600/20"
                >
                  계약 등록 확정
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

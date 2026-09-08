"use client";

import React, { useState, useMemo } from 'react';
import {
  ShieldCheck,
  RefreshCw,
  Search,
  AlertTriangle,
  CheckCircle2,
  FileDiff,
  Check,
  X,
  ArrowUpRight,
  Database,
  HelpCircle
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import AmountDisplay from '@/components/ui/AmountDisplay';
import Tabs from '@/components/ui/Tabs';
import { mockNtsComparisons, NtsComparisonDto } from '@/mocks/tax';

export default function NtsVerificationPage() {
  const [comparisons, setComparisons] = useState<NtsComparisonDto[]>(mockNtsComparisons);
  const [activeTab, setActiveTab] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [selectedItem, setSelectedItem] = useState<NtsComparisonDto | null>(null);
  const [isVerifying, setIsVerifying] = useState<boolean>(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  // Tabs
  const diffTabs = [
    { id: 'ALL', label: '전체 불일치/이슈' },
    { id: 'AMOUNT_MISMATCH', label: '금액 불일치' },
    { id: 'MISSING_IN_ERP', label: 'ERP 미등록 건' },
    { id: 'MISSING_IN_NTS', label: '국세청 미승인 건' },
    { id: 'STATUS_MISMATCH', label: '공제 상태 불일치' },
  ];

  // Filtered List
  const filteredList = useMemo(() => {
    return comparisons.filter(item => {
      const matchesTab = activeTab === 'ALL' || item.diffType === activeTab;
      const matchesQuery = 
        item.partnerName.toLowerCase().includes(searchQuery.toLowerCase()) ||
        item.partnerBizNum.includes(searchQuery) ||
        item.ntsApprovalNum.includes(searchQuery) ||
        item.note.toLowerCase().includes(searchQuery.toLowerCase());
      return matchesTab && matchesQuery;
    });
  }, [comparisons, activeTab, searchQuery]);

  // KPI Statistics
  const unresolvedCount = useMemo(() => comparisons.filter(item => item.status === 'UNRESOLVED').length, [comparisons]);
  const resolvedCount = useMemo(() => comparisons.filter(item => item.status === 'RESOLVED').length, [comparisons]);
  const totalDiffAmount = useMemo(() => {
    return comparisons.reduce((sum, item) => sum + Math.abs(item.erpSupplyAmount - item.ntsSupplyAmount), 0);
  }, [comparisons]);

  const handleRunVerification = () => {
    setIsVerifying(true);
    setTimeout(() => {
      setIsVerifying(false);
      setToastMessage('국세청 홈택스 DB와 실시간 교차 검증이 완료되었습니다. (새로운 미등록 건 0건)');
      setTimeout(() => setToastMessage(null), 4000);
    }, 1500);
  };

  const handleResolveItem = (id: string) => {
    setComparisons(prev =>
      prev.map(item => item.id === id ? { ...item, status: 'RESOLVED' as const } : item)
    );
    setSelectedItem(null);
    setToastMessage('해당 불일치 항목이 조정 완료(RESOLVED) 처리되었습니다.');
    setTimeout(() => setToastMessage(null), 3000);
  };

  const getDiffTypeLabel = (type: NtsComparisonDto['diffType']) => {
    switch (type) {
      case 'AMOUNT_MISMATCH': return '금액 불일치';
      case 'MISSING_IN_ERP': return 'ERP 미등록';
      case 'MISSING_IN_NTS': return '국세청 미승인';
      case 'STATUS_MISMATCH': return '공제상태 불일치';
      default: return type;
    }
  };

  const getDiffTypeVariant = (type: NtsComparisonDto['diffType']) => {
    switch (type) {
      case 'AMOUNT_MISMATCH': return 'error';
      case 'MISSING_IN_ERP': return 'warning';
      case 'MISSING_IN_NTS': return 'info';
      case 'STATUS_MISMATCH': return 'warning';
      default: return 'neutral';
    }
  };

  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      {/* Page Header */}
      <PageHeader
        title="국세청 승인 조회"
        description="내부 ERP 전표 및 세금계산서 데이터와 국세청 홈택스 승인 내역을 실시간 대조하여 금액 및 누락 내역을 교차 검증합니다."
        breadcrumbs={[
          { label: '세무 관리', href: '/tax/nts-verification' },
          { label: '국세청 승인 조회' },
        ]}
        icon={ShieldCheck}
        actions={
          <button
            onClick={handleRunVerification}
            disabled={isVerifying}
            className="flex items-center gap-2 px-5 py-2.5 rounded-2xl bg-blue-600 hover:bg-blue-500 text-white font-bold text-xs shadow-lg shadow-blue-600/20 transition-all disabled:opacity-50"
          >
            <RefreshCw size={16} className={isVerifying ? 'animate-spin' : ''} />
            <span>{isVerifying ? '국세청 데이터 대조 중...' : '국세청 실시간 교차 검증'}</span>
          </button>
        }
      />

      {/* Toast Notification */}
      {toastMessage && (
        <div className="p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-300 text-sm font-bold flex items-center justify-between animate-in fade-in slide-in-from-top-2">
          <div className="flex items-center gap-3">
            <CheckCircle2 size={18} className="text-emerald-400" />
            <span>{toastMessage}</span>
          </div>
          <button onClick={() => setToastMessage(null)} className="text-emerald-400 hover:text-emerald-200">
            <X size={16} />
          </button>
        </div>
      )}

      {/* Sync Status Banner Card */}
      <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-4">
        <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4 border-b border-white/5 pb-4">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
              <Database size={24} />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-lg font-black text-white italic">국세청 홈택스 API 대조 엔진</h3>
                <span className="px-2.5 py-0.5 rounded-full text-[10px] font-black bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                  정상 연결됨
                </span>
              </div>
              <p className="text-xs text-slate-400 mt-0.5">마지막 교차 검증 완료 시각: <span className="font-mono text-slate-300">오늘 14:30:12</span></p>
            </div>
          </div>

          <div className="flex items-center gap-6">
            <div className="text-right">
              <span className="text-[10px] font-black uppercase text-slate-400">미해결 이슈</span>
              <p className="text-2xl font-black text-amber-400">{unresolvedCount} <span className="text-xs text-slate-400">건</span></p>
            </div>
            <div className="text-right">
              <span className="text-[10px] font-black uppercase text-slate-400">조정 완료</span>
              <p className="text-2xl font-black text-emerald-400">{resolvedCount} <span className="text-xs text-slate-400">건</span></p>
            </div>
            <div className="text-right">
              <span className="text-[10px] font-black uppercase text-slate-400">총 차이 금액</span>
              <p className="text-2xl font-black text-rose-400 font-mono">
                <AmountDisplay amount={totalDiffAmount} />
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Main Differences Table Section */}
      <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-6">
        <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
          <Tabs tabs={diffTabs} activeTab={activeTab} onChange={setActiveTab} />

          {/* Search */}
          <div className="relative w-full md:w-80">
            <Search size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500" />
            <input
              type="text"
              placeholder="거래처명, 승인번호, 사유 검색..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2.5 rounded-2xl bg-white/5 border border-white/10 text-xs text-white placeholder-slate-500 outline-none focus:border-blue-500/50 transition-all"
            />
          </div>
        </div>

        {/* Differences Table */}
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-white/10 text-[11px] font-black uppercase tracking-wider text-slate-400">
                <th className="py-3 px-4">발행일자</th>
                <th className="py-3 px-4">국세청 승인번호</th>
                <th className="py-3 px-4">거래처 정보</th>
                <th className="py-3 px-4 text-center">불일치 유형</th>
                <th className="py-3 px-4 text-right">ERP 공급가액</th>
                <th className="py-3 px-4 text-right">국세청 공급가액</th>
                <th className="py-3 px-4 text-right">차액</th>
                <th className="py-3 px-4 text-center">조정 상태</th>
                <th className="py-3 px-4 text-right">조치</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5 text-xs font-medium">
              {filteredList.map((item) => {
                const diffAmount = item.erpSupplyAmount - item.ntsSupplyAmount;
                return (
                  <tr 
                    key={item.id}
                    className="transition-all hover:bg-white/5 group"
                  >
                    <td className="py-4 px-4 font-mono text-slate-300">
                      {item.issueDate}
                    </td>
                    <td className="py-4 px-4 font-mono text-slate-400 text-[11px]">
                      {item.ntsApprovalNum}
                    </td>
                    <td className="py-4 px-4 font-bold text-white">
                      <div>{item.partnerName}</div>
                      <div className="text-[11px] text-slate-400 font-mono font-normal">{item.partnerBizNum}</div>
                    </td>
                    <td className="py-4 px-4 text-center">
                      <StatusBadge
                        status={getDiffTypeLabel(item.diffType)}
                        variant={getDiffTypeVariant(item.diffType)}
                      />
                    </td>
                    <td className="py-4 px-4 text-right font-mono text-slate-300">
                      <AmountDisplay amount={item.erpSupplyAmount} />
                    </td>
                    <td className="py-4 px-4 text-right font-mono text-slate-300">
                      <AmountDisplay amount={item.ntsSupplyAmount} />
                    </td>
                    <td className="py-4 px-4 text-right font-mono font-bold">
                      <AmountDisplay amount={diffAmount} showSign className={diffAmount !== 0 ? 'text-rose-400' : 'text-slate-500'} />
                    </td>
                    <td className="py-4 px-4 text-center">
                      {item.status === 'RESOLVED' ? (
                        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                          <Check size={12} /> 완료
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-amber-500/10 text-amber-400 border border-amber-500/20">
                          <AlertTriangle size={12} /> 미해결
                        </span>
                      )}
                    </td>
                    <td className="py-4 px-4 text-right">
                      <button
                        onClick={() => setSelectedItem(item)}
                        className="px-3 py-1.5 rounded-xl bg-white/5 hover:bg-blue-600 hover:text-white text-slate-300 text-xs font-bold transition-all border border-white/5 flex items-center gap-1 ml-auto"
                      >
                        <span>조치 및 상세</span>
                        <ArrowUpRight size={12} />
                      </button>
                    </td>
                  </tr>
                );
              })}
              {filteredList.length === 0 && (
                <tr>
                  <td colSpan={9} className="py-12 text-center text-slate-500 font-medium">
                    조건에 해당하는 불일치 또는 검증 이슈가 없습니다.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Difference Detail & Reconciliation Modal */}
      {selectedItem && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-white/10 rounded-3xl max-w-2xl w-full p-6 space-y-6 shadow-2xl animate-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between pb-4 border-b border-white/10">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-2xl bg-amber-600/10 border border-amber-500/20 flex items-center justify-center text-amber-400">
                  <FileDiff size={20} />
                </div>
                <div>
                  <h3 className="text-lg font-black text-white italic">국세청 승인 대조 이슈 상세</h3>
                  <p className="text-xs font-mono text-slate-400">승인번호: {selectedItem.ntsApprovalNum}</p>
                </div>
              </div>
              <button 
                onClick={() => setSelectedItem(null)}
                className="p-2 rounded-xl bg-white/5 hover:bg-white/10 text-slate-400 hover:text-white"
              >
                <X size={18} />
              </button>
            </div>

            {/* Comparison Side-by-Side */}
            <div className="grid grid-cols-2 gap-4">
              <div className="p-4 rounded-2xl bg-white/5 border border-white/5 space-y-2">
                <span className="text-[10px] font-black uppercase text-blue-400 tracking-wider">ERP 시스템 기록</span>
                <p className="text-sm font-bold text-white">{selectedItem.partnerName}</p>
                <div className="pt-2 border-t border-white/5 space-y-1 text-xs">
                  <div className="flex justify-between text-slate-400">
                    <span>공급가액:</span>
                    <AmountDisplay amount={selectedItem.erpSupplyAmount} />
                  </div>
                  <div className="flex justify-between text-slate-400">
                    <span>세액 (VAT):</span>
                    <AmountDisplay amount={selectedItem.erpTaxAmount} />
                  </div>
                </div>
              </div>

              <div className="p-4 rounded-2xl bg-white/5 border border-white/5 space-y-2">
                <span className="text-[10px] font-black uppercase text-emerald-400 tracking-wider">국세청 홈택스 기록</span>
                <p className="text-sm font-bold text-white">{selectedItem.partnerName}</p>
                <div className="pt-2 border-t border-white/5 space-y-1 text-xs">
                  <div className="flex justify-between text-slate-400">
                    <span>공급가액:</span>
                    <AmountDisplay amount={selectedItem.ntsSupplyAmount} />
                  </div>
                  <div className="flex justify-between text-slate-400">
                    <span>세액 (VAT):</span>
                    <AmountDisplay amount={selectedItem.ntsTaxAmount} />
                  </div>
                </div>
              </div>
            </div>

            {/* Note & Analysis */}
            <div className="p-4 rounded-2xl bg-amber-500/10 border border-amber-500/20 text-amber-300 text-xs space-y-1">
              <div className="flex items-center gap-2 font-bold text-amber-400">
                <HelpCircle size={16} />
                <span>검증 분석 내용</span>
              </div>
              <p className="pl-6">{selectedItem.note}</p>
            </div>

            {/* Modal Action Buttons */}
            <div className="flex items-center justify-between pt-2 border-t border-white/10">
              <button 
                onClick={() => setSelectedItem(null)}
                className="px-5 py-2.5 rounded-xl bg-white/10 hover:bg-white/20 text-white font-bold text-xs transition-all"
              >
                닫기
              </button>

              {selectedItem.status !== 'RESOLVED' && (
                <button
                  onClick={() => handleResolveItem(selectedItem.id)}
                  className="px-6 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-black text-xs shadow-lg shadow-emerald-600/20 transition-all flex items-center gap-2"
                >
                  <Check size={14} />
                  <span>ERP 데이터 자동 맞춤 및 조치 완료</span>
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

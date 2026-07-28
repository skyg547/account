"use client";

import React, { useState, useMemo } from 'react';
import { 
  FileText, 
  RefreshCw, 
  Search, 
  Download, 
  CheckCircle2, 
  AlertTriangle, 
  Building, 
  Filter, 
  ShieldCheck, 
  X, 
  ArrowUpRight,
  Sparkles,
  Info
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import AmountDisplay from '@/components/ui/AmountDisplay';
import Tabs from '@/components/ui/Tabs';
import { mockPurchaseInvoices, TaxInvoiceDto } from '@/mocks/tax';

export default function PurchaseTaxInvoicePage() {
  const [invoices, setInvoices] = useState<TaxInvoiceDto[]>(mockPurchaseInvoices);
  const [activeTab, setActiveTab] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [selectedInvoice, setSelectedInvoice] = useState<TaxInvoiceDto | null>(null);
  const [isSyncing, setIsSyncing] = useState<boolean>(false);
  const [syncToast, setSyncToast] = useState<string | null>(null);

  // Status Filter Tabs
  const statusTabs = [
    { id: 'ALL', label: '전체 보기' },
    { id: 'APPROVED', label: '국세청 승인' },
    { id: 'SYNCHRONIZED', label: '동기화 완료' },
    { id: 'PENDING', label: '전송 대기' },
    { id: 'DISCREPANCY', label: '검증 이슈/불공제' },
  ];

  // Filtered List
  const filteredInvoices = useMemo(() => {
    return invoices.filter(inv => {
      const matchesTab = activeTab === 'ALL' || inv.ntsStatus === activeTab;
      const matchesQuery = 
        inv.supplierName.toLowerCase().includes(searchQuery.toLowerCase()) ||
        inv.supplierBizNum.includes(searchQuery) ||
        inv.ntsApprovalNum.includes(searchQuery) ||
        inv.itemSummary.toLowerCase().includes(searchQuery.toLowerCase());
      return matchesTab && matchesQuery;
    });
  }, [invoices, activeTab, searchQuery]);

  // KPI Calculations
  const totalSupplyAmount = useMemo(() => invoices.reduce((sum, item) => sum + item.supplyAmount, 0), [invoices]);
  const totalTaxAmount = useMemo(() => invoices.reduce((sum, item) => sum + item.taxAmount, 0), [invoices]);
  const approvedCount = useMemo(() => invoices.filter(item => item.ntsStatus === 'APPROVED' || item.ntsStatus === 'SYNCHRONIZED').length, [invoices]);
  const discrepancyCount = useMemo(() => invoices.filter(item => item.ntsStatus === 'DISCREPANCY' || item.deductible === false).length, [invoices]);

  const handleSyncHomeTax = () => {
    setIsSyncing(true);
    setTimeout(() => {
      setIsSyncing(false);
      setSyncToast('국세청 홈택스 매입 세금계산서 동기화가 성공적으로 완료되었습니다.');
      setTimeout(() => setSyncToast(null), 4000);
    }, 1200);
  };

  const getStatusVariant = (status: TaxInvoiceDto['ntsStatus']) => {
    switch (status) {
      case 'APPROVED': return 'success';
      case 'SYNCHRONIZED': return 'info';
      case 'PENDING': return 'warning';
      case 'DISCREPANCY': return 'error';
      case 'REJECTED': return 'error';
      default: return 'neutral';
    }
  };

  const getStatusLabel = (status: TaxInvoiceDto['ntsStatus']) => {
    switch (status) {
      case 'APPROVED': return '승인완료';
      case 'SYNCHRONIZED': return '동기화';
      case 'PENDING': return '전송대기';
      case 'DISCREPANCY': return '불일치/이슈';
      case 'REJECTED': return '반려됨';
      default: return status;
    }
  };

  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      {/* Page Header */}
      <PageHeader
        title="매입 세금계산서"
        description="국세청 홈택스 및 공급자로부터 수신된 매입 전자세금계산서를 조회하고 매입세액 공제 여부를 검증합니다."
        breadcrumbs={[
          { label: '세무 관리', href: '/tax/purchase' },
          { label: '매입 세금계산서' },
        ]}
        icon={FileText}
        actions={
          <div className="flex items-center gap-3">
            <button
              onClick={handleSyncHomeTax}
              disabled={isSyncing}
              className="flex items-center gap-2 px-4 py-2.5 rounded-2xl bg-blue-600 hover:bg-blue-500 text-white font-bold text-xs shadow-lg shadow-blue-600/20 transition-all disabled:opacity-50"
            >
              <RefreshCw size={14} className={isSyncing ? 'animate-spin' : ''} />
              <span>{isSyncing ? '홈택스 동기화 중...' : '홈택스 매입 수집'}</span>
            </button>
            <button className="flex items-center gap-2 px-4 py-2.5 rounded-2xl bg-white/5 hover:bg-white/10 text-white font-bold text-xs border border-white/10 transition-all">
              <Download size={14} className="text-slate-400" />
              <span>엑셀 다운로드</span>
            </button>
          </div>
        }
      />

      {/* Sync Toast Notification */}
      {syncToast && (
        <div className="p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-300 text-sm font-bold flex items-center justify-between animate-in fade-in slide-in-from-top-2">
          <div className="flex items-center gap-3">
            <CheckCircle2 size={18} className="text-emerald-400" />
            <span>{syncToast}</span>
          </div>
          <button onClick={() => setSyncToast(null)} className="text-emerald-400 hover:text-emerald-200">
            <X size={16} />
          </button>
        </div>
      )}

      {/* Summary KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="flex items-center justify-between text-slate-400 text-xs font-bold">
            <span>총 매입 공급가액</span>
            <Building size={16} className="text-blue-400" />
          </div>
          <div className="text-2xl font-black text-white">
            <AmountDisplay amount={totalSupplyAmount} />
          </div>
          <p className="text-[11px] text-slate-500">당월 총 {invoices.length}건 합산</p>
        </div>

        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="flex items-center justify-between text-slate-400 text-xs font-bold">
            <span>총 매입 세액 (VAT)</span>
            <Sparkles size={16} className="text-emerald-400" />
          </div>
          <div className="text-2xl font-black text-emerald-400">
            <AmountDisplay amount={totalTaxAmount} />
          </div>
          <p className="text-[11px] text-slate-500">공제 대상 매입 세액 검증 대상</p>
        </div>

        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="flex items-center justify-between text-slate-400 text-xs font-bold">
            <span>국세청 승인 완료</span>
            <ShieldCheck size={16} className="text-blue-400" />
          </div>
          <div className="flex items-baseline gap-2">
            <span className="text-3xl font-black text-white">{approvedCount}</span>
            <span className="text-xs text-slate-400 font-bold">/ {invoices.length} 건</span>
          </div>
          <p className="text-[11px] text-emerald-400 font-medium">정상 국세청 동기화 완료</p>
        </div>

        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="flex items-center justify-between text-slate-400 text-xs font-bold">
            <span>불공제 / 이슈 건</span>
            <AlertTriangle size={16} className="text-amber-400" />
          </div>
          <div className="flex items-baseline gap-2">
            <span className="text-3xl font-black text-amber-400">{discrepancyCount}</span>
            <span className="text-xs text-slate-400 font-bold">건 요확인</span>
          </div>
          <p className="text-[11px] text-amber-400/80 font-medium">비영업용 승용차 등 포함</p>
        </div>
      </div>

      {/* Filter Toolbar & Tabs */}
      <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-6">
        <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
          <Tabs tabs={statusTabs} activeTab={activeTab} onChange={setActiveTab} />

          {/* Search Box */}
          <div className="relative w-full md:w-80">
            <Search size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500" />
            <input
              type="text"
              placeholder="공급자명, 사업자번호, 승인번호 검색..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2.5 rounded-2xl bg-white/5 border border-white/10 text-xs text-white placeholder-slate-500 outline-none focus:border-blue-500/50 transition-all"
            />
          </div>
        </div>

        {/* Invoice Table */}
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-white/10 text-[11px] font-black uppercase tracking-wider text-slate-400">
                <th className="py-3 px-4">작성일자</th>
                <th className="py-3 px-4">공급자 (상호 / 사업자번호)</th>
                <th className="py-3 px-4">품목 및 내역</th>
                <th className="py-3 px-4 text-right">공급가액</th>
                <th className="py-3 px-4 text-right">세액 (VAT)</th>
                <th className="py-3 px-4 text-right">합계금액</th>
                <th className="py-3 px-4 text-center">공제여부</th>
                <th className="py-3 px-4 text-center">국세청 상태</th>
                <th className="py-3 px-4 text-right">상세</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5 text-xs font-medium">
              {filteredInvoices.map((inv) => (
                <tr 
                  key={inv.id}
                  className="transition-all hover:bg-white/5 group"
                >
                  <td className="py-4 px-4 font-mono text-slate-300">
                    <div>{inv.issueDate}</div>
                    <div className="text-[10px] text-slate-500 font-sans">{inv.id}</div>
                  </td>
                  <td className="py-4 px-4 font-bold text-white">
                    <div>{inv.supplierName}</div>
                    <div className="text-[11px] text-slate-400 font-mono font-normal">{inv.supplierBizNum}</div>
                  </td>
                  <td className="py-4 px-4 text-slate-300 max-w-xs truncate">
                    <span>{inv.itemSummary}</span>
                    {inv.modifiedReason && (
                      <span className="ml-2 px-1.5 py-0.5 rounded text-[10px] bg-amber-500/20 text-amber-300 font-bold">
                        {inv.modifiedReason}
                      </span>
                    )}
                  </td>
                  <td className="py-4 px-4 text-right">
                    <AmountDisplay amount={inv.supplyAmount} />
                  </td>
                  <td className="py-4 px-4 text-right">
                    <AmountDisplay amount={inv.taxAmount} className="text-emerald-400" />
                  </td>
                  <td className="py-4 px-4 text-right font-bold text-white">
                    <AmountDisplay amount={inv.totalAmount} />
                  </td>
                  <td className="py-4 px-4 text-center">
                    {inv.deductible !== false ? (
                      <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                        공제가능
                      </span>
                    ) : (
                      <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-rose-500/10 text-rose-400 border border-rose-500/20">
                        불공제
                      </span>
                    )}
                  </td>
                  <td className="py-4 px-4 text-center">
                    <StatusBadge
                      status={getStatusLabel(inv.ntsStatus)}
                      variant={getStatusVariant(inv.ntsStatus)}
                    />
                  </td>
                  <td className="py-4 px-4 text-right">
                    <button
                      onClick={() => setSelectedInvoice(inv)}
                      className="px-3 py-1.5 rounded-xl bg-white/5 hover:bg-blue-600 hover:text-white text-slate-300 text-xs font-bold transition-all border border-white/5 flex items-center gap-1 ml-auto"
                    >
                      <span>조회</span>
                      <ArrowUpRight size={12} />
                    </button>
                  </td>
                </tr>
              ))}
              {filteredInvoices.length === 0 && (
                <tr>
                  <td colSpan={9} className="py-12 text-center text-slate-500 font-medium">
                    조건에 부합하는 매입 세금계산서 내역이 없습니다.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Invoice Detail Modal */}
      {selectedInvoice && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-white/10 rounded-3xl max-w-2xl w-full p-6 space-y-6 shadow-2xl animate-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between pb-4 border-b border-white/10">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-2xl bg-blue-600/10 border border-blue-500/20 flex items-center justify-center text-blue-400">
                  <FileText size={20} />
                </div>
                <div>
                  <h3 className="text-lg font-black text-white italic">매입 전자세금계산서 상세</h3>
                  <p className="text-xs font-mono text-slate-400">승인번호: {selectedInvoice.ntsApprovalNum}</p>
                </div>
              </div>
              <button 
                onClick={() => setSelectedInvoice(null)}
                className="p-2 rounded-xl bg-white/5 hover:bg-white/10 text-slate-400 hover:text-white"
              >
                <X size={18} />
              </button>
            </div>

            <div className="grid grid-cols-2 gap-4">
              {/* Supplier Info */}
              <div className="p-4 rounded-2xl bg-white/5 border border-white/5 space-y-2">
                <span className="text-[10px] font-black uppercase text-blue-400 tracking-wider">공급자 정보</span>
                <p className="text-sm font-black text-white">{selectedInvoice.supplierName}</p>
                <p className="text-xs font-mono text-slate-400">사업자번호: {selectedInvoice.supplierBizNum}</p>
              </div>

              {/* Buyer Info */}
              <div className="p-4 rounded-2xl bg-white/5 border border-white/5 space-y-2">
                <span className="text-[10px] font-black uppercase text-emerald-400 tracking-wider">공급받는자 정보</span>
                <p className="text-sm font-black text-white">{selectedInvoice.buyerName}</p>
                <p className="text-xs font-mono text-slate-400">사업자번호: {selectedInvoice.buyerBizNum}</p>
              </div>
            </div>

            {/* Financial Details Table */}
            <div className="p-4 rounded-2xl bg-white/5 border border-white/5 space-y-3">
              <div className="flex items-center justify-between text-xs font-bold text-slate-400">
                <span>품목: {selectedInvoice.itemSummary}</span>
                <span>작성일자: {selectedInvoice.issueDate}</span>
              </div>
              <div className="grid grid-cols-3 gap-2 pt-2 border-t border-white/5 text-center">
                <div className="p-2 rounded-xl bg-slate-950/40">
                  <span className="text-[10px] text-slate-400 font-bold">공급가액</span>
                  <div className="text-sm font-black text-white mt-1">
                    <AmountDisplay amount={selectedInvoice.supplyAmount} />
                  </div>
                </div>
                <div className="p-2 rounded-xl bg-slate-950/40">
                  <span className="text-[10px] text-slate-400 font-bold">부가가치세액</span>
                  <div className="text-sm font-black text-emerald-400 mt-1">
                    <AmountDisplay amount={selectedInvoice.taxAmount} />
                  </div>
                </div>
                <div className="p-2 rounded-xl bg-slate-950/40">
                  <span className="text-[10px] text-slate-400 font-bold">총 합계금액</span>
                  <div className="text-sm font-black text-blue-400 mt-1">
                    <AmountDisplay amount={selectedInvoice.totalAmount} />
                  </div>
                </div>
              </div>
            </div>

            {/* Status & Remarks */}
            <div className="p-4 rounded-2xl bg-white/5 border border-white/5 space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-slate-400">국세청 동기화 일시</span>
                <span className="text-xs font-mono text-slate-300">{selectedInvoice.ntsStatusDate}</span>
              </div>
              {selectedInvoice.remark && (
                <div className="flex items-start gap-2 pt-2 border-t border-white/5 text-xs text-slate-300">
                  <Info size={14} className="text-blue-400 shrink-0 mt-0.5" />
                  <span>{selectedInvoice.remark}</span>
                </div>
              )}
            </div>

            <div className="flex justify-end gap-3 pt-2">
              <button 
                onClick={() => setSelectedInvoice(null)}
                className="px-5 py-2.5 rounded-xl bg-white/10 hover:bg-white/20 text-white font-bold text-xs transition-all"
              >
                닫기
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

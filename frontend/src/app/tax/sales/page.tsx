"use client";

import React, { useState, useMemo } from 'react';
import { 
  PlusCircle, 
  Send, 
  Search, 
  Download, 
  CheckCircle2, 
  Building2, 
  TrendingUp, 
  ShieldCheck, 
  Clock, 
  X, 
  ArrowUpRight,
  Calculator,
  FileCheck2,
  Sparkles
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import AmountDisplay from '@/components/ui/AmountDisplay';
import Tabs from '@/components/ui/Tabs';
import { mockSalesInvoices, TaxInvoiceDto } from '@/mocks/tax';

export default function SalesTaxInvoicePage() {
  const [invoices, setInvoices] = useState<TaxInvoiceDto[]>(mockSalesInvoices);
  const [activeTab, setActiveTab] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [selectedInvoice, setSelectedInvoice] = useState<TaxInvoiceDto | null>(null);
  const [isIssueModalOpen, setIsIssueModalOpen] = useState<boolean>(false);
  const [successToast, setSuccessToast] = useState<string | null>(null);

  // New Invoice Form State
  const [newBuyerName, setNewBuyerName] = useState<string>('');
  const [newBuyerBizNum, setNewBuyerBizNum] = useState<string>('');
  const [newInvoiceType, setNewInvoiceType] = useState<'NORMAL' | 'ZERO_RATE'>('NORMAL');
  const [newItemSummary, setNewItemSummary] = useState<string>('');
  const [newSupplyAmount, setNewSupplyAmount] = useState<string>('');
  const [newRemark, setNewRemark] = useState<string>('');

  // Status Filter Tabs
  const statusTabs = [
    { id: 'ALL', label: '전체 매출' },
    { id: 'APPROVED', label: '국세청 승인' },
    { id: 'SYNCHRONIZED', label: '동기화 완료' },
    { id: 'PENDING', label: '전송 대기' },
    { id: 'ZERO_RATE', label: '영세율 세금계산서' },
  ];

  // Filtered List
  const filteredInvoices = useMemo(() => {
    return invoices.filter(inv => {
      let matchesTab = true;
      if (activeTab === 'ZERO_RATE') {
        matchesTab = inv.invoiceType === 'ZERO_RATE';
      } else if (activeTab !== 'ALL') {
        matchesTab = inv.ntsStatus === activeTab;
      }

      const matchesQuery = 
        inv.buyerName.toLowerCase().includes(searchQuery.toLowerCase()) ||
        inv.buyerBizNum.includes(searchQuery) ||
        inv.ntsApprovalNum.includes(searchQuery) ||
        inv.itemSummary.toLowerCase().includes(searchQuery.toLowerCase());
      return matchesTab && matchesQuery;
    });
  }, [invoices, activeTab, searchQuery]);

  // KPI Calculations
  const totalSupplyAmount = useMemo(() => invoices.reduce((sum, item) => sum + item.supplyAmount, 0), [invoices]);
  const totalTaxAmount = useMemo(() => invoices.reduce((sum, item) => sum + item.taxAmount, 0), [invoices]);
  const approvedCount = useMemo(() => invoices.filter(item => item.ntsStatus === 'APPROVED' || item.ntsStatus === 'SYNCHRONIZED').length, [invoices]);
  const pendingCount = useMemo(() => invoices.filter(item => item.ntsStatus === 'PENDING').length, [invoices]);

  // Auto-calculated VAT for new invoice form
  const calculatedTax = useMemo(() => {
    const supply = parseFloat(newSupplyAmount) || 0;
    if (newInvoiceType === 'ZERO_RATE') return 0;
    return Math.round(supply * 0.1);
  }, [newSupplyAmount, newInvoiceType]);

  const calculatedTotal = useMemo(() => {
    const supply = parseFloat(newSupplyAmount) || 0;
    return supply + calculatedTax;
  }, [newSupplyAmount, calculatedTax]);

  const handleCreateInvoice = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newBuyerName || !newBuyerBizNum || !newSupplyAmount || !newItemSummary) return;

    const supply = parseFloat(newSupplyAmount) || 0;
    const today = new Date().toISOString().slice(0, 10);
    const ntsRandom = Math.floor(10000000 + Math.random() * 90000000);

    const newInvoice: TaxInvoiceDto = {
      id: `TXS-${today.replace(/-/g, '').slice(0, 6)}-${(invoices.length + 1).toString().padStart(3, '0')}`,
      ntsApprovalNum: `${today.replace(/-/g, '')}-41000000-${ntsRandom}`,
      issueDate: today,
      type: 'SALES',
      invoiceType: newInvoiceType,
      supplierName: '(주)스카이솔루션',
      supplierBizNum: '220-81-98765',
      buyerName: newBuyerName,
      buyerBizNum: newBuyerBizNum,
      supplyAmount: supply,
      taxAmount: calculatedTax,
      totalAmount: calculatedTotal,
      ntsStatus: 'PENDING',
      ntsStatusDate: `${today} ${new Date().toTimeString().slice(0, 5)}`,
      itemSummary: newItemSummary,
      remark: newRemark || undefined,
    };

    setInvoices([newInvoice, ...invoices]);
    setIsIssueModalOpen(false);
    setSuccessToast(`신규 매출 세금계산서(${newInvoice.id})가 성공적으로 발행되어 국세청 전송 대기열에 추가되었습니다.`);

    // Reset Form
    setNewBuyerName('');
    setNewBuyerBizNum('');
    setNewInvoiceType('NORMAL');
    setNewItemSummary('');
    setNewSupplyAmount('');
    setNewRemark('');

    setTimeout(() => setSuccessToast(null), 5000);
  };

  const getStatusVariant = (status: TaxInvoiceDto['ntsStatus']) => {
    switch (status) {
      case 'APPROVED': return 'success';
      case 'SYNCHRONIZED': return 'info';
      case 'PENDING': return 'warning';
      case 'REJECTED': return 'error';
      default: return 'neutral';
    }
  };

  const getStatusLabel = (status: TaxInvoiceDto['ntsStatus']) => {
    switch (status) {
      case 'APPROVED': return '승인완료';
      case 'SYNCHRONIZED': return '동기화';
      case 'PENDING': return '전송대기';
      case 'REJECTED': return '발행반려';
      default: return status;
    }
  };

  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      {/* Page Header */}
      <PageHeader
        title="매출 세금계산서"
        description="전자세금계산서를 신규 발행하고 거래처 전송 및 국세청(홈택스) 승인 실시간 현황을 관리합니다."
        breadcrumbs={[
          { label: '세무 관리', href: '/tax/sales' },
          { label: '매출 세금계산서' },
        ]}
        icon={Send}
        actions={
          <div className="flex items-center gap-3">
            <button
              onClick={() => setIsIssueModalOpen(true)}
              className="flex items-center gap-2 px-5 py-2.5 rounded-2xl bg-blue-600 hover:bg-blue-500 text-white font-bold text-xs shadow-lg shadow-blue-600/20 transition-all"
            >
              <PlusCircle size={16} />
              <span>신규 세금계산서 발행</span>
            </button>
            <button className="flex items-center gap-2 px-4 py-2.5 rounded-2xl bg-white/5 hover:bg-white/10 text-white font-bold text-xs border border-white/10 transition-all">
              <Download size={14} className="text-slate-400" />
              <span>엑셀 다운로드</span>
            </button>
          </div>
        }
      />

      {/* Success Notification */}
      {successToast && (
        <div className="p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-300 text-sm font-bold flex items-center justify-between animate-in fade-in slide-in-from-top-2">
          <div className="flex items-center gap-3">
            <CheckCircle2 size={18} className="text-emerald-400" />
            <span>{successToast}</span>
          </div>
          <button onClick={() => setSuccessToast(null)} className="text-emerald-400 hover:text-emerald-200">
            <X size={16} />
          </button>
        </div>
      )}

      {/* Summary KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="flex items-center justify-between text-slate-400 text-xs font-bold">
            <span>총 매출 공급가액</span>
            <TrendingUp size={16} className="text-blue-400" />
          </div>
          <div className="text-2xl font-black text-white">
            <AmountDisplay amount={totalSupplyAmount} />
          </div>
          <p className="text-[11px] text-slate-500">당월 매출 합계</p>
        </div>

        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="flex items-center justify-between text-slate-400 text-xs font-bold">
            <span>총 매출 세액 (VAT)</span>
            <Sparkles size={16} className="text-emerald-400" />
          </div>
          <div className="text-2xl font-black text-emerald-400">
            <AmountDisplay amount={totalTaxAmount} />
          </div>
          <p className="text-[11px] text-slate-500">예정 납부 대상 매출세액</p>
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
          <p className="text-[11px] text-emerald-400 font-medium">국세청 정상 수신 승인됨</p>
        </div>

        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="flex items-center justify-between text-slate-400 text-xs font-bold">
            <span>국세청 전송 대기</span>
            <Clock size={16} className="text-amber-400" />
          </div>
          <div className="flex items-baseline gap-2">
            <span className="text-3xl font-black text-amber-400">{pendingCount}</span>
            <span className="text-xs text-slate-400 font-bold">건 대기 중</span>
          </div>
          <p className="text-[11px] text-amber-400/80 font-medium">익일 18:00 자동 batch 전송 예정</p>
        </div>
      </div>

      {/* Main Content Area */}
      <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-6">
        <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
          <Tabs tabs={statusTabs} activeTab={activeTab} onChange={setActiveTab} />

          {/* Search */}
          <div className="relative w-full md:w-80">
            <Search size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500" />
            <input
              type="text"
              placeholder="공급받는자 상호, 사업자번호, 품목 검색..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2.5 rounded-2xl bg-white/5 border border-white/10 text-xs text-white placeholder-slate-500 outline-none focus:border-blue-500/50 transition-all"
            />
          </div>
        </div>

        {/* Sales Table */}
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-white/10 text-[11px] font-black uppercase tracking-wider text-slate-400">
                <th className="py-3 px-4">작성일자</th>
                <th className="py-3 px-4">공급받는자 (상호 / 사업자번호)</th>
                <th className="py-3 px-4">구분</th>
                <th className="py-3 px-4">품목 및 내역</th>
                <th className="py-3 px-4 text-right">공급가액</th>
                <th className="py-3 px-4 text-right">세액 (VAT)</th>
                <th className="py-3 px-4 text-right">총 합계금액</th>
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
                    <div>{inv.buyerName}</div>
                    <div className="text-[11px] text-slate-400 font-mono font-normal">{inv.buyerBizNum}</div>
                  </td>
                  <td className="py-4 px-4">
                    {inv.invoiceType === 'ZERO_RATE' ? (
                      <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-blue-500/10 text-blue-400 border border-blue-500/20">
                        영세율
                      </span>
                    ) : inv.invoiceType === 'MODIFIED' ? (
                      <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-500/10 text-amber-400 border border-amber-500/20">
                        수정
                      </span>
                    ) : (
                      <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-white/5 text-slate-400 border border-white/10">
                        과세
                      </span>
                    )}
                  </td>
                  <td className="py-4 px-4 text-slate-300 max-w-xs truncate">
                    {inv.itemSummary}
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
                    조건에 해당하는 매출 세금계산서 내역이 없습니다.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* New Invoice Issue Modal */}
      {isIssueModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-white/10 rounded-3xl max-w-2xl w-full p-6 space-y-6 shadow-2xl animate-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between pb-4 border-b border-white/10">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-2xl bg-blue-600/10 border border-blue-500/20 flex items-center justify-center text-blue-400">
                  <FileCheck2 size={20} />
                </div>
                <div>
                  <h3 className="text-lg font-black text-white italic">전자세금계산서 신규 발행</h3>
                  <p className="text-xs text-slate-400">공급받는자 및 품목 세부 정보를 입력하세요.</p>
                </div>
              </div>
              <button 
                onClick={() => setIsIssueModalOpen(false)}
                className="p-2 rounded-xl bg-white/5 hover:bg-white/10 text-slate-400 hover:text-white"
              >
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleCreateInvoice} className="space-y-4">
              {/* Supplier Info (Readonly) */}
              <div className="p-3.5 rounded-2xl bg-white/5 border border-white/5 flex items-center justify-between text-xs">
                <div>
                  <span className="text-[10px] font-black text-slate-500 uppercase">공급자 (자사)</span>
                  <p className="font-bold text-white mt-0.5">(주)스카이솔루션 <span className="font-mono text-slate-400 text-[11px]">(220-81-98765)</span></p>
                </div>
                <span className="px-2.5 py-1 rounded-xl bg-blue-500/10 text-blue-400 font-mono text-[10px] font-bold">
                  본점 사업자
                </span>
              </div>

              {/* Tax Invoice Type Selector */}
              <div className="space-y-1.5">
                <label className="text-xs font-bold text-slate-400">과세 구분 *</label>
                <div className="grid grid-cols-2 gap-2 p-1 bg-slate-950/50 rounded-2xl border border-white/5">
                  <button
                    type="button"
                    onClick={() => setNewInvoiceType('NORMAL')}
                    className={`py-2.5 rounded-xl text-xs font-black transition-all ${
                      newInvoiceType === 'NORMAL'
                        ? 'bg-blue-600 text-white shadow-md'
                        : 'text-slate-400 hover:text-white'
                    }`}
                  >
                    일반 과세 (10%)
                  </button>
                  <button
                    type="button"
                    onClick={() => setNewInvoiceType('ZERO_RATE')}
                    className={`py-2.5 rounded-xl text-xs font-black transition-all ${
                      newInvoiceType === 'ZERO_RATE'
                        ? 'bg-blue-600 text-white shadow-md'
                        : 'text-slate-400 hover:text-white'
                    }`}
                  >
                    영세율 적용 (0%)
                  </button>
                </div>
              </div>

              {/* Buyer Inputs */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <label className="text-xs font-bold text-slate-400">공급받는자 상호 *</label>
                  <input
                    type="text"
                    required
                    placeholder="예: (주)카카오"
                    value={newBuyerName}
                    onChange={(e) => setNewBuyerName(e.target.value)}
                    className="w-full px-4 py-2.5 rounded-xl bg-white/5 border border-white/10 text-xs text-white placeholder-slate-500 outline-none focus:border-blue-500/50"
                  />
                </div>
                <div className="space-y-1.5">
                  <label className="text-xs font-bold text-slate-400">공급받는자 사업자번호 *</label>
                  <input
                    type="text"
                    required
                    placeholder="123-45-67890"
                    value={newBuyerBizNum}
                    onChange={(e) => setNewBuyerBizNum(e.target.value)}
                    className="w-full px-4 py-2.5 rounded-xl bg-white/5 border border-white/10 text-xs text-white placeholder-slate-500 outline-none focus:border-blue-500/50 font-mono"
                  />
                </div>
              </div>

              {/* Item Summary */}
              <div className="space-y-1.5">
                <label className="text-xs font-bold text-slate-400">품목명 및 내역 *</label>
                <input
                  type="text"
                  required
                  placeholder="예: 7월분 시스템 개발 용역 수수료"
                  value={newItemSummary}
                  onChange={(e) => setNewItemSummary(e.target.value)}
                  className="w-full px-4 py-2.5 rounded-xl bg-white/5 border border-white/10 text-xs text-white placeholder-slate-500 outline-none focus:border-blue-500/50"
                />
              </div>

              {/* Supply Amount Input */}
              <div className="space-y-1.5">
                <label className="text-xs font-bold text-slate-400">공급가액 (원) *</label>
                <div className="relative">
                  <input
                    type="number"
                    required
                    placeholder="0"
                    value={newSupplyAmount}
                    onChange={(e) => setNewSupplyAmount(e.target.value)}
                    className="w-full px-4 py-2.5 rounded-xl bg-white/5 border border-white/10 text-xs text-white font-mono placeholder-slate-500 outline-none focus:border-blue-500/50"
                  />
                </div>
              </div>

              {/* Auto Calculated Tax & Total Card */}
              <div className="p-4 rounded-2xl bg-white/5 border border-white/5 space-y-2">
                <div className="flex items-center justify-between text-xs text-slate-400">
                  <span className="flex items-center gap-1.5">
                    <Calculator size={14} className="text-blue-400" />
                    <span>자동 계산 세액 (VAT)</span>
                  </span>
                  <span className="font-mono font-bold text-emerald-400">
                    <AmountDisplay amount={calculatedTax} />
                  </span>
                </div>
                <div className="flex items-center justify-between text-sm font-bold text-white pt-2 border-t border-white/5">
                  <span>총 합계금액</span>
                  <span className="font-mono font-black text-blue-400">
                    <AmountDisplay amount={calculatedTotal} />
                  </span>
                </div>
              </div>

              {/* Remark */}
              <div className="space-y-1.5">
                <label className="text-xs font-bold text-slate-400">비고 / 참고사항</label>
                <input
                  type="text"
                  placeholder="계약서 번호 또는 메모 입력 (선택)"
                  value={newRemark}
                  onChange={(e) => setNewRemark(e.target.value)}
                  className="w-full px-4 py-2.5 rounded-xl bg-white/5 border border-white/10 text-xs text-white placeholder-slate-500 outline-none focus:border-blue-500/50"
                />
              </div>

              {/* Modal Buttons */}
              <div className="flex items-center justify-end gap-3 pt-4 border-t border-white/10">
                <button
                  type="button"
                  onClick={() => setIsIssueModalOpen(false)}
                  className="px-5 py-2.5 rounded-xl bg-white/10 hover:bg-white/20 text-white font-bold text-xs transition-all"
                >
                  취소
                </button>
                <button
                  type="submit"
                  className="px-6 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-black text-xs shadow-lg shadow-blue-600/20 transition-all flex items-center gap-2"
                >
                  <Send size={14} />
                  <span>세금계산서 발행 승인</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Invoice Detail Modal */}
      {selectedInvoice && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-white/10 rounded-3xl max-w-2xl w-full p-6 space-y-6 shadow-2xl animate-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between pb-4 border-b border-white/10">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-2xl bg-blue-600/10 border border-blue-500/20 flex items-center justify-center text-blue-400">
                  <Building2 size={20} />
                </div>
                <div>
                  <h3 className="text-lg font-black text-white italic">매출 전자세금계산서 상세</h3>
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
              <div className="p-4 rounded-2xl bg-white/5 border border-white/5 space-y-2">
                <span className="text-[10px] font-black uppercase text-blue-400 tracking-wider">공급자 (자사)</span>
                <p className="text-sm font-black text-white">{selectedInvoice.supplierName}</p>
                <p className="text-xs font-mono text-slate-400">사업자번호: {selectedInvoice.supplierBizNum}</p>
              </div>
              <div className="p-4 rounded-2xl bg-white/5 border border-white/5 space-y-2">
                <span className="text-[10px] font-black uppercase text-emerald-400 tracking-wider">공급받는자</span>
                <p className="text-sm font-black text-white">{selectedInvoice.buyerName}</p>
                <p className="text-xs font-mono text-slate-400">사업자번호: {selectedInvoice.buyerBizNum}</p>
              </div>
            </div>

            <div className="p-4 rounded-2xl bg-white/5 border border-white/5 space-y-3">
              <div className="flex items-center justify-between text-xs font-bold text-slate-400">
                <span>품목: {selectedInvoice.itemSummary}</span>
                <span>발행일자: {selectedInvoice.issueDate}</span>
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

'use client';

import React, { useState, useEffect, useCallback } from 'react';
import { 
  FileSearch, Download, AlertCircle, Plus, RefreshCcw, 
  Search, Filter, ChevronRight, CheckCircle2, XCircle, Clock,
  ArrowUpRight, ArrowDownLeft, Receipt, Building2, Calendar
} from 'lucide-react';
import { taxService, TaxInvoice, TaxInvoiceRequest } from '@/services/taxService';

/**
 * [세무/부가세 신고 지원 화면]
 * 부가가치세 신고를 위해 매입/매출 증빙 데이터를 집계하고 국세청 데이터와 대조합니다.
 * 리팩토링된 헥사고날 백엔드 API와 연동하여 실데이터를 기반으로 동작합니다.
 */
export default function TaxVatSupportPage() {
  const [invoices, setInvoices] = useState<TaxInvoice[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  
  // 수기 등록 폼 상태
  const [formData, setFormData] = useState<TaxInvoiceRequest>({
    issueId: '',
    type: 'SALES',
    issueDate: new Date().toISOString().split('T')[0],
    businessPartnerCode: '',
    supplyAmount: 0,
    taxAmount: 0,
    totalAmount: 0
  });

  // 조회 기간 상태 (기본값: 이번 달)
  const [startDate, setStartDate] = useState(() => {
    const d = new Date();
    return new Date(d.getFullYear(), d.getMonth(), 1).toISOString().split('T')[0];
  });
  const [endDate, setEndDate] = useState(() => {
    const d = new Date();
    return new Date(d.getFullYear(), d.getMonth() + 1, 0).toISOString().split('T')[0];
  });

  const fetchInvoices = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await taxService.getInvoices(startDate, endDate);
      setInvoices(data);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : '데이터 로드 중 오류가 발생했습니다.';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, [startDate, endDate]);

  useEffect(() => {
    const timer = setTimeout(() => {
      void fetchInvoices();
    }, 0);
    return () => clearTimeout(timer);
  }, [fetchInvoices]);

  const handleCreateInvoice = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await taxService.createInvoice(formData);
      setIsModalOpen(false);
      fetchInvoices();
      // 초기화
      setFormData({
        issueId: '',
        type: 'SALES',
        issueDate: new Date().toISOString().split('T')[0],
        businessPartnerCode: '',
        supplyAmount: 0,
        taxAmount: 0,
        totalAmount: 0
      });
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : '등록 실패');
    }
  };

  const salesInvoices = invoices.filter(inv => inv.type === 'SALES');
  const purchaseInvoices = invoices.filter(inv => inv.type === 'PURCHASE');

  const salesTaxTotal = salesInvoices.reduce((sum, inv) => sum + inv.taxAmount, 0);
  const purchaseTaxTotal = purchaseInvoices.reduce((sum, inv) => sum + inv.taxAmount, 0);
  const netVat = salesTaxTotal - purchaseTaxTotal;

  const salesSupplyTotal = salesInvoices.reduce((sum, inv) => sum + inv.supplyAmount, 0);
  const purchaseSupplyTotal = purchaseInvoices.reduce((sum, inv) => sum + inv.supplyAmount, 0);

  return (
    <div className="flex flex-col gap-8 pb-20">
      {/* 헤더 섹션 */}
      <header className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <div className="flex items-center gap-3">
            <div className="p-2 bg-indigo-500/10 rounded-lg">
              <Receipt className="text-indigo-400" size={24} />
            </div>
            <h2 className="text-3xl font-black text-white italic tracking-tight uppercase">Tax & VAT Management</h2>
          </div>
          <p className="text-slate-500 mt-2 text-sm font-medium leading-none">부가가치세 신고 지원 및 전자세금계산서 통합 관리</p>
        </div>
        
        <div className="flex flex-wrap gap-2 items-center">
           <div className="flex bg-slate-900/50 backdrop-blur-md border border-white/5 rounded-xl px-3 items-center group focus-within:border-indigo-500/50 transition-all">
              <Calendar size={16} className="text-slate-500 group-focus-within:text-indigo-400" />
              <input 
                type="date" 
                value={startDate} 
                onChange={(e) => setStartDate(e.target.value)}
                className="bg-transparent text-slate-300 text-xs font-bold p-3 focus:outline-none"
              />
              <span className="text-slate-700 font-black">~</span>
              <input 
                type="date" 
                value={endDate} 
                onChange={(e) => setEndDate(e.target.value)}
                className="bg-transparent text-slate-300 text-xs font-bold p-3 focus:outline-none"
              />
           </div>
           
           <button 
              onClick={fetchInvoices}
              className="bg-slate-800 hover:bg-slate-700 text-slate-300 px-5 py-3 rounded-xl border border-white/5 transition-all flex items-center gap-2 text-sm font-bold shadow-xl active:scale-95">
              <RefreshCcw size={18} className={loading ? 'animate-spin' : ''} /> 갱신
           </button>
           
           <button 
              onClick={() => setIsModalOpen(true)}
              className="bg-indigo-600 hover:bg-indigo-500 text-white px-5 py-3 rounded-xl border border-indigo-500/20 transition-all flex items-center gap-2 text-sm font-bold shadow-xl shadow-indigo-500/10 active:scale-95">
              <Plus size={18} /> 전표 수기 등록
           </button>
        </div>
      </header>

      {/* 요약 대시보드 */}
      <section className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* 부가세 정산 카드 */}
        <div className="lg:col-span-8 bg-slate-900/40 border border-white/10 rounded-[32px] p-10 backdrop-blur-2xl relative overflow-hidden group shadow-2xl">
          <div className="absolute top-0 right-0 w-96 h-96 bg-indigo-600/10 blur-[120px] -z-10 rounded-full" />
          
          <div className="flex justify-between items-start mb-12">
            <div>
              <h4 className="text-xs font-black text-indigo-400 uppercase tracking-[0.2em] mb-2">VAT Settlement Status</h4>
              <p className="text-slate-400 text-sm font-medium italic">신고 기간: {startDate} - {endDate}</p>
            </div>
            <div className="px-4 py-1.5 bg-indigo-500/10 border border-indigo-500/20 rounded-full">
              <span className="text-[10px] font-black text-indigo-400 uppercase tracking-widest leading-none">Real-time Calculation</span>
            </div>
          </div>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-12 relative z-10">
            {/* 매출 섹션 */}
            <div className="space-y-6">
              <div className="flex items-center gap-2 text-blue-400">
                <ArrowUpRight size={18} />
                <span className="text-[10px] font-black uppercase tracking-widest">Output Tax (매출)</span>
              </div>
              <div>
                <span className="text-[10px] font-bold text-slate-500 uppercase block mb-1">공급가액</span>
                <span className="text-xl font-bold text-slate-300">₩{salesSupplyTotal.toLocaleString()}</span>
              </div>
              <div>
                <span className="text-[10px] font-bold text-slate-500 uppercase block mb-1">부가가치세</span>
                <span className="text-4xl font-black italic text-white tracking-tighter">₩{salesTaxTotal.toLocaleString()}</span>
              </div>
            </div>

            {/* 매입 섹션 */}
            <div className="space-y-6">
              <div className="flex items-center gap-2 text-rose-400">
                <ArrowDownLeft size={18} />
                <span className="text-[10px] font-black uppercase tracking-widest">Input Tax (매입)</span>
              </div>
              <div>
                <span className="text-[10px] font-bold text-slate-500 uppercase block mb-1">공급가액</span>
                <span className="text-xl font-bold text-slate-300">₩{purchaseSupplyTotal.toLocaleString()}</span>
              </div>
              <div>
                <span className="text-[10px] font-bold text-slate-500 uppercase block mb-1">매입세액 공제</span>
                <span className="text-4xl font-black italic text-white tracking-tighter">₩{purchaseTaxTotal.toLocaleString()}</span>
              </div>
            </div>
          </div>

          <div className="mt-12 pt-10 border-t border-white/5 flex flex-col md:flex-row justify-between items-end gap-6">
             <div>
                <span className="text-xl font-black text-white italic tracking-tight uppercase flex items-center gap-3">
                  Estimated Net VAT 
                  {netVat >= 0 ? 
                    <span className="text-[10px] not-italic font-bold bg-amber-500/10 text-amber-500 px-2 py-0.5 rounded border border-amber-500/20">납부 대상</span> : 
                    <span className="text-[10px] not-italic font-bold bg-emerald-500/10 text-emerald-500 px-2 py-0.5 rounded border border-emerald-500/20">환급 대상</span>
                  }
                </span>
                <p className="text-slate-500 text-xs font-medium mt-1">전자신고 시 확정 금액과 차이가 발생할 수 있습니다.</p>
             </div>
             <div className="text-right">
                <span className={`text-6xl font-black italic tracking-tighter leading-none ${netVat >= 0 ? 'text-white' : 'text-emerald-400'}`}>
                  ₩{Math.abs(netVat).toLocaleString()}
                </span>
             </div>
          </div>
        </div>

        {/* 액션 카드 */}
        <div className="lg:col-span-4 flex flex-col gap-6">
          <div className="bg-gradient-to-br from-indigo-600 to-blue-700 rounded-[32px] p-8 shadow-2xl shadow-indigo-500/20 group relative overflow-hidden">
            <div className="absolute -right-10 -bottom-10 opacity-20 rotate-12 group-hover:rotate-0 transition-transform duration-700">
              <Building2 size={180} />
            </div>
            <h3 className="text-xl font-black text-white flex items-center gap-3 tracking-tight mb-6 italic uppercase">
               Tax Filing Tools
            </h3>
            <p className="text-indigo-100 text-sm font-medium leading-relaxed mb-10 opacity-80">
              Hometax(국세청) 시스템과 연동하여 증빙 자료를 자동으로 수집하고 검증합니다.
            </p>
            <div className="space-y-3 relative z-10">
              <button className="w-full bg-white text-indigo-600 font-black text-xs py-4 rounded-2xl transition-all hover:bg-indigo-50 shadow-xl active:scale-[0.98] uppercase tracking-widest flex items-center justify-center gap-2">
                <RefreshCcw size={16} /> 국세청 데이터 동기화
              </button>
              <button className="w-full bg-indigo-500/30 text-white font-black text-xs py-4 rounded-2xl transition-all hover:bg-indigo-500/40 shadow-xl active:scale-[0.98] uppercase tracking-widest border border-white/20 flex items-center justify-center gap-2">
                <Download size={16} /> 신고용 엑셀 다운로드
              </button>
            </div>
          </div>

          <div className="bg-slate-900/60 border border-white/5 rounded-[32px] p-8 flex-grow">
            <h5 className="text-[10px] font-black text-slate-500 uppercase tracking-widest mb-6">최근 검증 리포트</h5>
            <div className="space-y-4">
              <div className="flex items-center gap-4">
                <div className="w-10 h-10 rounded-full bg-emerald-500/10 flex items-center justify-center text-emerald-500">
                  <CheckCircle2 size={20} />
                </div>
                <div>
                  <p className="text-xs font-bold text-white">매출 합계 검증 완료</p>
                  <p className="text-[10px] text-slate-500 mt-1">2026.04.29 14:20:01</p>
                </div>
              </div>
              <div className="flex items-center gap-4">
                <div className="w-10 h-10 rounded-full bg-amber-500/10 flex items-center justify-center text-amber-500">
                  <Clock size={20} />
                </div>
                <div>
                  <p className="text-xs font-bold text-white">매입 누락 가능성 감지 (3건)</p>
                  <p className="text-[10px] text-slate-500 mt-1">2026.04.28 09:15:33</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 데이터 리스트 필터 및 검색 */}
      <section className="bg-slate-900/40 border border-white/10 rounded-[32px] overflow-hidden backdrop-blur-xl shadow-2xl">
        <div className="p-8 border-b border-white/5 bg-white/[0.02] flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
            <div className="flex items-center gap-4">
              <div className="p-2 bg-amber-500/10 rounded-lg">
                <FileSearch className="text-amber-400" size={20} />
              </div>
              <h3 className="text-lg font-black text-white italic tracking-tight leading-none uppercase">
                 Invoice Registry <span className="text-slate-500 not-italic ml-2 text-sm font-medium">({invoices.length} records)</span>
              </h3>
            </div>
            
            <div className="flex gap-2 w-full md:w-auto">
              <div className="relative flex-grow md:flex-grow-0">
                <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500" size={16} />
                <input 
                  type="text" 
                  placeholder="거래처명 또는 승인번호 검색..."
                  className="bg-slate-950/50 border border-white/5 rounded-xl py-3 pl-12 pr-4 text-xs font-medium text-slate-300 focus:outline-none focus:border-indigo-500/50 transition-all w-full md:w-64"
                />
              </div>
              <button className="bg-slate-800 text-slate-400 p-3 rounded-xl border border-white/5 hover:bg-slate-700 transition-all">
                <Filter size={18} />
              </button>
            </div>
        </div>
        
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="text-slate-500 text-[10px] font-black uppercase tracking-[0.15em] bg-white/[0.03]">
                <th className="px-8 py-6">Status & Type</th>
                <th className="px-6 py-6">Issue Date</th>
                <th className="px-6 py-6">Issue Identification</th>
                <th className="px-6 py-6">Business Partner</th>
                <th className="px-6 py-6">Supply Amount</th>
                <th className="px-6 py-6">Tax (10%)</th>
                <th className="px-8 py-6 text-right">Total Amount</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5">
              {loading ? (
                <tr>
                  <td colSpan={7} className="px-8 py-32 text-center text-slate-500 font-bold italic">
                    <div className="flex flex-col items-center justify-center gap-4">
                      <div className="w-8 h-8 border-4 border-indigo-500 border-t-transparent rounded-full animate-spin" />
                      <span className="text-xs uppercase tracking-widest text-slate-400">데이터를 분석하고 있습니다</span>
                    </div>
                  </td>
                </tr>
              ) : invoices.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-8 py-32 text-center text-slate-500 font-bold italic">
                    <div className="flex flex-col items-center justify-center gap-2 opacity-30">
                      <AlertCircle size={48} className="mb-2" />
                      <span className="text-sm uppercase tracking-widest">조회된 증빙 내역이 없습니다.</span>
                    </div>
                  </td>
                </tr>
              ) : (
                invoices.map((inv) => (
                  <tr key={inv.id} className="hover:bg-white/[0.03] transition-colors group cursor-pointer">
                    <td className="px-8 py-6">
                      <div className="flex items-center gap-3">
                        <div className={`w-2 h-2 rounded-full ${inv.type === 'PURCHASE' ? 'bg-rose-500 shadow-[0_0_8px_rgba(244,63,94,0.5)]' : 'bg-blue-500 shadow-[0_0_8px_rgba(59,130,246,0.5)]'}`} />
                        <span className={`text-[10px] font-black uppercase tracking-widest italic ${inv.type === 'PURCHASE' ? 'text-rose-400' : 'text-blue-400'}`}>
                          {inv.type === 'PURCHASE' ? 'Purchase' : 'Sales'}
                        </span>
                      </div>
                    </td>
                    <td className="px-6 py-6">
                      <span className="text-sm font-mono text-slate-400 font-bold">{inv.issueDate}</span>
                    </td>
                    <td className="px-6 py-6">
                      <div className="flex flex-col">
                        <span className="text-xs font-mono text-slate-500 group-hover:text-slate-300 transition-colors uppercase">{inv.issueId}</span>
                        <span className="text-[8px] text-slate-600 uppercase font-black tracking-widest mt-1">Electronic</span>
                      </div>
                    </td>
                    <td className="px-6 py-6">
                      <div className="flex flex-col">
                        <span className="text-sm font-bold text-white tracking-tight">{inv.businessPartnerName || 'Unknown Partner'}</span>
                        <span className="text-[10px] text-slate-500 font-mono">{inv.businessPartnerCode}</span>
                      </div>
                    </td>
                    <td className="px-6 py-6">
                      <span className="text-sm font-mono text-slate-400 font-bold">₩{inv.supplyAmount.toLocaleString()}</span>
                    </td>
                    <td className="px-6 py-6">
                      <span className="text-sm font-mono text-indigo-400 font-black">₩{inv.taxAmount.toLocaleString()}</span>
                    </td>
                    <td className="px-8 py-6 text-right">
                      <div className="flex items-center justify-end gap-3">
                        <span className="text-sm font-black text-white italic tracking-tighter">₩{inv.totalAmount.toLocaleString()}</span>
                        <ChevronRight size={16} className="text-slate-700 group-hover:text-white transition-all transform group-hover:translate-x-1" />
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>

      {/* 수기 등록 모달 */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-slate-950/80 backdrop-blur-md" onClick={() => setIsModalOpen(false)} />
          <div className="bg-slate-900 border border-white/10 rounded-[40px] w-full max-w-2xl overflow-hidden relative shadow-2xl animate-in fade-in zoom-in duration-300">
            <div className="p-10 border-b border-white/5 flex justify-between items-center">
              <div>
                <h3 className="text-2xl font-black text-white italic uppercase tracking-tight">Manual Invoice Entry</h3>
                <p className="text-slate-500 text-sm mt-1">증빙 전표 정보를 정확히 입력하십시오.</p>
              </div>
              <button 
                onClick={() => setIsModalOpen(false)}
                className="w-10 h-10 rounded-full border border-white/10 flex items-center justify-center text-slate-500 hover:text-white hover:bg-white/5 transition-all"
              >
                <XCircle size={24} />
              </button>
            </div>
            
            <form onSubmit={handleCreateInvoice} className="p-10 space-y-8">
              <div className="grid grid-cols-2 gap-6">
                <div className="space-y-2">
                  <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest ml-1">Invoice Type</label>
                  <div className="flex gap-2">
                    <button 
                      type="button"
                      onClick={() => setFormData({...formData, type: 'SALES'})}
                      className={`flex-1 py-4 rounded-2xl font-black text-xs uppercase tracking-widest transition-all ${formData.type === 'SALES' ? 'bg-blue-600 text-white shadow-lg shadow-blue-500/20' : 'bg-slate-800 text-slate-500 border border-white/5'}`}
                    >
                      Sales (매출)
                    </button>
                    <button 
                      type="button"
                      onClick={() => setFormData({...formData, type: 'PURCHASE'})}
                      className={`flex-1 py-4 rounded-2xl font-black text-xs uppercase tracking-widest transition-all ${formData.type === 'PURCHASE' ? 'bg-rose-600 text-white shadow-lg shadow-rose-500/20' : 'bg-slate-800 text-slate-500 border border-white/5'}`}
                    >
                      Purchase (매입)
                    </button>
                  </div>
                </div>
                <div className="space-y-2">
                  <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest ml-1">Issue Date</label>
                  <input 
                    type="date"
                    required
                    value={formData.issueDate}
                    onChange={(e) => setFormData({...formData, issueDate: e.target.value})}
                    className="w-full bg-slate-800 border border-white/5 rounded-2xl p-4 text-white text-sm font-bold focus:outline-none focus:border-indigo-500 transition-all"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-6">
                <div className="space-y-2">
                  <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest ml-1">Identification Number</label>
                  <input 
                    type="text"
                    required
                    placeholder="승인번호 (예: 20260430-...)"
                    value={formData.issueId}
                    onChange={(e) => setFormData({...formData, issueId: e.target.value})}
                    className="w-full bg-slate-800 border border-white/5 rounded-2xl p-4 text-white text-sm font-bold focus:outline-none focus:border-indigo-500 transition-all"
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest ml-1">Partner Code</label>
                  <input 
                    type="text"
                    required
                    placeholder="BP-XXXX"
                    value={formData.businessPartnerCode}
                    onChange={(e) => setFormData({...formData, businessPartnerCode: e.target.value})}
                    className="w-full bg-slate-800 border border-white/5 rounded-2xl p-4 text-white text-sm font-bold focus:outline-none focus:border-indigo-500 transition-all"
                  />
                </div>
              </div>

              <div className="grid grid-cols-3 gap-6">
                <div className="space-y-2">
                  <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest ml-1">Supply Amt</label>
                  <input 
                    type="number"
                    required
                    value={formData.supplyAmount}
                    onChange={(e) => {
                      const supply = Number(e.target.value);
                      const tax = Math.floor(supply * 0.1);
                      setFormData({...formData, supplyAmount: supply, taxAmount: tax, totalAmount: supply + tax});
                    }}
                    className="w-full bg-slate-800 border border-white/5 rounded-2xl p-4 text-white text-sm font-bold focus:outline-none focus:border-indigo-500 transition-all"
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest ml-1">VAT (10%)</label>
                  <input 
                    type="number"
                    required
                    value={formData.taxAmount}
                    onChange={(e) => {
                      const tax = Number(e.target.value);
                      setFormData({...formData, taxAmount: tax, totalAmount: formData.supplyAmount + tax});
                    }}
                    className="w-full bg-slate-800 border border-white/5 rounded-2xl p-4 text-indigo-400 text-sm font-bold focus:outline-none focus:border-indigo-500 transition-all"
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest ml-1">Total Sum</label>
                  <div className="w-full bg-slate-950 border border-white/10 rounded-2xl p-4 text-white text-sm font-black italic">
                    ₩{formData.totalAmount.toLocaleString()}
                  </div>
                </div>
              </div>

              <div className="pt-6">
                <button 
                  type="submit"
                  className="w-full bg-indigo-600 hover:bg-indigo-500 text-white font-black py-5 rounded-2xl transition-all shadow-xl shadow-indigo-600/20 active:scale-[0.98] uppercase tracking-[0.2em]"
                >
                  Register Invoice
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {error && (
        <div className="fixed bottom-10 right-10 bg-rose-600 text-white px-6 py-4 rounded-2xl shadow-2xl flex items-center gap-4 animate-in slide-in-from-bottom-10">
          <AlertCircle size={20} />
          <span className="text-sm font-bold uppercase tracking-tight">{error}</span>
          <button onClick={() => setError(null)} className="ml-4 opacity-50 hover:opacity-100 transition-opacity">
            <XCircle size={18} />
          </button>
        </div>
      )}
    </div>
  );
}

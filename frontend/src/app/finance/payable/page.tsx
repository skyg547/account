"use client";

import React, { useState, useEffect } from 'react';
import { CreditCard, Calendar, Clock, CheckCircle2, ChevronRight } from 'lucide-react';
import { payableService, PayableInvoice } from '@/services/payableService';

/**
 * [매입채무 및 지급 관리 화면]
 * 협력사에 지급해야 할 대금을 관리하고 주별/월별 지급 스케줄을 조정합니다.
 * 백엔드 Payable API 연동 완료.
 */
export default function PayableManagementPage() {
  const [invoices, setInvoices] = useState<PayableInvoice[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchInvoices = async () => {
    setLoading(true);
    try {
      const data = await payableService.getInvoices();
      setInvoices(data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchInvoices();
  }, []);

  const totalPending = invoices.filter(inv => inv.status !== 'FULLY_PAID' && inv.status !== 'CANCELLED').length;
  const totalPaidAmount = invoices.filter(inv => inv.status === 'FULLY_PAID' || inv.status === 'PARTIALLY_PAID').reduce((sum, inv) => sum + (inv.paidAmount || 0), 0);
  
  const formatAmount = (val: number) => `₩${(val || 0).toLocaleString()}`;

  return (
    <div className="flex flex-col gap-8">
      <header className="flex justify-between items-center">
        <div>
          <h2 className="text-3xl font-black text-white italic tracking-tight uppercase">매입채무 및 지급 관리</h2>
          <p className="text-slate-500 mt-2 text-sm font-medium leading-none">공급업체 지급 대기 내역 정합성 확인 및 최적 자금 계획 기반 일괄 지급 실행</p>
        </div>
        <div className="flex gap-2">
          <button 
            onClick={fetchInvoices}
            className="bg-slate-800 hover:bg-slate-700 text-slate-300 px-4 py-2 rounded-xl border border-white/5 transition-all flex items-center gap-2 text-sm font-bold shadow-lg"
          >
            <Clock size={18} /> 새로고침
          </button>
          <button className="bg-white text-slate-950 hover:bg-indigo-400 hover:text-white px-6 py-3 rounded-2xl transition-all flex items-center gap-3 text-sm font-black uppercase tracking-widest shadow-xl active:scale-95">
            <CreditCard size={18} /> 일괄 지급 승인
          </button>
        </div>
      </header>

      {/* 지급 스케줄 대시보드 */}
      <section className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        <div className="lg:col-span-8 bg-white/5 border border-white/10 rounded-[32px] p-10 backdrop-blur-xl relative overflow-hidden group">
          <div className="flex justify-between items-center mb-10">
            <h3 className="text-xs font-black text-indigo-400 uppercase tracking-[0.2em] flex items-center gap-3">
              <Calendar size={18} /> 금주 지급 예정 (Weekly Runway)
            </h3>
            <span className="text-[10px] font-black text-slate-500 uppercase tracking-widest">NEXT 7 DAYS</span>
          </div>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-8 relative z-10">
            {[
              { date: '4월 22일 (수)', amount: '4,500,000', status: 'D-Day', color: 'text-rose-500' },
              { date: '4월 24일 (금)', amount: '12,800,000', status: 'D-2', color: 'text-blue-400' },
            ].map((item, i) => (
              <div key={i} className="bg-slate-950/40 border border-white/5 p-6 rounded-2xl hover:border-indigo-500/30 transition-all group/item">
                <div className="flex justify-between items-start mb-4">
                  <span className="text-[11px] font-bold text-slate-500 uppercase tracking-tighter">{item.date}</span>
                  <span className={`text-[10px] font-black px-2 py-0.5 rounded-full bg-white/5 border border-white/10 ${item.color}`}>{item.status}</span>
                </div>
                <div className="text-3xl font-black italic text-white tracking-tighter group-hover/item:text-indigo-400 transition-colors">
                  ₩{item.amount}
                </div>
              </div>
            ))}
          </div>

          <div className="mt-10 pt-8 border-t border-white/5 flex justify-between items-end">
             <div className="flex flex-col">
                <span className="text-[10px] font-black text-slate-600 uppercase tracking-widest mb-1 italic">Total Weekly commitment</span>
                <span className="text-4xl font-black italic text-white tracking-tighter shadow-indigo-500/50">₩17,300,000</span>
             </div>
             <div className="w-32 h-12 bg-indigo-500/10 rounded-full flex items-center justify-center border border-indigo-500/20">
                <div className="w-1.5 h-1.5 bg-indigo-500 rounded-full animate-ping mr-3" />
                <span className="text-[10px] font-black text-indigo-400 uppercase tracking-widest leading-none">Healthy Flow</span>
             </div>
          </div>

          <div className="absolute -right-20 -bottom-20 opacity-[0.02] group-hover:opacity-[0.05] transition-opacity duration-1000 pointer-events-none">
             <Calendar size={350} className="text-white" />
          </div>
        </div>

        <div className="lg:col-span-4 flex flex-col gap-6">
          <div className="bg-amber-500/5 border border-amber-500/20 rounded-[32px] p-8 flex items-center gap-6 group hover:bg-amber-500/10 transition-all border-dashed">
            <div className="w-14 h-14 rounded-2xl bg-amber-500/10 flex items-center justify-center text-amber-500 group-hover:scale-110 transition-transform">
              <Clock size={28} />
            </div>
            <div>
              <span className="text-[10px] font-black text-slate-500 uppercase tracking-widest block mb-1">지급 대기 (Pending)</span>
              <h4 className="text-3xl font-black italic text-white leading-none tracking-tighter">{totalPending}건</h4>
            </div>
          </div>

          <div className="bg-emerald-500/5 border border-emerald-500/20 rounded-[32px] p-8 flex items-center gap-6 group hover:bg-emerald-500/10 transition-all">
            <div className="w-14 h-14 rounded-2xl bg-emerald-500/10 flex items-center justify-center text-emerald-500 group-hover:scale-110 transition-transform">
              <CheckCircle2 size={28} />
            </div>
            <div>
              <span className="text-[10px] font-black text-slate-500 uppercase tracking-widest block mb-1">금월 완료 (Success)</span>
              <h4 className="text-2xl font-black italic text-white leading-none tracking-tighter uppercase tracking-[-0.05em]">{formatAmount(totalPaidAmount)}</h4>
            </div>
          </div>

          <div className="flex-1 bg-white/5 border border-white/10 rounded-[32px] p-8 flex flex-col justify-center items-center text-center">
             <p className="text-xs font-bold text-slate-500 leading-relaxed italic mb-6">현재 자금 가용성 대비 지급 여력이 충분합니다. 승인 대기 전표를 확인하십시오.</p>
             <button className="flex items-center gap-2 text-[10px] font-black text-indigo-400 uppercase tracking-widest hover:text-white transition-colors group">
               지급 보류 목록 확인 <ChevronRight size={14} className="group-hover:translate-x-1 transition-transform" />
             </button>
          </div>
        </div>
      </section>

      {/* 미지급 상세 그리드 */}
      <section className="bg-white/5 border border-white/10 rounded-[32px] overflow-hidden backdrop-blur-xl">
        <div className="p-8 border-b border-white/5 bg-white/[0.01] flex flex-col sm:flex-row justify-between items-center gap-4">
            <h3 className="text-lg font-black text-white flex items-center gap-3 tracking-tight leading-none uppercase italic">
               지급 대기 상세 내역 (Accounts Payable)
            </h3>
            <div className="flex gap-3">
              <select className="bg-slate-950 border border-white/5 rounded-xl px-4 py-2 text-[10px] font-black text-slate-400 uppercase outline-none focus:border-indigo-500 transition-all cursor-pointer">
                <option>ALL VENDORS</option>
              </select>
              <select className="bg-slate-950 border border-white/5 rounded-xl px-4 py-2 text-[10px] font-black text-slate-400 uppercase outline-none focus:border-indigo-500 transition-all cursor-pointer">
                <option>ALL ACCOUNTS</option>
              </select>
            </div>
        </div>
        
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="text-slate-500 text-[10px] font-black uppercase tracking-widest bg-white/[0.02]">
                <th className="px-8 py-5 w-10"><input type="checkbox" className="w-4 h-4 rounded border-white/10 bg-slate-900 accent-indigo-600" /></th>
                <th className="px-6 py-5">Due Date</th>
                <th className="px-6 py-5">Vendor / Invoice No</th>
                <th className="px-6 py-5">Description</th>
                <th className="px-6 py-5">Amount (Balance)</th>
                <th className="px-8 py-5 text-right">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5">
              {loading ? (
                <tr>
                  <td colSpan={6} className="text-center py-10 text-slate-500 font-bold italic">
                    Loading invoices...
                  </td>
                </tr>
              ) : invoices.length === 0 ? (
                <tr>
                  <td colSpan={6} className="text-center py-10 text-slate-500 font-bold italic">
                    조회된 매입채무 내역이 없습니다.
                  </td>
                </tr>
              ) : (
                invoices.map((row, idx) => {
                  const todayStr = new Date().toISOString().split('T')[0];
                  const isUrgent = row.dueDate <= todayStr && row.status !== 'FULLY_PAID';
                  return (
                    <tr key={idx} className="hover:bg-white/[0.03] transition-colors group">
                      <td className="px-8 py-6"><input type="checkbox" className="w-4 h-4 rounded border-white/10 bg-slate-900 accent-indigo-600" /></td>
                      <td className={`px-6 py-6 text-sm font-mono font-bold ${isUrgent ? 'text-rose-500' : 'text-slate-500'}`}>{row.dueDate}</td>
                      <td className="px-6 py-6">
                         <div className="flex flex-col gap-1">
                           <span className="text-sm font-black text-white tracking-tight">{row.businessPartnerCode}</span>
                           <span className="text-[10px] text-slate-600 font-bold tracking-widest uppercase">{row.invoiceNo}</span>
                         </div>
                      </td>
                      <td className="px-6 py-6 text-sm font-medium text-slate-400">{row.description || '-'}</td>
                      <td className="px-6 py-6">
                         <div className="flex flex-col gap-1">
                            <span className="text-sm font-mono font-black italic text-slate-200">{formatAmount(row.balanceAmount)}</span>
                            <span className="text-[10px] text-slate-600 italic">Total: {formatAmount(row.totalAmount)}</span>
                         </div>
                      </td>
                      <td className="px-8 py-6 text-right">
                        <span className={`text-[10px] font-black px-3 py-1 rounded-full border ${
                          isUrgent ? 'bg-rose-500/10 text-rose-500 border-rose-500/20 animate-pulse' : 
                          row.status === 'FULLY_PAID' ? 'bg-emerald-500/10 text-emerald-500 border-emerald-500/20' :
                          'bg-slate-500/10 text-slate-500 border-white/5'
                        }`}>
                          {row.status}
                        </span>
                      </td>
                    </tr>
                  )
                })
              )}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}

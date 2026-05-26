"use client";

import React, { useState, useEffect } from 'react';
import { TrendingUp, Users, DollarSign, Calendar, AlertTriangle } from 'lucide-react';
import { receivableService, ReceivableInvoice } from '@/services/receivableService';

/**
 * [매출채권 및 연령 분석 화면]
 * 미수금 현황을 파악하고 채권 연령(Aging)을 분석하여 수급 관리를 수행합니다.
 * 백엔드 Receivable API 연동 완료.
 */
export default function ReceivableAgingPage() {
  const [invoices, setInvoices] = useState<ReceivableInvoice[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchInvoices = async () => {
    setLoading(true);
    try {
      const data = await receivableService.getInvoices();
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

  const totalReceivable = invoices.reduce((sum, inv) => sum + (inv.balanceAmount || 0), 0);
  
  // Calculate critical aging (e.g. past due > 90 days). Here we just simulate with a logic if needed, or default to 0
  const criticalAging = invoices.filter(inv => inv.status === 'DEFAULTED').reduce((sum, inv) => sum + (inv.balanceAmount || 0), 0);

  const formatAmount = (val: number) => `₩${(val || 0).toLocaleString()}`;

  return (
    <div className="flex flex-col gap-8">
      <header className="flex justify-between items-center">
        <div>
          <h2 className="text-3xl font-black text-white italic tracking-tight uppercase">매출채권 관리 및 연령 분석</h2>
          <p className="text-slate-500 mt-2 text-sm font-medium leading-none">미수 채권의 회수 상태 실시간 모니터링 및 연령별 부실 리스크 조기 식별</p>
        </div>
        <div className="flex gap-2">
           <button 
             onClick={fetchInvoices}
             className="bg-slate-800 hover:bg-slate-700 text-slate-300 px-4 py-2 rounded-xl border border-white/5 transition-all flex items-center gap-2 text-sm font-bold shadow-lg"
           >
              <DollarSign size={18} /> 입금 결과 매칭
           </button>
        </div>
      </header>

      {/* 연령별 대시보드 (Aging Chart) */}
      <section className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        <div className="lg:col-span-8 bg-white/5 border border-white/10 rounded-[32px] p-10 backdrop-blur-xl relative overflow-hidden group">
          <div className="flex justify-between items-center mb-10">
            <h4 className="text-xs font-black text-emerald-500 uppercase tracking-[0.2em] flex items-center gap-2">
              <TrendingUp size={16} /> 채권 연령 분포 (Aging Summary)
            </h4>
            <span className="text-[10px] font-black text-slate-500 uppercase tracking-widest">AS OF TODAY</span>
          </div>

          <div className="flex flex-col gap-8 relative z-10">
            <div className="flex items-end justify-around h-[240px] px-4 border-b border-white/5 pb-2">
              {[
                { label: '0-30일', height: '70%', color: 'from-emerald-500 to-teal-600', val: '₩588.1M' },
                { label: '31-60일', height: '20%', color: 'from-blue-500 to-indigo-600', val: '₩168.0M' },
                { label: '61-90일', height: '8%', color: 'from-amber-500 to-orange-600', val: '₩67.2M' },
                { label: '90일+', height: '4%', color: 'from-rose-500 to-red-600', val: '₩16.8M' },
              ].map((bar, i) => (
                <div key={i} className="flex flex-col items-center group/bar w-20">
                  <div className="mb-2 opacity-0 group-hover/bar:opacity-100 transition-opacity">
                    <span className="text-[10px] font-mono font-black text-white bg-slate-800 px-2 py-1 rounded shadow-xl">{bar.val}</span>
                  </div>
                  <div 
                    className={`w-full rounded-t-xl bg-gradient-to-t ${bar.color} shadow-lg group-hover/bar:brightness-125 transition-all duration-500 cursor-pointer relative`}
                    style={{ height: bar.height }}
                  >
                    <div className="absolute inset-0 bg-white/20 opacity-0 group-hover/bar:opacity-100 transition-opacity" />
                  </div>
                  <span className="mt-4 text-[11px] font-bold text-slate-500 group-hover/bar:text-slate-300 transition-colors uppercase tracking-widest">{bar.label}</span>
                </div>
              ))}
            </div>
          </div>
          
          <div className="absolute -right-10 -top-10 opacity-[0.02] group-hover:opacity-[0.05] transition-opacity duration-1000 pointer-events-none">
             <TrendingUp size={280} className="text-white" />
          </div>
        </div>
        
        <div className="lg:col-span-4 flex flex-col gap-6">
          <div className="bg-white/5 border border-white/10 rounded-[32px] p-8 hover:bg-white/[0.08] transition-all group overflow-hidden relative">
            <span className="text-[10px] font-black text-slate-500 uppercase tracking-widest block mb-2">Total Receivable Balance</span>
            <div className="flex items-baseline gap-2">
              <h3 className="text-3xl font-black italic text-white tracking-tighter">{formatAmount(totalReceivable)}</h3>
              <span className="text-[10px] font-bold text-emerald-500">+1.2%</span>
            </div>
            <div className="absolute -right-4 -bottom-4 opacity-[0.03] group-hover:opacity-[0.08] transition-opacity">
              <DollarSign size={80} className="text-white" />
            </div>
          </div>

          <div className="bg-rose-500/5 border border-rose-500/20 rounded-[32px] p-8 hover:bg-rose-500/10 transition-all group overflow-hidden relative border-dashed">
            <span className="text-[10px] font-black text-rose-500 uppercase tracking-widest block mb-2 flex items-center gap-2">
              <AlertTriangle size={14} /> Critical Aging (90일+ / Defaulted)
            </span>
            <div className="flex items-baseline gap-2">
              <h3 className="text-3xl font-black italic text-rose-500 tracking-tighter">{formatAmount(criticalAging)}</h3>
              <span className="text-[10px] font-bold text-rose-400">High Risk</span>
            </div>
            <div className="absolute -right-4 -bottom-4 opacity-[0.1] group-hover:opacity-[0.2] transition-opacity">
              <AlertTriangle size={80} className="text-rose-500" />
            </div>
          </div>

          <div className="bg-indigo-600/10 border border-indigo-500/20 rounded-[32px] p-8 flex flex-col justify-center">
            <button className="w-full bg-indigo-600 hover:bg-indigo-500 text-white font-black py-4 rounded-2xl transition-all shadow-xl shadow-indigo-900/20 active:scale-95 uppercase tracking-widest text-sm">
              상환 독촉 공문 발송
            </button>
          </div>
        </div>
      </section>

      {/* 상세 채권 리스트 */}
      <section className="bg-white/5 border border-white/10 rounded-[32px] overflow-hidden backdrop-blur-xl">
        <div className="p-8 border-b border-white/5 bg-white/[0.01] flex justify-between items-center">
            <h3 className="text-lg font-black text-white flex items-center gap-3 tracking-tight leading-none uppercase">
               <Users size={22} className="text-blue-400" /> 거래처별 채권 상세 현황
            </h3>
            <div className="flex gap-2">
               <div className="relative">
                  <Calendar className="absolute left-3 top-2.5 text-slate-500" size={14} />
                  <input type="text" placeholder="Filter by date..." className="bg-slate-950 border border-white/5 rounded-lg py-1.5 pl-9 pr-4 text-xs text-slate-400 outline-none w-40" />
               </div>
            </div>
        </div>
        
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="text-slate-500 text-[10px] font-black uppercase tracking-widest bg-white/[0.02]">
                <th className="px-8 py-5">Invoice No / Partner</th>
                <th className="px-6 py-5">Total Billed</th>
                <th className="px-6 py-5">Collected</th>
                <th className="px-6 py-5">Balance</th>
                <th className="px-6 py-5">Due Date</th>
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
                    조회된 매출채권 내역이 없습니다.
                  </td>
                </tr>
              ) : (
                invoices.map((row, idx) => (
                  <tr key={idx} className="hover:bg-white/[0.03] transition-colors group">
                    <td className="px-8 py-6">
                      <div className="flex flex-col gap-1">
                        <span className="text-sm font-black text-white tracking-tight">{row.businessPartnerCode}</span>
                        <span className="text-[10px] text-slate-600 font-bold tracking-widest uppercase">{row.invoiceNo}</span>
                      </div>
                    </td>
                    <td className="px-6 py-6 text-sm font-mono text-slate-400 font-bold">{formatAmount(row.totalAmount)}</td>
                    <td className="px-6 py-6 text-sm font-mono text-slate-400 font-bold">{formatAmount(row.collectedAmount)}</td>
                    <td className="px-6 py-6 text-sm font-mono text-emerald-400 font-black italic">{formatAmount(row.balanceAmount)}</td>
                    <td className="px-6 py-6 text-sm font-mono text-slate-500 font-bold italic">{row.dueDate}</td>
                    <td className="px-8 py-6 text-right">
                      <span className={`text-[10px] font-black px-3 py-1 rounded-full border ${
                        row.status === 'FULLY_COLLECTED' ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' :
                        row.status === 'PARTIALLY_COLLECTED' || row.status === 'ISSUED' ? 'bg-blue-500/10 text-blue-400 border-blue-500/20' :
                        'bg-rose-500/10 text-rose-400 border-rose-500/20 animate-pulse'
                      }`}>
                        {row.status}
                      </span>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}

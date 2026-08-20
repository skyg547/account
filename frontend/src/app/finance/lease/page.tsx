"use client";

import React, { useState, useEffect, useCallback } from 'react';
import { 
  FileText, 
  Plus, 
  Search, 
  Landmark, 
  ChevronRight,
  RefreshCcw,
  Play
} from 'lucide-react';
import { leaseService, LeaseContract } from '@/services/leaseService';

/**
 * [리스 회계 관리 화면 (IFRS 16)]
 * 리스 계약을 등록하고 사용권자산 및 리스부채의 상각/이자비용을 자동 산출합니다.
 * 리팩토링된 API와 연동하여 실데이터를 기반으로 동작합니다.
 */
export default function LeaseAccountingPage() {
  const [leases, setLeases] = useState<LeaseContract[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');

  const fetchLeases = useCallback(async () => {
    try {
      setError(null);
      const data = await leaseService.getLeases();
      setLeases(data);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : '리스 계약 목록을 불러오지 못했습니다.';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    let ignore = false;
    const trigger = async () => {
      if (!ignore) await fetchLeases();
    };
    setTimeout(() => trigger(), 0);
    return () => { ignore = true; };
  }, [fetchLeases]);

  const handleProcessMonthly = async () => {
    const today = new Date().toISOString().split('T')[0];
    if (confirm(`${today} 기준으로 리스 월별 회계처리를 실행하시겠습니까?`)) {
      try {
        await leaseService.processMonthly(today);
        alert('리스 회계 처리가 완료되었습니다.');
        fetchLeases();
      } catch (err: unknown) {
        const message = err instanceof Error ? err.message : '알 수 없는 오류';
        alert('리스 회계 처리 중 오류: ' + message);
      }
    }
  };

  const filteredLeases = leases.filter(lease => 
    lease.contractName.toLowerCase().includes(searchTerm.toLowerCase()) ||
    lease.contractNo.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const totalROU = leases.reduce((sum, l) => sum + l.initialRightOfUseAssetValue, 0);
  const totalLiability = leases.reduce((sum, l) => sum + l.initialLeaseLiabilityValue, 0);

  return (
    <div className="space-y-10">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
        <div className="space-y-2">
          <div className="flex items-center gap-3 text-slate-500 mb-2">
            <Landmark size={20} className="text-blue-500" />
            <span className="text-xs font-black uppercase tracking-[0.3em]">IFRS 16 Lease Management</span>
          </div>
          <h2 className="text-4xl font-black text-white tracking-tighter italic">
            리스 회계 통합 관리
          </h2>
          <p className="text-slate-500 font-medium max-w-2xl">
            IFRS 16 국제 표준에 따른 사용권자산(ROU) 및 리스부채를 산출하고 월별 상각/이자 비용을 자동 관리합니다.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button 
            onClick={fetchLeases}
            className="px-4 py-3 bg-white/5 hover:bg-white/10 border border-white/10 rounded-2xl text-slate-400 text-sm font-black transition-all">
            <RefreshCcw size={18} />
          </button>
          <button 
            onClick={handleProcessMonthly}
            className="px-6 py-3 bg-white/5 hover:bg-white/10 border border-white/10 rounded-2xl text-slate-400 text-sm font-black transition-all flex items-center gap-2">
            <Play size={18} className="text-emerald-500" /> 월별 회계 처리
          </button>
          <button className="px-6 py-3 bg-blue-600 hover:bg-blue-500 rounded-2xl text-white text-sm font-black transition-all shadow-lg shadow-blue-600/20 flex items-center gap-2 px-8">
            <Plus size={18} /> 신규 리스 계약
          </button>
        </div>
      </div>

      {error && (
        <div className="rounded-2xl border border-rose-500/30 bg-rose-500/10 px-5 py-3 text-sm font-bold text-rose-300">
          {error}
        </div>
      )}

      {/* Lease Overview Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
           <div className="glass-panel p-8 rounded-[3rem] border border-white/10 bg-white/[0.01]">
              <span className="text-[10px] font-black text-slate-600 uppercase tracking-widest">사용권자산 (ROU Assets)</span>
              <div className="text-3xl font-black text-white mt-1 italic tracking-tighter">₩{totalROU.toLocaleString()}</div>
              <p className="text-[10px] text-slate-700 font-bold mt-2 uppercase tracking-tight">Initial Net Value</p>
           </div>
           
           <div className="glass-panel p-8 rounded-[3rem] border border-white/10 bg-white/[0.01]">
              <span className="text-[10px] font-black text-slate-600 uppercase tracking-widest">리스부채 (Lease Liabilities)</span>
              <div className="text-3xl font-black text-white mt-1 italic tracking-tighter">₩{totalLiability.toLocaleString()}</div>
              <p className="text-[10px] text-slate-700 font-bold mt-2 uppercase tracking-tight">Initial Recognition</p>
           </div>

           <div className="glass-panel p-8 rounded-[3rem] border border-white/10 bg-white/[0.01]">
              <span className="text-[10px] font-black text-slate-600 uppercase tracking-widest">평균 할인율</span>
              <div className="text-3xl font-black text-white mt-1 italic tracking-tighter">
                {leases.length > 0 ? (leases.reduce((sum, l) => sum + l.discountRate, 0) / leases.length).toFixed(2) : 0}%
              </div>
              <p className="text-[10px] text-slate-700 font-bold mt-2 uppercase tracking-tight">Weighted Average</p>
           </div>
      </div>

      {/* Lease Registry List */}
      <div className="glass-panel p-10 rounded-[3rem] border border-white/10 bg-white/[0.01]">
         <div className="flex items-center justify-between mb-10">
            <h3 className="text-xl font-black text-white italic tracking-tight uppercase">Lease Contract Registry</h3>
            <div className="relative group max-w-sm w-full">
               <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-700" size={18} />
               <input 
                type="text" 
                placeholder="Search by contract name..." 
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full bg-slate-950 border border-white/5 rounded-2xl py-3.5 pl-12 pr-6 text-sm text-white outline-none focus:border-blue-500/30 transition-all font-bold" 
               />
            </div>
         </div>

         <div className="space-y-4">
            {loading ? (
              <div className="py-20 text-center text-slate-500 italic font-bold">Loading lease data...</div>
            ) : filteredLeases.length === 0 ? (
              <div className="py-20 text-center text-slate-500 italic font-bold">No lease contracts found.</div>
            ) : filteredLeases.map((lease) => (
              <div key={lease.id} className="flex items-center justify-between p-6 rounded-3xl bg-white/[0.01] border border-white/[0.03] hover:border-white/10 hover:bg-white/[0.02] transition-all group/item">
                 <div className="flex items-center gap-6">
                    <div className="w-12 h-12 rounded-2xl bg-blue-600/10 border border-blue-500/20 flex items-center justify-center text-blue-500">
                       <FileText size={20} />
                    </div>
                    <div className="flex flex-col">
                       <span className="text-base font-black text-white tracking-tight">{lease.contractName}</span>
                       <span className="text-[10px] text-slate-700 font-bold uppercase tracking-widest leading-none mt-1">Lessor Code: {lease.lessorCode}</span>
                    </div>
                 </div>

                 <div className="flex items-center gap-12">
                    <div className="flex flex-col items-end">
                       <span className="text-xs font-black text-slate-500 uppercase tracking-widest">{lease.endDate}</span>
                       <span className="text-[10px] text-slate-700 font-bold mt-1">Contract End Date</span>
                    </div>
                    <div className="flex flex-col items-end w-32">
                       <span className="text-sm font-mono font-black text-white tracking-tighter">₩{lease.monthlyPayment.toLocaleString()}</span>
                       <span className="text-[10px] text-slate-700 font-bold mt-1 uppercase tracking-widest">Monthly Rent</span>
                    </div>
                    <button className="p-3 text-slate-700 group-hover/item:text-white transition-colors hover:bg-white/5 rounded-xl"><ChevronRight size={18} /></button>
                 </div>
              </div>
            ))}
         </div>
      </div>
    </div>
  );
}

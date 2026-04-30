"use client";

import React, { useState, useEffect, useCallback } from 'react';
import { 
  Package, 
  Plus, 
  Search, 
  History,
  Building,
  FileText,
  RotateCw,
  RefreshCcw,
  Calculator
} from 'lucide-react';
import { assetService, FixedAsset } from '@/services/assetService';

/**
 * [고정자산 관리 화면]
 * 회사의 유무형 자산을 등록하고 감가상각비를 자동 계산 및 전표화합니다.
 * 리팩토링된 API와 연동하여 실데이터를 기반으로 동작합니다.
 */
export default function AssetsPage() {
  const [assets, setAssets] = useState<FixedAsset[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');

  const fetchAssets = useCallback(async () => {
    try {
      setError(null);
      const data = await assetService.getAssets();
      setAssets(data);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : '자산 목록을 불러오지 못했습니다.';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    let ignore = false;
    const trigger = async () => {
      if (!ignore) await fetchAssets();
    };
    setTimeout(() => trigger(), 0);
    return () => { ignore = true; };
  }, [fetchAssets]);

  const handleRunDepreciation = async () => {
    const today = new Date().toISOString().split('T')[0];
    if (confirm(`${today} 기준으로 감가상각을 실행하시겠습니까?`)) {
      try {
        await assetService.runDepreciation(today);
        alert('감가상각 처리가 완료되었습니다.');
        fetchAssets();
      } catch (err: unknown) {
        const message = err instanceof Error ? err.message : '알 수 없는 오류';
        alert('감가상각 처리 중 오류: ' + message);
      }
    }
  };

  const filteredAssets = assets.filter(asset => 
    asset.assetName.toLowerCase().includes(searchTerm.toLowerCase()) ||
    asset.assetCode.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const totalCost = assets.reduce((sum, a) => sum + a.acquisitionCost, 0);
  const totalBookValue = assets.reduce((sum, a) => sum + a.currentBookValue, 0);

  return (
    <div className="space-y-10">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
        <div className="space-y-2">
          <div className="flex items-center gap-3 text-blue-500 mb-2">
            <Package size={20} />
            <span className="text-xs font-black uppercase tracking-[0.3em]">Fixed Asset Management</span>
          </div>
          <h2 className="text-4xl font-black text-white tracking-tighter italic">
            고정자산 통합 관리
          </h2>
          <p className="text-slate-500 font-medium max-w-2xl">
            유무형 자산의 취득, 감가상각, 이동 및 처분 프로세스를 디지털화하고 재무제표와 실시간 연동합니다.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button 
            onClick={fetchAssets}
            className="px-4 py-3 bg-white/5 hover:bg-white/10 border border-white/10 rounded-2xl text-slate-400 text-sm font-black transition-all flex items-center gap-2">
            <RefreshCcw size={18} />
          </button>
          <button 
            onClick={handleRunDepreciation}
            className="px-6 py-3 bg-white/5 hover:bg-white/10 border border-white/10 rounded-2xl text-slate-400 text-sm font-black transition-all flex items-center gap-2">
            <RotateCw size={18} /> 감가상각 실행
          </button>
          <button className="px-6 py-3 bg-blue-600 hover:bg-blue-500 rounded-2xl text-white text-sm font-black transition-all shadow-lg shadow-blue-600/20 flex items-center gap-2 px-8">
            <Plus size={18} /> 자산 신규 취득
          </button>
        </div>
      </div>

      {error && (
        <div className="rounded-2xl border border-rose-500/30 bg-rose-500/10 px-5 py-3 text-sm font-bold text-rose-300">
          {error}
        </div>
      )}

      {/* Asset Quick Stats */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
           <div className="glass-panel p-8 rounded-[3rem] border border-white/10 bg-white/[0.01] hover:bg-white/[0.03] transition-all group cursor-pointer">
              <div className="w-12 h-12 rounded-2xl bg-blue-600/10 flex items-center justify-center text-blue-500 mb-6 group-hover:scale-110 transition-transform">
                 <Building size={24} />
              </div>
              <span className="text-[10px] font-black text-slate-600 uppercase tracking-widest">전체 취득 가액</span>
              <div className="text-2xl font-black text-white mt-1 italic tracking-tighter">₩{totalCost.toLocaleString()}</div>
              <div className="text-xs font-bold text-slate-700 mt-2">{assets.length} Items Listed</div>
           </div>
           
           <div className="glass-panel p-8 rounded-[3rem] border border-white/10 bg-white/[0.01] hover:bg-white/[0.03] transition-all group cursor-pointer">
              <div className="w-12 h-12 rounded-2xl bg-emerald-600/10 flex items-center justify-center text-emerald-500 mb-6 group-hover:scale-110 transition-transform">
                 <FileText size={24} />
              </div>
              <span className="text-[10px] font-black text-slate-600 uppercase tracking-widest">현재 장부 가액</span>
              <div className="text-2xl font-black text-emerald-400 mt-1 italic tracking-tighter">₩{totalBookValue.toLocaleString()}</div>
              <div className="text-xs font-bold text-slate-700 mt-2">Current Value</div>
           </div>

           <div className="glass-panel p-8 rounded-[3rem] border border-white/10 bg-white/[0.01] hover:bg-white/[0.03] transition-all group cursor-pointer">
              <div className="w-12 h-12 rounded-2xl bg-rose-600/10 flex items-center justify-center text-rose-500 mb-6 group-hover:scale-110 transition-transform">
                 <Calculator size={24} />
              </div>
              <span className="text-[10px] font-black text-slate-600 uppercase tracking-widest">누적 감가상각액</span>
              <div className="text-2xl font-black text-rose-400 mt-1 italic tracking-tighter">₩{(totalCost - totalBookValue).toLocaleString()}</div>
              <div className="text-xs font-bold text-slate-700 mt-2">Accumulated</div>
           </div>

           <div className="glass-panel p-8 rounded-[3rem] border border-white/10 bg-white/[0.01] hover:bg-white/[0.03] transition-all group cursor-pointer">
              <div className="w-12 h-12 rounded-2xl bg-amber-600/10 flex items-center justify-center text-amber-500 mb-6 group-hover:scale-110 transition-transform">
                 <History size={24} />
              </div>
              <span className="text-[10px] font-black text-slate-600 uppercase tracking-widest">최종 상각일</span>
              <div className="text-2xl font-black text-amber-400 mt-1 italic tracking-tighter">
                {assets.find(a => a.lastDepreciationDate)?.lastDepreciationDate || '-'}
              </div>
              <div className="text-xs font-bold text-slate-700 mt-2">Latest Record</div>
           </div>
      </div>

      {/* Asset Inventory Table */}
      <div className="glass-panel p-10 rounded-[3rem] border border-white/10 bg-white/[0.01]">
         <div className="flex items-center justify-between mb-10">
            <div className="flex flex-col gap-1">
               <h3 className="text-xl font-black text-white italic tracking-tight uppercase">Asset Inventory Registry</h3>
               <span className="text-[10px] text-slate-700 font-bold uppercase tracking-widest leading-none">Real-time assets in system</span>
            </div>
            <div className="relative group max-w-sm w-full">
               <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-700" size={18} />
               <input 
                type="text" 
                placeholder="Asset code, name..." 
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full bg-slate-950 border border-white/5 rounded-2xl py-3.5 pl-12 pr-6 text-sm text-white outline-none focus:border-blue-500/30 transition-all font-bold" 
               />
            </div>
         </div>

         <div className="overflow-x-auto">
            <table className="w-full text-left">
               <thead>
                  <tr className="border-b border-white/5 text-[10px] font-black text-slate-600 uppercase tracking-[0.2em]">
                     <th className="pb-6 px-4">Code / Asset Name</th>
                     <th className="pb-6 px-4">Acquisition Date</th>
                     <th className="pb-6 px-4">Cost (Book Value)</th>
                     <th className="pb-6 px-4">Status / Method</th>
                     <th className="pb-6 px-4 text-right">Action</th>
                  </tr>
               </thead>
               <tbody className="divide-y divide-white/[0.02]">
                  {loading ? (
                    <tr><td colSpan={5} className="py-20 text-center text-slate-500 italic font-bold">Data Loading...</td></tr>
                  ) : filteredAssets.length === 0 ? (
                    <tr><td colSpan={5} className="py-20 text-center text-slate-500 italic font-bold">No assets found.</td></tr>
                  ) : filteredAssets.map((asset) => (
                    <tr key={asset.id} className="group/row hover:bg-white/[0.02] transition-colors">
                       <td className="py-6 px-4">
                          <div className="flex flex-col">
                             <span className="text-sm font-black text-white">{asset.assetName}</span>
                             <span className="text-[10px] text-slate-700 font-mono tracking-tighter uppercase">{asset.assetCode}</span>
                          </div>
                       </td>
                       <td className="py-6 px-4">
                          <span className="text-xs font-bold text-slate-500">{asset.acquisitionDate}</span>
                       </td>
                       <td className="py-6 px-4">
                          <div className="flex flex-col">
                             <span className="text-sm font-black text-white">₩{asset.currentBookValue.toLocaleString()}</span>
                             <span className="text-[10px] text-slate-700 italic">Cost: ₩{asset.acquisitionCost.toLocaleString()}</span>
                          </div>
                       </td>
                       <td className="py-6 px-4">
                          <div className="flex items-center gap-3">
                             <div className={`w-1.5 h-1.5 rounded-full ${asset.status === 'ACTIVE' ? 'bg-blue-500' : 'bg-slate-700'}`} />
                             <div className="flex flex-col">
                                <span className="text-xs font-bold text-white uppercase">{asset.status}</span>
                                <span className="text-[10px] font-bold text-slate-600">{asset.depreciationMethod}</span>
                             </div>
                          </div>
                       </td>
                       <td className="py-6 px-4 text-right">
                          <button className="p-3 bg-white/5 hover:bg-white/10 rounded-xl text-slate-500 hover:text-white transition-all"><History size={16} /></button>
                       </td>
                    </tr>
                  ))}
               </tbody>
            </table>
         </div>
      </div>
    </div>
  );
}

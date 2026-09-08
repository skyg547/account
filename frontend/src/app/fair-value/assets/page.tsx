"use client";

import React, { useState, useMemo } from 'react';
import {
  Building2,
  Search,
  Plus,
  Download,
  TrendingUp,
  Boxes,
  FileSpreadsheet,
  CheckCircle2,
  XCircle,
  Eye,
  SlidersHorizontal
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import AmountDisplay from '@/components/ui/AmountDisplay';
import Tabs, { TabItem } from '@/components/ui/Tabs';
import EmptyState from '@/components/ui/EmptyState';
import { mockAssets, AssetDto } from '@/mocks/fair-value';

export default function FixedAssetsPage() {
  const [assets, setAssets] = useState<AssetDto[]>(mockAssets);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL');
  const [selectedStatus, setSelectedStatus] = useState<string>('ALL');
  const [selectedAsset, setSelectedAsset] = useState<AssetDto | null>(null);
  const [isRegisterOpen, setIsRegisterOpen] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  // New asset form state
  const [newAsset, setNewAsset] = useState({
    assetName: '',
    category: '유형자산' as AssetDto['category'],
    subCategory: '건물',
    acquisitionDate: new Date().toISOString().split('T')[0],
    acquisitionCost: 0,
    usefulLifeYears: 5,
    depreciationMethod: 'STRAIGHT_LINE' as AssetDto['depreciationMethod'],
    salvageValue: 0,
    department: '경영지원팀',
    location: '본사 5층',
  });

  const categories: TabItem[] = [
    { id: 'ALL', label: '전체 자산' },
    { id: '유형자산', label: '유형자산' },
    { id: '무형자산', label: '무형자산' },
    { id: '투자자산', label: '투자자산' },
  ];

  const filteredAssets = useMemo(() => {
    return assets.filter(asset => {
      const matchSearch = 
        asset.assetName.toLowerCase().includes(searchTerm.toLowerCase()) ||
        asset.assetCode.toLowerCase().includes(searchTerm.toLowerCase()) ||
        asset.department.toLowerCase().includes(searchTerm.toLowerCase());
      
      const matchCategory = selectedCategory === 'ALL' || asset.category === selectedCategory;
      const matchStatus = selectedStatus === 'ALL' || asset.status === selectedStatus;

      return matchSearch && matchCategory && matchStatus;
    });
  }, [assets, searchTerm, selectedCategory, selectedStatus]);

  // KPI Calculations
  const totalAcquisition = useMemo(() => assets.reduce((sum, a) => sum + a.acquisitionCost, 0), [assets]);
  const totalBookValue = useMemo(() => assets.reduce((sum, a) => sum + a.bookValue, 0), [assets]);
  const totalDepreciation = useMemo(() => assets.reduce((sum, a) => sum + a.accumulatedDepreciation, 0), [assets]);
  const activeCount = useMemo(() => assets.filter(a => a.status === 'ACTIVE').length, [assets]);

  const handleRegisterAsset = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newAsset.assetName || newAsset.acquisitionCost <= 0) return;

    const created: AssetDto = {
      id: `ast-${Date.now()}`,
      assetCode: `FA-2026-${String(assets.length + 1).padStart(3, '0')}`,
      assetName: newAsset.assetName,
      category: newAsset.category,
      subCategory: newAsset.subCategory,
      acquisitionDate: newAsset.acquisitionDate,
      acquisitionCost: Number(newAsset.acquisitionCost),
      accumulatedDepreciation: 0,
      bookValue: Number(newAsset.acquisitionCost),
      usefulLifeYears: Number(newAsset.usefulLifeYears),
      depreciationMethod: newAsset.depreciationMethod,
      salvageValue: Number(newAsset.salvageValue),
      department: newAsset.department,
      location: newAsset.location,
      status: 'ACTIVE',
      lastDepreciationDate: newAsset.acquisitionDate,
    };

    setAssets(prev => [created, ...prev]);
    setIsRegisterOpen(false);
    setToastMessage(`신규 고정자산 [${created.assetCode}] ${created.assetName}이(가) 등록되었습니다.`);
    setTimeout(() => setToastMessage(null), 4000);
    setNewAsset({
      assetName: '',
      category: '유형자산',
      subCategory: '건물',
      acquisitionDate: new Date().toISOString().split('T')[0],
      acquisitionCost: 0,
      usefulLifeYears: 5,
      depreciationMethod: 'STRAIGHT_LINE',
      salvageValue: 0,
      department: '경영지원팀',
      location: '본사 5층',
    });
  };

  const getStatusVariant = (status: AssetDto['status']) => {
    switch (status) {
      case 'ACTIVE': return 'success';
      case 'IMPAIRED': return 'warning';
      case 'DISPOSED': return 'error';
      case 'UNDER_MAINTENANCE': return 'info';
      default: return 'neutral';
    }
  };

  const getStatusLabel = (status: AssetDto['status']) => {
    switch (status) {
      case 'ACTIVE': return '정상 사용';
      case 'IMPAIRED': return '손상 차손';
      case 'DISPOSED': return '처분 완료';
      case 'UNDER_MAINTENANCE': return '수리/정비 중';
      default: return status;
    }
  };

  const getMethodLabel = (method: AssetDto['depreciationMethod']) => {
    switch (method) {
      case 'STRAIGHT_LINE': return '정액법';
      case 'DECLINING_BALANCE': return '정률법';
      case 'UNITS_OF_PRODUCTION': return '생산량비례법';
      default: return method;
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
        title="고정자산 대장"
        description="기업 소유 유형·무형 고정자산의 신규 취득, 장부가액, 감가상각 누계액 및 운영 현황을 통합 관리합니다."
        breadcrumbs={[
          { label: '공정가치 / 자산회계', href: '/fair-value/assets' },
          { label: '고정자산 대장' },
        ]}
        icon={Building2}
        actions={
          <div className="flex items-center gap-3">
            <button 
              onClick={() => alert('고정자산 대장 엑셀 내보내기가 완료되었습니다.')}
              className="px-4 py-2.5 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-slate-300 hover:text-white text-xs font-bold transition-all flex items-center gap-2"
            >
              <Download size={16} />
              <span>엑셀 다운로드</span>
            </button>
            <button
              onClick={() => setIsRegisterOpen(true)}
              className="px-5 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-xs font-black transition-all shadow-lg shadow-blue-600/20 flex items-center gap-2"
            >
              <Plus size={16} />
              <span>신규 자산 등록</span>
            </button>
          </div>
        }
      />

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2 hover:border-white/10 transition-all">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-black uppercase tracking-wider text-slate-400">총 취득 원가</span>
            <div className="w-9 h-9 rounded-xl bg-blue-500/10 border border-blue-500/20 flex items-center justify-center text-blue-400">
              <Building2 size={18} />
            </div>
          </div>
          <div className="text-2xl font-black text-white">
            <AmountDisplay amount={totalAcquisition} />
          </div>
          <p className="text-[11px] text-slate-500 font-medium">전체 보유 고정자산 원가</p>
        </div>

        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2 hover:border-white/10 transition-all">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-black uppercase tracking-wider text-slate-400">현재 총 장부가액</span>
            <div className="w-9 h-9 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
              <TrendingUp size={18} />
            </div>
          </div>
          <div className="text-2xl font-black text-emerald-400">
            <AmountDisplay amount={totalBookValue} />
          </div>
          <p className="text-[11px] text-slate-500 font-medium">상각 차감 후 미상각 잔액</p>
        </div>

        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2 hover:border-white/10 transition-all">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-black uppercase tracking-wider text-slate-400">누적 감가상각액</span>
            <div className="w-9 h-9 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center justify-center text-rose-400">
              <FileSpreadsheet size={18} />
            </div>
          </div>
          <div className="text-2xl font-black text-rose-400">
            <AmountDisplay amount={totalDepreciation} />
          </div>
          <p className="text-[11px] text-slate-500 font-medium">현재까지 손익 반영 상각비</p>
        </div>

        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2 hover:border-white/10 transition-all">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-black uppercase tracking-wider text-slate-400">운용 자산 수량</span>
            <div className="w-9 h-9 rounded-xl bg-purple-500/10 border border-purple-500/20 flex items-center justify-center text-purple-400">
              <Boxes size={18} />
            </div>
          </div>
          <div className="text-2xl font-black text-white font-mono">
            {activeCount} <span className="text-xs font-normal text-slate-500">/ 총 {assets.length}건</span>
          </div>
          <p className="text-[11px] text-slate-500 font-medium">정상 운용 중인 자산 수</p>
        </div>
      </div>

      {/* Filter Toolbar */}
      <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-4">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <Tabs 
            tabs={categories} 
            activeTab={selectedCategory} 
            onChange={setSelectedCategory} 
          />

          <div className="flex items-center gap-3">
            {/* Search Input */}
            <div className="relative min-w-[240px]">
              <Search size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500" />
              <input
                type="text"
                placeholder="자산코드, 자산명, 부서..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-white/5 border border-white/10 text-xs text-white placeholder-slate-500 outline-none focus:border-blue-500/50 transition-all"
              />
            </div>

            {/* Status Select */}
            <div className="relative">
              <select
                value={selectedStatus}
                onChange={(e) => setSelectedStatus(e.target.value)}
                className="px-4 py-2.5 rounded-xl bg-white/5 border border-white/10 text-xs text-slate-300 outline-none focus:border-blue-500/50 transition-all appearance-none cursor-pointer pr-8"
              >
                <option value="ALL" className="bg-slate-900 text-white">상태 전체</option>
                <option value="ACTIVE" className="bg-slate-900 text-white">정상 사용</option>
                <option value="IMPAIRED" className="bg-slate-900 text-white">손상 차손</option>
                <option value="DISPOSED" className="bg-slate-900 text-white">처분 완료</option>
                <option value="UNDER_MAINTENANCE" className="bg-slate-900 text-white">수리/정비 중</option>
              </select>
              <SlidersHorizontal size={14} className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 pointer-events-none" />
            </div>
          </div>
        </div>

        {/* Table */}
        <div className="overflow-x-auto">
          {filteredAssets.length === 0 ? (
            <EmptyState
              icon={Building2}
              title="검색 조건에 맞는 자산이 없습니다."
              description="검색어나 필터 조건을 변경하여 다시 확인해 주세요."
              action={
                <button
                  onClick={() => {
                    setSearchTerm('');
                    setSelectedCategory('ALL');
                    setSelectedStatus('ALL');
                  }}
                  className="px-4 py-2 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-xs text-white font-bold"
                >
                  필터 초기화
                </button>
              }
            />
          ) : (
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-white/10 text-[11px] font-black uppercase tracking-wider text-slate-400">
                  <th className="py-3 px-4">자산코드</th>
                  <th className="py-3 px-4">자산명 / 분류</th>
                  <th className="py-3 px-4">취득일 / 내용연수</th>
                  <th className="py-3 px-4 text-right">취득원가</th>
                  <th className="py-3 px-4 text-right">감가상각누계액</th>
                  <th className="py-3 px-4 text-right">장부가액</th>
                  <th className="py-3 px-4">상각방법</th>
                  <th className="py-3 px-4">상태</th>
                  <th className="py-3 px-4 text-center">상세</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5 text-xs font-medium">
                {filteredAssets.map((asset) => (
                  <tr 
                    key={asset.id}
                    className="hover:bg-white/5 transition-all group"
                  >
                    <td className="py-4 px-4 font-mono font-bold text-blue-400">
                      {asset.assetCode}
                    </td>
                    <td className="py-4 px-4">
                      <div>
                        <p className="font-bold text-white group-hover:text-blue-300 transition-colors">
                          {asset.assetName}
                        </p>
                        <p className="text-[11px] text-slate-500">
                          {asset.category} &gt; {asset.subCategory} ({asset.department})
                        </p>
                      </div>
                    </td>
                    <td className="py-4 px-4">
                      <p className="font-mono text-slate-300">{asset.acquisitionDate}</p>
                      <p className="text-[11px] text-slate-500">{asset.usefulLifeYears}년</p>
                    </td>
                    <td className="py-4 px-4 text-right">
                      <AmountDisplay amount={asset.acquisitionCost} />
                    </td>
                    <td className="py-4 px-4 text-right">
                      <AmountDisplay amount={asset.accumulatedDepreciation} className="text-rose-400" />
                    </td>
                    <td className="py-4 px-4 text-right">
                      <AmountDisplay amount={asset.bookValue} className="text-emerald-400 font-bold" />
                    </td>
                    <td className="py-4 px-4 text-slate-400">
                      {getMethodLabel(asset.depreciationMethod)}
                    </td>
                    <td className="py-4 px-4">
                      <StatusBadge
                        status={getStatusLabel(asset.status)}
                        variant={getStatusVariant(asset.status)}
                      />
                    </td>
                    <td className="py-4 px-4 text-center">
                      <button
                        onClick={() => setSelectedAsset(asset)}
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

      {/* Asset Detail Modal */}
      {selectedAsset && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4 animate-in fade-in">
          <div className="bg-slate-900 border border-white/10 rounded-3xl max-w-xl w-full p-6 space-y-6 shadow-2xl">
            <div className="flex items-center justify-between border-b border-white/10 pb-4">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-2xl bg-blue-600/10 border border-blue-500/20 flex items-center justify-center text-blue-400">
                  <Building2 size={20} />
                </div>
                <div>
                  <h3 className="text-lg font-black text-white">{selectedAsset.assetName}</h3>
                  <p className="text-xs font-mono text-blue-400">{selectedAsset.assetCode}</p>
                </div>
              </div>
              <button 
                onClick={() => setSelectedAsset(null)}
                className="p-2 text-slate-400 hover:text-white rounded-xl hover:bg-white/10"
              >
                <XCircle size={20} />
              </button>
            </div>

            <div className="grid grid-cols-2 gap-4 text-xs">
              <div className="p-3.5 rounded-2xl bg-white/5 border border-white/5 space-y-1">
                <span className="text-[10px] text-slate-500 font-bold uppercase">자산 구분</span>
                <p className="font-bold text-white">{selectedAsset.category} ({selectedAsset.subCategory})</p>
              </div>
              <div className="p-3.5 rounded-2xl bg-white/5 border border-white/5 space-y-1">
                <span className="text-[10px] text-slate-500 font-bold uppercase">취득 일자</span>
                <p className="font-bold text-white font-mono">{selectedAsset.acquisitionDate}</p>
              </div>
              <div className="p-3.5 rounded-2xl bg-white/5 border border-white/5 space-y-1">
                <span className="text-[10px] text-slate-500 font-bold uppercase">취득 원가</span>
                <p className="font-bold text-white"><AmountDisplay amount={selectedAsset.acquisitionCost} /></p>
              </div>
              <div className="p-3.5 rounded-2xl bg-white/5 border border-white/5 space-y-1">
                <span className="text-[10px] text-slate-500 font-bold uppercase">현재 장부가액</span>
                <p className="font-bold text-emerald-400"><AmountDisplay amount={selectedAsset.bookValue} /></p>
              </div>
              <div className="p-3.5 rounded-2xl bg-white/5 border border-white/5 space-y-1">
                <span className="text-[10px] text-slate-500 font-bold uppercase">내용연수 / 상각법</span>
                <p className="font-bold text-white">{selectedAsset.usefulLifeYears}년 / {getMethodLabel(selectedAsset.depreciationMethod)}</p>
              </div>
              <div className="p-3.5 rounded-2xl bg-white/5 border border-white/5 space-y-1">
                <span className="text-[10px] text-slate-500 font-bold uppercase">잔존 가치</span>
                <p className="font-bold text-white"><AmountDisplay amount={selectedAsset.salvageValue} /></p>
              </div>
              <div className="p-3.5 rounded-2xl bg-white/5 border border-white/5 space-y-1">
                <span className="text-[10px] text-slate-500 font-bold uppercase">관리 부서</span>
                <p className="font-bold text-white">{selectedAsset.department}</p>
              </div>
              <div className="p-3.5 rounded-2xl bg-white/5 border border-white/5 space-y-1">
                <span className="text-[10px] text-slate-500 font-bold uppercase">설치 장소</span>
                <p className="font-bold text-white">{selectedAsset.location}</p>
              </div>
            </div>

            {selectedAsset.notes && (
              <div className="p-3.5 rounded-2xl bg-white/5 border border-white/5 text-xs">
                <span className="text-[10px] text-slate-500 font-bold uppercase block mb-1">비고 사항</span>
                <p className="text-slate-300">{selectedAsset.notes}</p>
              </div>
            )}

            <div className="flex justify-end pt-2">
              <button
                onClick={() => setSelectedAsset(null)}
                className="px-5 py-2.5 rounded-xl bg-white/10 hover:bg-white/20 text-white font-bold text-xs transition-all"
              >
                닫기
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Register Asset Modal */}
      {isRegisterOpen && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4 animate-in fade-in">
          <div className="bg-slate-900 border border-white/10 rounded-3xl max-w-xl w-full p-6 space-y-6 shadow-2xl">
            <div className="flex items-center justify-between border-b border-white/10 pb-4">
              <h3 className="text-lg font-black text-white italic">신규 고정자산 취득 등록</h3>
              <button 
                onClick={() => setIsRegisterOpen(false)}
                className="p-2 text-slate-400 hover:text-white rounded-xl hover:bg-white/10"
              >
                <XCircle size={20} />
              </button>
            </div>

            <form onSubmit={handleRegisterAsset} className="space-y-4 text-xs">
              <div className="grid grid-cols-2 gap-4">
                <div className="col-span-2 space-y-1">
                  <label className="font-bold text-slate-400">자산명 *</label>
                  <input
                    type="text"
                    required
                    placeholder="예: 강남 사옥 전산 서버 랙"
                    value={newAsset.assetName}
                    onChange={e => setNewAsset({ ...newAsset, assetName: e.target.value })}
                    className="w-full p-3 rounded-xl bg-white/5 border border-white/10 text-white outline-none focus:border-blue-500"
                  />
                </div>

                <div className="space-y-1">
                  <label className="font-bold text-slate-400">자산 대분류</label>
                  <select
                    value={newAsset.category}
                    onChange={e => setNewAsset({ ...newAsset, category: e.target.value as AssetDto['category'] })}
                    className="w-full p-3 rounded-xl bg-slate-900 border border-white/10 text-white outline-none focus:border-blue-500"
                  >
                    <option value="유형자산">유형자산</option>
                    <option value="무형자산">무형자산</option>
                    <option value="투자자산">투자자산</option>
                  </select>
                </div>

                <div className="space-y-1">
                  <label className="font-bold text-slate-400">소분류 (계정이름)</label>
                  <input
                    type="text"
                    value={newAsset.subCategory}
                    onChange={e => setNewAsset({ ...newAsset, subCategory: e.target.value })}
                    className="w-full p-3 rounded-xl bg-white/5 border border-white/10 text-white outline-none focus:border-blue-500"
                  />
                </div>

                <div className="space-y-1">
                  <label className="font-bold text-slate-400">취득 일자</label>
                  <input
                    type="date"
                    value={newAsset.acquisitionDate}
                    onChange={e => setNewAsset({ ...newAsset, acquisitionDate: e.target.value })}
                    className="w-full p-3 rounded-xl bg-white/5 border border-white/10 text-white outline-none focus:border-blue-500"
                  />
                </div>

                <div className="space-y-1">
                  <label className="font-bold text-slate-400">취득 가액 (원) *</label>
                  <input
                    type="number"
                    min={1}
                    required
                    value={newAsset.acquisitionCost || ''}
                    onChange={e => setNewAsset({ ...newAsset, acquisitionCost: Number(e.target.value) })}
                    className="w-full p-3 rounded-xl bg-white/5 border border-white/10 text-white outline-none focus:border-blue-500"
                  />
                </div>

                <div className="space-y-1">
                  <label className="font-bold text-slate-400">내용연수 (년)</label>
                  <input
                    type="number"
                    min={1}
                    value={newAsset.usefulLifeYears}
                    onChange={e => setNewAsset({ ...newAsset, usefulLifeYears: Number(e.target.value) })}
                    className="w-full p-3 rounded-xl bg-white/5 border border-white/10 text-white outline-none focus:border-blue-500"
                  />
                </div>

                <div className="space-y-1">
                  <label className="font-bold text-slate-400">상각 방법</label>
                  <select
                    value={newAsset.depreciationMethod}
                    onChange={e => setNewAsset({ ...newAsset, depreciationMethod: e.target.value as AssetDto['depreciationMethod'] })}
                    className="w-full p-3 rounded-xl bg-slate-900 border border-white/10 text-white outline-none focus:border-blue-500"
                  >
                    <option value="STRAIGHT_LINE">정액법 (Straight Line)</option>
                    <option value="DECLINING_BALANCE">정률법 (Declining Balance)</option>
                    <option value="UNITS_OF_PRODUCTION">생산량비례법</option>
                  </select>
                </div>

                <div className="space-y-1">
                  <label className="font-bold text-slate-400">관리 부서</label>
                  <input
                    type="text"
                    value={newAsset.department}
                    onChange={e => setNewAsset({ ...newAsset, department: e.target.value })}
                    className="w-full p-3 rounded-xl bg-white/5 border border-white/10 text-white outline-none focus:border-blue-500"
                  />
                </div>

                <div className="space-y-1">
                  <label className="font-bold text-slate-400">설치 장소</label>
                  <input
                    type="text"
                    value={newAsset.location}
                    onChange={e => setNewAsset({ ...newAsset, location: e.target.value })}
                    className="w-full p-3 rounded-xl bg-white/5 border border-white/10 text-white outline-none focus:border-blue-500"
                  />
                </div>
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-white/10">
                <button
                  type="button"
                  onClick={() => setIsRegisterOpen(false)}
                  className="px-5 py-2.5 rounded-xl bg-white/5 hover:bg-white/10 text-slate-300 font-bold"
                >
                  취소
                </button>
                <button
                  type="submit"
                  className="px-6 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-bold shadow-lg shadow-blue-600/20"
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

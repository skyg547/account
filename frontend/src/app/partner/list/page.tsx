"use client";

import React, { useState } from 'react';
import { 
  Building, 
  Search, 
  Plus, 
  Building2, 
  User, 
  MoreVertical,
  Download
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import { mockPartners, PartnerDto } from '@/mocks/master';

export default function PartnerListPage() {
  const [searchTerm, setSearchTerm] = useState('');
  const [typeFilter, setTypeFilter] = useState<'ALL' | 'CORPORATE' | 'INDIVIDUAL'>('ALL');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');

  const filteredPartners = mockPartners.filter((p) => {
    const matchSearch =
      p.name.includes(searchTerm) ||
      p.businessNumber.includes(searchTerm) ||
      p.ceoName.includes(searchTerm) ||
      p.id.includes(searchTerm);

    const matchType = typeFilter === 'ALL' || p.type === typeFilter;
    const matchStatus = statusFilter === 'ALL' || p.status === statusFilter;

    return matchSearch && matchType && matchStatus;
  });

  const getStatusVariant = (status: PartnerDto['status']) => {
    switch (status) {
      case 'ACTIVE':
        return 'success';
      case 'PENDING':
        return 'warning';
      case 'BLOCKED':
        return 'error';
      default:
        return 'neutral';
    }
  };

  return (
    <div className="space-y-8">
      <PageHeader
        title="거래처 조회/등록"
        description="개인/법인 거래처 마스터"
        breadcrumbs={[
          { label: '기준정보 마스터' },
          { label: '거래처 관리' },
          { label: '거래처 조회/등록' },
        ]}
        icon={Building}
        actions={
          <div className="flex items-center gap-3">
            <button className="px-4 py-2.5 bg-white/5 hover:bg-white/10 border border-white/10 rounded-2xl text-slate-300 font-bold text-xs transition-all flex items-center gap-2">
              <Download size={16} /> 엑셀 내보내기
            </button>
            <button className="px-6 py-3 bg-blue-600 hover:bg-blue-500 rounded-2xl text-white text-sm font-black transition-all shadow-lg shadow-blue-600/20 flex items-center gap-2">
              <Plus size={18} /> 신규 거래처 등록
            </button>
          </div>
        }
      />

      {/* Stats Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="p-6 rounded-[2.5rem] bg-white/[0.02] border border-white/5 relative overflow-hidden group hover:border-white/10 transition-all">
          <p className="text-xs font-black text-slate-500 uppercase tracking-widest mb-3">
            전체 등록 거래처
          </p>
          <div className="flex items-end justify-between">
            <span className="text-3xl font-black text-white italic tracking-tighter">
              {mockPartners.length}건
            </span>
            <span className="text-xs font-bold text-blue-400 bg-blue-500/10 px-2.5 py-1 rounded-full border border-blue-500/20">
              정상 등록
            </span>
          </div>
        </div>

        <div className="p-6 rounded-[2.5rem] bg-white/[0.02] border border-white/5 relative overflow-hidden group hover:border-white/10 transition-all">
          <p className="text-xs font-black text-slate-500 uppercase tracking-widest mb-3">
            법인 거래처
          </p>
          <div className="flex items-end justify-between">
            <span className="text-3xl font-black text-white italic tracking-tighter">
              {mockPartners.filter((p) => p.type === 'CORPORATE').length}건
            </span>
            <span className="text-xs font-bold text-emerald-400 bg-emerald-500/10 px-2.5 py-1 rounded-full border border-emerald-500/20">
              Corporate
            </span>
          </div>
        </div>

        <div className="p-6 rounded-[2.5rem] bg-white/[0.02] border border-white/5 relative overflow-hidden group hover:border-white/10 transition-all">
          <p className="text-xs font-black text-slate-500 uppercase tracking-widest mb-3">
            개인 거래처
          </p>
          <div className="flex items-end justify-between">
            <span className="text-3xl font-black text-white italic tracking-tighter">
              {mockPartners.filter((p) => p.type === 'INDIVIDUAL').length}건
            </span>
            <span className="text-xs font-bold text-purple-400 bg-purple-500/10 px-2.5 py-1 rounded-full border border-purple-500/20">
              Individual
            </span>
          </div>
        </div>

        <div className="p-6 rounded-[2.5rem] bg-white/[0.02] border border-white/5 relative overflow-hidden group hover:border-white/10 transition-all">
          <p className="text-xs font-black text-slate-500 uppercase tracking-widest mb-3">
            승인 대기 / 차단건
          </p>
          <div className="flex items-end justify-between">
            <span className="text-3xl font-black text-white italic tracking-tighter">
              {mockPartners.filter((p) => p.status !== 'ACTIVE').length}건
            </span>
            <span className="text-xs font-bold text-amber-400 bg-amber-500/10 px-2.5 py-1 rounded-full border border-amber-500/20">
              Action Required
            </span>
          </div>
        </div>
      </div>

      {/* Main Table Container */}
      <div className="glass-panel p-8 rounded-[3rem] border border-white/10 relative overflow-hidden space-y-6">
        {/* Filters Bar */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="relative max-w-md w-full">
            <Search
              size={18}
              className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500"
            />
            <input
              type="text"
              placeholder="거래처명, 사업자/주민번호, 대표자명 검색..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full bg-slate-950/80 border border-white/10 focus:border-blue-500/50 rounded-2xl py-3 pl-12 pr-4 text-xs font-bold text-white outline-none transition-all placeholder:text-slate-600"
            />
          </div>

          <div className="flex items-center gap-3">
            {/* Type Filter Dropdown */}
            <select
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value as 'ALL' | 'CORPORATE' | 'INDIVIDUAL')}
              className="bg-slate-950 border border-white/10 rounded-xl py-2.5 px-3 text-xs font-bold text-slate-200 outline-none cursor-pointer"
            >
              <option value="ALL">전체 구분 (법인/개인)</option>
              <option value="CORPORATE">법인 거래처 (CORPORATE)</option>
              <option value="INDIVIDUAL">개인 거래처 (INDIVIDUAL)</option>
            </select>

            {/* Status Filter Dropdown */}
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="bg-slate-950 border border-white/10 rounded-xl py-2.5 px-3 text-xs font-bold text-slate-200 outline-none cursor-pointer"
            >
              <option value="ALL">전체 상태</option>
              <option value="ACTIVE">ACTIVE (사용 중)</option>
              <option value="PENDING">PENDING (승인 대기)</option>
              <option value="BLOCKED">BLOCKED (거래 차단)</option>
            </select>
          </div>
        </div>

        {/* Partners Table */}
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="border-b border-white/5">
                <th className="pb-4 px-4 text-[10px] font-black text-slate-500 uppercase tracking-widest">
                  거래처 ID / 구분
                </th>
                <th className="pb-4 px-4 text-[10px] font-black text-slate-500 uppercase tracking-widest">
                  거래처명
                </th>
                <th className="pb-4 px-4 text-[10px] font-black text-slate-500 uppercase tracking-widest">
                  사업자/주민등록번호
                </th>
                <th className="pb-4 px-4 text-[10px] font-black text-slate-500 uppercase tracking-widest">
                  대표자명
                </th>
                <th className="pb-4 px-4 text-[10px] font-black text-slate-500 uppercase tracking-widest">
                  상태
                </th>
                <th className="pb-4 px-4 text-[10px] font-black text-slate-500 uppercase tracking-widest">
                  등록일자
                </th>
                <th className="pb-4 px-4 text-right text-[10px] font-black text-slate-500 uppercase tracking-widest">
                  관리
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/[0.02]">
              {filteredPartners.map((partner) => (
                <tr
                  key={partner.id}
                  className="group hover:bg-white/[0.02] transition-colors"
                >
                  <td className="py-5 px-4">
                    <div className="flex items-center gap-3">
                      <div className="w-9 h-9 rounded-xl bg-white/5 border border-white/5 flex items-center justify-center text-slate-400 group-hover:scale-105 transition-transform">
                        {partner.type === 'CORPORATE' ? (
                          <Building2 size={18} className="text-blue-400" />
                        ) : (
                          <User size={18} className="text-purple-400" />
                        )}
                      </div>
                      <div>
                        <span className="font-mono text-xs font-black text-blue-400 block">
                          {partner.id}
                        </span>
                        <span className="text-[10px] font-bold text-slate-500">
                          {partner.type === 'CORPORATE' ? '법인사업자' : '개인'}
                        </span>
                      </div>
                    </div>
                  </td>

                  <td className="py-5 px-4">
                    <span className="text-sm font-black text-white leading-tight block">
                      {partner.name}
                    </span>
                  </td>

                  <td className="py-5 px-4">
                    <span className="font-mono text-xs font-bold text-slate-300">
                      {partner.businessNumber}
                    </span>
                  </td>

                  <td className="py-5 px-4">
                    <span className="text-xs font-bold text-slate-300">
                      {partner.ceoName}
                    </span>
                  </td>

                  <td className="py-5 px-4">
                    <StatusBadge
                      status={partner.status}
                      variant={getStatusVariant(partner.status)}
                    />
                  </td>

                  <td className="py-5 px-4">
                    <span className="font-mono text-xs font-bold text-slate-500">
                      {partner.registeredAt}
                    </span>
                  </td>

                  <td className="py-5 px-4 text-right">
                    <div className="flex items-center justify-end gap-2">
                      <button className="px-3 py-1.5 rounded-xl bg-white/5 hover:bg-white/10 text-xs font-bold text-slate-300 transition-all">
                        상세보기
                      </button>
                      <button className="p-2 rounded-xl bg-white/5 hover:bg-white/10 text-slate-400 hover:text-white transition-all">
                        <MoreVertical size={16} />
                      </button>
                    </div>
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

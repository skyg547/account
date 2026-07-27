"use client";

import React, { useState } from 'react';
import { 
  Package, 
  Search, 
  Plus, 
  ArrowUpDown, 
  MoreVertical,
  Edit2
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import Tabs, { TabItem } from '@/components/ui/Tabs';

interface ProductMapping {
  id: string;
  code: string;
  name: string;
  category: 'LOAN' | 'DEPOSIT' | 'FX' | 'SECURITIES';
  categoryLabel: string;
  accountCode: string;
  accountName: string;
  managingDept: string;
  status: 'ACTIVE' | 'PENDING' | 'DRAFT';
  updatedAt: string;
}

const mockProducts: ProductMapping[] = [
  {
    id: 'PRD-001',
    code: 'LOAN-ENT-101',
    name: '기업 일반운전자금 대출',
    category: 'LOAN',
    categoryLabel: '여신(대출)',
    accountCode: '1120100',
    accountName: '기업자금대출금',
    managingDept: '기업금융부',
    status: 'ACTIVE',
    updatedAt: '2026-07-20',
  },
  {
    id: 'PRD-002',
    code: 'DEP-TIME-201',
    name: '복리 정기예금 (3년)',
    category: 'DEPOSIT',
    categoryLabel: '수신(예금)',
    accountCode: '2110200',
    accountName: '정기예금부채',
    managingDept: '수신기획부',
    status: 'ACTIVE',
    updatedAt: '2026-07-18',
  },
  {
    id: 'PRD-003',
    code: 'FX-CURR-305',
    name: '외화 통화스왑 파생상품',
    category: 'FX',
    categoryLabel: '외환/파생',
    accountCode: '1140300',
    accountName: '파생상품자산',
    managingDept: '외환파생팀',
    status: 'PENDING',
    updatedAt: '2026-07-25',
  },
  {
    id: 'PRD-004',
    code: 'SEC-GOV-401',
    name: '국고채 5년물 매도가능증권',
    category: 'SECURITIES',
    categoryLabel: '유가증권',
    accountCode: '1130100',
    accountName: '기타포괄손익-공정가치측정지분증권',
    managingDept: '자금운용부',
    status: 'ACTIVE',
    updatedAt: '2026-06-30',
  },
  {
    id: 'PRD-005',
    code: 'LOAN-MORT-102',
    name: '주택담보대출 (모기지론)',
    category: 'LOAN',
    categoryLabel: '여신(대출)',
    accountCode: '1120200',
    accountName: '가계가계자금대출금',
    managingDept: '리테일금융부',
    status: 'ACTIVE',
    updatedAt: '2026-07-15',
  },
  {
    id: 'PRD-006',
    code: 'DEP-MMDA-202',
    name: 'MMDA 수시입출금식 예금',
    category: 'DEPOSIT',
    categoryLabel: '수신(예금)',
    accountCode: '1110200',
    accountName: '보통예금',
    managingDept: '수신기획부',
    status: 'DRAFT',
    updatedAt: '2026-07-27',
  },
];

export default function ProductCodePage() {
  const [searchTerm, setSearchTerm] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('ALL');

  const tabItems: TabItem[] = [
    { id: 'ALL', label: '전체 상품' },
    { id: 'LOAN', label: '여신 (대출)' },
    { id: 'DEPOSIT', label: '수신 (예금)' },
    { id: 'FX', label: '외환/파생' },
    { id: 'SECURITIES', label: '유가증권' },
  ];

  const filteredProducts = mockProducts.filter((prd) => {
    const matchSearch =
      prd.name.includes(searchTerm) ||
      prd.code.includes(searchTerm) ||
      prd.accountName.includes(searchTerm) ||
      prd.accountCode.includes(searchTerm);

    const matchCategory = categoryFilter === 'ALL' || prd.category === categoryFilter;

    return matchSearch && matchCategory;
  });

  return (
    <div className="space-y-8">
      <PageHeader
        title="상품 코드 관리"
        description="여신/수신 등 금융상품 코드 맵핑"
        breadcrumbs={[
          { label: '기준정보 마스터' },
          { label: '계정과목 관리' },
          { label: '상품 코드 관리' },
        ]}
        icon={Package}
        actions={
          <button className="px-6 py-3 bg-blue-600 hover:bg-blue-500 rounded-2xl text-white text-sm font-black transition-all shadow-lg shadow-blue-600/20 flex items-center gap-2">
            <Plus size={18} /> 신규 상품 매핑 등록
          </button>
        }
      />

      {/* Summary KPI Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="p-6 rounded-[2rem] bg-white/[0.02] border border-white/5 relative overflow-hidden">
          <p className="text-xs font-black text-slate-500 uppercase tracking-widest mb-2">
            전체 상품 코드
          </p>
          <div className="flex items-end justify-between">
            <span className="text-3xl font-black text-white italic tracking-tighter">
              {mockProducts.length}건
            </span>
            <span className="text-xs font-bold text-blue-400 bg-blue-500/10 px-2.5 py-1 rounded-full border border-blue-500/20">
              정상 등록
            </span>
          </div>
        </div>

        <div className="p-6 rounded-[2rem] bg-white/[0.02] border border-white/5 relative overflow-hidden">
          <p className="text-xs font-black text-slate-500 uppercase tracking-widest mb-2">
            여신(대출) 매핑
          </p>
          <div className="flex items-end justify-between">
            <span className="text-3xl font-black text-white italic tracking-tighter">
              {mockProducts.filter((p) => p.category === 'LOAN').length}건
            </span>
            <span className="text-xs font-bold text-emerald-400 bg-emerald-500/10 px-2.5 py-1 rounded-full border border-emerald-500/20">
              100% 매핑
            </span>
          </div>
        </div>

        <div className="p-6 rounded-[2rem] bg-white/[0.02] border border-white/5 relative overflow-hidden">
          <p className="text-xs font-black text-slate-500 uppercase tracking-widest mb-2">
            수신(예금) 매핑
          </p>
          <div className="flex items-end justify-between">
            <span className="text-3xl font-black text-white italic tracking-tighter">
              {mockProducts.filter((p) => p.category === 'DEPOSIT').length}건
            </span>
            <span className="text-xs font-bold text-amber-400 bg-amber-500/10 px-2.5 py-1 rounded-full border border-amber-500/20">
              검증 완료
            </span>
          </div>
        </div>

        <div className="p-6 rounded-[2rem] bg-white/[0.02] border border-white/5 relative overflow-hidden">
          <p className="text-xs font-black text-slate-500 uppercase tracking-widest mb-2">
            검토 및 임시저장
          </p>
          <div className="flex items-end justify-between">
            <span className="text-3xl font-black text-white italic tracking-tighter">
              {mockProducts.filter((p) => p.status !== 'ACTIVE').length}건
            </span>
            <span className="text-xs font-bold text-purple-400 bg-purple-500/10 px-2.5 py-1 rounded-full border border-purple-500/20">
              승인 진행 중
            </span>
          </div>
        </div>
      </div>

      {/* Main Table Panel */}
      <div className="glass-panel p-8 rounded-[3rem] border border-white/10 relative overflow-hidden space-y-6">
        {/* Category Tabs & Search Bar */}
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
          <Tabs
            tabs={tabItems}
            activeTab={categoryFilter}
            onChange={setCategoryFilter}
          />

          <div className="relative max-w-md w-full">
            <Search
              size={18}
              className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500"
            />
            <input
              type="text"
              placeholder="상품명, 상품코드, 매핑계정 검색..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full bg-slate-950/80 border border-white/10 focus:border-blue-500/50 rounded-2xl py-3 pl-12 pr-4 text-xs font-bold text-white outline-none transition-all placeholder:text-slate-600"
            />
          </div>
        </div>

        {/* Product Mapping Table */}
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="border-b border-white/5">
                <th className="pb-4 px-4 text-[10px] font-black text-slate-500 uppercase tracking-widest">
                  상품 코드 / 상품명
                </th>
                <th className="pb-4 px-4 text-[10px] font-black text-slate-500 uppercase tracking-widest">
                  상품 분류
                </th>
                <th className="pb-4 px-4 text-[10px] font-black text-slate-500 uppercase tracking-widest">
                  매핑 계정과목 (Account)
                </th>
                <th className="pb-4 px-4 text-[10px] font-black text-slate-500 uppercase tracking-widest">
                  담당 부서
                </th>
                <th className="pb-4 px-4 text-[10px] font-black text-slate-500 uppercase tracking-widest">
                  상태
                </th>
                <th className="pb-4 px-4 text-[10px] font-black text-slate-500 uppercase tracking-widest">
                  최근 수정일
                </th>
                <th className="pb-4 px-4 text-right text-[10px] font-black text-slate-500 uppercase tracking-widest">
                  관리
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/[0.02]">
              {filteredProducts.map((prd) => (
                <tr
                  key={prd.id}
                  className="group hover:bg-white/[0.02] transition-colors"
                >
                  <td className="py-5 px-4">
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-xs font-black text-blue-400">
                          {prd.code}
                        </span>
                      </div>
                      <div className="text-sm font-bold text-white leading-tight">
                        {prd.name}
                      </div>
                    </div>
                  </td>

                  <td className="py-5 px-4">
                    <span className="text-xs font-bold text-slate-300 bg-white/5 px-3 py-1.5 rounded-xl border border-white/5">
                      {prd.categoryLabel}
                    </span>
                  </td>

                  <td className="py-5 px-4">
                    <div className="flex items-center gap-2">
                      <div className="w-8 h-8 rounded-lg bg-blue-600/10 border border-blue-500/20 flex items-center justify-center text-blue-400 shrink-0">
                        <ArrowUpDown size={14} />
                      </div>
                      <div>
                        <div className="font-mono text-xs font-bold text-slate-300">
                          {prd.accountCode}
                        </div>
                        <div className="text-xs font-medium text-slate-400">
                          {prd.accountName}
                        </div>
                      </div>
                    </div>
                  </td>

                  <td className="py-5 px-4">
                    <span className="text-xs font-bold text-slate-400">
                      {prd.managingDept}
                    </span>
                  </td>

                  <td className="py-5 px-4">
                    <StatusBadge
                      status={prd.status}
                      variant={
                        prd.status === 'ACTIVE'
                          ? 'success'
                          : prd.status === 'PENDING'
                          ? 'warning'
                          : 'neutral'
                      }
                    />
                  </td>

                  <td className="py-5 px-4">
                    <span className="font-mono text-xs font-bold text-slate-500">
                      {prd.updatedAt}
                    </span>
                  </td>

                  <td className="py-5 px-4 text-right">
                    <div className="flex items-center justify-end gap-2">
                      <button
                        title="매핑 수정"
                        className="p-2 rounded-xl bg-white/5 hover:bg-white/10 text-slate-400 hover:text-white transition-all"
                      >
                        <Edit2 size={14} />
                      </button>
                      <button
                        title="더보기"
                        className="p-2 rounded-xl bg-white/5 hover:bg-white/10 text-slate-400 hover:text-white transition-all"
                      >
                        <MoreVertical size={14} />
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

'use client';

import React, { useState, useSyncExternalStore } from 'react';
import {
  Layers,
  Search,
  Filter,
  ShieldAlert,
  ShieldCheck,
  AlertTriangle,
  FileSpreadsheet,
  ArrowUpDown,
  Eye,
  Building2,
  UserCheck,
  Percent,
  TrendingUp,
  X
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import AmountDisplay from '@/components/ui/AmountDisplay';
import Tabs, { TabItem } from '@/components/ui/Tabs';
import { mockExposures, EclExposureDto } from '@/mocks/ecl';

const emptySubscribe = () => () => {};

export default function EclExposuresPage() {
  const mounted = useSyncExternalStore(emptySubscribe, () => true, () => false);
  const [exposures, setExposures] = useState<EclExposureDto[]>(mockExposures);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedPortfolio, setSelectedPortfolio] = useState('ALL');
  const [selectedStage, setSelectedStage] = useState('ALL');
  const [selectedExposure, setSelectedExposure] = useState<EclExposureDto | null>(null);

  // Summary Metrics
  const totalEad = exposures.reduce((sum, item) => sum + item.exposureAmount, 0);
  const totalEcl = exposures.reduce((sum, item) => sum + item.eclAmount, 0);

  const stage1Exposures = exposures.filter(item => item.stage === 'Stage 1');
  const stage2Exposures = exposures.filter(item => item.stage === 'Stage 2');
  const stage3Exposures = exposures.filter(item => item.stage === 'Stage 3');

  const stage1Ead = stage1Exposures.reduce((sum, item) => sum + item.exposureAmount, 0);
  const stage2Ead = stage2Exposures.reduce((sum, item) => sum + item.exposureAmount, 0);
  const stage3Ead = stage3Exposures.reduce((sum, item) => sum + item.exposureAmount, 0);

  const filterTabs: TabItem[] = [
    { id: 'ALL', label: '전체 익스포저' },
    { id: 'Stage 1', label: 'Stage 1 (정상)' },
    { id: 'Stage 2', label: 'Stage 2 (유의적증가)' },
    { id: 'Stage 3', label: 'Stage 3 (손상)' },
  ];

  const filteredExposures = exposures.filter(item => {
    const matchesStage = selectedStage === 'ALL' || item.stage === selectedStage;
    const matchesPortfolio = selectedPortfolio === 'ALL' || item.portfolio === selectedPortfolio;
    const matchesSearch =
      item.contractNo.toLowerCase().includes(searchQuery.toLowerCase()) ||
      item.customerName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      item.productType.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesStage && matchesPortfolio && matchesSearch;
  });

  const getStageBadgeVariant = (stage: EclExposureDto['stage']) => {
    switch (stage) {
      case 'Stage 1': return 'success';
      case 'Stage 2': return 'warning';
      case 'Stage 3': return 'error';
      default: return 'neutral';
    }
  };

  const getCreditGradeBadge = (grade: string) => {
    if (['AAA', 'AA', 'A', 'A+'].includes(grade)) return 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20';
    if (['BBB', 'BB', 'BB-'].includes(grade)) return 'bg-amber-500/10 text-amber-400 border-amber-500/20';
    return 'bg-rose-500/10 text-rose-400 border-rose-500/20';
  };

  if (!mounted) {
    return <div className="p-8 text-slate-400">Loading 여신 익스포저 Page...</div>;
  }

  return (
    <div className="space-y-8 pb-16">
      {/* Page Header */}
      <PageHeader
        title="여신 익스포저"
        description="IFRS9 기준 여신 포트폴리오별 익스포저(EAD) 및 Stage 1/2/3 신용분류 관리"
        breadcrumbs={[
          { label: 'ECL' },
          { label: '여신 익스포저' }
        ]}
        icon={Layers}
        actions={
          <div className="flex items-center gap-3">
            <button className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 border border-white/10 font-bold text-sm transition-all">
              <FileSpreadsheet size={16} />
              익스포저 엑셀 내보내기
            </button>
          </div>
        }
      />

      {/* Stage Summary Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <div className="bg-slate-900/50 backdrop-blur-md p-6 rounded-2xl border border-white/5 space-y-3">
          <div className="flex items-center justify-between text-slate-400 text-xs font-bold uppercase tracking-wider">
            <span>총 EAD 익스포저</span>
            <Building2 size={18} className="text-blue-400" />
          </div>
          <div>
            <AmountDisplay amount={totalEad} className="text-2xl font-black" />
          </div>
          <div className="text-xs text-slate-400 flex items-center justify-between pt-1">
            <span>총 계량 충당금</span>
            <AmountDisplay amount={totalEcl} className="text-xs font-bold text-rose-400" />
          </div>
        </div>

        <div className="bg-emerald-500/5 backdrop-blur-md p-6 rounded-2xl border border-emerald-500/20 space-y-3">
          <div className="flex items-center justify-between text-emerald-400 text-xs font-bold uppercase tracking-wider">
            <span>Stage 1 (12개월 ECL)</span>
            <ShieldCheck size={18} />
          </div>
          <div>
            <AmountDisplay amount={stage1Ead} className="text-2xl font-black text-emerald-400" />
          </div>
          <div className="flex items-center justify-between text-xs text-emerald-500/80 font-bold pt-1">
            <span>{stage1Exposures.length}건</span>
            <span>{((stage1Ead / totalEad) * 100).toFixed(1)}%</span>
          </div>
        </div>

        <div className="bg-amber-500/5 backdrop-blur-md p-6 rounded-2xl border border-amber-500/20 space-y-3">
          <div className="flex items-center justify-between text-amber-400 text-xs font-bold uppercase tracking-wider">
            <span>Stage 2 (생애 ECL - SICR)</span>
            <AlertTriangle size={18} />
          </div>
          <div>
            <AmountDisplay amount={stage2Ead} className="text-2xl font-black text-amber-400" />
          </div>
          <div className="flex items-center justify-between text-xs text-amber-500/80 font-bold pt-1">
            <span>{stage2Exposures.length}건</span>
            <span>{((stage2Ead / totalEad) * 100).toFixed(1)}%</span>
          </div>
        </div>

        <div className="bg-rose-500/5 backdrop-blur-md p-6 rounded-2xl border border-rose-500/20 space-y-3">
          <div className="flex items-center justify-between text-rose-400 text-xs font-bold uppercase tracking-wider">
            <span>Stage 3 (생애 ECL - 손상)</span>
            <ShieldAlert size={18} />
          </div>
          <div>
            <AmountDisplay amount={stage3Ead} className="text-2xl font-black text-rose-400" />
          </div>
          <div className="flex items-center justify-between text-xs text-rose-500/80 font-bold pt-1">
            <span>{stage3Exposures.length}건</span>
            <span>{((stage3Ead / totalEad) * 100).toFixed(1)}%</span>
          </div>
        </div>
      </div>

      {/* Tabs & Filter Bar */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <Tabs tabs={filterTabs} activeTab={selectedStage} onChange={setSelectedStage} />

        <div className="flex flex-col sm:flex-row items-center gap-3">
          {/* Portfolio Filter */}
          <select
            value={selectedPortfolio}
            onChange={(e) => setSelectedPortfolio(e.target.value)}
            className="px-4 py-2.5 bg-slate-900/50 backdrop-blur-md border border-white/5 rounded-xl text-sm font-bold text-slate-300 focus:outline-none focus:border-blue-500 w-full sm:w-auto"
          >
            <option value="ALL">전체 포트폴리오</option>
            <option value="기업 여신">기업 여신</option>
            <option value="가계 주택담보대출">가계 주택담보대출</option>
            <option value="개인 신용대출">개인 신용대출</option>
            <option value="신용카드">신용카드</option>
            <option value="상장 유가증권">상장 유가증권</option>
          </select>

          {/* Search Box */}
          <div className="relative w-full sm:w-64">
            <Search size={16} className="absolute left-3.5 top-3 text-slate-500" />
            <input
              type="text"
              placeholder="계약번호 또는 차주명..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 bg-slate-900/50 border border-white/5 rounded-xl text-sm text-white placeholder-slate-500 focus:outline-none focus:border-blue-500"
            />
          </div>
        </div>
      </div>

      {/* Exposures Table */}
      <div className="bg-slate-900/50 backdrop-blur-md rounded-2xl border border-white/5 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-white/5 bg-slate-900/80 text-slate-400 text-xs font-black uppercase tracking-wider">
                <th className="px-6 py-4">계약번호 / 차주명</th>
                <th className="px-6 py-4">포트폴리오 / 상품</th>
                <th className="px-6 py-4 text-center">신용등급</th>
                <th className="px-6 py-4 text-center">연체일수</th>
                <th className="px-6 py-4 text-center">Stage 분류</th>
                <th className="px-6 py-4 text-right">익스포저 (EAD)</th>
                <th className="px-6 py-4 text-center">PD / LGD</th>
                <th className="px-6 py-4 text-right">산출 ECL</th>
                <th className="px-6 py-4 text-center">상세</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5 text-slate-300">
              {filteredExposures.length === 0 ? (
                <tr>
                  <td colSpan={9} className="px-6 py-12 text-center text-slate-500 font-medium">
                    조건에 해당하는 익스포저 데이터가 없습니다.
                  </td>
                </tr>
              ) : (
                filteredExposures.map((item) => (
                  <tr key={item.id} className="hover:bg-white/5 transition-all">
                    <td className="px-6 py-4">
                      <div className="font-mono font-bold text-white">{item.contractNo}</div>
                      <div className="text-xs text-slate-400 font-medium">{item.customerName}</div>
                    </td>
                    <td className="px-6 py-4">
                      <div className="font-bold text-slate-200">{item.portfolio}</div>
                      <div className="text-xs text-slate-500">{item.productType}</div>
                    </td>
                    <td className="px-6 py-4 text-center">
                      <span className={`inline-block px-2.5 py-0.5 rounded-full text-xs font-black border ${getCreditGradeBadge(item.creditGrade)}`}>
                        {item.creditGrade}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-center font-mono">
                      {item.delinquencyDays > 0 ? (
                        <span className="text-rose-400 font-bold">{item.delinquencyDays}일</span>
                      ) : (
                        <span className="text-slate-500">0일</span>
                      )}
                    </td>
                    <td className="px-6 py-4 text-center">
                      <StatusBadge status={item.stage} variant={getStageBadgeVariant(item.stage)} />
                    </td>
                    <td className="px-6 py-4 text-right">
                      <AmountDisplay amount={item.exposureAmount} className="font-bold" />
                    </td>
                    <td className="px-6 py-4 text-center font-mono text-xs">
                      <div className="text-slate-300">PD: {item.pd}%</div>
                      <div className="text-slate-500">LGD: {item.lgd}%</div>
                    </td>
                    <td className="px-6 py-4 text-right">
                      <AmountDisplay amount={item.eclAmount} className="font-bold text-rose-400" />
                    </td>
                    <td className="px-6 py-4 text-center">
                      <button
                        onClick={() => setSelectedExposure(item)}
                        className="p-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white transition-all"
                        title="상세보기"
                      >
                        <Eye size={16} />
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Exposure Detail Modal Drawer */}
      {selectedExposure && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-white/10 rounded-2xl max-w-2xl w-full p-6 space-y-6 animate-scale-up">
            <div className="flex items-center justify-between pb-4 border-b border-white/10">
              <div className="space-y-1">
                <span className="text-xs font-bold text-blue-400 uppercase tracking-widest">EXPOSURE DETAIL</span>
                <h3 className="text-xl font-bold text-white">{selectedExposure.contractNo} - {selectedExposure.customerName}</h3>
              </div>
              <button
                onClick={() => setSelectedExposure(null)}
                className="p-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white"
              >
                <X size={18} />
              </button>
            </div>

            <div className="grid grid-cols-2 gap-4 text-sm font-mono">
              <div className="p-4 rounded-xl bg-slate-950/60 border border-white/5 space-y-1">
                <span className="text-xs text-slate-500">포트폴리오</span>
                <div className="text-white font-bold">{selectedExposure.portfolio}</div>
              </div>
              <div className="p-4 rounded-xl bg-slate-950/60 border border-white/5 space-y-1">
                <span className="text-xs text-slate-500">상품 분류</span>
                <div className="text-white font-bold">{selectedExposure.productType}</div>
              </div>
              <div className="p-4 rounded-xl bg-slate-950/60 border border-white/5 space-y-1">
                <span className="text-xs text-slate-500">익스포저 (EAD)</span>
                <div><AmountDisplay amount={selectedExposure.exposureAmount} className="text-lg font-bold text-blue-400" /></div>
              </div>
              <div className="p-4 rounded-xl bg-slate-950/60 border border-white/5 space-y-1">
                <span className="text-xs text-slate-500">산출 대손충당금 (ECL)</span>
                <div><AmountDisplay amount={selectedExposure.eclAmount} className="text-lg font-bold text-rose-400" /></div>
              </div>
            </div>

            <div className="p-4 rounded-xl bg-slate-950/60 border border-white/5 space-y-2">
              <div className="flex items-center justify-between text-xs text-slate-400">
                <span>Stage 분류 사유</span>
                <StatusBadge status={selectedExposure.stage} variant={getStageBadgeVariant(selectedExposure.stage)} />
              </div>
              <p className="text-sm font-medium text-slate-200">{selectedExposure.stageReason}</p>
            </div>

            <div className="grid grid-cols-3 gap-4 text-xs font-mono text-center">
              <div className="p-3 rounded-lg bg-slate-800/40 border border-white/5">
                <div className="text-slate-500 mb-1">적용 PD</div>
                <div className="text-sm font-bold text-white">{selectedExposure.pd}%</div>
              </div>
              <div className="p-3 rounded-lg bg-slate-800/40 border border-white/5">
                <div className="text-slate-500 mb-1">적용 LGD</div>
                <div className="text-sm font-bold text-white">{selectedExposure.lgd}%</div>
              </div>
              <div className="p-3 rounded-lg bg-slate-800/40 border border-white/5">
                <div className="text-slate-500 mb-1">담보 평가액</div>
                <div className="text-sm font-bold text-emerald-400">
                  ₩{(selectedExposure.collateralValue / 100000000).toFixed(1)}억원
                </div>
              </div>
            </div>

            <div className="pt-2 flex justify-end">
              <button
                onClick={() => setSelectedExposure(null)}
                className="px-5 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-bold text-sm shadow-lg shadow-blue-600/30"
              >
                확인
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

"use client";

import React, { useState } from 'react';
import { 
  Calculator, 
  FileCheck, 
  Download, 
  CheckCircle2, 
  Calendar, 
  AlertCircle, 
  PieChart, 
  ArrowRight, 
  Lock,
  Layers,
  Sparkles,
  Info
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import AmountDisplay from '@/components/ui/AmountDisplay';
import Tabs from '@/components/ui/Tabs';
import { mockVatSummary, VatSummaryDto } from '@/mocks/tax';

export default function VatFilingBasisPage() {
  const [vatSummary, setVatSummary] = useState<VatSummaryDto>(mockVatSummary);
  const [selectedPeriod, setSelectedPeriod] = useState<string>('2025_Q3');
  const [isLocked, setIsLocked] = useState<boolean>(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const periodTabs = [
    { id: '2025_Q3', label: '2025년 2기 예정' },
    { id: '2025_Q2', label: '2025년 1기 확정' },
    { id: '2025_Q1', label: '2025년 1기 예정' },
  ];

  const handlePeriodChange = (id: string) => {
    setSelectedPeriod(id);
    if (id === '2025_Q3') {
      setVatSummary(mockVatSummary);
    } else if (id === '2025_Q2') {
      setVatSummary({
        ...mockVatSummary,
        period: '2025년 1기 확정 (04월~06월)',
        quarter: '1기 확정',
        salesTotalSupply: 480000000,
        salesTotalTax: 48000000,
        purchaseTotalSupply: 220000000,
        purchaseTotalTax: 22000000,
        deductiblePurchaseTax: 20000000,
        nonDeductiblePurchaseTax: 2000000,
        netTaxPayable: 28000000,
        status: 'SUBMITTED',
        filingDueDate: '2025-07-25',
      });
    } else {
      setVatSummary({
        ...mockVatSummary,
        period: '2025년 1기 예정 (01월~03월)',
        quarter: '1기 예정',
        salesTotalSupply: 390000000,
        salesTotalTax: 39000000,
        purchaseTotalSupply: 180000000,
        purchaseTotalTax: 18000000,
        deductiblePurchaseTax: 16500000,
        nonDeductiblePurchaseTax: 1500000,
        netTaxPayable: 22500000,
        status: 'SUBMITTED',
        filingDueDate: '2025-04-25',
      });
    }
  };

  const handleGenerateFilingData = () => {
    setToastMessage('홈택스 전송용 전자신고 파일(.txt / .xml)이 생성되어 다운로드되었습니다.');
    setTimeout(() => setToastMessage(null), 4000);
  };

  const handleLockData = () => {
    setIsLocked(true);
    setToastMessage('해당 기간의 부가가치세 신고 기초 자료가 최종 마감되었습니다.');
    setTimeout(() => setToastMessage(null), 4000);
  };

  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      {/* Page Header */}
      <PageHeader
        title="부가세 신고 기초"
        description="매출/매입 세금계산서 이력을 집계하여 국세청 홈택스 표준 부가가치세 신고서 항목별 기초 데이터를 사전 검증합니다."
        breadcrumbs={[
          { label: '세무 관리', href: '/tax/vat' },
          { label: '부가세 신고 기초' },
        ]}
        icon={Calculator}
        actions={
          <div className="flex items-center gap-3">
            <button
              onClick={handleGenerateFilingData}
              className="flex items-center gap-2 px-5 py-2.5 rounded-2xl bg-blue-600 hover:bg-blue-500 text-white font-bold text-xs shadow-lg shadow-blue-600/20 transition-all"
            >
              <FileCheck size={16} />
              <span>홈택스 신고 파일 생성</span>
            </button>
            <button
              onClick={handleLockData}
              disabled={isLocked}
              className={`flex items-center gap-2 px-4 py-2.5 rounded-2xl font-bold text-xs border transition-all ${
                isLocked 
                  ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20 cursor-default'
                  : 'bg-white/5 hover:bg-white/10 text-white border-white/10'
              }`}
            >
              <Lock size={14} className={isLocked ? 'text-emerald-400' : 'text-slate-400'} />
              <span>{isLocked ? '신고 마감완료' : '기초자료 마감'}</span>
            </button>
          </div>
        }
      />

      {/* Toast Notification */}
      {toastMessage && (
        <div className="p-4 rounded-2xl bg-blue-500/10 border border-blue-500/20 text-blue-300 text-sm font-bold flex items-center justify-between animate-in fade-in slide-in-from-top-2">
          <div className="flex items-center gap-3">
            <CheckCircle2 size={18} className="text-blue-400" />
            <span>{toastMessage}</span>
          </div>
        </div>
      )}

      {/* Period Selection Tabs & Info Bar */}
      <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <Tabs tabs={periodTabs} activeTab={selectedPeriod} onChange={handlePeriodChange} />
        
        <div className="flex items-center gap-4 text-xs">
          <div className="flex items-center gap-2 text-slate-400">
            <Calendar size={14} className="text-blue-400" />
            <span>신고기한: <strong className="text-white font-mono">{vatSummary.filingDueDate}</strong></span>
          </div>
          <StatusBadge
            status={vatSummary.status === 'SUBMITTED' ? '신고완료' : isLocked ? '마감완료' : '작성중'}
            variant={vatSummary.status === 'SUBMITTED' || isLocked ? 'success' : 'warning'}
          />
        </div>
      </div>

      {/* VAT Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <span className="text-xs font-bold text-slate-400">① 매출세액 (가)</span>
          <div className="text-2xl font-black text-white">
            <AmountDisplay amount={vatSummary.salesTotalTax} />
          </div>
          <p className="text-[11px] text-slate-500">공급가액 <AmountDisplay amount={vatSummary.salesTotalSupply} className="text-slate-400" /></p>
        </div>

        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <span className="text-xs font-bold text-slate-400">② 공제 대상 매입세액 (나)</span>
          <div className="text-2xl font-black text-emerald-400">
            <AmountDisplay amount={vatSummary.deductiblePurchaseTax} />
          </div>
          <p className="text-[11px] text-slate-500">일반 및 고정자산 매입세액</p>
        </div>

        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <span className="text-xs font-bold text-slate-400">③ 불공제 매입세액</span>
          <div className="text-2xl font-black text-rose-400">
            <AmountDisplay amount={vatSummary.nonDeductiblePurchaseTax} />
          </div>
          <p className="text-[11px] text-rose-400/80 font-medium">비영업용승용차 등 (공제불가)</p>
        </div>

        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-blue-500/30 bg-blue-600/5 space-y-2">
          <span className="text-xs font-black text-blue-400 uppercase tracking-wider">④ 차감 납부예정 세액 (① - ②)</span>
          <div className="text-3xl font-black text-blue-400">
            <AmountDisplay amount={vatSummary.netTaxPayable} />
          </div>
          <p className="text-[11px] text-slate-400 font-medium">부가가치세 최종 신고 납부액</p>
        </div>
      </div>

      {/* Preview Grid (HomeTax Standard Format) */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Sales Tax Breakdown */}
        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-4">
          <div className="flex items-center justify-between pb-3 border-b border-white/5">
            <div className="flex items-center gap-2">
              <PieChart size={18} className="text-blue-400" />
              <h3 className="text-base font-black text-white italic">1. 과세표준 및 매출세액 집계</h3>
            </div>
            <span className="text-xs font-mono font-bold text-slate-400 bg-white/5 px-2.5 py-1 rounded-xl">
              매출세액 합계
            </span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-white/10 text-[11px] font-black uppercase text-slate-400">
                  <th className="py-2.5 px-3">구분 (홈택스 서식 번호)</th>
                  <th className="py-2.5 px-3 text-right">공급가액 (원)</th>
                  <th className="py-2.5 px-3 text-right">세액 (원)</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5 text-xs">
                <tr>
                  <td className="py-3 px-3 font-bold text-white">
                    (1) 과세 - 전자세금계산서 발급분
                  </td>
                  <td className="py-3 px-3 text-right font-mono text-slate-300">
                    <AmountDisplay amount={vatSummary.salesBreakdown.taxableNormal.supply} />
                  </td>
                  <td className="py-3 px-3 text-right font-mono font-bold text-emerald-400">
                    <AmountDisplay amount={vatSummary.salesBreakdown.taxableNormal.tax} />
                  </td>
                </tr>
                <tr>
                  <td className="py-3 px-3 font-bold text-white">
                    (3) 과세 - 신용카드 · 현금영수증 수령분
                  </td>
                  <td className="py-3 px-3 text-right font-mono text-slate-300">
                    <AmountDisplay amount={vatSummary.salesBreakdown.taxableElectronic.supply} />
                  </td>
                  <td className="py-3 px-3 text-right font-mono font-bold text-emerald-400">
                    <AmountDisplay amount={vatSummary.salesBreakdown.taxableElectronic.tax} />
                  </td>
                </tr>
                <tr>
                  <td className="py-3 px-3 font-bold text-white">
                    (5) 영세율 - 세금계산서 발급분 (구매확인서)
                  </td>
                  <td className="py-3 px-3 text-right font-mono text-slate-300">
                    <AmountDisplay amount={vatSummary.salesBreakdown.zeroRateDirect.supply} />
                  </td>
                  <td className="py-3 px-3 text-right font-mono font-bold text-slate-500">
                    0
                  </td>
                </tr>
                <tr>
                  <td className="py-3 px-3 font-bold text-white">
                    (6) 영세율 - 기타 (직수출 등)
                  </td>
                  <td className="py-3 px-3 text-right font-mono text-slate-300">
                    <AmountDisplay amount={vatSummary.salesBreakdown.zeroRateLocalLC.supply} />
                  </td>
                  <td className="py-3 px-3 text-right font-mono font-bold text-slate-500">
                    0
                  </td>
                </tr>
                <tr className="bg-white/5 font-black text-sm">
                  <td className="py-3 px-3 text-white">매출세액 합계 (계)</td>
                  <td className="py-3 px-3 text-right text-white">
                    <AmountDisplay amount={vatSummary.salesTotalSupply} />
                  </td>
                  <td className="py-3 px-3 text-right text-blue-400">
                    <AmountDisplay amount={vatSummary.salesTotalTax} />
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        {/* Purchase Tax Breakdown */}
        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-4">
          <div className="flex items-center justify-between pb-3 border-b border-white/5">
            <div className="flex items-center gap-2">
              <Layers size={18} className="text-emerald-400" />
              <h3 className="text-base font-black text-white italic">2. 매입세액 집계 및 공제 검증</h3>
            </div>
            <span className="text-xs font-mono font-bold text-slate-400 bg-white/5 px-2.5 py-1 rounded-xl">
              매입세액 합계
            </span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-white/10 text-[11px] font-black uppercase text-slate-400">
                  <th className="py-2.5 px-3">구분 (홈택스 서식 번호)</th>
                  <th className="py-2.5 px-3 text-right">공급가액 (원)</th>
                  <th className="py-2.5 px-3 text-right">세액 (원)</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5 text-xs">
                <tr>
                  <td className="py-3 px-3 font-bold text-white">
                    (10) 일반매입 - 세금계산서 수취분
                  </td>
                  <td className="py-3 px-3 text-right font-mono text-slate-300">
                    <AmountDisplay amount={vatSummary.purchaseBreakdown.generalPurchase.supply} />
                  </td>
                  <td className="py-3 px-3 text-right font-mono font-bold text-emerald-400">
                    <AmountDisplay amount={vatSummary.purchaseBreakdown.generalPurchase.tax} />
                  </td>
                </tr>
                <tr>
                  <td className="py-3 px-3 font-bold text-white">
                    (11) 고정자산 매입 - 세금계산서 수취분
                  </td>
                  <td className="py-3 px-3 text-right font-mono text-slate-300">
                    <AmountDisplay amount={vatSummary.purchaseBreakdown.fixedAssetPurchase.supply} />
                  </td>
                  <td className="py-3 px-3 text-right font-mono font-bold text-emerald-400">
                    <AmountDisplay amount={vatSummary.purchaseBreakdown.fixedAssetPurchase.tax} />
                  </td>
                </tr>
                <tr>
                  <td className="py-3 px-3 font-bold text-rose-400">
                    (16) 공제받지 못할 매입세액 (불공제)
                  </td>
                  <td className="py-3 px-3 text-right font-mono text-rose-400">
                    <AmountDisplay amount={vatSummary.purchaseBreakdown.nonDeductible.supply} />
                  </td>
                  <td className="py-3 px-3 text-right font-mono font-bold text-rose-400">
                    <AmountDisplay amount={vatSummary.purchaseBreakdown.nonDeductible.tax} />
                  </td>
                </tr>
                <tr className="bg-white/5 font-black text-sm">
                  <td className="py-3 px-3 text-white">공제 대상 매입세액 합계</td>
                  <td className="py-3 px-3 text-right text-white">
                    <AmountDisplay amount={vatSummary.purchaseTotalSupply - vatSummary.purchaseBreakdown.nonDeductible.supply} />
                  </td>
                  <td className="py-3 px-3 text-right text-emerald-400">
                    <AmountDisplay amount={vatSummary.deductiblePurchaseTax} />
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Tax Audit Summary Box */}
      <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-4">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-2xl bg-blue-600/10 border border-blue-500/20 flex items-center justify-center text-blue-400">
            <Sparkles size={20} />
          </div>
          <div>
            <h4 className="text-base font-black text-white italic">부가가치세 전자신고 가이드 및 검증 상태</h4>
            <p className="text-xs text-slate-400">국세청 홈택스 표준 스키마 2025v2.1 준수 검증 완료</p>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 pt-2">
          <div className="p-4 rounded-2xl bg-white/5 border border-white/5 space-y-1">
            <span className="text-[10px] font-black uppercase text-slate-400">매출-매입 전표 정합성</span>
            <p className="text-sm font-bold text-emerald-400 flex items-center gap-1.5">
              <CheckCircle2 size={16} /> 100% 검증 승인
            </p>
          </div>
          <div className="p-4 rounded-2xl bg-white/5 border border-white/5 space-y-1">
            <span className="text-[10px] font-black uppercase text-slate-400">불공제 대상 사전 탐지</span>
            <p className="text-sm font-bold text-amber-400 flex items-center gap-1.5">
              <AlertCircle size={16} /> 비영업용 승용차 1건
            </p>
          </div>
          <div className="p-4 rounded-2xl bg-white/5 border border-white/5 space-y-1">
            <span className="text-[10px] font-black uppercase text-slate-400">전자신고 파일 연동</span>
            <p className="text-sm font-bold text-blue-400 flex items-center gap-1.5">
              <ArrowRight size={16} /> 국세청 변환 이상없음
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

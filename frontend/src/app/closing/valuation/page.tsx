"use client";

import React, { useState } from 'react';
import { 
  Globe, 
  Play, 
  Clock, 
  CheckCircle2, 
  TrendingUp, 
  TrendingDown, 
  DollarSign, 
  Layers
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import AmountDisplay from '@/components/ui/AmountDisplay';

interface ExchangeRate {
  currency: string;
  currencyName: string;
  bookRate: number;
  closingRate: number;
  diff: number;
  diffPercent: number;
}

interface ForeignAccount {
  id: string;
  accountCode: string;
  accountName: string;
  currency: string;
  foreignAmount: number;
  bookKrwAmount: number;
  evaluatedKrwAmount: number;
  unrealizedGainLoss: number;
}

interface BatchLog {
  id: string;
  executedAt: string;
  targetPeriod: string;
  status: 'SUCCESS' | 'RUNNING' | 'FAILED';
  totalAccounts: number;
  netGainLoss: number;
  operator: string;
}

export default function ForeignValuationPage() {
  const [rates] = useState<ExchangeRate[]>([
    { currency: 'USD', currencyName: '미국 달러', bookRate: 1320.50, closingRate: 1358.20, diff: 37.70, diffPercent: 2.85 },
    { currency: 'EUR', currencyName: '유로화', bookRate: 1450.00, closingRate: 1472.50, diff: 22.50, diffPercent: 1.55 },
    { currency: 'JPY', currencyName: '일본 엔 (100엔)', bookRate: 880.00, closingRate: 895.40, diff: 15.40, diffPercent: 1.75 },
    { currency: 'CNY', currencyName: '중국 위안', bookRate: 184.20, closingRate: 182.10, diff: -2.10, diffPercent: -1.14 },
  ]);

  const [foreignAccounts] = useState<ForeignAccount[]>([
    {
      id: 'FA-001',
      accountCode: '1110300',
      accountName: 'USD 외화보통예금 (씨티은행)',
      currency: 'USD',
      foreignAmount: 250000.00,
      bookKrwAmount: 330125000,
      evaluatedKrwAmount: 339550000,
      unrealizedGainLoss: 9425000,
    },
    {
      id: 'FA-002',
      accountCode: '1120100',
      accountName: 'USD 외상매출금 (Apple Inc.)',
      currency: 'USD',
      foreignAmount: 480000.00,
      bookKrwAmount: 633840000,
      evaluatedKrwAmount: 651936000,
      unrealizedGainLoss: 18096000,
    },
    {
      id: 'FA-003',
      accountCode: '2110100',
      accountName: 'EUR 외상매입금 (SAP SE)',
      currency: 'EUR',
      foreignAmount: 120000.00,
      bookKrwAmount: 174000000,
      evaluatedKrwAmount: 176700000,
      unrealizedGainLoss: -2700000,
    },
    {
      id: 'FA-004',
      accountCode: '2130100',
      accountName: 'JPY 외화단기차입금 (MUFG)',
      currency: 'JPY',
      foreignAmount: 35000000.00,
      bookKrwAmount: 308000000,
      evaluatedKrwAmount: 313390000,
      unrealizedGainLoss: -5390000,
    },
  ]);

  const [batchLogs, setBatchLogs] = useState<BatchLog[]>([
    {
      id: 'VAL-202607-001',
      executedAt: '2026-07-25 11:15:20',
      targetPeriod: '2026년 07월',
      status: 'SUCCESS',
      totalAccounts: 4,
      netGainLoss: 19431000,
      operator: '이재무 대리 (자금팀)',
    },
    {
      id: 'VAL-202606-001',
      executedAt: '2026-06-25 16:40:10',
      targetPeriod: '2026년 06월',
      status: 'SUCCESS',
      totalAccounts: 4,
      netGainLoss: -4250000,
      operator: '이재무 대리 (자금팀)',
    },
  ]);

  const [isRunningBatch, setIsRunningBatch] = useState(false);

  const totalUnrealizedGainLoss = foreignAccounts.reduce((acc, cur) => acc + cur.unrealizedGainLoss, 0);

  const handleRunValuationBatch = () => {
    setIsRunningBatch(true);
    setTimeout(() => {
      const newLog: BatchLog = {
        id: `VAL-202607-${String(batchLogs.length + 1).padStart(3, '0')}`,
        executedAt: new Date().toISOString().slice(0, 19).replace('T', ' '),
        targetPeriod: '2026년 07월',
        status: 'SUCCESS',
        totalAccounts: foreignAccounts.length,
        netGainLoss: totalUnrealizedGainLoss,
        operator: '이재무 대리 (자금팀)',
      };
      setBatchLogs([newLog, ...batchLogs]);
      setIsRunningBatch(false);
    }, 1500);
  };

  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      {/* Page Header */}
      <PageHeader
        title="외화 평가 배치"
        description="월말 기말 고시 환율을 기준으로 외화 자산 및 부채의 평가손익을 자동 산출하고 배치 전표를 반영합니다."
        breadcrumbs={[
          { label: '결산 관리', href: '/closing' },
          { label: '외화 평가 배치' },
        ]}
        icon={Globe}
        actions={
          <button
            onClick={handleRunValuationBatch}
            disabled={isRunningBatch}
            className="flex items-center gap-2 px-5 py-3 rounded-2xl bg-blue-600 hover:bg-blue-500 text-white font-bold text-sm shadow-lg shadow-blue-600/20 transition-all disabled:opacity-50"
          >
            <Play size={18} className={isRunningBatch ? 'animate-spin' : ''} />
            <span>{isRunningBatch ? '평가 배치 계산 중...' : '외화 평가 배치 실행'}</span>
          </button>
        }
      />

      {/* Top Banner KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2 relative overflow-hidden">
          <div className="flex items-center justify-between text-xs font-bold text-slate-400">
            <span>당월 예상 평가 손익 총액</span>
            <DollarSign size={18} className="text-blue-400" />
          </div>
          <div className="flex items-baseline gap-2">
            <AmountDisplay amount={totalUnrealizedGainLoss} className="text-3xl" showSign />
          </div>
          <p className="text-xs text-slate-400">
            {totalUnrealizedGainLoss >= 0 ? (
              <span className="text-emerald-400 font-bold flex items-center gap-1">
                <TrendingUp size={14} /> 미실현 외화평가이익 발생
              </span>
            ) : (
              <span className="text-rose-400 font-bold flex items-center gap-1">
                <TrendingDown size={14} /> 미실현 외화평가손실 발생
              </span>
            )}
          </p>
        </div>

        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="flex items-center justify-between text-xs font-bold text-slate-400">
            <span>평가 대상 계좌 수</span>
            <Layers size={18} className="text-indigo-400" />
          </div>
          <p className="text-3xl font-black text-white">{foreignAccounts.length} <span className="text-xs font-normal text-slate-500">개 계정</span></p>
          <p className="text-xs text-slate-400">USD, EUR, JPY 통화 포함</p>
        </div>

        <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-2">
          <div className="flex items-center justify-between text-xs font-bold text-slate-400">
            <span>최근 배치 상태</span>
            <Clock size={18} className="text-emerald-400" />
          </div>
          <div className="flex items-center gap-2">
            <StatusBadge status="배치 정상 완료" variant="success" />
          </div>
          <p className="text-xs text-slate-400 font-mono">최종 실행: {batchLogs[0]?.executedAt}</p>
        </div>
      </div>

      {/* Exchange Rates Grid */}
      <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-lg font-black text-white italic">당월 고시 환율 매트릭스</h3>
            <p className="text-xs text-slate-400">장부 평가 환율 vs 기말 종가 환율 비교</p>
          </div>
          <span className="text-xs text-blue-400 font-mono font-bold bg-blue-500/10 px-3 py-1 rounded-xl border border-blue-500/20">
            기준일자: 2026-07-28
          </span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {rates.map((r) => (
            <div key={r.currency} className="p-5 rounded-2xl bg-white/5 border border-white/5 space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-xs font-mono font-bold text-blue-400 bg-blue-500/10 px-2.5 py-0.5 rounded-md border border-blue-500/20">
                  {r.currency}
                </span>
                <span className="text-xs text-slate-400 font-bold">{r.currencyName}</span>
              </div>
              <div>
                <span className="text-xs text-slate-500 font-bold block">기말 고시 환율</span>
                <span className="text-2xl font-black text-white font-mono">₩{r.closingRate.toLocaleString()}</span>
              </div>
              <div className="flex items-center justify-between text-xs pt-2 border-t border-white/5">
                <span className="text-slate-500">장부환율: ₩{r.bookRate.toLocaleString()}</span>
                <span className={`font-bold font-mono ${r.diff >= 0 ? 'text-emerald-400' : 'text-rose-400'}`}>
                  {r.diff >= 0 ? '+' : ''}{r.diff.toFixed(2)} ({r.diffPercent >= 0 ? '+' : ''}{r.diffPercent}%)
                </span>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Main Table: Foreign Account Balances & Valuation */}
      <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-lg font-black text-white italic">외화 계정별 평가 상세 명세서</h3>
            <p className="text-xs text-slate-400">외폐 원장 잔액 및 평가 환산 금액 내역</p>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-white/10 text-[11px] font-black uppercase tracking-wider text-slate-400">
                <th className="py-3 px-4">계정 코드 / 명칭</th>
                <th className="py-3 px-4">통화</th>
                <th className="py-3 px-4 text-right">외화 잔액</th>
                <th className="py-3 px-4 text-right">장부 원화 금액</th>
                <th className="py-3 px-4 text-right">평가 원화 금액</th>
                <th className="py-3 px-4 text-right">미실현 평가 손익</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5 text-xs font-medium">
              {foreignAccounts.map((acc) => (
                <tr key={acc.id} className="hover:bg-white/5 transition-all">
                  <td className="py-4 px-4">
                    <div>
                      <p className="font-bold text-white">{acc.accountName}</p>
                      <p className="text-[10px] font-mono text-slate-500">{acc.accountCode}</p>
                    </div>
                  </td>
                  <td className="py-4 px-4">
                    <span className="font-mono font-bold text-blue-400 bg-blue-500/10 px-2 py-0.5 rounded-md border border-blue-500/20">
                      {acc.currency}
                    </span>
                  </td>
                  <td className="py-4 px-4 text-right font-mono font-bold text-slate-200">
                    {acc.foreignAmount.toLocaleString(undefined, { minimumFractionDigits: 2 })} {acc.currency}
                  </td>
                  <td className="py-4 px-4 text-right">
                    <AmountDisplay amount={acc.bookKrwAmount} />
                  </td>
                  <td className="py-4 px-4 text-right">
                    <AmountDisplay amount={acc.evaluatedKrwAmount} />
                  </td>
                  <td className="py-4 px-4 text-right">
                    <AmountDisplay amount={acc.unrealizedGainLoss} showSign className="font-bold" />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Batch Logs History */}
      <div className="p-6 rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-4">
        <h3 className="text-lg font-black text-white italic">평가 배치 실행 이력 Log</h3>
        <div className="space-y-3">
          {batchLogs.map((log) => (
            <div key={log.id} className="p-4 rounded-2xl bg-white/5 border border-white/5 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs">
              <div className="flex items-center gap-3">
                <CheckCircle2 size={18} className="text-emerald-400 shrink-0" />
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-mono font-bold text-white">{log.id}</span>
                    <StatusBadge status="성공" variant="success" />
                    <span className="text-slate-400 font-bold">[{log.targetPeriod}]</span>
                  </div>
                  <p className="text-slate-400 text-[11px] mt-0.5">
                    작업자: {log.operator} | 대상: {log.totalAccounts}개 계정
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-4 sm:justify-end">
                <span className="font-mono text-slate-500 text-[11px]">{log.executedAt}</span>
                <AmountDisplay amount={log.netGainLoss} showSign className="text-sm font-bold" />
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

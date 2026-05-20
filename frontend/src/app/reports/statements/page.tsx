"use client";

import { useEffect, useMemo, useState } from 'react';
import { FileBarChart, Layers, Download, ChevronRight, ChevronDown } from 'lucide-react';
import { FinancialStatementDto, reportingService, StatementType } from '@/services/reportingService';

const statementLabels: Record<StatementType, string> = {
  BALANCE_SHEET: '재무상태표 (Balance Sheet)',
  INCOME_STATEMENT: '손익계산서 (Income Statement)',
};

/**
 * [재무제표 보고서 조회 화면]
 * 재무상태표(BS) 및 손익계산서(PL)를 계층적으로 조회하고 비교 분석합니다.
 * 설계서 파트 4-⑪ 기반.
 */
export default function FinancialStatementsPage() {
  const [statementType, setStatementType] = useState<StatementType>('BALANCE_SHEET');
  const [baseDate, setBaseDate] = useState('2026-04-22T00:00:00');
  const [statement, setStatement] = useState<FinancialStatementDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadStatement = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await reportingService.generateStatement(statementType, baseDate);
      setStatement(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : '재무제표 생성 중 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    let cancelled = false;
    reportingService.generateStatement('BALANCE_SHEET', '2026-04-22T00:00:00')
      .then((data) => {
        if (!cancelled) {
          setStatement(data);
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : '재무제표 생성 중 오류가 발생했습니다.');
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const totalCurrent = useMemo(() => {
    return statement?.lines.reduce((sum, line) => sum + Number(line.currentAmount), 0) ?? 0;
  }, [statement]);

  const totalPrevious = useMemo(() => {
    return statement?.lines.reduce((sum, line) => sum + Number(line.previousAmount), 0) ?? 0;
  }, [statement]);

  const exportDocument = async (format: 'PDF' | 'EXCEL') => {
    try {
      await reportingService.exportDocument(statementType, baseDate, format);
    } catch (err) {
      setError(err instanceof Error ? err.message : '문서 생성 중 오류가 발생했습니다.');
    }
  };

  return (
    <div className="flex flex-col gap-10">
      <header className="flex justify-between items-center">
        <div>
          <h2 className="text-3xl font-black text-white italic tracking-tight uppercase">재무제표 보고서 (Financial Statements)</h2>
          <p className="text-slate-500 mt-2 text-sm font-medium leading-none">전사 재무상태표(BS) 및 손익계산서(PL) 통합 시계열 조회 및 비교 분석</p>
        </div>
        <div className="flex gap-3">
          <button
            onClick={() => exportDocument('PDF')}
            className="bg-slate-800 hover:bg-slate-700 text-slate-300 px-5 py-2.5 rounded-2xl border border-white/5 transition-all flex items-center gap-2 text-xs font-black uppercase tracking-widest shadow-lg"
          >
            <Download size={18} className="text-slate-500" /> PDF Export
          </button>
          <button
            onClick={() => exportDocument('EXCEL')}
            className="bg-blue-600 hover:bg-blue-500 text-white px-5 py-2.5 rounded-2xl border border-blue-500/20 transition-all flex items-center gap-2 text-xs font-black uppercase tracking-widest shadow-xl shadow-blue-500/20 active:scale-95"
          >
            <Layers size={18} /> Excel Engine
          </button>
        </div>
      </header>

      <section className="bg-white/5 border border-white/10 rounded-[32px] p-8 backdrop-blur-xl group">
        <div className="flex flex-col lg:flex-row items-end gap-10">
          <div className="flex flex-col gap-3">
            <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">보고서 종류 (Statement Type)</label>
            <div className="bg-white/5 p-1.5 rounded-2xl border border-white/5 flex gap-1">
              {Object.entries(statementLabels).map(([type, label]) => (
                <button
                  key={type}
                  onClick={() => setStatementType(type as StatementType)}
                  className={`${statementType === type ? 'bg-blue-600 text-white shadow-lg shadow-blue-600/20' : 'text-slate-500 hover:text-slate-300'} px-6 py-2 rounded-xl text-xs font-black uppercase tracking-tight transition-all`}
                >
                  {label.startsWith('재무') ? 'BS' : 'PL'}
                </button>
              ))}
            </div>
          </div>

          <div className="flex flex-col gap-3">
            <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">Fiscal Period</label>
            <input
              type="datetime-local"
              value={baseDate}
              onChange={(event) => setBaseDate(event.target.value)}
              className="bg-slate-950 border border-white/10 text-white text-xs rounded-2xl px-6 py-3 focus:border-blue-500 outline-none transition-all cursor-pointer font-black min-w-[240px]"
            />
          </div>

          <button
            onClick={loadStatement}
            className="bg-slate-100 hover:bg-white text-slate-950 px-10 py-3 rounded-2xl text-xs font-black uppercase tracking-widest transition-all shadow-xl active:scale-95 ml-auto"
          >
            {loading ? 'Running...' : 'Run Analysis'}
          </button>
        </div>
      </section>

      {error && (
        <div className="px-5 py-4 rounded-2xl bg-rose-500/10 border border-rose-500/20 text-rose-300 text-sm font-bold">
          {error}
        </div>
      )}

      <section className="bg-white/5 border border-white/10 rounded-[40px] p-12 backdrop-blur-xl relative overflow-hidden">
        <div className="text-center mb-16 relative z-10">
          <h3 className="text-4xl font-black text-white italic tracking-tighter uppercase mb-4">
            {statement ? statementLabels[statement.type] : statementLabels[statementType]}
          </h3>
          <p className="text-slate-500 text-sm font-bold tracking-widest uppercase flex items-center justify-center gap-3">
            <span className="w-8 h-px bg-slate-800" />
            As of {baseDate.slice(0, 10)} (Unit: KRW) · {statement?.status ?? 'DRAFT'}
            <span className="w-8 h-px bg-slate-800" />
          </p>
        </div>

        <div className="flex flex-col border-t-2 border-white/10 relative z-10">
          <div className="grid grid-cols-12 gap-4 px-8 py-6 border-b border-white/5 text-[10px] font-black text-slate-500 uppercase tracking-[0.2em] bg-white/[0.01]">
            <div className="col-span-6">Account Item</div>
            <div className="col-span-2 text-right">Current Period</div>
            <div className="col-span-2 text-right">Previous Period</div>
            <div className="col-span-2 text-right text-blue-400">Variance (%)</div>
          </div>

          <div className="space-y-px">
            <ReportRow
              label={statementType === 'BALANCE_SHEET' ? '[ I ] 보고 합계' : '[ I ] 손익 합계'}
              currentAmount={totalCurrent}
              previousAmount={totalPrevious}
              level={0}
              expanded
            />
            {statement?.lines.map((line) => (
              <ReportRow
                key={line.lineCode}
                label={`${line.label} · 주석 ${line.noteNumber || '-'}`}
                currentAmount={line.currentAmount}
                previousAmount={line.previousAmount}
                level={line.level}
              />
            ))}
          </div>
        </div>

        <div className="absolute top-0 right-0 p-12 opacity-[0.02] pointer-events-none">
          <FileBarChart size={300} className="text-white" />
        </div>
      </section>
    </div>
  );
}

function ReportRow({
  label,
  currentAmount,
  previousAmount,
  level,
  expanded = false,
}: {
  label: string;
  currentAmount: number;
  previousAmount: number;
  level: number;
  expanded?: boolean;
}) {
  const variance = previousAmount === 0 ? 0 : ((currentAmount - previousAmount) / Math.abs(previousAmount)) * 100;
  const Icon = expanded ? ChevronDown : ChevronRight;
  const indent = level <= 0 ? 'pl-8' : level === 1 ? 'pl-14' : 'pl-24';

  return (
    <div className={`grid grid-cols-12 gap-4 px-8 py-5 border-b border-white/5 ${indent} group hover:bg-white/[0.02] transition-colors items-center`}>
      <div className="col-span-6 text-sm font-black text-slate-200 flex items-center gap-3">
        <Icon size={14} className={expanded ? 'text-blue-500' : 'text-slate-500'} />
        {label}
      </div>
      <div className="col-span-2 text-sm font-mono font-black text-white text-right italic">{formatAmount(currentAmount)}</div>
      <div className="col-span-2 text-sm font-mono font-bold text-slate-400 text-right">{formatAmount(previousAmount)}</div>
      <div className={`col-span-2 text-sm font-black text-right ${variance >= 0 ? 'text-emerald-400' : 'text-rose-400'}`}>
        {variance >= 0 ? '+' : ''}{variance.toFixed(1)}%
      </div>
    </div>
  );
}

function formatAmount(value: number) {
  return new Intl.NumberFormat('ko-KR').format(value);
}

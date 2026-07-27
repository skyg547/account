"use client";

import { useEffect, useState, useCallback } from 'react';
import { Database, Search, FileText, Activity, ShieldAlert, Coins } from 'lucide-react';
import { DisclosureNoteMartDto, DisclosureNoteMartEntryDto, JournalDetailSummaryDto, reportingService, StatementType } from '@/services/reportingService';

const statementLabels: Record<StatementType, string> = {
  BALANCE_SHEET: '재무상태표 주석 (Balance Sheet Notes)',
  INCOME_STATEMENT: '손익계산서 주석 (Income Statement Notes)',
};

/**
 * [주석 마트 및 역추적(Drill-through) 조회 화면]
 */
export default function DisclosureNotesPage() {
  const [statementType, setStatementType] = useState<StatementType>('BALANCE_SHEET');
  const [baseDate, setBaseDate] = useState('2026-04-22T00:00:00');
  const [mart, setMart] = useState<DisclosureNoteMartDto | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  
  const [drillingEntryId, setDrillingEntryId] = useState<string | null>(null);
  const [drillDownResults, setDrillDownResults] = useState<JournalDetailSummaryDto[] | null>(null);

  const handleDrillDown = async (entry: DisclosureNoteMartEntryDto) => {
    setDrillingEntryId(entry.entryId);
    setError('');
    try {
      const results = await reportingService.drillDownDisclosureNote(statementType, baseDate, entry.entryId);
      setDrillDownResults(results);
    } catch (err) {
      setError(err instanceof Error ? err.message : '역추적 중 오류가 발생했습니다.');
    } finally {
      setDrillingEntryId(null);
    }
  };

  const loadMart = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      let data;
      try {
        data = await reportingService.getDisclosureNoteMart(statementType, baseDate);
      } catch {
        data = await reportingService.generateDisclosureNoteMart(statementType, baseDate);
      }
      setMart(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : '주석 마트 로드 중 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  }, [statementType, baseDate]);

  useEffect(() => {
    let ignore = false;
    const run = async () => {
      try {
        let data;
        try {
          data = await reportingService.getDisclosureNoteMart(statementType, baseDate);
        } catch {
          data = await reportingService.generateDisclosureNoteMart(statementType, baseDate);
        }
        if (!ignore) setMart(data);
      } catch (err) {
        if (!ignore) setError(err instanceof Error ? err.message : '주석 마트 로드 중 오류가 발생했습니다.');
      }
    };
    run();
    return () => { ignore = true; };
  }, [statementType, baseDate]);

  return (
    <div className="flex flex-col gap-10">
      <header className="flex justify-between items-center">
        <div>
          <h2 className="text-3xl font-black text-white italic tracking-tight uppercase">주석 마트 (Disclosure Notes)</h2>
          <p className="text-slate-500 mt-2 text-sm font-medium leading-none">주석별 집계 금액 및 원천 전표 역추적 (Drill-through)</p>
        </div>
      </header>

      <section className="bg-white/5 border border-white/10 rounded-[32px] p-8 backdrop-blur-xl">
        <div className="flex flex-col lg:flex-row items-end gap-10">
          <div className="flex flex-col gap-3">
            <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">보고서 종류</label>
            <div className="bg-white/5 p-1.5 rounded-2xl border border-white/5 flex gap-1">
              {Object.keys(statementLabels).map((type) => (
                <button
                  key={type}
                  onClick={() => setStatementType(type as StatementType)}
                  className={`${statementType === type ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-600/20' : 'text-slate-500 hover:text-slate-300'} px-6 py-2 rounded-xl text-xs font-black uppercase tracking-tight transition-all`}
                >
                  {type === 'BALANCE_SHEET' ? 'BS Notes' : 'PL Notes'}
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
              className="bg-slate-950 border border-white/10 text-white text-xs rounded-2xl px-6 py-3 focus:border-indigo-500 outline-none transition-all cursor-pointer font-black min-w-[240px]"
            />
          </div>

          <button
            onClick={loadMart}
            className="bg-indigo-500 hover:bg-indigo-400 text-white px-10 py-3 rounded-2xl text-xs font-black uppercase tracking-widest transition-all shadow-xl active:scale-95 ml-auto"
          >
            {loading ? 'Processing...' : 'Load Mart'}
          </button>
        </div>
      </section>

      {error && (
        <div className="px-5 py-4 rounded-2xl bg-rose-500/10 border border-rose-500/20 text-rose-300 text-sm font-bold">
          {error}
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-10">
        {/* 주석 마트 리스트 */}
        <section className="bg-white/5 border border-white/10 rounded-[40px] p-10 backdrop-blur-xl relative overflow-hidden flex flex-col gap-6">
          <div className="flex items-center gap-4 text-white">
            <FileText size={24} className="text-indigo-400" />
            <h3 className="text-2xl font-black italic tracking-tighter uppercase">Note Entries</h3>
          </div>
          
          <div className="flex flex-col border border-white/5 rounded-2xl overflow-hidden bg-slate-950/50">
            {mart?.entries.map((entry) => (
              <div key={entry.entryId} className="border-b border-white/5 p-6 hover:bg-white/5 transition-colors group flex justify-between items-center">
                <div className="flex flex-col gap-2">
                  <div className="flex items-center gap-3">
                    <span className="bg-indigo-500/20 text-indigo-300 text-[10px] px-2 py-1 rounded font-black uppercase">Note {entry.noteNumber}</span>
                    <span className="text-sm font-bold text-white">{entry.sourceLineLabel}</span>
                  </div>
                  <div className="flex gap-4 text-[11px] font-medium text-slate-500">
                    <span className="flex items-center gap-1"><Activity size={12} /> {entry.noteCategory}</span>
                    <span className="flex items-center gap-1"><ShieldAlert size={12} /> {entry.riskCategory}</span>
                    <span className="flex items-center gap-1"><Coins size={12} /> {entry.currencyCode}</span>
                  </div>
                </div>
                <div className="flex items-center gap-6">
                  <div className="text-right">
                    <div className="text-sm font-mono font-black text-white">{new Intl.NumberFormat('ko-KR').format(entry.currentAmount)}</div>
                    <div className="text-[10px] text-slate-500">Current Amount</div>
                  </div>
                  <button
                    onClick={() => handleDrillDown(entry)}
                    disabled={drillingEntryId === entry.entryId}
                    className="bg-slate-800 hover:bg-indigo-600 text-slate-300 hover:text-white w-10 h-10 rounded-full flex items-center justify-center transition-all disabled:opacity-50"
                  >
                    <Search size={16} />
                  </button>
                </div>
              </div>
            ))}
            {!mart && !loading && (
              <div className="p-10 text-center text-slate-500 text-sm font-medium">No mart data loaded.</div>
            )}
          </div>
        </section>

        {/* Drill-down 결과 (원천 전표) */}
        <section className="bg-slate-900 border border-indigo-500/30 rounded-[40px] p-10 relative overflow-hidden flex flex-col gap-6 shadow-2xl shadow-indigo-900/20">
          <div className="flex items-center gap-4 text-white">
            <Database size={24} className="text-indigo-400" />
            <h3 className="text-2xl font-black italic tracking-tighter uppercase">Source Journals</h3>
            {drillDownResults && (
              <span className="ml-auto bg-indigo-500 text-white text-xs font-black px-3 py-1 rounded-full">
                {drillDownResults.length} records
              </span>
            )}
          </div>

          <div className="flex flex-col gap-4 overflow-y-auto max-h-[600px] pr-2 custom-scrollbar">
            {drillingEntryId && (
              <div className="text-center text-indigo-400 text-sm py-10 animate-pulse">Tracing source data...</div>
            )}
            
            {!drillingEntryId && drillDownResults?.map((detail) => (
              <div key={detail.id} className="bg-slate-950 p-5 rounded-2xl border border-white/5 flex flex-col gap-3">
                <div className="flex justify-between items-start">
                  <div className="flex items-center gap-3">
                    <span className={`text-[10px] font-black px-2 py-1 rounded ${detail.side === 'DEBIT' ? 'bg-blue-500/20 text-blue-300' : 'bg-rose-500/20 text-rose-300'}`}>
                      {detail.side}
                    </span>
                    <span className="text-sm font-bold text-white">{detail.accountName} <span className="text-slate-500 text-xs font-normal">({detail.accountCode})</span></span>
                  </div>
                  <span className="text-sm font-mono font-black text-white">{new Intl.NumberFormat('ko-KR').format(detail.amount)}</span>
                </div>
                <div className="flex flex-col gap-1 mt-2">
                  <div className="text-xs text-slate-400">{detail.detailDescription || detail.headerDescription}</div>
                  <div className="text-[10px] font-mono text-slate-600 flex justify-between">
                    <span>Slip: {detail.slipNo}</span>
                    <span>Date: {detail.accountingDate}</span>
                  </div>
                </div>
              </div>
            ))}
            
            {!drillingEntryId && !drillDownResults && (
              <div className="p-20 flex flex-col items-center justify-center gap-4 text-slate-500">
                <Search size={48} className="opacity-20" />
                <p className="text-sm font-medium">Select a note entry to drill down into source journals.</p>
              </div>
            )}
          </div>
        </section>
      </div>
    </div>
  );
}
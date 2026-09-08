'use client';

import React, { useState, useSyncExternalStore } from 'react';
import { 
  Database, 
  Search, 
  Download, 
  Filter, 
  Code, 
  Layers, 
  Table as TableIcon, 
  RefreshCw,
  HardDrive,
  FileSpreadsheet,
  CheckCircle2
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import AmountDisplay from '@/components/ui/AmountDisplay';
import Tabs, { TabItem } from '@/components/ui/Tabs';
import EmptyState from '@/components/ui/EmptyState';
import { 
  mockMartDatasets, 
  mockExplorerRows, 
  MartDatasetDto 
} from '@/mocks/mart';

const emptySubscribe = () => () => {};

export default function MartExplorerPage() {
  const mounted = useSyncExternalStore(emptySubscribe, () => true, () => false);
  const [selectedDatasetId, setSelectedDatasetId] = useState<string>('DS-01');
  const [searchQuery, setSearchQuery] = useState('');
  const [activeView, setActiveView] = useState<'DATA' | 'SCHEMA'>('DATA');
  const [isExporting, setIsExporting] = useState(false);
  const [sqlQueryOpen, setSqlQueryOpen] = useState(false);

  const selectedDataset: MartDatasetDto = 
    mockMartDatasets.find(d => d.datasetId === selectedDatasetId) || mockMartDatasets[0];

  const datasetTabs: TabItem[] = mockMartDatasets.map(ds => ({
    id: ds.datasetId,
    label: ds.datasetName,
    icon: Database,
  }));

  const filteredRows = mockExplorerRows.filter(row => {
    if (!searchQuery) return true;
    const q = searchQuery.toLowerCase();
    return (
      row.JOURNAL_ID.toLowerCase().includes(q) ||
      row.ACCT_CODE.toLowerCase().includes(q) ||
      row.ACCT_NAME.toLowerCase().includes(q) ||
      row.REMARKS.toLowerCase().includes(q)
    );
  });

  const handleExport = () => {
    setIsExporting(true);
    setTimeout(() => {
      setIsExporting(false);
      alert(`[${selectedDataset.datasetName}] 데이터가 성공적으로 추출되었습니다. (CSV)`);
    }, 800);
  };

  if (!mounted) return null;

  return (
    <div className="space-y-8 pb-12">
      {/* Page Header */}
      <PageHeader
        title="마트 탐색기"
        description="금융 데이터마트 Tables 및 데이터 그리드 탐색, 데이터 스키마 확인 및 CSV 추출"
        breadcrumbs={[
          { label: '홈', href: '/' },
          { label: '데이터 마트' },
          { label: '마트 탐색기' },
        ]}
        icon={Database}
        actions={
          <div className="flex items-center gap-3">
            <button
              onClick={() => setSqlQueryOpen(!sqlQueryOpen)}
              className="px-4 py-2.5 rounded-xl text-xs font-black bg-white/5 text-slate-300 hover:text-white border border-white/10 hover:bg-white/10 transition-all flex items-center gap-2"
            >
              <Code size={14} className="text-blue-400" />
              {sqlQueryOpen ? 'SQL 미리보기 닫기' : 'SQL 미리보기'}
            </button>
            <button
              onClick={handleExport}
              disabled={isExporting}
              className="px-5 py-2.5 rounded-xl text-xs font-black bg-blue-600 hover:bg-blue-500 text-white shadow-lg shadow-blue-600/20 transition-all flex items-center gap-2 disabled:opacity-50"
            >
              <Download size={14} />
              {isExporting ? '추출 중...' : '데이터 EXPORT'}
            </button>
          </div>
        }
      />

      {/* SQL Preview Banner */}
      {sqlQueryOpen && (
        <div className="p-4 rounded-2xl bg-slate-950/80 border border-blue-500/20 font-mono text-xs text-blue-300 space-y-1">
          <div className="flex items-center justify-between text-slate-400 border-b border-white/5 pb-2 mb-2 font-sans text-xs">
            <span className="font-bold text-white flex items-center gap-2">
              <Code size={14} className="text-blue-400" />
              Generated SQL Query
            </span>
            <span className="text-[10px] text-slate-500">Read-Only View</span>
          </div>
          <p><span className="text-purple-400">SELECT</span> * <span className="text-purple-400">FROM</span> financial_mart.{selectedDataset.datasetName}</p>
          <p><span className="text-purple-400">WHERE</span> PARTITION_KEY = <span className="text-emerald-400">&apos;2026-07-28&apos;</span></p>
          <p><span className="text-purple-400">ORDER BY</span> POSTING_DATE <span className="text-purple-400">DESC</span> <span className="text-purple-400">LIMIT</span> 500;</p>
        </div>
      )}

      {/* Overview Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-5">
        <div className="p-5 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5">
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">선택 마트 테이블</span>
            <Database className="text-blue-400" size={18} />
          </div>
          <div className="text-xl font-black text-white truncate">{selectedDataset.datasetName}</div>
          <div className="text-xs text-slate-500 mt-1 flex items-center gap-2">
            <span>분류: {selectedDataset.category}</span>
          </div>
        </div>

        <div className="p-5 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5">
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">총 건수 (Rows)</span>
            <Layers className="text-emerald-400" size={18} />
          </div>
          <div className="text-2xl font-black text-white">{selectedDataset.rowCount.toLocaleString()} <span className="text-xs text-slate-500 font-normal">건</span></div>
          <div className="text-xs text-emerald-400 mt-1 flex items-center gap-1">
            <CheckCircle2 size={12} /> 파티션: {selectedDataset.partitionKey}
          </div>
        </div>

        <div className="p-5 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5">
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">컬럼 수 / 데이터 용량</span>
            <HardDrive className="text-purple-400" size={18} />
          </div>
          <div className="text-2xl font-black text-white">{selectedDataset.columnCount} <span className="text-sm font-medium text-slate-400">Cols</span> / {selectedDataset.sizeMb} <span className="text-sm font-medium text-slate-400">MB</span></div>
          <div className="text-xs text-slate-500 mt-1">
            컬럼 당 평균 크기 약 {(selectedDataset.sizeMb / selectedDataset.columnCount).toFixed(1)} MB
          </div>
        </div>

        <div className="p-5 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5">
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">최종 동기화 일시</span>
            <RefreshCw className="text-amber-400" size={18} />
          </div>
          <div className="text-lg font-black text-white truncate">{selectedDataset.lastRefreshed}</div>
          <div className="text-xs text-slate-500 mt-1 flex items-center gap-1">
            <StatusBadge status="정상 동기화" variant="success" />
          </div>
        </div>
      </div>

      {/* Dataset Selection Tabs */}
      <div className="space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <Tabs tabs={datasetTabs} activeTab={selectedDatasetId} onChange={setSelectedDatasetId} />

          <div className="flex items-center gap-2 p-1 bg-slate-900/50 border border-white/5 rounded-xl">
            <button
              onClick={() => setActiveView('DATA')}
              className={`px-4 py-1.5 rounded-lg text-xs font-bold transition-all ${
                activeView === 'DATA' ? 'bg-blue-600 text-white shadow' : 'text-slate-400 hover:text-white'
              }`}
            >
              <TableIcon size={14} className="inline mr-1.5" />
              데이터 그리드
            </button>
            <button
              onClick={() => setActiveView('SCHEMA')}
              className={`px-4 py-1.5 rounded-lg text-xs font-bold transition-all ${
                activeView === 'SCHEMA' ? 'bg-blue-600 text-white shadow' : 'text-slate-400 hover:text-white'
              }`}
            >
              <FileSpreadsheet size={14} className="inline mr-1.5" />
              테이블 스키마 ({selectedDataset.columns.length})
            </button>
          </div>
        </div>
      </div>

      {/* View Content */}
      {activeView === 'DATA' ? (
        <div className="p-6 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-6">
          {/* Controls Bar */}
          <div className="flex flex-col md:flex-row items-center justify-between gap-4">
            <div className="relative w-full md:w-80">
              <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500" size={16} />
              <input
                type="text"
                placeholder="전표번호, 계정코드, 적요 검색..."
                value={searchQuery}
                onChange={e => setSearchQuery(e.target.value)}
                className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-white/5 border border-white/10 text-white text-xs placeholder:text-slate-600 focus:outline-none focus:border-blue-500 transition-all"
              />
            </div>
            
            <div className="flex items-center gap-3 w-full md:w-auto justify-end">
              <span className="text-xs text-slate-500 font-medium">
                조회 결과: <strong className="text-white">{filteredRows.length}</strong> 건
              </span>
              <button className="p-2.5 rounded-xl bg-white/5 text-slate-400 hover:text-white border border-white/10">
                <Filter size={16} />
              </button>
            </div>
          </div>

          {/* Data Grid Table */}
          {filteredRows.length === 0 ? (
            <EmptyState
              icon={Search}
              title="검색 결과가 없습니다"
              description="입력한 키워드와 일치하는 마트 데이터 행이 존재하지 않습니다."
            />
          ) : (
            <div className="overflow-x-auto rounded-xl border border-white/5">
              <table className="w-full text-left text-xs border-collapse">
                <thead>
                  <tr className="bg-white/5 border-b border-white/10 text-slate-400 font-black uppercase tracking-wider">
                    <th className="py-3.5 px-4">#</th>
                    <th className="py-3.5 px-4">회계일자</th>
                    <th className="py-3.5 px-4">전표 식별번호</th>
                    <th className="py-3.5 px-4">계정코드</th>
                    <th className="py-3.5 px-4">계정과목명</th>
                    <th className="py-3.5 px-4 text-right">차변금액</th>
                    <th className="py-3.5 px-4 text-right">대변금액</th>
                    <th className="py-3.5 px-4">통화</th>
                    <th className="py-3.5 px-4">부서코드</th>
                    <th className="py-3.5 px-4">적요</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/5 text-slate-300">
                  {filteredRows.map((row) => (
                    <tr key={row.id} className="hover:bg-white/[0.02] transition-colors">
                      <td className="py-3 px-4 font-mono text-slate-500">{row.id}</td>
                      <td className="py-3 px-4 font-mono text-slate-400">{row.POSTING_DATE}</td>
                      <td className="py-3 px-4 font-mono font-bold text-blue-400">{row.JOURNAL_ID}</td>
                      <td className="py-3 px-4 font-mono text-slate-300">{row.ACCT_CODE}</td>
                      <td className="py-3 px-4 font-bold text-white">{row.ACCT_NAME}</td>
                      <td className="py-3 px-4 text-right">
                        {row.DEBIT_AMT > 0 ? (
                          <AmountDisplay amount={row.DEBIT_AMT} />
                        ) : (
                          <span className="text-slate-600 font-mono">-</span>
                        )}
                      </td>
                      <td className="py-3 px-4 text-right">
                        {row.CREDIT_AMT > 0 ? (
                          <AmountDisplay amount={row.CREDIT_AMT} />
                        ) : (
                          <span className="text-slate-600 font-mono">-</span>
                        )}
                      </td>
                      <td className="py-3 px-4">
                        <span className="px-2 py-0.5 rounded bg-slate-800 text-slate-400 font-mono text-[11px]">
                          {row.CURRENCY}
                        </span>
                      </td>
                      <td className="py-3 px-4 font-mono text-slate-400">{row.DEPT_CODE}</td>
                      <td className="py-3 px-4 text-slate-400 max-w-xs truncate">{row.REMARKS}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {/* Pagination Footer */}
          <div className="flex items-center justify-between text-xs text-slate-500 pt-2 border-t border-white/5">
            <div>Page 1 of 120 (Top 500 records preview)</div>
            <div className="flex items-center gap-2">
              <button disabled className="px-3 py-1.5 rounded-lg bg-white/5 text-slate-600 cursor-not-allowed">이전</button>
              <button className="px-3 py-1.5 rounded-lg bg-white/5 text-slate-300 hover:bg-white/10">다음</button>
            </div>
          </div>
        </div>
      ) : (
        /* Schema View */
        <div className="p-6 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-6">
          <div className="flex items-center justify-between border-b border-white/5 pb-4">
            <div>
              <h3 className="text-base font-black text-white flex items-center gap-2">
                <FileSpreadsheet className="text-purple-400" size={18} />
                {selectedDataset.datasetName} 메타데이터 스키마
              </h3>
              <p className="text-xs text-slate-400 mt-1">테이블 컬럼 타입, Null 여부 및 비즈니스 정의 설명</p>
            </div>
            <StatusBadge status={`파티션: ${selectedDataset.partitionKey}`} variant="info" />
          </div>

          <div className="overflow-x-auto rounded-xl border border-white/5">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="bg-white/5 border-b border-white/10 text-slate-400 font-black uppercase tracking-wider">
                  <th className="py-3.5 px-4">#</th>
                  <th className="py-3.5 px-4">컬럼명 (Column Name)</th>
                  <th className="py-3.5 px-4">데이터 타입 (Data Type)</th>
                  <th className="py-3.5 px-4">Null 허용</th>
                  <th className="py-3.5 px-4">컬럼 설명 (Description)</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5 text-slate-300">
                {selectedDataset.columns.map((col, idx) => (
                  <tr key={col.name} className="hover:bg-white/[0.02]">
                    <td className="py-3 px-4 font-mono text-slate-500">{idx + 1}</td>
                    <td className="py-3 px-4 font-mono font-bold text-blue-400">{col.name}</td>
                    <td className="py-3 px-4 font-mono text-emerald-400">{col.type}</td>
                    <td className="py-3 px-4">
                      {col.nullable ? (
                        <span className="text-amber-400 font-medium">NULL</span>
                      ) : (
                        <span className="text-slate-500 font-medium">NOT NULL</span>
                      )}
                    </td>
                    <td className="py-3 px-4 text-slate-300">{col.description}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}

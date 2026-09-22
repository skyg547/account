"use client";

import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  FileText,
  Download,
  Search,
  ChevronRight,
  ChevronDown,
  Maximize2,
  Minimize2,
  TrendingUp,
  Building2,
  DollarSign,
  PieChart,
  RefreshCw,
  FileSpreadsheet
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import AmountDisplay from '@/components/ui/AmountDisplay';
import StatusBadge from '@/components/ui/StatusBadge';
import Tabs from '@/components/ui/Tabs';
import EmptyState from '@/components/ui/EmptyState';
import LoadingSkeleton from '@/components/ui/LoadingSkeleton';
import { mockStatements, FinancialStatementDto as StatementRow } from '@/mocks/reporting';
import {
  FinancialStatementDto as ApiFinancialStatement,
  ReportLineDto,
  StatementType,
  reportingService,
} from '@/services/reportingService';

const STATEMENT_TYPES: StatementType[] = ['BALANCE_SHEET', 'INCOME_STATEMENT'];

const CATEGORY_BY_NOTE: Record<string, string> = {
  AST: '자산',
  LIA: '부채',
  EQU: '자본',
  REV: '수익',
  EXP: '비용',
};

const CATEGORY_ROOT_CODES: Partial<Record<StatementType, Record<string, string>>> = {
  BALANCE_SHEET: {
    자산: 'BS_TOTAL_ASSETS',
    부채: 'BS_TOTAL_LIABILITIES',
    자본: 'BS_TOTAL_EQUITY',
  },
  INCOME_STATEMENT: {
    수익: 'IS_TOTAL_REVENUE',
    비용: 'IS_TOTAL_EXPENSE',
    이익: 'IS_NET_INCOME',
  },
};

const CATEGORY_ORDER: Record<StatementType, string[]> = {
  BALANCE_SHEET: ['자산', '부채', '자본'],
  INCOME_STATEMENT: ['수익', '비용', '이익'],
};

function inferCategory(type: StatementType, line: ReportLineDto): string {
  if (line.category) return line.category;
  if (line.noteNumber && CATEGORY_BY_NOTE[line.noteNumber]) return CATEGORY_BY_NOTE[line.noteNumber];

  const searchableText = `${line.lineCode} ${line.label}`.toUpperCase();
  if (type === 'BALANCE_SHEET') {
    if (searchableText.includes('ASSET') || line.label.includes('자산')) return '자산';
    if (searchableText.includes('LIABILIT') || line.label.includes('부채')) return '부채';
    return '자본';
  }

  if (searchableText.includes('NET_INCOME') || line.label.includes('이익')) return '이익';
  if (searchableText.includes('EXPENSE') || line.label.includes('비용')) return '비용';
  return '수익';
}

function adaptStatement(statement: ApiFinancialStatement): StatementRow[] {
  const rootCodes = CATEGORY_ROOT_CODES[statement.type] ?? {};
  const hasMockHierarchy = statement.lines.some(
    (line) => line.category !== undefined || line.parentLineCode !== undefined
  );
  const rows = statement.lines.map((line) => {
    const category = inferCategory(statement.type, line);
    const categoryRoot = rootCodes[category];
    const isCategoryRoot = line.lineCode === categoryRoot;

    return {
      accountCode: line.lineCode,
      accountName: line.label,
      amountCurrent: line.currentAmount,
      amountPrevious: line.previousAmount,
      variance: line.currentAmount - line.previousAmount,
      statementType: statement.type === 'BALANCE_SHEET' ? 'BS' as const : 'IS' as const,
      category,
      // API level은 1부터 시작하고, 화면 level은 0부터 시작합니다.
      level: hasMockHierarchy ? line.level : Math.max(line.level - 1, 0),
      parentId: hasMockHierarchy
        ? line.parentLineCode ?? null
        : isCategoryRoot
          ? null
          : categoryRoot ?? null,
    };
  });

  if (hasMockHierarchy) return rows;

  // Core 응답은 상세 라인 뒤에 합계가 오므로, 카테고리 합계를 먼저 배치해 트리 계층을 유지합니다.
  const orderedRows = CATEGORY_ORDER[statement.type].flatMap((category) => {
    const categoryRows = rows.filter((row) => row.category === category);
    const categoryRoot = rootCodes[category];
    return [
      ...categoryRows.filter((row) => row.accountCode === categoryRoot),
      ...categoryRows.filter((row) => row.accountCode !== categoryRoot),
    ];
  });
  const orderedCodes = new Set(orderedRows.map((row) => row.accountCode));
  return [...orderedRows, ...rows.filter((row) => !orderedCodes.has(row.accountCode))];
}

export default function FinancialStatementsPage() {
  const [activeTab, setActiveTab] = useState<string>('BS');
  const [searchTerm, setSearchTerm] = useState<string>('');
  const [selectedPeriod, setSelectedPeriod] = useState<string>('2026-Q2');
  const [collapsedNodes, setCollapsedNodes] = useState<Record<string, boolean>>({});
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [isOfflineMock, setIsOfflineMock] = useState<boolean>(false);
  const [statements, setStatements] = useState<StatementRow[]>(mockStatements);
  const [exportingFormat, setExportingFormat] = useState<'PDF' | 'EXCEL' | null>(null);
  const [notification, setNotification] = useState<string | null>(null);
  const requestIdRef = useRef(0);

  const tabs = [
    { id: 'BS', label: '재무상태표 (BS)', icon: Building2 },
    { id: 'IS', label: '손익계산서 (IS)', icon: PieChart },
    { id: 'ALL', label: '통합 재무제표', icon: FileText },
  ];

  const kpiSummary = useMemo(() => {
    const totalAssets = statements.find(s => ['1000000', 'BS_TOTAL_ASSETS'].includes(s.accountCode))?.amountCurrent || 0;
    const prevAssets = statements.find(s => ['1000000', 'BS_TOTAL_ASSETS'].includes(s.accountCode))?.amountPrevious || 0;
    const assetVar = totalAssets - prevAssets;

    const totalLiab = statements.find(s => ['2000000', 'BS_TOTAL_LIABILITIES'].includes(s.accountCode))?.amountCurrent || 0;
    
    const totalEquity = statements.find(s => ['3000000', 'BS_TOTAL_EQUITY'].includes(s.accountCode))?.amountCurrent || 0;
    
    const netIncome = statements.find(s => ['9900000', 'IS_NET_INCOME'].includes(s.accountCode))?.amountCurrent || 0;
    const prevIncome = statements.find(s => ['9900000', 'IS_NET_INCOME'].includes(s.accountCode))?.amountPrevious || 0;
    const incomeVar = netIncome - prevIncome;

    return { totalAssets, assetVar, totalLiab, totalEquity, netIncome, incomeVar };
  }, [statements]);

  // Filter statements based on tab and search
  const filteredStatements = useMemo(() => {
    return statements.filter(item => {
      // Tab filter
      if (activeTab === 'BS' && item.statementType !== 'BS') return false;
      if (activeTab === 'IS' && item.statementType !== 'IS') return false;

      // Search term filter
      if (searchTerm.trim()) {
        const query = searchTerm.toLowerCase();
        const matchesCode = item.accountCode.toLowerCase().includes(query);
        const matchesName = item.accountName.toLowerCase().includes(query);
        const matchesCategory = item.category?.toLowerCase().includes(query);
        return matchesCode || matchesName || matchesCategory;
      }
      return true;
    });
  }, [activeTab, searchTerm, statements]);

  // Determine child existence for collapse toggle
  const hasChildrenMap = useMemo(() => {
    const map = new Map<string, boolean>();
    statements.forEach(item => {
      if (item.parentId) {
        map.set(item.parentId, true);
      }
    });
    return map;
  }, [statements]);

  const loadStatements = useCallback(async () => {
    const requestId = ++requestIdRef.current;
    setIsLoading(true);

    try {
      const loadedStatements = await Promise.all(
        STATEMENT_TYPES.map((type) => reportingService.generateStatement(type, selectedPeriod))
      );
      if (requestId !== requestIdRef.current) return;

      setStatements(loadedStatements.flatMap(adaptStatement));
      setIsOfflineMock(loadedStatements.some((statement) => statement.dataSource === 'MOCK'));
      setCollapsedNodes({});
    } catch (error) {
      console.warn('Unable to load reporting statements, using mock data:', error);
      if (requestId !== requestIdRef.current) return;
      setStatements(mockStatements);
      setIsOfflineMock(true);
    } finally {
      if (requestId === requestIdRef.current) setIsLoading(false);
    }
  }, [selectedPeriod]);

  useEffect(() => {
    void loadStatements();
    return () => {
      requestIdRef.current += 1;
    };
  }, [loadStatements]);

  const toggleNode = (code: string) => {
    setCollapsedNodes(prev => ({
      ...prev,
      [code]: !prev[code],
    }));
  };

  const expandAll = () => setCollapsedNodes({});
  const collapseAll = () => {
    const next: Record<string, boolean> = {};
    statements.forEach(s => {
      if (hasChildrenMap.get(s.accountCode)) {
        next[s.accountCode] = true;
      }
    });
    setCollapsedNodes(next);
  };

  const isRowVisible = (item: StatementRow): boolean => {
    if (searchTerm.trim()) return true; // When searching, display flat matches
    let currParentId = item.parentId;
    while (currParentId) {
      if (collapsedNodes[currParentId]) return false;
      const parentObj = statements.find(s => s.accountCode === currParentId);
      currParentId = parentObj ? parentObj.parentId ?? null : null;
    }
    return true;
  };

  const handleExport = async (format: 'PDF' | 'EXCEL') => {
    const exportTypes = activeTab === 'BS'
      ? ['BALANCE_SHEET' as const]
      : activeTab === 'IS'
        ? ['INCOME_STATEMENT' as const]
        : STATEMENT_TYPES;

    setExportingFormat(format);
    try {
      const results = await Promise.all(
        exportTypes.map((type) => reportingService.exportDocument(type, selectedPeriod, format))
      );
      if (results.every((result) => result === 'DOWNLOADED')) {
        setNotification(`${activeTab === 'BS' ? '재무상태표' : activeTab === 'IS' ? '손익계산서' : '통합재무제표'} (${selectedPeriod}) ${format} 내보내기가 완료되었습니다.`);
        setTimeout(() => setNotification(null), 4000);
      }
    } finally {
      setExportingFormat(null);
    }
  };

  return (
    <div className="space-y-8 pb-12">
      {/* Toast Notification */}
      {notification && (
        <div className="fixed top-5 right-5 z-50 bg-emerald-500/20 border border-emerald-500/40 text-emerald-300 px-6 py-4 rounded-2xl backdrop-blur-xl shadow-2xl flex items-center gap-3 animate-fade-in">
          <FileSpreadsheet className="w-5 h-5 text-emerald-400" />
          <span className="text-sm font-bold">{notification}</span>
        </div>
      )}

      {/* Page Header */}
      <PageHeader
        title="재무제표 조회"
        description="전사 재무상태표(BS) 및 손익계산서(IS) 계층별 통합 조회 및 전기 대비 실적 비교 분석"
        breadcrumbs={[
          { label: '보고서 관리' },
          { label: '재무제표 조회' },
        ]}
        icon={FileText}
        actions={
          <div className="flex items-center gap-3">
            <button
              onClick={() => void handleExport('PDF')}
              disabled={exportingFormat !== null}
              className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-slate-900/60 hover:bg-slate-800 text-slate-300 border border-white/10 text-xs font-black uppercase tracking-wider transition-all duration-200 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <Download size={15} className="text-rose-400" />
              {exportingFormat === 'PDF' ? '내보내는 중...' : 'PDF 내보내기'}
            </button>
            <button
              onClick={() => void handleExport('EXCEL')}
              disabled={exportingFormat !== null}
              className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-blue-600/20 hover:bg-blue-600/30 text-blue-300 border border-blue-500/30 text-xs font-black uppercase tracking-wider transition-all duration-200 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <FileSpreadsheet size={15} className="text-blue-400" />
              {exportingFormat === 'EXCEL' ? '내보내는 중...' : 'Excel 내보내기'}
            </button>
            <button
              onClick={() => void loadStatements()}
              disabled={isLoading}
              className="p-2.5 rounded-xl bg-white/5 hover:bg-white/10 text-slate-400 hover:text-white border border-white/5 transition-all disabled:cursor-not-allowed disabled:opacity-50"
              title="새로고침"
            >
              <RefreshCw size={16} className={isLoading ? 'animate-spin' : ''} />
            </button>
          </div>
        }
      />

      {isOfflineMock && (
        <div className="inline-flex items-center gap-2 rounded-full border border-amber-500/30 bg-amber-500/10 px-3 py-1.5 text-xs font-bold text-amber-300">
          <span className="h-2 w-2 rounded-full bg-amber-400" />
          오프라인 Mock 데이터 표시 중
        </div>
      )}

      {/* Summary KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
        <div className="p-6 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 relative overflow-hidden group hover:border-blue-500/30 transition-all">
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-widest">자산 총계</span>
            <div className="w-9 h-9 rounded-xl bg-blue-500/10 border border-blue-500/20 flex items-center justify-center text-blue-400">
              <Building2 size={18} />
            </div>
          </div>
          <div className="text-2xl font-black text-white tracking-tight mb-2">
            <AmountDisplay amount={kpiSummary.totalAssets} />
          </div>
          <div className="flex items-center gap-2 text-xs">
            <span className="flex items-center font-bold text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded-md border border-emerald-500/20">
              <TrendingUp size={12} className="mr-1" /> +{((kpiSummary.assetVar / (kpiSummary.totalAssets - kpiSummary.assetVar)) * 100).toFixed(1)}%
            </span>
            <span className="text-slate-500">전기 대비</span>
          </div>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 relative overflow-hidden group hover:border-amber-500/30 transition-all">
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-widest">부채 총계</span>
            <div className="w-9 h-9 rounded-xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-center text-amber-400">
              <DollarSign size={18} />
            </div>
          </div>
          <div className="text-2xl font-black text-white tracking-tight mb-2">
            <AmountDisplay amount={kpiSummary.totalLiab} />
          </div>
          <div className="flex items-center gap-2 text-xs">
            <StatusBadge status="안정적 부채 비율" variant="info" />
            <span className="text-slate-500">자산 대비 42.1%</span>
          </div>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 relative overflow-hidden group hover:border-indigo-500/30 transition-all">
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-widest">자본 총계</span>
            <div className="w-9 h-9 rounded-xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center text-indigo-400">
              <PieChart size={18} />
            </div>
          </div>
          <div className="text-2xl font-black text-white tracking-tight mb-2">
            <AmountDisplay amount={kpiSummary.totalEquity} />
          </div>
          <div className="flex items-center gap-2 text-xs">
            <StatusBadge status="자기자본 충실" variant="success" />
            <span className="text-slate-500">전기 대비 +13.5%</span>
          </div>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 relative overflow-hidden group hover:border-emerald-500/30 transition-all">
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-widest">당기 순이익</span>
            <div className="w-9 h-9 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
              <TrendingUp size={18} />
            </div>
          </div>
          <div className="text-2xl font-black text-emerald-400 tracking-tight mb-2">
            <AmountDisplay amount={kpiSummary.netIncome} />
          </div>
          <div className="flex items-center gap-2 text-xs">
            <span className="flex items-center font-bold text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded-md border border-emerald-500/20">
              <TrendingUp size={12} className="mr-1" /> +31.3%
            </span>
            <span className="text-slate-500">순이익 성장</span>
          </div>
        </div>
      </div>

      {/* Filter and Controls Toolbar */}
      <div className="p-4 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 flex flex-col md:flex-row items-center justify-between gap-4">
        {/* Statement Type Tabs */}
        <Tabs tabs={tabs} activeTab={activeTab} onChange={setActiveTab} />

        {/* Right side controls */}
        <div className="flex items-center gap-3 w-full md:w-auto">
          {/* Period selector */}
          <select
            value={selectedPeriod}
            onChange={e => setSelectedPeriod(e.target.value)}
            className="px-4 py-2.5 bg-slate-950/80 border border-white/10 rounded-xl text-xs font-bold text-white outline-none focus:border-blue-500 transition-colors"
          >
            <option value="2026-Q2">2026년 2분기 (당기) vs 2025년 2분기 (전기)</option>
            <option value="2026-Q1">2026년 1분기 (당기) vs 2025년 1분기 (전기)</option>
            <option value="2025-FY">2025년 연간 결산 vs 2024년 연간 결산</option>
          </select>

          {/* Search bar */}
          <div className="relative flex-1 md:w-64">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500" size={15} />
            <input
              type="text"
              placeholder="계정코드 또는 계정명 검색..."
              value={searchTerm}
              onChange={e => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2.5 bg-slate-950/80 border border-white/10 rounded-xl text-xs font-medium text-white placeholder-slate-500 outline-none focus:border-blue-500 transition-colors"
            />
          </div>

          {/* Expand/Collapse buttons */}
          <div className="flex items-center gap-1 bg-white/5 p-1 rounded-xl border border-white/5">
            <button
              onClick={expandAll}
              className="p-2 rounded-lg text-slate-400 hover:text-white hover:bg-white/10 transition-all text-xs font-bold flex items-center gap-1"
              title="모두 펼치기"
            >
              <Maximize2 size={14} />
            </button>
            <button
              onClick={collapseAll}
              className="p-2 rounded-lg text-slate-400 hover:text-white hover:bg-white/10 transition-all text-xs font-bold flex items-center gap-1"
              title="모두 접기"
            >
              <Minimize2 size={14} />
            </button>
          </div>
        </div>
      </div>

      {/* Main Tree Grid View */}
      {isLoading ? (
        <LoadingSkeleton rows={8} height="h-16" />
      ) : filteredStatements.length === 0 ? (
        <EmptyState
          icon={Search}
          title="검색 결과가 없습니다"
          description={`'${searchTerm}'에 부합하는 계정 항목이 존재하지 않습니다.`}
          action={
            <button
              onClick={() => setSearchTerm('')}
              className="px-4 py-2 rounded-xl bg-blue-600 text-white text-xs font-bold hover:bg-blue-500 transition-all"
            >
              검색 조건 초기화
            </button>
          }
        />
      ) : (
        <div className="rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-white/10 bg-white/[0.02] text-[11px] font-black text-slate-400 uppercase tracking-widest">
                  <th className="py-4 px-6 w-5/12">계정과목 (Account Item)</th>
                  <th className="py-4 px-4 w-2/12 text-center">계정 구분</th>
                  <th className="py-4 px-4 w-2/12 text-right">당기 금액 (Current)</th>
                  <th className="py-4 px-4 w-2/12 text-right">전기 금액 (Previous)</th>
                  <th className="py-4 px-4 w-2/12 text-right">증감액 (Variance)</th>
                  <th className="py-4 px-4 w-1/12 text-right">증감률</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {filteredStatements.map(item => {
                  if (!isRowVisible(item)) return null;

                  const hasChildren = hasChildrenMap.get(item.accountCode);
                  const isCollapsed = collapsedNodes[item.accountCode];
                  const level = item.level ?? 0;
                  const variancePct = item.amountPrevious !== 0 
                    ? ((item.variance / Math.abs(item.amountPrevious)) * 100).toFixed(1)
                    : '0.0';

                  // Dynamic row styling based on tree level
                  const isHeaderLevel = level === 0;
                  const isSubHeaderLevel = level === 1;

                  let indentPadding = 'pl-6';
                  if (level === 1) indentPadding = 'pl-12';
                  if (level === 2) indentPadding = 'pl-18';
                  if (level === 3) indentPadding = 'pl-24';

                  return (
                    <tr
                      key={item.accountCode}
                      className={`group transition-colors duration-150 hover:bg-white/[0.03] ${
                        isHeaderLevel 
                          ? 'bg-white/[0.04] font-black text-white' 
                          : isSubHeaderLevel 
                          ? 'bg-white/[0.015] font-bold text-slate-200' 
                          : 'text-slate-300 font-medium'
                      }`}
                    >
                      {/* Account Code & Name with indentation & toggle button */}
                      <td className={`py-3.5 pr-4 ${indentPadding}`}>
                        <div className="flex items-center gap-2.5">
                          {hasChildren ? (
                            <button
                              onClick={() => toggleNode(item.accountCode)}
                              className="w-5 h-5 rounded flex items-center justify-center bg-white/5 hover:bg-white/10 text-slate-400 hover:text-white transition-all"
                            >
                              {isCollapsed ? <ChevronRight size={14} /> : <ChevronDown size={14} />}
                            </button>
                          ) : (
                            <span className="w-5 h-5 inline-block" />
                          )}
                          <span className="font-mono text-xs text-slate-500 font-semibold">
                            {item.accountCode}
                          </span>
                          <span className={`${isHeaderLevel ? 'text-base font-extrabold text-white' : isSubHeaderLevel ? 'text-sm font-bold text-slate-100' : 'text-xs text-slate-300'}`}>
                            {item.accountName}
                          </span>
                        </div>
                      </td>

                      {/* Category Badge */}
                      <td className="py-3.5 px-4 text-center">
                        {item.category && (
                          <span className={`inline-flex px-2.5 py-0.5 rounded-full text-[10px] font-bold ${
                            item.category === '자산' ? 'bg-blue-500/10 text-blue-400 border border-blue-500/20' :
                            item.category === '부채' ? 'bg-amber-500/10 text-amber-400 border border-amber-500/20' :
                            item.category === '자본' ? 'bg-indigo-500/10 text-indigo-400 border border-indigo-500/20' :
                            item.category === '수익' ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' :
                            'bg-rose-500/10 text-rose-400 border border-rose-500/20'
                          }`}>
                            {item.category}
                          </span>
                        )}
                      </td>

                      {/* Current Period Amount */}
                      <td className="py-3.5 px-4 text-right">
                        <AmountDisplay
                          amount={item.amountCurrent}
                          className={isHeaderLevel ? 'text-base text-white font-extrabold' : 'text-xs'}
                        />
                      </td>

                      {/* Previous Period Amount */}
                      <td className="py-3.5 px-4 text-right">
                        <AmountDisplay
                          amount={item.amountPrevious}
                          className={isHeaderLevel ? 'text-base text-slate-400 font-bold' : 'text-xs text-slate-400'}
                        />
                      </td>

                      {/* Variance Amount */}
                      <td className="py-3.5 px-4 text-right">
                        <AmountDisplay
                          amount={item.variance}
                          showSign
                          className={`text-xs ${item.variance > 0 ? 'text-emerald-400' : item.variance < 0 ? 'text-rose-400' : 'text-slate-400'}`}
                        />
                      </td>

                      {/* Variance % */}
                      <td className="py-3.5 px-4 text-right">
                        <span className={`inline-flex items-center text-xs font-mono font-bold ${
                          item.variance > 0 ? 'text-emerald-400' : item.variance < 0 ? 'text-rose-400' : 'text-slate-400'
                        }`}>
                          {item.variance > 0 ? '+' : ''}{variancePct}%
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
          <div className="p-4 bg-white/[0.01] border-t border-white/5 flex items-center justify-between text-xs text-slate-400">
            <span>총 {filteredStatements.length}개 계정 항목 표시 중</span>
            <span>기준 기간: {selectedPeriod} 결산 기준 (단위: 원)</span>
          </div>
        </div>
      )}
    </div>
  );
}

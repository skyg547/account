"use client";

import React, { useState, useMemo } from 'react';
import {
  Download,
  FileText,
  FileSpreadsheet,
  History,
  Calendar,
  Clock,
  Search,
  ArrowDownToLine,
  Sparkles
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import Tabs from '@/components/ui/Tabs';
import EmptyState from '@/components/ui/EmptyState';import { mockExportHistory, ReportExportHistoryDto } from '@/mocks/reporting';

export default function ReportExportPage() {
  const [activeTab, setActiveTab] = useState<string>('ALL');
  const [searchTerm, setSearchTerm] = useState<string>('');
  const [selectedFormat, setSelectedFormat] = useState<string>('ALL');
  const [historyList, setHistoryList] = useState<ReportExportHistoryDto[]>(mockExportHistory);
  const [isGenerating, setIsGenerating] = useState<boolean>(false);
  const [notification, setNotification] = useState<string | null>(null);

  const tabs = [
    { id: 'ALL', label: '전체 이력' },
    { id: 'PDF', label: 'PDF 문서' },
    { id: 'EXCEL', label: 'Excel 스프레드시트' },
    { id: 'COMPLETED', label: '완료 항목' },
  ];

  const filteredHistory = useMemo(() => {
    return historyList.filter(item => {
      // Tab filter
      if (activeTab === 'PDF' && item.format !== 'PDF') return false;
      if (activeTab === 'EXCEL' && item.format !== 'EXCEL') return false;
      if (activeTab === 'COMPLETED' && item.status !== 'COMPLETED') return false;

      // Format dropdown filter
      if (selectedFormat !== 'ALL' && item.format !== selectedFormat) return false;

      // Search term
      if (searchTerm.trim()) {
        const query = searchTerm.toLowerCase();
        return (
          item.reportName.toLowerCase().includes(query) ||
          item.createdBy.toLowerCase().includes(query) ||
          item.id.toLowerCase().includes(query)
        );
      }
      return true;
    });
  }, [historyList, activeTab, selectedFormat, searchTerm]);

  const handleGenerateReport = (reportTitle: string, format: 'PDF' | 'EXCEL') => {
    setIsGenerating(true);
    const newId = `EXP-${new Date().toISOString().slice(0,10).replace(/-/g,'')}-${Math.floor(100 + Math.random() * 900)}`;
    const nowStr = new Date().toISOString().replace('T', ' ').slice(0, 19);

    setTimeout(() => {
      const newEntry: ReportExportHistoryDto = {
        id: newId,
        reportName: reportTitle,
        reportType: reportTitle.includes('BS') ? 'BS' : reportTitle.includes('IS') ? 'IS' : 'REGULATORY',
        format,
        period: '2026 Q2',
        createdAt: nowStr,
        createdBy: '현재 사용자 (나)',
        fileSize: format === 'PDF' ? '3.1 MB' : '5.4 MB',
        status: 'COMPLETED',
        downloadUrl: '#',
      };
      setHistoryList(prev => [newEntry, ...prev]);
      setIsGenerating(false);
      setNotification(`'${reportTitle}' (${format}) 보고서 생성이 완료되었습니다.`);
      setTimeout(() => setNotification(null), 4000);
    }, 1000);
  };

  const handleDownload = (item: ReportExportHistoryDto) => {
    setNotification(`'${item.reportName}' 파일 다운로드를 시작합니다.`);
    setTimeout(() => setNotification(null), 3000);
  };

  return (
    <div className="space-y-8 pb-12">
      {/* Toast Notification */}
      {notification && (
        <div className="fixed top-5 right-5 z-50 bg-blue-600/20 border border-blue-500/40 text-blue-200 px-6 py-4 rounded-2xl backdrop-blur-xl shadow-2xl flex items-center gap-3 animate-fade-in">
          <Sparkles className="w-5 h-5 text-blue-400" />
          <span className="text-sm font-bold">{notification}</span>
        </div>
      )}

      {/* Page Header */}
      <PageHeader
        title="보고서 내보내기"
        description="재무상태표, 손익계산서, 현금흐름표 및 각종 결산 보고서의 PDF/Excel 맞춤형 내보내기 및 다운로드 이력 관리"
        breadcrumbs={[
          { label: '보고서 관리' },
          { label: '보고서 내보내기' },
        ]}
        icon={Download}
        actions={
          <div className="flex items-center gap-3">
            <span className="text-xs text-slate-400 font-medium">총 내보내기 건수: <strong className="text-white">{historyList.length}건</strong></span>
          </div>
        }
      />

      {/* Quick Export Cards Section */}
      <div>
        <h3 className="text-sm font-black text-slate-400 uppercase tracking-widest mb-4 flex items-center gap-2">
          <Sparkles size={16} className="text-blue-400" />
          신규 보고서 실시간 생성 및 내보내기
        </h3>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
          {/* Card 1: BS */}
          <div className="p-6 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 hover:border-blue-500/30 transition-all flex flex-col justify-between group">
            <div>
              <div className="flex items-center justify-between mb-4">
                <span className="text-[10px] font-black tracking-wider uppercase px-2.5 py-1 rounded-md bg-blue-500/10 text-blue-400 border border-blue-500/20">
                  재무상태표 (BS)
                </span>
                <FileText className="text-slate-500 group-hover:text-blue-400 transition-colors" size={20} />
              </div>
              <h4 className="text-lg font-extrabold text-white tracking-tight mb-2">2026년 2분기 재무상태표</h4>
              <p className="text-xs text-slate-400 font-medium mb-6">
                자산, 부채, 자본 계정의 전기 대비 세부 구성 및 세부 주석 통합 표기
              </p>
            </div>
            <div className="flex items-center gap-2 pt-4 border-t border-white/5">
              <button
                disabled={isGenerating}
                onClick={() => handleGenerateReport('2026년 2분기 재무상태표', 'PDF')}
                className="flex-1 py-2 px-3 rounded-xl bg-slate-800 hover:bg-slate-700 text-rose-300 border border-white/10 text-xs font-bold flex items-center justify-center gap-1.5 transition-all"
              >
                <FileText size={14} /> PDF
              </button>
              <button
                disabled={isGenerating}
                onClick={() => handleGenerateReport('2026년 2분기 재무상태표', 'EXCEL')}
                className="flex-1 py-2 px-3 rounded-xl bg-emerald-600/20 hover:bg-emerald-600/30 text-emerald-300 border border-emerald-500/30 text-xs font-bold flex items-center justify-center gap-1.5 transition-all"
              >
                <FileSpreadsheet size={14} /> Excel
              </button>
            </div>
          </div>

          {/* Card 2: IS */}
          <div className="p-6 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 hover:border-emerald-500/30 transition-all flex flex-col justify-between group">
            <div>
              <div className="flex items-center justify-between mb-4">
                <span className="text-[10px] font-black tracking-wider uppercase px-2.5 py-1 rounded-md bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                  손익계산서 (IS)
                </span>
                <FileSpreadsheet className="text-slate-500 group-hover:text-emerald-400 transition-colors" size={20} />
              </div>
              <h4 className="text-lg font-extrabold text-white tracking-tight mb-2">2026년 2분기 손익계산서</h4>
              <p className="text-xs text-slate-400 font-medium mb-6">
                매출액, 매출원가, 판관비 및 영업이익 시계열 실적 추이 상세 분석 데이터
              </p>
            </div>
            <div className="flex items-center gap-2 pt-4 border-t border-white/5">
              <button
                disabled={isGenerating}
                onClick={() => handleGenerateReport('2026년 2분기 손익계산서', 'PDF')}
                className="flex-1 py-2 px-3 rounded-xl bg-slate-800 hover:bg-slate-700 text-rose-300 border border-white/10 text-xs font-bold flex items-center justify-center gap-1.5 transition-all"
              >
                <FileText size={14} /> PDF
              </button>
              <button
                disabled={isGenerating}
                onClick={() => handleGenerateReport('2026년 2분기 손익계산서', 'EXCEL')}
                className="flex-1 py-2 px-3 rounded-xl bg-emerald-600/20 hover:bg-emerald-600/30 text-emerald-300 border border-emerald-500/30 text-xs font-bold flex items-center justify-center gap-1.5 transition-all"
              >
                <FileSpreadsheet size={14} /> Excel
              </button>
            </div>
          </div>

          {/* Card 3: CASH FLOW */}
          <div className="p-6 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 hover:border-indigo-500/30 transition-all flex flex-col justify-between group">
            <div>
              <div className="flex items-center justify-between mb-4">
                <span className="text-[10px] font-black tracking-wider uppercase px-2.5 py-1 rounded-md bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
                  현금흐름표
                </span>
                <History className="text-slate-500 group-hover:text-indigo-400 transition-colors" size={20} />
              </div>
              <h4 className="text-lg font-extrabold text-white tracking-tight mb-2">2026년 상반기 현금흐름표</h4>
              <p className="text-xs text-slate-400 font-medium mb-6">
                영업활동, 투자활동, 재무활동 현금흐름 요약 및 순현금 변동 분석
              </p>
            </div>
            <div className="flex items-center gap-2 pt-4 border-t border-white/5">
              <button
                disabled={isGenerating}
                onClick={() => handleGenerateReport('2026년 상반기 현금흐름표', 'PDF')}
                className="flex-1 py-2 px-3 rounded-xl bg-slate-800 hover:bg-slate-700 text-rose-300 border border-white/10 text-xs font-bold flex items-center justify-center gap-1.5 transition-all"
              >
                <FileText size={14} /> PDF
              </button>
              <button
                disabled={isGenerating}
                onClick={() => handleGenerateReport('2026년 상반기 현금흐름표', 'EXCEL')}
                className="flex-1 py-2 px-3 rounded-xl bg-emerald-600/20 hover:bg-emerald-600/30 text-emerald-300 border border-emerald-500/30 text-xs font-bold flex items-center justify-center gap-1.5 transition-all"
              >
                <FileSpreadsheet size={14} /> Excel
              </button>
            </div>
          </div>

          {/* Card 4: DISCLOSURE PACKAGE */}
          <div className="p-6 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 hover:border-amber-500/30 transition-all flex flex-col justify-between group">
            <div>
              <div className="flex items-center justify-between mb-4">
                <span className="text-[10px] font-black tracking-wider uppercase px-2.5 py-1 rounded-md bg-amber-500/10 text-amber-400 border border-amber-500/20">
                  주석 공시 패키지
                </span>
                <Calendar className="text-slate-500 group-hover:text-amber-400 transition-colors" size={20} />
              </div>
              <h4 className="text-lg font-extrabold text-white tracking-tight mb-2">K-IFRS 주석 정보 패키지</h4>
              <p className="text-xs text-slate-400 font-medium mb-6">
                외부감사인 제출용 1~4번 개별 주석 내역서 및 계정 마트 세부 명세
              </p>
            </div>
            <div className="flex items-center gap-2 pt-4 border-t border-white/5">
              <button
                disabled={isGenerating}
                onClick={() => handleGenerateReport('K-IFRS 주석 정보 패키지', 'PDF')}
                className="flex-1 py-2 px-3 rounded-xl bg-slate-800 hover:bg-slate-700 text-rose-300 border border-white/10 text-xs font-bold flex items-center justify-center gap-1.5 transition-all"
              >
                <FileText size={14} /> PDF
              </button>
              <button
                disabled={isGenerating}
                onClick={() => handleGenerateReport('K-IFRS 주석 정보 패키지', 'EXCEL')}
                className="flex-1 py-2 px-3 rounded-xl bg-emerald-600/20 hover:bg-emerald-600/30 text-emerald-300 border border-emerald-500/30 text-xs font-bold flex items-center justify-center gap-1.5 transition-all"
              >
                <FileSpreadsheet size={14} /> Excel
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Export History Table Section */}
      <div className="space-y-4">
        {/* Table Filter Toolbar */}
        <div className="p-4 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 flex flex-col md:flex-row items-center justify-between gap-4">
          <Tabs tabs={tabs} activeTab={activeTab} onChange={setActiveTab} />

          <div className="flex items-center gap-3 w-full md:w-auto">
            {/* Format dropdown */}
            <select
              value={selectedFormat}
              onChange={e => setSelectedFormat(e.target.value)}
              className="px-4 py-2.5 bg-slate-950/80 border border-white/10 rounded-xl text-xs font-bold text-white outline-none focus:border-blue-500 transition-colors"
            >
              <option value="ALL">전체 서식 (Format)</option>
              <option value="PDF">PDF 포맷</option>
              <option value="EXCEL">Excel 포맷</option>
            </select>

            {/* Search Input */}
            <div className="relative flex-1 md:w-64">
              <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500" size={15} />
              <input
                type="text"
                placeholder="보고서명 또는 작성자 검색..."
                value={searchTerm}
                onChange={e => setSearchTerm(e.target.value)}
                className="w-full pl-10 pr-4 py-2.5 bg-slate-950/80 border border-white/10 rounded-xl text-xs font-medium text-white placeholder-slate-500 outline-none focus:border-blue-500 transition-colors"
              />
            </div>
          </div>
        </div>

        {/* History Table */}
        {filteredHistory.length === 0 ? (
          <EmptyState
            icon={History}
            title="내보내기 이력이 없습니다"
            description="선택한 조건에 해당하는 내보내기 생성 이력이 없습니다."
          />
        ) : (
          <div className="rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-white/10 bg-white/[0.02] text-[11px] font-black text-slate-400 uppercase tracking-widest">
                    <th className="py-4 px-6">보고서 번호 / 보고서명</th>
                    <th className="py-4 px-4 text-center">서식</th>
                    <th className="py-4 px-4">대상 기간</th>
                    <th className="py-4 px-4">생성 일시</th>
                    <th className="py-4 px-4">작성자</th>
                    <th className="py-4 px-4 text-right">용량</th>
                    <th className="py-4 px-4 text-center">처리 상태</th>
                    <th className="py-4 px-6 text-right">다운로드</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/5 text-xs font-medium">
                  {filteredHistory.map(item => {
                    const isCompleted = item.status === 'COMPLETED';
                    const isProcessing = item.status === 'PROCESSING';

                    return (
                      <tr key={item.id} className="hover:bg-white/[0.03] transition-colors">
                        <td className="py-4 px-6">
                          <div>
                            <div className="text-white font-bold text-sm mb-0.5">{item.reportName}</div>
                            <span className="font-mono text-[10px] text-slate-500">{item.id}</span>
                          </div>
                        </td>
                        <td className="py-4 px-4 text-center">
                          <span className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-[10px] font-black tracking-wider ${
                            item.format === 'PDF' 
                              ? 'bg-rose-500/10 text-rose-400 border border-rose-500/20' 
                              : 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                          }`}>
                            {item.format === 'PDF' ? <FileText size={12} /> : <FileSpreadsheet size={12} />}
                            {item.format}
                          </span>
                        </td>
                        <td className="py-4 px-4 text-slate-300 font-semibold">{item.period}</td>
                        <td className="py-4 px-4 text-slate-400 font-mono">{item.createdAt}</td>
                        <td className="py-4 px-4 text-slate-300 font-medium">{item.createdBy}</td>
                        <td className="py-4 px-4 text-right font-mono text-slate-400">{item.fileSize}</td>
                        <td className="py-4 px-4 text-center">
                          {isCompleted && <StatusBadge status="생성 완료" variant="success" />}
                          {isProcessing && <StatusBadge status="생성 중..." variant="warning" />}
                          {item.status === 'FAILED' && <StatusBadge status="실패" variant="error" />}
                        </td>
                        <td className="py-4 px-6 text-right">
                          {isCompleted ? (
                            <button
                              onClick={() => handleDownload(item)}
                              className="px-3 py-1.5 rounded-xl bg-blue-600/20 hover:bg-blue-600/30 text-blue-300 border border-blue-500/30 text-xs font-bold inline-flex items-center gap-1.5 transition-all active:scale-95"
                            >
                              <ArrowDownToLine size={14} /> 다운로드
                            </button>
                          ) : (
                            <button
                              disabled
                              className="px-3 py-1.5 rounded-xl bg-white/5 text-slate-600 border border-white/5 text-xs font-bold inline-flex items-center gap-1.5 cursor-not-allowed"
                            >
                              <Clock size={14} className="animate-spin" /> 대기 중
                            </button>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

"use client";

import React, { useState, useMemo } from 'react';
import {
  ShieldAlert,
  Send,
  FileCheck,
  Calendar,
  Building2,
  Clock,
  CheckCircle2,
  AlertCircle,
  Plus,
  Search,
  FileText,
  X,
  UploadCloud,
  Sparkles,
  ChevronRight,
  Eye,
  Check
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import Tabs from '@/components/ui/Tabs';
import EmptyState from '@/components/ui/EmptyState';
import LoadingSkeleton from '@/components/ui/LoadingSkeleton';
import { mockRegulatorySubmissions, RegulatorySubmissionDto } from '@/mocks/reporting';

export default function RegulatoryReportsPage() {
  const [submissions, setSubmissions] = useState<RegulatorySubmissionDto[]>(mockRegulatorySubmissions);
  const [activeTab, setActiveTab] = useState<string>('ALL');
  const [searchTerm, setSearchTerm] = useState<string>('');
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const [selectedSubmission, setSelectedSubmission] = useState<RegulatorySubmissionDto | null>(null);
  const [notification, setNotification] = useState<string | null>(null);

  // New submission form state
  const [formType, setFormType] = useState<'FSS_KIFRS' | 'TAX_VAT' | 'TAX_CIT' | 'BANK_REGULATORY'>('FSS_KIFRS');
  const [formTitle, setFormTitle] = useState<string>('2026년 3분기 금융감독원 K-IFRS 공시');
  const [reportingPeriod, setReportingPeriod] = useState<string>('2026년 3분기');
  const [dueDate, setDueDate] = useState<string>('2026-11-14');

  const tabs = [
    { id: 'ALL', label: '전체 보고서' },
    { id: 'SUBMITTED', label: '제출 완료' },
    { id: 'PENDING_APPROVAL', label: '승인 대기' },
    { id: 'DRAFT', label: '작성 중 (DRAFT)' },
  ];

  const filteredSubmissions = useMemo(() => {
    return submissions.filter(item => {
      if (activeTab !== 'ALL' && item.status !== activeTab) return false;
      if (searchTerm.trim()) {
        const query = searchTerm.toLowerCase();
        return (
          item.title.toLowerCase().includes(query) ||
          item.id.toLowerCase().includes(query) ||
          (item.submittedBy && item.submittedBy.toLowerCase().includes(query))
        );
      }
      return true;
    });
  }, [submissions, activeTab, searchTerm]);

  const handleSubmitNewReport = (e: React.FormEvent) => {
    e.preventDefault();
    const newId = `REG-2026-00${submissions.length + 1}`;
    const newEntry: RegulatorySubmissionDto = {
      id: newId,
      formType,
      title: formTitle,
      reportingPeriod,
      dueDate,
      status: 'PENDING_APPROVAL',
      submittedBy: '현재 사용자 (나)',
      remarks: '신규 규제 보고서 결재 제출 완료',
    };

    setSubmissions([newEntry, ...submissions]);
    setIsModalOpen(false);
    setNotification(`'${formTitle}' 제출 결재가 완료되었습니다.`);
    setTimeout(() => setNotification(null), 4000);
  };

  const handleDirectSubmit = (id: string, title: string) => {
    setSubmissions(prev =>
      prev.map(item =>
        item.id === id
          ? {
              ...item,
              status: 'SUBMITTED',
              submissionDate: new Date().toISOString().slice(0, 10),
              remarks: '기관 전자제출 시스템 송신 완료',
            }
          : item
      )
    );
    setNotification(`'${title}' 전자제출이 성공적으로 송신되었습니다.`);
    setTimeout(() => setNotification(null), 4000);
  };

  return (
    <div className="space-y-8 pb-12">
      {/* Toast Notification */}
      {notification && (
        <div className="fixed top-5 right-5 z-50 bg-emerald-600/20 border border-emerald-500/40 text-emerald-200 px-6 py-4 rounded-2xl backdrop-blur-xl shadow-2xl flex items-center gap-3 animate-fade-in">
          <CheckCircle2 className="w-5 h-5 text-emerald-400" />
          <span className="text-sm font-bold">{notification}</span>
        </div>
      )}

      {/* Page Header */}
      <PageHeader
        title="규제 보고 제출"
        description="금융감독원(DART), 국세청(HomeTax), 한국은행 대외 규제 기관 법정 보고서 전자제출 및 상태 통합 관리"
        breadcrumbs={[
          { label: '보고서 관리' },
          { label: '규제 보고 제출' },
        ]}
        icon={ShieldAlert}
        actions={
          <button
            onClick={() => setIsModalOpen(true)}
            className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-bold text-xs uppercase tracking-wider shadow-lg shadow-blue-600/20 transition-all active:scale-95"
          >
            <Plus size={16} /> 신규 규제 보고서 제출 작성
          </button>
        }
      />

      {/* Process Stepper & Deadline Alert Card */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left: Stepper Process */}
        <div className="lg:col-span-2 p-6 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 space-y-4">
          <h3 className="text-xs font-black text-slate-400 uppercase tracking-widest flex items-center gap-2">
            <Sparkles size={15} className="text-blue-400" />
            규제 보고서 전자 제출 프로세스
          </h3>

          <div className="grid grid-cols-1 sm:grid-cols-4 gap-3 pt-2">
            <div className="p-4 rounded-xl bg-white/5 border border-white/5 relative">
              <div className="text-[10px] font-black text-blue-400 uppercase tracking-widest mb-1">Step 01</div>
              <div className="text-sm font-bold text-white mb-1">데이터 검증</div>
              <div className="text-[11px] text-slate-400">재무제표 & 주석 자동 추출</div>
            </div>
            <div className="p-4 rounded-xl bg-white/5 border border-white/5 relative">
              <div className="text-[10px] font-black text-indigo-400 uppercase tracking-widest mb-1">Step 02</div>
              <div className="text-sm font-bold text-white mb-1">내부 결재</div>
              <div className="text-[11px] text-slate-400">재무이사 & 감사 승인</div>
            </div>
            <div className="p-4 rounded-xl bg-white/5 border border-white/5 relative">
              <div className="text-[10px] font-black text-amber-400 uppercase tracking-widest mb-1">Step 03</div>
              <div className="text-sm font-bold text-white mb-1">규제기관 검증</div>
              <div className="text-[11px] text-slate-400">DART / 홈택스 오류 체크</div>
            </div>
            <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/20 relative">
              <div className="text-[10px] font-black text-emerald-400 uppercase tracking-widest mb-1">Step 04</div>
              <div className="text-sm font-bold text-emerald-300 mb-1">최종 전자 제출</div>
              <div className="text-[11px] text-emerald-400/80">접수증 수신 & 완료</div>
            </div>
          </div>
        </div>

        {/* Right: Upcoming Deadline Alert */}
        <div className="p-6 rounded-2xl bg-amber-500/10 border border-amber-500/20 backdrop-blur-md flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-3">
              <span className="px-2.5 py-0.5 rounded-full text-[10px] font-black uppercase tracking-widest bg-amber-500/20 text-amber-300 border border-amber-500/30">
                제출 마감 임박 (D-17)
              </span>
              <Clock size={18} className="text-amber-400" />
            </div>
            <h4 className="text-base font-extrabold text-white mb-1">
              2026년 2분기 K-IFRS 분기보고서
            </h4>
            <p className="text-xs text-slate-300 font-medium">
              금융감독원 전자공시시스템(DART) 제출 마감일: <strong className="text-amber-300">2026-08-14</strong>
            </p>
          </div>

          <div className="pt-4 border-t border-amber-500/20 flex items-center justify-between">
            <span className="text-[11px] text-slate-400">담당: 김재무 이사</span>
            <button
              onClick={() => setIsModalOpen(true)}
              className="px-3 py-1.5 rounded-xl bg-amber-500 hover:bg-amber-400 text-slate-950 text-xs font-black transition-all"
            >
              제출 작성하기
            </button>
          </div>
        </div>
      </div>

      {/* Main Submissions List Section */}
      <div className="space-y-4">
        {/* Toolbar */}
        <div className="p-4 rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 flex flex-col md:flex-row items-center justify-between gap-4">
          <Tabs tabs={tabs} activeTab={activeTab} onChange={setActiveTab} />

          <div className="relative flex-1 md:w-72">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500" size={15} />
            <input
              type="text"
              placeholder="보고서 제목 또는 담당자 검색..."
              value={searchTerm}
              onChange={e => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2.5 bg-slate-950/80 border border-white/10 rounded-xl text-xs font-medium text-white placeholder-slate-500 outline-none focus:border-blue-500 transition-colors"
            />
          </div>
        </div>

        {/* Submissions Table */}
        {filteredSubmissions.length === 0 ? (
          <EmptyState
            icon={ShieldAlert}
            title="제출 이력이 없습니다"
            description="선택한 조건에 부합하는 규제 보고 제출건이 없습니다."
          />
        ) : (
          <div className="rounded-2xl bg-slate-900/50 backdrop-blur-md border border-white/5 overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-white/10 bg-white/[0.02] text-[11px] font-black text-slate-400 uppercase tracking-widest">
                    <th className="py-4 px-6">기관 / 제출 서식 유형</th>
                    <th className="py-4 px-6">보고서 제목</th>
                    <th className="py-4 px-4">대상 기간</th>
                    <th className="py-4 px-4">제출 마감일</th>
                    <th className="py-4 px-4">제출 일시</th>
                    <th className="py-4 px-4 text-center">제출 상태</th>
                    <th className="py-4 px-6 text-right">작업</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/5 text-xs">
                  {filteredSubmissions.map(item => {
                    const isSubmitted = item.status === 'SUBMITTED';
                    const isPending = item.status === 'PENDING_APPROVAL';
                    const isDraft = item.status === 'DRAFT';

                    return (
                      <tr key={item.id} className="hover:bg-white/[0.03] transition-colors">
                        <td className="py-4 px-6">
                          <div className="flex items-center gap-2.5">
                            <div className="w-8 h-8 rounded-xl bg-blue-500/10 border border-blue-500/20 flex items-center justify-center text-blue-400 font-bold">
                              <Building2 size={16} />
                            </div>
                            <div>
                              <span className="font-bold text-white text-xs block">
                                {item.formType === 'FSS_KIFRS' ? '금융감독원 (DART)' :
                                 item.formType === 'TAX_VAT' ? '국세청 부가가치세' :
                                 item.formType === 'TAX_CIT' ? '국세청 법인세' : '한국은행 금융통계'}
                              </span>
                              <span className="font-mono text-[10px] text-slate-500">{item.id}</span>
                            </div>
                          </div>
                        </td>

                        <td className="py-4 px-6">
                          <div className="font-extrabold text-white text-sm">{item.title}</div>
                          {item.remarks && (
                            <span className="text-[11px] text-slate-400 block mt-0.5">{item.remarks}</span>
                          )}
                        </td>

                        <td className="py-4 px-4 text-slate-300 font-medium">{item.reportingPeriod}</td>

                        <td className="py-4 px-4 font-mono font-bold text-amber-400">{item.dueDate}</td>

                        <td className="py-4 px-4 font-mono text-slate-400">
                          {item.submissionDate || '-'}
                        </td>

                        <td className="py-4 px-4 text-center">
                          {isSubmitted && <StatusBadge status="제출 완료" variant="success" />}
                          {isPending && <StatusBadge status="승인 대기중" variant="warning" />}
                          {isDraft && <StatusBadge status="작성 중 (DRAFT)" variant="neutral" />}
                          {item.status === 'REJECTED' && <StatusBadge status="반려" variant="error" />}
                        </td>

                        <td className="py-4 px-6 text-right">
                          <div className="flex items-center justify-end gap-2">
                            <button
                              onClick={() => setSelectedSubmission(item)}
                              className="px-3 py-1.5 rounded-xl bg-white/5 hover:bg-white/10 text-slate-300 border border-white/10 font-bold text-xs flex items-center gap-1 transition-all"
                            >
                              <Eye size={14} /> 보기
                            </button>
                            {!isSubmitted && (
                              <button
                                onClick={() => handleDirectSubmit(item.id, item.title)}
                                className="px-3 py-1.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-bold text-xs flex items-center gap-1 shadow-md transition-all active:scale-95"
                              >
                                <Send size={14} /> 전자제출
                              </button>
                            )}
                          </div>
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

      {/* NEW SUBMISSION MODAL */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/75 backdrop-blur-md flex items-center justify-center p-4 animate-fade-in">
          <div className="bg-slate-900 border border-white/10 rounded-3xl w-full max-w-xl overflow-hidden shadow-2xl p-8 space-y-6">
            <div className="flex items-center justify-between border-b border-white/10 pb-4">
              <h3 className="text-xl font-extrabold text-white flex items-center gap-2">
                <Send className="text-blue-400" size={20} />
                신규 규제 보고서 제출 작성
              </h3>
              <button
                onClick={() => setIsModalOpen(false)}
                className="p-2 rounded-xl bg-white/5 hover:bg-white/10 text-slate-400 hover:text-white transition-all"
              >
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleSubmitNewReport} className="space-y-4 text-xs">
              <div>
                <label className="block text-slate-400 font-bold mb-1 uppercase tracking-wider">
                  규제 기관 서식 종류
                </label>
                <select
                  value={formType}
                  onChange={e => setFormType(e.target.value as any)}
                  className="w-full px-4 py-3 bg-slate-950 border border-white/10 rounded-xl text-white outline-none focus:border-blue-500 font-bold"
                >
                  <option value="FSS_KIFRS">금융감독원 (DART) - K-IFRS 보고서</option>
                  <option value="TAX_VAT">국세청 (HomeTax) - 부가가치세 신고서</option>
                  <option value="TAX_CIT">국세청 (HomeTax) - 법인세 중간예납 신고서</option>
                  <option value="BANK_REGULATORY">한국은행 - 금융통계 보고서</option>
                </select>
              </div>

              <div>
                <label className="block text-slate-400 font-bold mb-1 uppercase tracking-wider">
                  보고서 제목
                </label>
                <input
                  type="text"
                  value={formTitle}
                  onChange={e => setFormTitle(e.target.value)}
                  className="w-full px-4 py-3 bg-slate-950 border border-white/10 rounded-xl text-white outline-none focus:border-blue-500 font-bold"
                  required
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-slate-400 font-bold mb-1 uppercase tracking-wider">
                    보고 대상 기간
                  </label>
                  <input
                    type="text"
                    value={reportingPeriod}
                    onChange={e => setReportingPeriod(e.target.value)}
                    className="w-full px-4 py-3 bg-slate-950 border border-white/10 rounded-xl text-white outline-none focus:border-blue-500 font-bold"
                    required
                  />
                </div>
                <div>
                  <label className="block text-slate-400 font-bold mb-1 uppercase tracking-wider">
                    제출 마감일
                  </label>
                  <input
                    type="date"
                    value={dueDate}
                    onChange={e => setDueDate(e.target.value)}
                    className="w-full px-4 py-3 bg-slate-950 border border-white/10 rounded-xl text-white outline-none focus:border-blue-500 font-bold"
                    required
                  />
                </div>
              </div>

              <div className="p-4 rounded-2xl bg-white/5 border border-white/10 space-y-2">
                <div className="text-slate-300 font-bold flex items-center gap-2">
                  <UploadCloud size={16} className="text-blue-400" />
                  첨부 재무 데이터 패키지 자동 연동
                </div>
                <p className="text-slate-500">
                  2026년 2분기 결산 재무상태표 및 손익계산서 주석 데이터 패키지가 자동으로 함께 전송됩니다.
                </p>
              </div>

              <div className="pt-4 border-t border-white/10 flex items-center justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-5 py-2.5 rounded-xl bg-white/5 text-slate-300 font-bold hover:bg-white/10 transition-all"
                >
                  취소
                </button>
                <button
                  type="submit"
                  className="px-6 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-bold transition-all shadow-lg shadow-blue-600/20"
                >
                  결재 및 제출 등록
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* SUBMISSION DETAIL VIEW MODAL */}
      {selectedSubmission && (
        <div className="fixed inset-0 z-50 bg-black/75 backdrop-blur-md flex items-center justify-center p-4 animate-fade-in">
          <div className="bg-slate-900 border border-white/10 rounded-3xl w-full max-w-lg overflow-hidden shadow-2xl p-8 space-y-6">
            <div className="flex items-center justify-between border-b border-white/10 pb-4">
              <h3 className="text-xl font-extrabold text-white">규제 제출 상세 정보</h3>
              <button
                onClick={() => setSelectedSubmission(null)}
                className="p-2 rounded-xl bg-white/5 hover:bg-white/10 text-slate-400 hover:text-white transition-all"
              >
                <X size={20} />
              </button>
            </div>

            <div className="space-y-4 text-xs">
              <div className="flex items-center justify-between p-4 rounded-2xl bg-white/5 border border-white/5">
                <span className="text-slate-400 font-bold">보고서 ID</span>
                <span className="font-mono text-white font-bold">{selectedSubmission.id}</span>
              </div>
              <div className="flex items-center justify-between p-4 rounded-2xl bg-white/5 border border-white/5">
                <span className="text-slate-400 font-bold">보고서 제목</span>
                <span className="text-white font-bold">{selectedSubmission.title}</span>
              </div>
              <div className="flex items-center justify-between p-4 rounded-2xl bg-white/5 border border-white/5">
                <span className="text-slate-400 font-bold">제출 상태</span>
                <StatusBadge status={selectedSubmission.status} variant="info" />
              </div>
              <div className="flex items-center justify-between p-4 rounded-2xl bg-white/5 border border-white/5">
                <span className="text-slate-400 font-bold">담당자</span>
                <span className="text-white font-bold">{selectedSubmission.submittedBy || '-'}</span>
              </div>
              <div className="p-4 rounded-2xl bg-white/5 border border-white/5">
                <span className="text-slate-400 font-bold block mb-1">비고 및 검증 메세지</span>
                <span className="text-slate-200 font-medium">{selectedSubmission.remarks || '특이사항 없음'}</span>
              </div>
            </div>

            <div className="pt-4 border-t border-white/10 text-right">
              <button
                onClick={() => setSelectedSubmission(null)}
                className="px-6 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-bold text-xs"
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

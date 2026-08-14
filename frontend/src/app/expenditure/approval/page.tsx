'use client';

import React, { useState } from 'react';
import { 
  CheckSquare, 
  Search, 
  Filter, 
  CheckCircle2, 
  XCircle, 
  Clock, 
  FileText, 
  User, 
  Building, 
  Calendar,
  X,
  MessageSquare,
  ChevronRight,
  ShieldCheck
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import AmountDisplay from '@/components/ui/AmountDisplay';
import Tabs, { TabItem } from '@/components/ui/Tabs';
import EmptyState from '@/components/ui/EmptyState';
import { mockResolutions, ResolutionDto } from '@/mocks/expenditure';

export default function ApprovalPage() {
  const [resolutions, setResolutions] = useState<ResolutionDto[]>(mockResolutions);
  const [activeTab, setActiveTab] = useState<string>('PENDING');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedResolution, setSelectedResolution] = useState<ResolutionDto | null>(null);
  const [comment, setComment] = useState('');

  const filterTabs: TabItem[] = [
    { id: 'PENDING', label: '승인 대기', icon: Clock },
    { id: 'APPROVED', label: '승인 완료', icon: CheckCircle2 },
    { id: 'REJECTED', label: '반려됨', icon: XCircle },
    { id: 'ALL', label: '전체 결의건', icon: FileText }
  ];

  const filteredResolutions = resolutions.filter(res => {
    const matchesTab = activeTab === 'ALL' || res.status === activeTab;
    const matchesSearch = 
      res.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      res.resolutionNo.toLowerCase().includes(searchQuery.toLowerCase()) ||
      res.writerName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      res.vendorName.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesTab && matchesSearch;
  });

  const handleApprove = (id: string) => {
    setResolutions(resolutions.map(res => {
      if (res.id !== id) return res;
      return {
        ...res,
        status: 'APPROVED' as const,
        approvalLine: res.approvalLine.map(step => {
          if (step.status === 'PENDING') {
            return {
              ...step,
              status: 'APPROVED' as const,
              comment: comment || '승인 완료',
              approvedAt: new Date().toISOString().replace('T', ' ').substring(0, 16)
            };
          }
          return step;
        })
      };
    }));
    alert(`[지출결의 승인 완료] ${selectedResolution?.resolutionNo} 건이 최종 승인 처리되었습니다.`);
    setSelectedResolution(null);
    setComment('');
  };

  const handleReject = (id: string) => {
    if (!comment.trim()) {
      alert('반려 처리 시 반려 사유를 작성해 주세요.');
      return;
    }
    setResolutions(resolutions.map(res => {
      if (res.id !== id) return res;
      return {
        ...res,
        status: 'REJECTED' as const,
        approvalLine: res.approvalLine.map(step => {
          if (step.status === 'PENDING') {
            return {
              ...step,
              status: 'REJECTED' as const,
              comment: comment,
              approvedAt: new Date().toISOString().replace('T', ' ').substring(0, 16)
            };
          }
          return step;
        })
      };
    }));
    alert(`[지출결의 반려 완료] ${selectedResolution?.resolutionNo} 건이 반려 처리되었습니다.`);
    setSelectedResolution(null);
    setComment('');
  };

  const getStatusVariant = (status: string) => {
    switch (status) {
      case 'APPROVED': return 'success';
      case 'PENDING': return 'warning';
      case 'REJECTED': return 'error';
      case 'PAID': return 'info';
      default: return 'neutral';
    }
  };

  const getStatusLabel = (status: string) => {
    switch (status) {
      case 'APPROVED': return '승인완료';
      case 'PENDING': return '결재대기';
      case 'REJECTED': return '반려됨';
      case 'PAID': return '지급완료';
      case 'DRAFT': return '작성중';
      default: return status;
    }
  };

  return (
    <div className="space-y-8 pb-16">
      <PageHeader
        title="지출 승인 워크플로"
        description="상신된 지출결의서 내역을 검토하고 전자 서명으로 승인 또는 반려 처리합니다."
        breadcrumbs={[
          { label: '지출관리' },
          { label: '지출 승인' }
        ]}
        icon={CheckSquare}
        actions={
          <div className="flex items-center gap-3 bg-slate-900/50 backdrop-blur-md px-4 py-2 rounded-2xl border border-white/5 text-xs text-slate-400">
            <ShieldCheck size={16} className="text-emerald-400" />
            <span>결재 권한: 재무기획본부 / 대표이사 결재권</span>
          </div>
        }
      />

      {/* Tabs & Search controls */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <Tabs tabs={filterTabs} activeTab={activeTab} onChange={setActiveTab} />

        <div className="relative w-full md:w-80">
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500" size={16} />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="문서번호, 제목, 작성자, 거래처 검색"
            className="w-full bg-slate-900/50 border border-white/10 rounded-2xl py-2.5 pl-11 pr-4 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 transition-all font-medium"
          />
        </div>
      </div>

      {/* Resolutions Table List */}
      <div className="rounded-3xl bg-slate-900/50 backdrop-blur-md border border-white/5 p-7 shadow-xl space-y-4">
        {filteredResolutions.length === 0 ? (
          <EmptyState
            icon={FileText}
            title="조건에 일치하는 지출결의서가 없습니다"
            description="다른 상태 탭을 클릭하거나 검색어를 변경해 보세요."
          />
        ) : (
          <div className="space-y-3">
            {filteredResolutions.map((res) => (
              <div
                key={res.id}
                onClick={() => setSelectedResolution(res)}
                className="group flex flex-col md:flex-row md:items-center justify-between p-5 rounded-2xl bg-white/[0.02] border border-white/5 hover:border-blue-500/30 hover:bg-white/[0.04] transition-all cursor-pointer gap-4"
              >
                <div className="flex items-start gap-4">
                  <div className={`w-11 h-11 rounded-2xl flex items-center justify-center flex-shrink-0 border ${
                    res.status === 'PENDING' ? 'bg-amber-500/10 border-amber-500/20 text-amber-400' :
                    res.status === 'APPROVED' ? 'bg-emerald-500/10 border-emerald-500/20 text-emerald-400' :
                    res.status === 'REJECTED' ? 'bg-rose-500/10 border-rose-500/20 text-rose-400' :
                    'bg-slate-500/10 border-slate-500/20 text-slate-400'
                  }`}>
                    {res.status === 'PENDING' ? <Clock size={20} /> :
                     res.status === 'APPROVED' ? <CheckCircle2 size={20} /> :
                     res.status === 'REJECTED' ? <XCircle size={20} /> : <FileText size={20} />}
                  </div>

                  <div className="space-y-1">
                    <div className="flex items-center gap-3">
                      <span className="font-mono text-xs font-black text-blue-400">{res.resolutionNo}</span>
                      <StatusBadge status={getStatusLabel(res.status)} variant={getStatusVariant(res.status)} />
                      <span className="text-xs text-slate-500 font-bold">{res.paymentMethod === 'BANK_TRANSFER' ? '계좌이체' : '법인카드'}</span>
                    </div>

                    <h4 className="text-base font-black text-white group-hover:text-blue-300 transition-colors">
                      {res.title}
                    </h4>

                    <div className="flex items-center gap-4 text-xs text-slate-400 font-medium pt-1">
                      <span className="flex items-center gap-1"><User size={13} className="text-slate-500" /> {res.writerName} ({res.department})</span>
                      <span>•</span>
                      <span className="flex items-center gap-1"><Building size={13} className="text-slate-500" /> {res.vendorName}</span>
                      <span>•</span>
                      <span className="flex items-center gap-1"><Calendar size={13} className="text-slate-500" /> 기안일: {res.requestDate}</span>
                    </div>
                  </div>
                </div>

                <div className="flex items-center justify-between md:justify-end gap-6 pt-3 md:pt-0 border-t md:border-t-0 border-white/5">
                  <div className="text-right">
                    <span className="text-[10px] text-slate-500 uppercase tracking-widest block font-bold">결의 총액</span>
                    <span className="text-lg font-black text-white italic tracking-tight font-mono">
                      <AmountDisplay amount={res.totalAmount + res.totalTax} />
                    </span>
                  </div>

                  <div className="flex items-center gap-2">
                    <button className="px-4 py-2 bg-white/5 group-hover:bg-blue-600 group-hover:text-white rounded-xl text-xs font-black text-slate-300 transition-all flex items-center gap-1.5">
                      상세 검토 <ChevronRight size={14} />
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Detail Modal Dialog */}
      {selectedResolution && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-md animate-fadeIn">
          <div className="w-full max-w-3xl rounded-3xl bg-slate-900 border border-white/10 p-7 shadow-2xl space-y-6 max-h-[90vh] overflow-y-auto">
            {/* Modal Header */}
            <div className="flex items-center justify-between border-b border-white/10 pb-4">
              <div>
                <div className="flex items-center gap-3 mb-1">
                  <span className="font-mono text-xs font-black text-blue-400">{selectedResolution.resolutionNo}</span>
                  <StatusBadge 
                    status={getStatusLabel(selectedResolution.status)} 
                    variant={getStatusVariant(selectedResolution.status)} 
                  />
                </div>
                <h3 className="text-xl font-black text-white italic">{selectedResolution.title}</h3>
              </div>
              <button 
                onClick={() => setSelectedResolution(null)}
                className="p-2 text-slate-400 hover:text-white rounded-xl hover:bg-white/10 transition-all"
              >
                <X size={20} />
              </button>
            </div>

            {/* General Info Grid */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 p-4 rounded-2xl bg-white/[0.02] border border-white/5 text-xs">
              <div>
                <span className="text-slate-500 font-bold block mb-1">기안자</span>
                <span className="text-white font-black">{selectedResolution.writerName} ({selectedResolution.department})</span>
              </div>
              <div>
                <span className="text-slate-500 font-bold block mb-1">지급처</span>
                <span className="text-white font-black">{selectedResolution.vendorName}</span>
              </div>
              <div>
                <span className="text-slate-500 font-bold block mb-1">지급 방식</span>
                <span className="text-white font-black">{selectedResolution.paymentMethod === 'BANK_TRANSFER' ? '계좌이체' : '법인카드'}</span>
              </div>
              <div>
                <span className="text-slate-500 font-bold block mb-1">지급 희망일</span>
                <span className="text-white font-black">{selectedResolution.dueDate}</span>
              </div>
            </div>

            {/* Line Items Detail */}
            <div className="space-y-3">
              <h4 className="text-xs font-black uppercase text-slate-400 tracking-wider">지출 항목 명세</h4>
              <div className="rounded-2xl border border-white/5 overflow-hidden">
                <table className="w-full text-left text-xs">
                  <thead className="bg-white/5 text-slate-400 font-bold">
                    <tr>
                      <th className="py-2.5 px-3">계정코드 / 명</th>
                      <th className="py-2.5 px-3">적요</th>
                      <th className="py-2.5 px-3">코스트센터</th>
                      <th className="py-2.5 px-3 text-right">공급가액</th>
                      <th className="py-2.5 px-3 text-right">부가세</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-white/5 text-slate-300">
                    {selectedResolution.lineItems.map((line) => (
                      <tr key={line.id}>
                        <td className="py-2.5 px-3 font-mono font-bold text-white">{line.accountCode} ({line.accountName})</td>
                        <td className="py-2.5 px-3">{line.description}</td>
                        <td className="py-2.5 px-3 text-slate-400">{line.costCenter}</td>
                        <td className="py-2.5 px-3 text-right font-mono"><AmountDisplay amount={line.amount} /></td>
                        <td className="py-2.5 px-3 text-right font-mono"><AmountDisplay amount={line.taxAmount} /></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <div className="flex justify-end pt-2 text-right">
                <div>
                  <span className="text-xs text-slate-400 font-bold mr-3">합계 금액:</span>
                  <span className="text-xl font-black text-emerald-400 font-mono italic">
                    <AmountDisplay amount={selectedResolution.totalAmount + selectedResolution.totalTax} />
                  </span>
                </div>
              </div>
            </div>

            {/* Approval Progress History */}
            <div className="space-y-3">
              <h4 className="text-xs font-black uppercase text-slate-400 tracking-wider">결재 진행 이력</h4>
              <div className="grid grid-cols-1 sm:grid-cols-4 gap-3">
                {selectedResolution.approvalLine.map((step) => (
                  <div key={step.step} className="p-3 rounded-2xl bg-white/[0.02] border border-white/5 space-y-1">
                    <div className="flex justify-between items-center text-[10px]">
                      <span className="font-mono text-slate-500 font-bold">Step {step.step}</span>
                      <StatusBadge 
                        status={step.status === 'APPROVED' ? '승인' : step.status === 'REJECTED' ? '반려' : '대기'}
                        variant={step.status === 'APPROVED' ? 'success' : step.status === 'REJECTED' ? 'error' : 'warning'}
                      />
                    </div>
                    <div className="text-xs font-black text-white">{step.approverName} ({step.position})</div>
                    {step.comment && <p className="text-[11px] text-slate-400 italic">"{step.comment}"</p>}
                    {step.approvedAt && <span className="text-[10px] text-slate-500 block font-mono">{step.approvedAt}</span>}
                  </div>
                ))}
              </div>
            </div>

            {/* Decision Input Section */}
            {selectedResolution.status === 'PENDING' && (
              <div className="space-y-4 pt-4 border-t border-white/10">
                <div className="space-y-2">
                  <label className="text-xs font-black uppercase text-slate-400 flex items-center gap-1.5">
                    <MessageSquare size={14} className="text-blue-400" /> 결재 의견 / 반려 사유 작성
                  </label>
                  <textarea
                    value={comment}
                    onChange={(e) => setComment(e.target.value)}
                    placeholder="결재 승인 메시지 또는 반려 사유를 구체적으로 적어주세요."
                    className="w-full h-20 bg-slate-950 border border-white/10 rounded-2xl p-3 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 transition-all"
                  />
                </div>

                <div className="flex items-center justify-end gap-3">
                  <button
                    type="button"
                    onClick={() => handleReject(selectedResolution.id)}
                    className="px-5 py-2.5 bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 border border-rose-500/20 rounded-xl text-xs font-black transition-all flex items-center gap-1.5"
                  >
                    <XCircle size={16} /> 반려 처리
                  </button>
                  <button
                    type="button"
                    onClick={() => handleApprove(selectedResolution.id)}
                    className="px-6 py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl text-xs font-black transition-all shadow-lg shadow-emerald-600/20 flex items-center gap-1.5"
                  >
                    <CheckCircle2 size={16} /> 결재 승인 확정
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

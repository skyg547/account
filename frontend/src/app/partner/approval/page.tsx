"use client";

import React, { useState } from 'react';
import { 
  GitPullRequest, 
  CheckCircle2, 
  XCircle, 
  User, 
  Clock, 
  FileText, 
  Check, 
  X
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';

interface ChangeRequest {
  id: string;
  partnerId: string;
  partnerName: string;
  requester: string;
  requestDate: string;
  reason: string;
  urgency: 'HIGH' | 'MEDIUM' | 'NORMAL';
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  before: Record<string, string>;
  after: Record<string, string>;
}

const mockRequests: ChangeRequest[] = [
  {
    id: 'REQ-2026-0492',
    partnerId: 'PT-001',
    partnerName: '(주)안티그래비티',
    requester: '김재무 과장 (회계팀)',
    requestDate: '2026-07-27 14:30',
    reason: '대표자 변경에 따른 사업자등록증 변경 업데이트 및 결제 주계좌 변경 요청',
    urgency: 'HIGH',
    status: 'PENDING',
    before: {
      '대표자명 (CEO)': '김대표',
      '사업자등록번호': '123-45-67890',
      '주거래 은행': '신한은행',
      '결제 계좌번호': '110-123-456789',
      '신용 한도 금액': '₩500,000,000',
      '여신 결제 주기': '월말 30일 익월지급',
    },
    after: {
      '대표자명 (CEO)': '이성진 (변경)',
      '사업자등록번호': '123-45-67890',
      '주거래 은행': '하나은행 (변경)',
      '결제 계좌번호': '351-987-654321 (변경)',
      '신용 한도 금액': '₩800,000,000 (증액)',
      '여신 결제 주기': '월말 30일 익월지급',
    },
  },
  {
    id: 'REQ-2026-0491',
    partnerId: 'PT-002',
    partnerName: '글로벌테크',
    requester: '이영희 차장 (영업기획팀)',
    requestDate: '2026-07-26 11:15',
    reason: '거래처 상태 변경 (PENDING -> ACTIVE 승인 요청)',
    urgency: 'MEDIUM',
    status: 'PENDING',
    before: {
      '거래 상태': 'PENDING (승인대기)',
      '여신 한도': '₩0',
      '담당 부서': '미지정',
    },
    after: {
      '거래 상태': 'ACTIVE (정상거래)',
      '여신 한도': '₩200,000,000',
      '담당 부서': '기업금융1팀',
    },
  },
];

export default function PartnerApprovalPage() {
  const [selectedReqId, setSelectedReqId] = useState<string>('REQ-2026-0492');
  const [opinion, setOpinion] = useState('');
  const [actionStatus, setActionStatus] = useState<{
    type: 'APPROVED' | 'REJECTED' | null;
    msg: string;
  }>({ type: null, msg: '' });

  const currentReq = mockRequests.find((r) => r.id === selectedReqId) || mockRequests[0];

  const handleApprove = () => {
    setActionStatus({
      type: 'APPROVED',
      msg: `요청 [${currentReq.id}] 승인이 완료되었습니다. 기준정보 마스터가 업데이트됩니다.`,
    });
  };

  const handleReject = () => {
    if (!opinion.trim()) {
      alert('반려 시에는 반려 사유/의견을 작성해야 합니다.');
      return;
    }
    setActionStatus({
      type: 'REJECTED',
      msg: `요청 [${currentReq.id}] 건이 반려 처리되었습니다.`,
    });
  };

  return (
    <div className="space-y-8">
      <PageHeader
        title="기준정보 변경 승인"
        description="중요 속성 변경건에 대한 워크플로 승인"
        breadcrumbs={[
          { label: '기준정보 마스터' },
          { label: '거래처 관리' },
          { label: '기준정보 변경 승인' },
        ]}
        icon={GitPullRequest}
      />

      {/* Action Notification Banner */}
      {actionStatus.type && (
        <div
          className={`p-4 rounded-2xl border font-bold flex items-center justify-between animate-fade-in ${
            actionStatus.type === 'APPROVED'
              ? 'bg-emerald-500/10 border-emerald-500/20 text-emerald-300'
              : 'bg-rose-500/10 border-rose-500/20 text-rose-300'
          }`}
        >
          <div className="flex items-center gap-3">
            {actionStatus.type === 'APPROVED' ? (
              <CheckCircle2 size={20} className="text-emerald-400" />
            ) : (
              <XCircle size={20} className="text-rose-400" />
            )}
            <span>{actionStatus.msg}</span>
          </div>
          <button
            onClick={() => setActionStatus({ type: null, msg: '' })}
            className="text-xs hover:underline"
          >
            닫기
          </button>
        </div>
      )}

      {/* Select Request Cards Bar */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {mockRequests.map((req) => {
          const isSelected = req.id === currentReq.id;
          return (
            <div
              key={req.id}
              onClick={() => {
                setSelectedReqId(req.id);
                setActionStatus({ type: null, msg: '' });
              }}
              className={`p-5 rounded-2xl cursor-pointer transition-all border ${
                isSelected
                  ? 'bg-blue-600/15 border-blue-500/50 shadow-lg shadow-blue-500/10'
                  : 'bg-white/[0.02] border-white/5 hover:border-white/10'
              }`}
            >
              <div className="flex items-center justify-between mb-2">
                <span className="font-mono text-xs font-black text-blue-400">
                  {req.id}
                </span>
                <span
                  className={`text-[10px] font-black px-2 py-0.5 rounded-full ${
                    req.urgency === 'HIGH'
                      ? 'bg-rose-500/10 text-rose-400 border border-rose-500/20'
                      : 'bg-amber-500/10 text-amber-400 border border-amber-500/20'
                  }`}
                >
                  {req.urgency === 'HIGH' ? '긴급 승인' : '일반 승인'}
                </span>
              </div>
              <h4 className="text-base font-black text-white italic tracking-tight mb-1">
                {req.partnerName} ({req.partnerId})
              </h4>
              <p className="text-xs text-slate-400 line-clamp-1 font-medium">
                {req.reason}
              </p>
            </div>
          );
        })}
      </div>

      {/* Request Detail Header Panel */}
      <div className="glass-panel p-8 rounded-[3rem] border border-white/10 space-y-6">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-6 border-b border-white/5">
          <div>
            <div className="flex items-center gap-3 mb-2">
              <span className="font-mono text-xs font-black text-blue-400 bg-blue-500/10 px-2.5 py-1 rounded-lg border border-blue-500/20">
                {currentReq.id}
              </span>
              <StatusBadge status={currentReq.status} variant="warning" />
              <span className="text-xs text-slate-500 font-bold flex items-center gap-1">
                <Clock size={14} /> {currentReq.requestDate}
              </span>
            </div>
            <h2 className="text-2xl font-black text-white italic tracking-tight">
              {currentReq.partnerName} 변경 승인 요청
            </h2>
          </div>

          <div className="flex items-center gap-4 bg-slate-950/60 px-4 py-3 rounded-2xl border border-white/5 text-xs font-bold text-slate-300">
            <User size={16} className="text-blue-400" />
            <div>
              <span className="text-slate-500 block text-[10px]">요청자</span>
              <span>{currentReq.requester}</span>
            </div>
          </div>
        </div>

        {/* Change Reason Note */}
        <div className="p-4 rounded-2xl bg-white/[0.02] border border-white/5 flex items-start gap-3 text-xs text-slate-300">
          <FileText size={18} className="text-blue-400 shrink-0 mt-0.5" />
          <div>
            <span className="font-bold text-white block mb-1">변경 신청 사유:</span>
            <span>{currentReq.reason}</span>
          </div>
        </div>

        {/* Split Diff View (Before vs After) */}
        <div>
          <h3 className="text-sm font-black text-white uppercase tracking-widest mb-4 flex items-center gap-2">
            <GitPullRequest size={16} className="text-blue-400" />
            속성 변경 비교 (Before vs After Diff)
          </h3>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Left Column: Before (Current State) */}
            <div className="p-6 rounded-[2rem] bg-rose-950/10 border border-rose-500/20 space-y-4">
              <div className="flex items-center justify-between pb-3 border-b border-rose-500/20">
                <span className="text-xs font-black text-rose-400 uppercase tracking-widest flex items-center gap-1.5">
                  <XCircle size={16} /> 변경 전 (Current Master)
                </span>
                <span className="text-[10px] font-bold text-rose-300/60">기존 마스터 값</span>
              </div>

              <div className="space-y-3">
                {Object.entries(currentReq.before).map(([key, val]) => (
                  <div
                    key={key}
                    className="flex items-center justify-between p-3 rounded-xl bg-slate-950/60 border border-white/5 text-xs"
                  >
                    <span className="font-bold text-slate-400">{key}</span>
                    <span className="font-mono font-bold text-slate-300">{val}</span>
                  </div>
                ))}
              </div>
            </div>

            {/* Right Column: After (Proposed State) */}
            <div className="p-6 rounded-[2rem] bg-emerald-950/10 border border-emerald-500/20 space-y-4">
              <div className="flex items-center justify-between pb-3 border-b border-emerald-500/20">
                <span className="text-xs font-black text-emerald-400 uppercase tracking-widest flex items-center gap-1.5">
                  <CheckCircle2 size={16} /> 변경 후 (Proposed Target)
                </span>
                <span className="text-[10px] font-bold text-emerald-300/60">신규 신청 값</span>
              </div>

              <div className="space-y-3">
                {Object.entries(currentReq.after).map(([key, val]) => {
                  const isChanged = val.includes('(변경)') || val.includes('(증액)');
                  return (
                    <div
                      key={key}
                      className={`flex items-center justify-between p-3 rounded-xl text-xs transition-all border ${
                        isChanged
                          ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-200 font-bold'
                          : 'bg-slate-950/60 border-white/5 text-slate-300'
                      }`}
                    >
                      <span className="font-bold text-slate-300">{key}</span>
                      <span
                        className={`font-mono ${
                          isChanged ? 'text-emerald-300 font-black' : 'text-slate-300 font-bold'
                        }`}
                      >
                        {val}
                      </span>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        </div>

        {/* Approval Opinion & Actions */}
        <div className="pt-6 border-t border-white/5 space-y-4">
          <label className="text-xs font-black text-slate-400 uppercase tracking-widest block">
            승인 / 반려 검토 의견
          </label>
          <textarea
            rows={3}
            placeholder="결제계좌 검증 결과 정상이며, 대표자 변경 서류 확인 완료함..."
            value={opinion}
            onChange={(e) => setOpinion(e.target.value)}
            className="w-full bg-slate-950 border border-white/10 focus:border-blue-500 rounded-2xl p-4 text-xs font-bold text-white outline-none transition-all placeholder:text-slate-700"
          />

          <div className="flex items-center justify-end gap-4">
            <button
              onClick={handleReject}
              className="px-6 py-3.5 bg-rose-500/10 hover:bg-rose-500/20 border border-rose-500/30 text-rose-300 font-black text-xs rounded-2xl transition-all flex items-center gap-2"
            >
              <X size={16} /> 반려 (Reject)
            </button>
            <button
              onClick={handleApprove}
              className="px-8 py-3.5 bg-emerald-600 hover:bg-emerald-500 text-white font-black text-xs rounded-2xl transition-all shadow-lg shadow-emerald-600/25 flex items-center gap-2"
            >
              <Check size={16} /> 변경 승인 (Approve)
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

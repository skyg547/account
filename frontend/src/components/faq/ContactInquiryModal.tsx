"use client";

import React, { useState } from 'react';
import { X, Send, CheckCircle2, AlertCircle, HelpCircle } from 'lucide-react';

interface ContactInquiryModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export default function ContactInquiryModal({
  isOpen,
  onClose,
}: ContactInquiryModalProps) {
  const [category, setCategory] = useState('JOURNAL');
  const [title, setTitle] = useState('');
  const [referenceNo, setReferenceNo] = useState('');
  const [content, setContent] = useState('');
  const [email, setEmail] = useState('user@account.ai');
  const [isSubmitted, setIsSubmitted] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !content.trim()) return;

    setIsLoading(true);
    setTimeout(() => {
      setIsLoading(false);
      setIsSubmitted(true);
    }, 800);
  };

  const handleResetAndClose = () => {
    setIsSubmitted(false);
    setTitle('');
    setContent('');
    setReferenceNo('');
    onClose();
  };

  return (
    <div className="fixed inset-0 z-[200] flex items-center justify-center p-4 bg-slate-950/70 backdrop-blur-md animate-in fade-in duration-200">
      <div className="bg-white dark:bg-[#0b101f] w-full max-w-xl rounded-3xl border border-slate-200 dark:border-white/10 shadow-2xl overflow-hidden animate-in zoom-in-95 duration-300">
        {/* Header */}
        <div className="flex items-center justify-between px-7 py-5 border-b border-slate-100 dark:border-white/5 bg-slate-50/50 dark:bg-white/[0.02]">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-[#4262ff]/10 text-[#4262ff] flex items-center justify-center">
              <HelpCircle size={22} />
            </div>
            <div>
              <h3 className="text-lg font-black text-slate-900 dark:text-white">
                1:1 맞춤 문의하기
              </h3>
              <p className="text-xs text-slate-500">
                전문 상담원이 영업일 기준 2시간 이내에 신속히 답변드립니다.
              </p>
            </div>
          </div>
          <button
            onClick={handleResetAndClose}
            className="w-8 h-8 rounded-full bg-slate-100 dark:bg-white/5 hover:bg-slate-200 dark:hover:bg-white/10 text-slate-500 flex items-center justify-center transition-colors"
          >
            <X size={18} />
          </button>
        </div>

        {/* Content Body */}
        <div className="p-7">
          {isSubmitted ? (
            <div className="py-8 text-center space-y-4">
              <div className="w-16 h-16 rounded-3xl bg-emerald-500/10 text-emerald-500 flex items-center justify-center mx-auto animate-in zoom-in-50 duration-500">
                <CheckCircle2 size={36} />
              </div>
              <div className="space-y-1">
                <h4 className="text-xl font-bold text-slate-900 dark:text-white">
                  문의가 정상적으로 접수되었습니다!
                </h4>
                <p className="text-xs text-slate-500 max-w-sm mx-auto">
                  접수 번호: <strong className="text-[#4262ff]">INQ-{Date.now().toString().slice(-6)}</strong>
                  <br />
                  입력하신 이메일(<strong className="text-slate-700 dark:text-slate-300">{email}</strong>)로 답변 완료 시 알림 메일이 발송됩니다.
                </p>
              </div>

              <div className="pt-4">
                <button
                  onClick={handleResetAndClose}
                  className="px-6 py-3 bg-[#4262ff] hover:bg-[#3452e6] text-white rounded-xl font-bold text-sm transition-all shadow-lg shadow-blue-500/25"
                >
                  확인 및 닫기
                </button>
              </div>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-4">
              {/* Inquiry Category */}
              <div>
                <label className="block text-xs font-bold text-slate-600 dark:text-slate-400 mb-1.5">
                  문의 유형 <span className="text-rose-500">*</span>
                </label>
                <select
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  className="w-full px-4 py-2.5 bg-slate-50 dark:bg-[#121829] border border-slate-200 dark:border-white/10 rounded-xl text-sm font-medium text-slate-800 dark:text-slate-200 outline-none focus:border-[#4262ff] focus:ring-2 focus:ring-[#4262ff]/20"
                >
                  <option value="JOURNAL">전표 작성 / 역분개 / 결재 승인</option>
                  <option value="CLOSING">일마감 / 월결산 / 회계기간 잠금</option>
                  <option value="LOAN">여신 계약 / 이자 일할 계산 / 상환</option>
                  <option value="TAX">전자세금계산서 / 국세청 전송 / 부가세</option>
                  <option value="EXPENDITURE">지출결의서 / 예산 배정 및 전용</option>
                  <option value="AUTH">2차 인증(2FA) / 로그인 / 사용자 권한</option>
                  <option value="OTHER">기타 시스템 기능 및 오류 제보</option>
                </select>
              </div>

              {/* Reference No (Optional) */}
              <div>
                <label className="block text-xs font-bold text-slate-600 dark:text-slate-400 mb-1.5">
                  관련 번호 (전표번호, 계좌번호, 계약번호 등 선택)
                </label>
                <input
                  type="text"
                  value={referenceNo}
                  onChange={(e) => setReferenceNo(e.target.value)}
                  placeholder="예: JRN-202608-0042, ACC-992-019"
                  className="w-full px-4 py-2.5 bg-slate-50 dark:bg-[#121829] border border-slate-200 dark:border-white/10 rounded-xl text-sm text-slate-800 dark:text-slate-200 outline-none focus:border-[#4262ff] focus:ring-2 focus:ring-[#4262ff]/20 placeholder:text-slate-400"
                />
              </div>

              {/* Title */}
              <div>
                <label className="block text-xs font-bold text-slate-600 dark:text-slate-400 mb-1.5">
                  문의 제목 <span className="text-rose-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  placeholder="문의하실 내용의 핵심 제목을 입력해주세요."
                  className="w-full px-4 py-2.5 bg-slate-50 dark:bg-[#121829] border border-slate-200 dark:border-white/10 rounded-xl text-sm text-slate-800 dark:text-slate-200 outline-none focus:border-[#4262ff] focus:ring-2 focus:ring-[#4262ff]/20 placeholder:text-slate-400"
                />
              </div>

              {/* Content */}
              <div>
                <label className="block text-xs font-bold text-slate-600 dark:text-slate-400 mb-1.5">
                  상세 내용 <span className="text-rose-500">*</span>
                </label>
                <textarea
                  required
                  rows={4}
                  value={content}
                  onChange={(e) => setContent(e.target.value)}
                  placeholder="발생한 문제 상황, 화면 위치, 오류 메시지 등을 자세히 적어주시면 더욱 빠른 처리가 가능합니다."
                  className="w-full px-4 py-3 bg-slate-50 dark:bg-[#121829] border border-slate-200 dark:border-white/10 rounded-xl text-sm text-slate-800 dark:text-slate-200 outline-none focus:border-[#4262ff] focus:ring-2 focus:ring-[#4262ff]/20 placeholder:text-slate-400 resize-none"
                />
              </div>

              {/* Notification Email */}
              <div>
                <label className="block text-xs font-bold text-slate-600 dark:text-slate-400 mb-1.5">
                  답변 수신 이메일 <span className="text-rose-500">*</span>
                </label>
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full px-4 py-2.5 bg-slate-50 dark:bg-[#121829] border border-slate-200 dark:border-white/10 rounded-xl text-sm text-slate-800 dark:text-slate-200 outline-none focus:border-[#4262ff] focus:ring-2 focus:ring-[#4262ff]/20"
                />
              </div>

              {/* Notice */}
              <div className="flex items-center gap-2 text-[11px] text-slate-400">
                <AlertCircle size={13} className="shrink-0 text-slate-500" />
                <span>개인정보 및 비밀번호, 보안키는 본문에 기재하지 마세요.</span>
              </div>

              {/* Submit Buttons */}
              <div className="pt-3 flex items-center justify-end gap-3">
                <button
                  type="button"
                  onClick={handleResetAndClose}
                  className="px-5 py-2.5 rounded-xl border border-slate-200 dark:border-white/10 text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-white/5 font-bold text-sm transition-colors"
                >
                  취소
                </button>
                <button
                  type="submit"
                  disabled={isLoading || !title.trim() || !content.trim()}
                  className="px-6 py-2.5 rounded-xl bg-[#4262ff] hover:bg-[#3452e6] disabled:opacity-50 disabled:pointer-events-none text-white font-bold text-sm transition-all shadow-lg shadow-blue-500/25 flex items-center gap-2"
                >
                  {isLoading ? (
                    <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  ) : (
                    <>
                      <Send size={15} />
                      <span>문의 등록</span>
                    </>
                  )}
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}

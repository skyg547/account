"use client";

import React, { useState } from 'react';
import { MessageSquare, Mail, PhoneCall, Clock, ChevronRight, Bot } from 'lucide-react';
import ContactInquiryModal from './ContactInquiryModal';

export default function FaqSupportSection() {
  const [isInquiryModalOpen, setIsInquiryModalOpen] = useState(false);
  const [isChatbotOpen, setIsChatbotOpen] = useState(false);

  return (
    <>
      <div className="w-full bg-gradient-to-br from-slate-900 to-[#0b1329] dark:from-[#060a14] dark:to-[#0f172a] rounded-3xl p-8 sm:p-12 border border-slate-800 dark:border-white/10 shadow-2xl relative overflow-hidden">
        {/* Background Ambient Glow */}
        <div className="absolute top-0 right-0 w-96 h-96 bg-[#4262ff]/10 rounded-full blur-3xl pointer-events-none" />

        <div className="relative z-10 space-y-8">
          {/* Header */}
          <div className="text-center max-w-2xl mx-auto space-y-2">
            <span className="text-xs font-black uppercase tracking-widest text-[#4262ff] bg-[#4262ff]/10 px-3.5 py-1 rounded-full border border-blue-500/20 inline-block">
              CUSTOMER SUPPORT
            </span>
            <h2 className="text-2xl sm:text-3xl font-black text-white tracking-tight">
              원하시는 답변을 찾지 못하셨나요?
            </h2>
            <p className="text-sm text-slate-400">
              Account.AI 전담 고객지원 센터가 신속하고 친절하게 도와드리겠습니다.
            </p>
          </div>

          {/* 3 Support Channels */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 max-w-5xl mx-auto">
            {/* Card 1: 24/7 AI Chatbot */}
            <div className="bg-white/5 hover:bg-white/10 border border-white/5 hover:border-[#4262ff]/40 rounded-2xl p-6 transition-all duration-300 group flex flex-col justify-between backdrop-blur-sm">
              <div className="space-y-4">
                <div className="w-12 h-12 rounded-2xl bg-[#4262ff]/20 text-[#4262ff] flex items-center justify-center group-hover:scale-110 transition-transform">
                  <Bot size={26} />
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="text-base font-bold text-white">AI 챗봇 상담</h3>
                    <span className="text-[10px] font-black text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded-full">24시간</span>
                  </div>
                  <p className="text-xs text-slate-400 mt-1 leading-relaxed">
                    실시간 AI가 전표 작성, 결산, 규정 질문에 즉시 답변해 드립니다.
                  </p>
                </div>
              </div>

              <button
                onClick={() => setIsChatbotOpen(!isChatbotOpen)}
                className="mt-6 flex items-center justify-between w-full text-xs font-bold text-[#4262ff] group-hover:text-blue-300 transition-colors pt-4 border-t border-white/5"
              >
                <span>{isChatbotOpen ? '챗봇 닫기' : '챗봇 시작하기'}</span>
                <ChevronRight size={15} className="group-hover:translate-x-1 transition-transform" />
              </button>
            </div>

            {/* Card 2: 1:1 Online Inquiry */}
            <div className="bg-white/5 hover:bg-white/10 border border-white/5 hover:border-emerald-500/40 rounded-2xl p-6 transition-all duration-300 group flex flex-col justify-between backdrop-blur-sm">
              <div className="space-y-4">
                <div className="w-12 h-12 rounded-2xl bg-emerald-500/20 text-emerald-400 flex items-center justify-center group-hover:scale-110 transition-transform">
                  <Mail size={26} />
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="text-base font-bold text-white">1:1 맞춤 문의</h3>
                    <span className="text-[10px] font-black text-blue-400 bg-blue-500/10 px-2 py-0.5 rounded-full">전담 배정</span>
                  </div>
                  <p className="text-xs text-slate-400 mt-1 leading-relaxed">
                    복잡한 회계 처리 및 시스템 오류에 대해 전문 상담원이 분석 후 회신합니다.
                  </p>
                </div>
              </div>

              <button
                onClick={() => setIsInquiryModalOpen(true)}
                className="mt-6 flex items-center justify-between w-full text-xs font-bold text-emerald-400 group-hover:text-emerald-300 transition-colors pt-4 border-t border-white/5"
              >
                <span>문의 작성하기</span>
                <ChevronRight size={15} className="group-hover:translate-x-1 transition-transform" />
              </button>
            </div>

            {/* Card 3: Phone Support */}
            <div className="bg-white/5 hover:bg-white/10 border border-white/5 hover:border-amber-500/40 rounded-2xl p-6 transition-all duration-300 group flex flex-col justify-between backdrop-blur-sm">
              <div className="space-y-4">
                <div className="w-12 h-12 rounded-2xl bg-amber-500/20 text-amber-400 flex items-center justify-center group-hover:scale-110 transition-transform">
                  <PhoneCall size={26} />
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="text-base font-bold text-white">고객센터 전화상담</h3>
                  </div>
                  <div className="text-xl font-black text-white italic tracking-tight mt-1">
                    1522-1000
                  </div>
                  <p className="text-xs text-slate-400 mt-1 flex items-center gap-1.5">
                    <Clock size={12} className="text-slate-500" />
                    평일 09:00 ~ 18:00 (공휴일 휴무)
                  </p>
                </div>
              </div>

              <div className="mt-6 flex items-center justify-between w-full text-xs font-bold text-amber-400 pt-4 border-t border-white/5">
                <span>금융사고 전용 24시간 연결</span>
              </div>
            </div>
          </div>

          {/* Interactive Mini Chatbot Simulation Drawer */}
          {isChatbotOpen && (
            <div className="max-w-2xl mx-auto bg-slate-950 border border-blue-500/30 rounded-2xl p-6 space-y-4 animate-in slide-in-from-bottom-2 duration-300">
              <div className="flex items-center justify-between border-b border-white/5 pb-3">
                <div className="flex items-center gap-2 text-white text-sm font-bold">
                  <Bot size={18} className="text-[#4262ff]" />
                  <span>AI 상담 어시스턴트</span>
                </div>
                <button
                  onClick={() => setIsChatbotOpen(false)}
                  className="text-xs text-slate-400 hover:text-white"
                >
                  닫기
                </button>
              </div>

              <div className="space-y-3 max-h-48 overflow-y-auto custom-scrollbar text-xs">
                <div className="flex gap-2">
                  <div className="w-6 h-6 rounded-full bg-[#4262ff] text-white flex items-center justify-center shrink-0 font-bold text-[10px]">
                    AI
                  </div>
                  <div className="bg-white/10 text-slate-200 p-3 rounded-xl rounded-tl-none max-w-md">
                    안녕하세요! 무엇을 도와드릴까요? 아래 추천 질문을 선택하시거나 질문을 입력해 주세요.
                  </div>
                </div>
                <div className="pl-8 flex flex-wrap gap-1.5">
                  {['전표 역분개 방법', '결산 마감 해제', '대출 이자 계산'].map((q) => (
                    <button
                      key={q}
                      onClick={() => {}}
                      className="px-2.5 py-1 bg-[#4262ff]/20 hover:bg-[#4262ff]/40 text-blue-300 rounded-lg border border-blue-400/30 text-[11px]"
                    >
                      {q}
                    </button>
                  ))}
                </div>
              </div>

              <div className="flex gap-2">
                <input
                  type="text"
                  placeholder="메시지를 입력하세요..."
                  className="flex-1 bg-slate-900 border border-white/10 rounded-xl px-4 py-2 text-xs text-white outline-none focus:border-[#4262ff]"
                />
                <button className="px-4 py-2 bg-[#4262ff] text-white rounded-xl text-xs font-bold hover:bg-[#3452e6] transition-colors">
                  전송
                </button>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* 1:1 Contact Modal */}
      <ContactInquiryModal
        isOpen={isInquiryModalOpen}
        onClose={() => setIsInquiryModalOpen(false)}
      />
    </>
  );
}

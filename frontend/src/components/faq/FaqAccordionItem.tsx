"use client";

import React, { useState } from 'react';
import Link from 'next/link';
import { 
  ChevronDown, 
  ThumbsUp, 
  ThumbsDown, 
  ExternalLink, 
  AlertCircle, 
  Lightbulb, 
  CheckCircle2, 
  Eye, 
  Flame 
} from 'lucide-react';
import { FaqItem, FAQ_CATEGORIES } from '@/mocks/faq';

interface FaqAccordionItemProps {
  item: FaqItem;
  isOpen: boolean;
  onToggle: () => void;
}

export default function FaqAccordionItem({
  item,
  isOpen,
  onToggle,
}: FaqAccordionItemProps) {
  const [feedback, setFeedback] = useState<'HELPFUL' | 'UNHELPFUL' | null>(null);
  const [helpfulCount, setHelpfulCount] = useState(item.helpfulCount);
  const [unhelpfulCount, setUnhelpfulCount] = useState(item.unhelpfulCount);

  const categoryLabel = FAQ_CATEGORIES.find((c) => c.id === item.category)?.label || item.category;

  const handleFeedback = (type: 'HELPFUL' | 'UNHELPFUL', e: React.MouseEvent) => {
    e.stopPropagation();
    if (feedback === type) return;

    if (type === 'HELPFUL') {
      setHelpfulCount((prev) => prev + 1);
      if (feedback === 'UNHELPFUL') setUnhelpfulCount((prev) => Math.max(0, prev - 1));
    } else {
      setUnhelpfulCount((prev) => prev + 1);
      if (feedback === 'HELPFUL') setHelpfulCount((prev) => Math.max(0, prev - 1));
    }
    setFeedback(type);
  };

  return (
    <div
      className={`rounded-2xl border transition-all duration-300 overflow-hidden ${
        isOpen
          ? 'bg-white dark:bg-[#0a0f1d] border-[#4262ff]/40 shadow-xl shadow-blue-500/5 ring-1 ring-[#4262ff]/20'
          : 'bg-white/80 dark:bg-[#060a15]/80 border-slate-200/80 dark:border-white/5 hover:border-slate-300 dark:hover:border-white/15 hover:shadow-md'
      }`}
    >
      {/* ── Question Header (Click to Toggle) ── */}
      <button
        onClick={onToggle}
        className="w-full px-7 py-6 text-left flex items-start justify-between gap-5 cursor-pointer select-none group"
      >
        <div className="flex items-start gap-4 flex-1">
          {/* Q Badge */}
          <div
            className={`w-8 h-8 rounded-xl flex items-center justify-center font-black text-sm shrink-0 transition-colors duration-300 ${
              isOpen
                ? 'bg-[#4262ff] text-white shadow-md shadow-blue-600/30'
                : 'bg-blue-50 dark:bg-blue-950/50 text-[#4262ff] group-hover:bg-[#4262ff] group-hover:text-white'
            }`}
          >
            Q
          </div>

          <div className="space-y-1.5 flex-1">
            {/* Category & Tags Row */}
            <div className="flex flex-wrap items-center gap-2">
              <span className="text-[11px] font-extrabold text-[#4262ff] bg-blue-50 dark:bg-blue-950/40 px-2.5 py-0.5 rounded-md border border-blue-200/50 dark:border-blue-800/30">
                {categoryLabel}
              </span>
              {item.isPopular && (
                <span className="text-[10px] font-black text-rose-500 bg-rose-50 dark:bg-rose-950/40 px-2 py-0.5 rounded-md border border-rose-200/50 dark:border-rose-800/30 flex items-center gap-0.5">
                  <Flame size={12} className="fill-rose-500" />
                  인기
                </span>
              )}
              <span className="text-[11px] text-slate-400 dark:text-slate-500 flex items-center gap-1 ml-auto">
                <Eye size={12} />
                조회 {item.viewCount.toLocaleString()}
              </span>
            </div>

            {/* Question Text */}
            <h3
              className={`text-base sm:text-lg font-bold tracking-tight transition-colors duration-200 ${
                isOpen
                  ? 'text-[#4262ff] dark:text-[#5875ff]'
                  : 'text-slate-800 dark:text-slate-100 group-hover:text-[#4262ff]'
              }`}
            >
              {item.question}
            </h3>
          </div>
        </div>

        {/* Arrow Chevron */}
        <div
          className={`w-9 h-9 rounded-xl flex items-center justify-center shrink-0 transition-all duration-300 ${
            isOpen
              ? 'bg-[#4262ff]/10 text-[#4262ff] rotate-180'
              : 'bg-slate-100 dark:bg-slate-800/50 text-slate-400 group-hover:text-slate-700 dark:group-hover:text-white'
          }`}
        >
          <ChevronDown size={19} />
        </div>
      </button>

      {/* ── Answer Accordion Body ── */}
      {isOpen && (
        <div className="px-7 pb-7 pt-2 border-t border-slate-100 dark:border-white/5 animate-in fade-in slide-in-from-top-2 duration-300">
          <div className="pl-12 space-y-6">
            {/* Summary Box */}
            <div className="text-slate-700 dark:text-slate-200 text-sm sm:text-base leading-relaxed font-medium bg-slate-50/70 dark:bg-white/[0.02] p-4 rounded-xl border border-slate-100 dark:border-white/5">
              {item.answer.summary}
            </div>

            {/* Step by Step Guide */}
            {item.answer.steps && item.answer.steps.length > 0 && (
              <div className="space-y-3">
                <h4 className="text-xs font-black uppercase tracking-wider text-slate-500 dark:text-slate-400 flex items-center gap-2">
                  <CheckCircle2 size={15} className="text-[#4262ff]" />
                  단계별 처리 방법
                </h4>
                <ol className="space-y-2">
                  {item.answer.steps.map((step, idx) => (
                    <li
                      key={idx}
                      className="flex items-start gap-3 text-sm text-slate-700 dark:text-slate-300"
                    >
                      <span className="w-5 h-5 rounded-full bg-[#4262ff]/10 text-[#4262ff] text-xs font-black flex items-center justify-center shrink-0 mt-0.5">
                        {idx + 1}
                      </span>
                      <span className="leading-snug">{step}</span>
                    </li>
                  ))}
                </ol>
              </div>
            )}

            {/* Notice Callout */}
            {item.answer.notice && (
              <div className="flex items-start gap-3 bg-amber-500/10 border border-amber-500/20 text-amber-900 dark:text-amber-300 p-4 rounded-xl text-xs sm:text-sm leading-relaxed">
                <AlertCircle size={18} className="text-amber-500 shrink-0 mt-0.5" />
                <div>
                  <strong className="font-bold mr-1">주의사항:</strong>
                  {item.answer.notice}
                </div>
              </div>
            )}

            {/* Tips Callout */}
            {item.answer.tips && item.answer.tips.length > 0 && (
              <div className="space-y-2 bg-blue-500/5 border border-blue-500/15 p-4 rounded-xl">
                <h4 className="text-xs font-black text-[#4262ff] flex items-center gap-1.5">
                  <Lightbulb size={15} />
                  알아두면 유용한 팁
                </h4>
                <ul className="space-y-1.5 pl-1">
                  {item.answer.tips.map((tip, idx) => (
                    <li
                      key={idx}
                      className="text-xs sm:text-sm text-slate-600 dark:text-slate-300 flex items-start gap-2"
                    >
                      <span className="text-[#4262ff] font-bold">•</span>
                      <span>{tip}</span>
                    </li>
                  ))}
                </ul>
              </div>
            )}

            {/* Related Direct Link */}
            {item.answer.relatedLink && (
              <div className="pt-1">
                <Link
                  href={item.answer.relatedLink.href}
                  className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-[#4262ff] hover:bg-[#3452e6] text-white text-xs font-bold transition-all shadow-md shadow-blue-600/20 active:scale-95 group/link"
                >
                  <span>{item.answer.relatedLink.label}</span>
                  <ExternalLink size={13} className="group-hover/link:translate-x-0.5 transition-transform" />
                </Link>
              </div>
            )}

            {/* ── Helpfulness Feedback Section ── */}
            <div className="pt-4 mt-4 border-t border-slate-100 dark:border-white/5 flex flex-wrap items-center justify-between gap-4">
              <div className="text-xs font-bold text-slate-500 dark:text-slate-400">
                이 답변이 도움이 되셨나요?
              </div>

              <div className="flex items-center gap-2">
                <button
                  onClick={(e) => handleFeedback('HELPFUL', e)}
                  className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-bold transition-all border ${
                    feedback === 'HELPFUL'
                      ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/30'
                      : 'bg-slate-100 dark:bg-white/5 text-slate-600 dark:text-slate-300 border-transparent hover:border-slate-300 dark:hover:border-white/10'
                  }`}
                >
                  <ThumbsUp size={13} className={feedback === 'HELPFUL' ? 'fill-current' : ''} />
                  <span>네 ({helpfulCount})</span>
                </button>

                <button
                  onClick={(e) => handleFeedback('UNHELPFUL', e)}
                  className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-bold transition-all border ${
                    feedback === 'UNHELPFUL'
                      ? 'bg-rose-500/10 text-rose-600 dark:text-rose-400 border-rose-500/30'
                      : 'bg-slate-100 dark:bg-white/5 text-slate-600 dark:text-slate-300 border-transparent hover:border-slate-300 dark:hover:border-white/10'
                  }`}
                >
                  <ThumbsDown size={13} className={feedback === 'UNHELPFUL' ? 'fill-current' : ''} />
                  <span>아니오 ({unhelpfulCount})</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

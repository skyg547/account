"use client";

import React from 'react';
import { Search, X, Sparkles } from 'lucide-react';
import { POPULAR_SEARCH_TAGS } from '@/mocks/faq';

interface FaqSearchProps {
  searchQuery: string;
  setSearchQuery: (query: string) => void;
  onSearch?: (query: string) => void;
  totalCount: number;
}

export default function FaqSearch({
  searchQuery,
  setSearchQuery,
  onSearch,
  totalCount,
}: FaqSearchProps) {
  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && onSearch) {
      onSearch(searchQuery);
    }
  };

  const handleTagClick = (tag: string) => {
    setSearchQuery(tag);
    if (onSearch) {
      onSearch(tag);
    }
  };

  return (
    <div className="w-full max-w-4xl mx-auto space-y-4">
      {/* ── KBank Style Big Search Box ── */}
      <div className="relative group">
        <div className="absolute inset-y-0 left-0 pl-6 flex items-center pointer-events-none text-slate-400 group-focus-within:text-[#4262ff] transition-colors">
          <Search size={22} />
        </div>
        
        <input
          type="text"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="궁금한 내용을 검색해보세요 (예: 전표 역분개, 결산 마감, 대출 이자)"
          className="w-full pl-15 pr-14 py-5 bg-white dark:bg-[#0a0f1d] text-slate-900 dark:text-white placeholder:text-slate-400 rounded-2xl border border-slate-200 dark:border-white/10 shadow-xl shadow-blue-500/5 focus:border-[#4262ff] focus:ring-4 focus:ring-[#4262ff]/15 outline-none transition-all duration-300 text-base font-medium"
        />

        {searchQuery && (
          <button
            onClick={() => setSearchQuery('')}
            className="absolute inset-y-0 right-4 my-auto w-8 h-8 rounded-full bg-slate-100 dark:bg-slate-800 text-slate-400 hover:text-slate-600 dark:hover:text-white flex items-center justify-center transition-colors"
            title="검색어 지우기"
          >
            <X size={16} />
          </button>
        )}
      </div>

      {/* ── Popular Search Tags ── */}
      <div className="flex flex-wrap items-center gap-2 pt-1 px-2">
        <span className="text-xs font-bold text-slate-400 dark:text-slate-500 flex items-center gap-1.5 mr-1 shrink-0">
          <Sparkles size={14} className="text-[#4262ff]" />
          인기 검색어:
        </span>
        {POPULAR_SEARCH_TAGS.map((tag) => {
          const isSelected = searchQuery === tag;
          return (
            <button
              key={tag}
              onClick={() => handleTagClick(tag)}
              className={`text-xs px-3.5 py-1.5 rounded-full transition-all duration-200 border font-medium ${
                isSelected
                  ? 'bg-[#4262ff] text-white border-[#4262ff] shadow-md shadow-blue-500/25 scale-105'
                  : 'bg-white/80 dark:bg-white/5 text-slate-600 dark:text-slate-300 border-slate-200 dark:border-white/5 hover:border-[#4262ff]/40 hover:text-[#4262ff] hover:bg-blue-50/50 dark:hover:bg-blue-500/10'
              }`}
            >
              #{tag}
            </button>
          );
        })}
      </div>

      {/* ── Search Status Hint ── */}
      {searchQuery && (
        <div className="text-xs text-slate-500 dark:text-slate-400 px-2 pt-1 flex items-center justify-between">
          <span>
            <strong className="text-[#4262ff] font-bold">&lsquo;{searchQuery}&rsquo;</strong> 검색 결과 총 <strong className="text-slate-900 dark:text-white font-bold">{totalCount}</strong>건
          </span>
          <button
            onClick={() => setSearchQuery('')}
            className="text-xs text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 underline"
          >
            전체 목록 보기
          </button>
        </div>
      )}
    </div>
  );
}

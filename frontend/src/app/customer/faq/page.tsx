"use client";

import React, { useState, useMemo, useSyncExternalStore } from 'react';
import { 
  HelpCircle, 
  Sparkles, 
  SlidersHorizontal, 
  RotateCcw, 
  ChevronLeft, 
  ChevronRight,
  Headphones,
  Check
} from 'lucide-react';
import { mockFaqList, FaqCategory, FaqItem } from '@/mocks/faq';
import FaqSearch from '@/components/faq/FaqSearch';
import FaqCategoryTabs from '@/components/faq/FaqCategoryTabs';
import FaqAccordionItem from '@/components/faq/FaqAccordionItem';
import FaqSupportSection from '@/components/faq/FaqSupportSection';
import PageHeader from '@/components/ui/PageHeader';

const emptySubscribe = () => () => {};

type SortOption = 'POPULAR' | 'VIEWS' | 'DEFAULT';

export default function CustomerFaqPage() {
  const isMounted = useSyncExternalStore(emptySubscribe, () => true, () => false);

  const [searchQuery, setSearchQuery] = useState('');
  const [activeCategory, setActiveCategory] = useState<FaqCategory>('ALL');
  const [openItemId, setOpenItemId] = useState<string | null>('faq-01');
  const [sortOption, setSortOption] = useState<SortOption>('POPULAR');
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 8;

  // ── 필터링 및 검색 로직 ──
  const filteredFaqs = useMemo(() => {
    return mockFaqList.filter((item) => {
      // 1. 카테고리 필터
      if (activeCategory === 'POPULAR' && !item.isPopular) return false;
      if (activeCategory !== 'ALL' && activeCategory !== 'POPULAR' && item.category !== activeCategory) {
        return false;
      }

      // 2. 검색어 필터
      if (!searchQuery.trim()) return true;
      const query = searchQuery.toLowerCase().trim();
      const inQuestion = item.question.toLowerCase().includes(query);
      const inSummary = item.answer.summary.toLowerCase().includes(query);
      const inTags = item.tags.some((tag) => tag.toLowerCase().includes(query));
      const inSteps = item.answer.steps?.some((s) => s.toLowerCase().includes(query)) || false;

      return inQuestion || inSummary || inTags || inSteps;
    }).sort((a, b) => {
      if (sortOption === 'POPULAR') {
        if (a.isPopular && !b.isPopular) return -1;
        if (!a.isPopular && b.isPopular) return 1;
        return b.helpfulCount - a.helpfulCount;
      }
      if (sortOption === 'VIEWS') {
        return b.viewCount - a.viewCount;
      }
      return 0;
    });
  }, [searchQuery, activeCategory, sortOption]);

  // ── 페이지네이션 계산 ──
  const totalPages = Math.ceil(filteredFaqs.length / itemsPerPage) || 1;
  const paginatedFaqs = useMemo(() => {
    const startIndex = (currentPage - 1) * itemsPerPage;
    return filteredFaqs.slice(startIndex, startIndex + itemsPerPage);
  }, [filteredFaqs, currentPage, itemsPerPage]);

  const handleToggleItem = (id: string) => {
    setOpenItemId((prev) => (prev === id ? null : id));
  };

  const handleCategoryChange = (cat: FaqCategory) => {
    setActiveCategory(cat);
    setCurrentPage(1);
  };

  const handleResetFilters = () => {
    setSearchQuery('');
    setActiveCategory('ALL');
    setSortOption('POPULAR');
    setCurrentPage(1);
  };

  if (!isMounted) return null;

  return (
    <div className="space-y-12 pb-16">
      {/* ── Top Page Header ── */}
      <PageHeader
        title="고객센터 자주 묻는 질문 (FAQ)"
        description="케이뱅크 스타일의 직관적이고 편리한 고객지원 안내 센터입니다. 궁금하신 내용을 빠르게 찾아보세요."
        breadcrumbs={[
          { label: '고객지원' },
          { label: '자주 묻는 질문 (FAQ)' },
        ]}
        icon={Headphones}
        actions={
          <div className="flex items-center gap-2 bg-[#4262ff]/10 text-[#4262ff] px-4 py-2 rounded-2xl border border-blue-500/20 text-xs font-bold">
            <Sparkles size={15} />
            <span>24시간 실시간 고객지원 운영 중</span>
          </div>
        }
      />

      {/* ── KBank Style Hero Search Banner ── */}
      <div className="relative rounded-3xl bg-gradient-to-b from-blue-600/10 via-slate-900/40 to-slate-900/60 dark:from-blue-600/15 dark:to-[#050914] p-8 sm:p-14 border border-blue-500/20 text-center space-y-8 backdrop-blur-xl shadow-2xl">
        <div className="max-w-3xl mx-auto space-y-3">
          <div className="inline-flex items-center gap-2 px-3.5 py-1 rounded-full bg-[#4262ff]/10 border border-blue-500/20 text-[#4262ff] text-xs font-black tracking-widest uppercase">
            <span>K-BANK STYLE CUSTOMER FAQ</span>
          </div>
          <h1 className="text-3xl sm:text-4xl font-black text-slate-900 dark:text-white tracking-tight leading-tight">
            무엇을 도와드릴까요?
          </h1>
          <p className="text-slate-500 dark:text-slate-400 text-sm sm:text-base font-medium">
            전표 작성부터 결산 마감, 대출 이자, 세금계산서 발행까지 자주 묻는 질문을 한곳에서 확인하세요.
          </p>
        </div>

        {/* Big Search Bar */}
        <FaqSearch
          searchQuery={searchQuery}
          setSearchQuery={(q) => {
            setSearchQuery(q);
            setCurrentPage(1);
          }}
          totalCount={filteredFaqs.length}
        />
      </div>

      {/* ── Category Tabs & Filter Toolbar ── */}
      <div className="space-y-6">
        <FaqCategoryTabs
          activeCategory={activeCategory}
          onSelectCategory={handleCategoryChange}
          faqList={mockFaqList}
        />

        {/* Toolbar (Result Count & Sort Options) */}
        <div className="flex flex-wrap items-center justify-between gap-4 px-2 py-1">
          <div className="text-sm font-bold text-slate-700 dark:text-slate-300 flex items-center gap-2">
            <span>
              검색 결과 <strong className="text-[#4262ff] font-extrabold">{filteredFaqs.length}</strong>건
            </span>
            {(searchQuery || activeCategory !== 'ALL') && (
              <button
                onClick={handleResetFilters}
                className="inline-flex items-center gap-1 text-xs text-slate-400 hover:text-[#4262ff] font-bold transition-colors ml-2"
              >
                <RotateCcw size={12} />
                필터 초기화
              </button>
            )}
          </div>

          {/* Sort Selector */}
          <div className="flex items-center gap-2">
            <SlidersHorizontal size={14} className="text-slate-400" />
            <div className="inline-flex bg-white dark:bg-[#0a0f1d] p-1 rounded-xl border border-slate-200 dark:border-white/5 text-xs font-bold">
              <button
                onClick={() => setSortOption('POPULAR')}
                className={`px-3 py-1.5 rounded-lg transition-all ${
                  sortOption === 'POPULAR'
                    ? 'bg-[#4262ff] text-white shadow-sm'
                    : 'text-slate-500 hover:text-slate-900 dark:hover:text-white'
                }`}
              >
                인기순
              </button>
              <button
                onClick={() => setSortOption('VIEWS')}
                className={`px-3 py-1.5 rounded-lg transition-all ${
                  sortOption === 'VIEWS'
                    ? 'bg-[#4262ff] text-white shadow-sm'
                    : 'text-slate-500 hover:text-slate-900 dark:hover:text-white'
                }`}
              >
                조회수순
              </button>
              <button
                onClick={() => setSortOption('DEFAULT')}
                className={`px-3 py-1.5 rounded-lg transition-all ${
                  sortOption === 'DEFAULT'
                    ? 'bg-[#4262ff] text-white shadow-sm'
                    : 'text-slate-500 hover:text-slate-900 dark:hover:text-white'
                }`}
              >
                기본순
              </button>
            </div>
          </div>
        </div>

        {/* ── FAQ Accordion List ── */}
        {paginatedFaqs.length > 0 ? (
          <div className="space-y-4">
            {paginatedFaqs.map((faq) => (
              <FaqAccordionItem
                key={faq.id}
                item={faq}
                isOpen={openItemId === faq.id}
                onToggle={() => handleToggleItem(faq.id)}
              />
            ))}
          </div>
        ) : (
          /* Empty Search State */
          <div className="text-center py-16 px-4 bg-white/50 dark:bg-[#0a0f1d]/50 rounded-3xl border border-dashed border-slate-200 dark:border-white/10 space-y-4">
            <div className="w-16 h-16 rounded-3xl bg-slate-100 dark:bg-white/5 text-slate-400 flex items-center justify-center mx-auto">
              <HelpCircle size={32} />
            </div>
            <div className="space-y-1 max-w-sm mx-auto">
              <h3 className="text-lg font-bold text-slate-800 dark:text-slate-200">
                검색 결과가 없습니다
              </h3>
              <p className="text-xs text-slate-500">
                입력하신 검색어에 해당하는 질문이 없습니다. 다른 키워드로 검색하시거나 필터를 초기화해 보세요.
              </p>
            </div>
            <button
              onClick={handleResetFilters}
              className="px-5 py-2.5 rounded-xl bg-[#4262ff] hover:bg-[#3452e6] text-white text-xs font-bold transition-all shadow-md shadow-blue-500/20 inline-flex items-center gap-2"
            >
              <RotateCcw size={14} />
              <span>전체 질문 보기</span>
            </button>
          </div>
        )}

        {/* ── Pagination ── */}
        {totalPages > 1 && (
          <div className="flex items-center justify-center gap-2 pt-6">
            <button
              onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
              disabled={currentPage === 1}
              className="w-10 h-10 rounded-xl bg-white dark:bg-[#0a0f1d] border border-slate-200 dark:border-white/5 text-slate-500 hover:text-slate-900 dark:hover:text-white disabled:opacity-40 disabled:pointer-events-none flex items-center justify-center transition-colors"
            >
              <ChevronLeft size={18} />
            </button>

            {Array.from({ length: totalPages }, (_, i) => i + 1).map((pageNum) => (
              <button
                key={pageNum}
                onClick={() => setCurrentPage(pageNum)}
                className={`w-10 h-10 rounded-xl font-bold text-sm transition-all ${
                  currentPage === pageNum
                    ? 'bg-[#4262ff] text-white shadow-md shadow-blue-600/30'
                    : 'bg-white dark:bg-[#0a0f1d] border border-slate-200 dark:border-white/5 text-slate-600 dark:text-slate-400 hover:border-slate-300'
                }`}
              >
                {pageNum}
              </button>
            ))}

            <button
              onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
              disabled={currentPage === totalPages}
              className="w-10 h-10 rounded-xl bg-white dark:bg-[#0a0f1d] border border-slate-200 dark:border-white/5 text-slate-500 hover:text-slate-900 dark:hover:text-white disabled:opacity-40 disabled:pointer-events-none flex items-center justify-center transition-colors"
            >
              <ChevronRight size={18} />
            </button>
          </div>
        )}
      </div>

      {/* ── Bottom KBank Style Customer Support Banner ── */}
      <FaqSupportSection />
    </div>
  );
}

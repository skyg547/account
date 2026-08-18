"use client";

import React from 'react';
import * as LucideIcons from 'lucide-react';
import { FAQ_CATEGORIES, FaqCategory, FaqItem } from '@/mocks/faq';

interface FaqCategoryTabsProps {
  activeCategory: FaqCategory;
  onSelectCategory: (category: FaqCategory) => void;
  faqList: FaqItem[];
}

function getCategoryIcon(name: string): React.ElementType {
  const iconMap = LucideIcons as Record<string, unknown>;
  const Component = iconMap[name] as React.ElementType;
  return Component || LucideIcons.HelpCircle;
}

export default function FaqCategoryTabs({
  activeCategory,
  onSelectCategory,
  faqList,
}: FaqCategoryTabsProps) {
  // 카테고리별 항목 개수 계산
  const getCategoryCount = (categoryId: FaqCategory) => {
    if (categoryId === 'ALL') return faqList.length;
    if (categoryId === 'POPULAR') return faqList.filter((item) => item.isPopular).length;
    return faqList.filter((item) => item.category === categoryId).length;
  };

  return (
    <div className="w-full">
      {/* ── Scrollable Tab Bar ── */}
      <div className="flex items-center gap-2 overflow-x-auto pb-3 pt-1 scrollbar-none no-scrollbar">
        {FAQ_CATEGORIES.map((cat) => {
          const Icon = getCategoryIcon(cat.iconName);
          const isActive = activeCategory === cat.id;
          const count = getCategoryCount(cat.id);

          return (
            <button
              key={cat.id}
              onClick={() => onSelectCategory(cat.id)}
              className={`flex items-center gap-2 px-5 py-3 rounded-2xl font-bold text-sm transition-all duration-300 shrink-0 border select-none active:scale-95 ${
                isActive
                  ? 'bg-[#4262ff] text-white border-[#4262ff] shadow-lg shadow-blue-600/25 ring-2 ring-blue-400/30 font-extrabold'
                  : 'bg-white dark:bg-[#0a0f1d] text-slate-600 dark:text-slate-400 border-slate-200 dark:border-white/5 hover:border-slate-300 dark:hover:border-white/15 hover:text-slate-900 dark:hover:text-white shadow-sm'
              }`}
            >
              <Icon
                size={17}
                className={`transition-transform duration-300 ${
                  isActive ? 'text-white scale-110' : 'text-slate-400 dark:text-slate-500'
                }`}
              />
              <span>{cat.label}</span>
              <span
                className={`text-[11px] px-2 py-0.5 rounded-full font-bold transition-colors ${
                  isActive
                    ? 'bg-white/20 text-white'
                    : 'bg-slate-100 dark:bg-white/5 text-slate-500 dark:text-slate-400'
                }`}
              >
                {count}
              </span>
            </button>
          );
        })}
      </div>
    </div>
  );
}

import React from 'react';
import { Loader2 } from 'lucide-react';
import { PageSkeleton } from '@/components/ui/LoadingSkeleton';

/**
 * [Next.js App Router Root Loading Skeleton]
 * 온디맨드 컴파일 지연 및 라우트 전환 시 깜빡임이나 흰 화면 없이
 * 부드러운 스켈레톤과 로딩 뱃지 피드백을 제공합니다.
 */
export default function Loading() {
  return (
    <div className="w-full relative animate-in fade-in duration-200">
      {/* 상단 로딩 상태 뱃지 */}
      <div className="flex items-center justify-between mb-6">
        <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-blue-50/80 dark:bg-blue-950/40 border border-blue-200/60 dark:border-blue-800/60 text-[#4262ff] dark:text-blue-400 text-xs font-bold shadow-xs">
          <Loader2 className="w-3.5 h-3.5 animate-spin" />
          <span>페이지 화면을 준비하고 있습니다...</span>
        </div>
      </div>

      {/* 종합 레이아웃 스켈레톤 */}
      <PageSkeleton />
    </div>
  );
}

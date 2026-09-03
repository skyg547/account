import React from 'react';

interface LoadingSkeletonProps {
  rows?: number;
  height?: string;
  className?: string;
}

export default function LoadingSkeleton({ rows = 3, height = 'h-12', className = '' }: LoadingSkeletonProps) {
  return (
    <div className={`space-y-3 w-full ${className}`}>
      {Array.from({ length: rows }).map((_, idx) => (
        <div
          key={idx}
          className={`${height} w-full bg-[#eaedf4]/70 dark:bg-slate-800/60 animate-pulse rounded-xl border border-[#eaedf4]/60 dark:border-slate-700/60`}
        />
      ))}
    </div>
  );
}

/**
 * [카드형 지표 스켈레톤]
 * 대시보드 및 재무 요약 카드 로딩에 적합한 스켈레톤 UI를 제공합니다.
 */
export function CardSkeleton({ count = 3 }: { count?: number }) {
  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-6 w-full">
      {Array.from({ length: count }).map((_, idx) => (
        <div
          key={idx}
          className="rounded-2xl bg-white dark:bg-[#131b2e] border border-[#eaedf4] dark:border-slate-800 p-6 space-y-4 animate-pulse shadow-xs"
        >
          <div className="flex items-center justify-between">
            <div className="w-12 h-12 rounded-xl bg-[#eaedf4]/70 dark:bg-slate-800/80" />
            <div className="w-16 h-6 rounded-full bg-[#eaedf4]/70 dark:bg-slate-800/80" />
          </div>
          <div className="w-28 h-3 rounded bg-[#eaedf4]/60 dark:bg-slate-800/60" />
          <div className="w-40 h-8 rounded-lg bg-[#eaedf4]/80 dark:bg-slate-800/80" />
          <div className="h-2 w-full rounded-full bg-[#eaedf4]/50 dark:bg-slate-800/50" />
        </div>
      ))}
    </div>
  );
}

/**
 * [테이블 및 그리드 스켈레톤]
 * 대량 데이터 목록 조회 시 빈 테이블 영역을 채우는 반응형 스켈레톤입니다.
 */
export function TableSkeleton({ rows = 5, cols = 4 }: { rows?: number; cols?: number }) {
  return (
    <div className="w-full bg-white dark:bg-[#131b2e] border border-[#eaedf4] dark:border-slate-800 rounded-2xl p-6 shadow-xs animate-pulse space-y-4">
      <div className="flex items-center justify-between pb-3 border-b border-[#eaedf4] dark:border-slate-800">
        <div className="w-36 h-5 rounded-lg bg-[#eaedf4]/80 dark:bg-slate-800/80" />
        <div className="w-20 h-7 rounded-xl bg-[#eaedf4]/60 dark:bg-slate-800/60" />
      </div>
      <div className="space-y-3 pt-2">
        {Array.from({ length: rows }).map((_, rIdx) => (
          <div key={rIdx} className="flex items-center gap-4 py-2">
            {Array.from({ length: cols }).map((_, cIdx) => (
              <div
                key={cIdx}
                className={`h-4 rounded bg-[#eaedf4]/70 dark:bg-slate-800/60 ${
                  cIdx === 0 ? 'w-1/4' : cIdx === 1 ? 'w-1/3' : 'w-1/6'
                }`}
              />
            ))}
          </div>
        ))}
      </div>
    </div>
  );
}

/**
 * [전체 페이지 트랜지션 스켈레톤]
 * 라우트 전환 및 온디맨드 컴파일 대기 시 사용자에게 정돈된 레이아웃을 즉시 보여줍니다.
 */
export function PageSkeleton() {
  return (
    <div className="space-y-8 animate-in fade-in duration-300 w-full pb-12">
      {/* 헤더 스켈레톤 */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-2">
        <div className="space-y-2">
          <div className="w-32 h-4 rounded bg-[#eaedf4]/70 dark:bg-slate-800/60 animate-pulse" />
          <div className="w-64 h-8 rounded-xl bg-[#eaedf4]/80 dark:bg-slate-800/80 animate-pulse" />
          <div className="w-96 h-4 rounded bg-[#eaedf4]/50 dark:bg-slate-800/50 animate-pulse" />
        </div>
        <div className="flex items-center gap-3">
          <div className="w-24 h-9 rounded-xl bg-[#eaedf4]/70 dark:bg-slate-800/60 animate-pulse" />
          <div className="w-28 h-9 rounded-xl bg-[#eaedf4]/70 dark:bg-slate-800/60 animate-pulse" />
        </div>
      </div>

      {/* KPI 카드 스켈레톤 */}
      <CardSkeleton count={3} />

      {/* 본문 콘텐츠 스켈레톤 */}
      <TableSkeleton rows={6} cols={4} />
    </div>
  );
}

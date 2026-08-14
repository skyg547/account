import React from 'react';

interface LoadingSkeletonProps {
  rows?: number;
  height?: string;
  className?: string;
}

export default function LoadingSkeleton({ rows = 3, height = 'h-12', className = '' }: LoadingSkeletonProps) {
  return (
    <div className={`space-y-4 w-full ${className}`}>
      {Array.from({ length: rows }).map((_, idx) => (
        <div key={idx} className={`${height} w-full bg-white/5 animate-pulse rounded-2xl border border-white/5`} />
      ))}
    </div>
  );
}

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
        <div key={idx} className={`${height} w-full bg-[#eaedf4]/70 animate-pulse rounded-xl border border-[#eaedf4]/60`} />
      ))}
    </div>
  );
}

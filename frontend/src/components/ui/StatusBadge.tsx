import React from 'react';

type BadgeVariant = 'success' | 'warning' | 'error' | 'info' | 'neutral';

interface StatusBadgeProps {
  status: string;
  variant?: BadgeVariant;
  className?: string;
}

const variantStyles: Record<BadgeVariant, string> = {
  success: 'bg-emerald-50 text-emerald-700 border-emerald-200/80',
  warning: 'bg-amber-50 text-amber-700 border-amber-200/80',
  error: 'bg-rose-50 text-rose-700 border-rose-200/80',
  info: 'bg-blue-50 text-[#4262ff] border-blue-200/80',
  neutral: 'bg-slate-100 text-slate-600 border-slate-200/80',
};

export default function StatusBadge({ status, variant = 'neutral', className = '' }: StatusBadgeProps) {
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-[11px] font-bold tracking-tight border ${variantStyles[variant]} ${className}`}>
      {status}
    </span>
  );
}

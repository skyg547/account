import React from 'react';

interface EmptyStateProps {
  icon: React.ElementType;
  title: string;
  description: string;
  action?: React.ReactNode;
  className?: string;
}

export default function EmptyState({ icon: Icon, title, description, action, className = '' }: EmptyStateProps) {
  return (
    <div className={`flex flex-col items-center justify-center p-12 text-center glass-card bg-white/[0.01] border-dashed ${className}`}>
      <div className="w-20 h-20 rounded-3xl bg-slate-900/50 flex items-center justify-center text-slate-700 mb-6 border border-white/5 shadow-inner">
        <Icon size={40} />
      </div>
      <h3 className="text-xl font-black text-white tracking-tight mb-2">{title}</h3>
      <p className="text-slate-500 font-medium max-w-sm mb-8">{description}</p>
      {action && <div>{action}</div>}
    </div>
  );
}

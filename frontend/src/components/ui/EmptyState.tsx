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
    <div className={`flex flex-col items-center justify-center p-12 text-center bg-white rounded-2xl border border-dashed border-[#eaedf4] shadow-sm ${className}`}>
      <div className="w-16 h-16 rounded-2xl bg-blue-50 flex items-center justify-center text-[#4262ff] mb-4 border border-blue-100/80 shadow-xs">
        <Icon size={30} />
      </div>
      <h3 className="text-lg font-bold text-[#17191e] tracking-tight mb-1">{title}</h3>
      <p className="text-xs text-[#545b69] font-medium max-w-sm mb-6">{description}</p>
      {action && <div>{action}</div>}
    </div>
  );
}

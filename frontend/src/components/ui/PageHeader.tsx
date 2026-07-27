import React from 'react';
import { ChevronRight } from 'lucide-react';

interface Breadcrumb {
  label: string;
  href?: string;
}

interface PageHeaderProps {
  title: string;
  description: string;
  breadcrumbs: Breadcrumb[];
  icon?: React.ElementType;
  actions?: React.ReactNode;
}

export default function PageHeader({ title, description, breadcrumbs, icon: Icon, actions }: PageHeaderProps) {
  return (
    <div className="flex flex-col md:flex-row md:items-end justify-between gap-6 mb-10">
      <div className="space-y-4">
        {/* Breadcrumbs */}
        <div className="flex items-center gap-2 text-xs font-black uppercase tracking-[0.2em] text-slate-500">
          {breadcrumbs.map((bc, idx) => (
            <React.Fragment key={idx}>
              {idx > 0 && <ChevronRight size={14} className="text-slate-700" />}
              <span className={idx === breadcrumbs.length - 1 ? 'text-blue-500' : ''}>
                {bc.label}
              </span>
            </React.Fragment>
          ))}
        </div>

        {/* Title & Desc */}
        <div className="flex items-center gap-4">
          {Icon && (
            <div className="w-12 h-12 rounded-2xl bg-blue-600/10 flex items-center justify-center text-blue-500 border border-blue-500/20">
              <Icon size={24} />
            </div>
          )}
          <div>
            <h2 className="text-4xl font-black text-white tracking-tighter italic">
              {title}
            </h2>
            <p className="text-slate-400 font-medium mt-2">
              {description}
            </p>
          </div>
        </div>
      </div>

      {/* Actions */}
      {actions && (
        <div className="flex items-center gap-3">
          {actions}
        </div>
      )}
    </div>
  );
}

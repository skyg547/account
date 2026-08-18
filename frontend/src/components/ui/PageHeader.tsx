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
    <div className="flex flex-col md:flex-row md:items-end justify-between gap-6 mb-8">
      <div className="space-y-3">
        {/* Breadcrumbs */}
        <div className="flex items-center gap-1.5 text-xs font-semibold tracking-tight text-[#8c94a4]">
          {breadcrumbs.map((bc, idx) => (
            <React.Fragment key={idx}>
              {idx > 0 && <ChevronRight size={13} className="text-[#aab4c4]" />}
              <span className={idx === breadcrumbs.length - 1 ? 'text-[#4262ff] font-bold' : 'hover:text-[#545b69]'}>
                {bc.label}
              </span>
            </React.Fragment>
          ))}
        </div>

        {/* Title & Desc */}
        <div className="flex items-center gap-3.5">
          {Icon && (
            <div className="w-11 h-11 rounded-xl bg-blue-50 flex items-center justify-center text-[#4262ff] border border-blue-200/50 shadow-sm shrink-0">
              <Icon size={22} />
            </div>
          )}
          <div>
            <h2 className="text-2xl sm:text-3xl font-black text-[#17191e] tracking-tight leading-tight">
              {title}
            </h2>
            <p className="text-[#545b69] text-xs sm:text-sm font-medium mt-1">
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

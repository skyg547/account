import React from 'react';

export interface TabItem {
  id: string;
  label: string;
  icon?: React.ElementType;
}

interface TabsProps {
  tabs: TabItem[];
  activeTab: string;
  onChange: (id: string) => void;
  className?: string;
}

export default function Tabs({ tabs, activeTab, onChange, className = '' }: TabsProps) {
  return (
    <div className={`flex items-center gap-1.5 p-1.5 bg-white dark:bg-[#131b2e] rounded-2xl border border-[#eaedf4] dark:border-slate-800 shadow-xs w-max transition-colors ${className}`}>
      {tabs.map(tab => {
        const isActive = activeTab === tab.id;
        return (
          <button
            key={tab.id}
            onClick={() => onChange(tab.id)}
            className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold tracking-tight transition-all duration-200 cursor-pointer ${
              isActive 
                ? 'bg-[#4262ff] text-white shadow-md shadow-blue-600/25 ring-2 ring-blue-400/20' 
                : 'text-[#545b69] dark:text-slate-400 hover:text-[#17191e] dark:hover:text-white hover:bg-[#f7f8fb] dark:hover:bg-slate-800'
            }`}
          >
            {tab.icon && <tab.icon size={15} className={isActive ? 'text-white' : 'text-[#8c94a4] dark:text-slate-500'} />}
            {tab.label}
          </button>
        );
      })}
    </div>
  );
}

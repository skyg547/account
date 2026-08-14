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
    <div className={`flex items-center gap-2 p-1.5 bg-slate-900/50 backdrop-blur-md rounded-2xl border border-white/5 w-max ${className}`}>
      {tabs.map(tab => {
        const isActive = activeTab === tab.id;
        return (
          <button
            key={tab.id}
            onClick={() => onChange(tab.id)}
            className={`flex items-center gap-2 px-5 py-2.5 rounded-xl text-sm font-black tracking-tight transition-all duration-300 ${
              isActive 
                ? 'bg-white/10 text-white shadow-lg border border-white/10' 
                : 'text-slate-500 hover:text-slate-300 hover:bg-white/5'
            }`}
          >
            {tab.icon && <tab.icon size={16} className={isActive ? 'text-blue-400' : 'text-slate-600'} />}
            {tab.label}
          </button>
        );
      })}
    </div>
  );
}

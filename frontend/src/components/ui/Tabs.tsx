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
    <div className={`flex items-center gap-1.5 p-1.5 bg-white rounded-2xl border border-[#eaedf4] shadow-sm w-max ${className}`}>
      {tabs.map(tab => {
        const isActive = activeTab === tab.id;
        return (
          <button
            key={tab.id}
            onClick={() => onChange(tab.id)}
            className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold tracking-tight transition-all duration-200 ${
              isActive 
                ? 'bg-[#4262ff] text-white shadow-md shadow-blue-600/25 ring-2 ring-blue-400/20' 
                : 'text-[#545b69] hover:text-[#17191e] hover:bg-[#f7f8fb]'
            }`}
          >
            {tab.icon && <tab.icon size={15} className={isActive ? 'text-white' : 'text-[#8c94a4]'} />}
            {tab.label}
          </button>
        );
      })}
    </div>
  );
}

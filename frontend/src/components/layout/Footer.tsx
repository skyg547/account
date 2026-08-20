import React from 'react';

/**
 * [K-Bank Style 시스템 하단 푸터]
 * 저작권 정보 및 현재 실행 환경(DEV/PROD)을 정갈하게 표시합니다.
 */
export default function Footer() {
  const isDev = process.env.NODE_ENV === 'development';

  return (
    <footer className="h-11 w-full bg-white dark:bg-[#131b2e] border-t border-[#eaedf4] dark:border-slate-800 flex items-center justify-between px-8 text-xs text-[#8c94a4] dark:text-slate-400 font-medium transition-colors duration-300">
      <div className="flex items-center gap-4">
        <p>&copy; 2026 Account.AI Financial Platform. All rights reserved.</p>
      </div>
      
      <div className="flex items-center gap-3">
        <div className="flex items-center gap-1.5 bg-[#f7f8fb] dark:bg-slate-900 px-2.5 py-1 rounded-lg border border-[#eaedf4] dark:border-slate-800">
          <span className={`w-1.5 h-1.5 rounded-full ${isDev ? 'bg-amber-500' : 'bg-emerald-500'}`} />
          <span className="text-[11px] text-[#545b69] dark:text-slate-400">Runtime: </span>
          <span className={`text-[11px] font-bold ${isDev ? 'text-amber-600 dark:text-amber-400' : 'text-emerald-600 dark:text-emerald-400'}`}>
            {isDev ? 'Local Development' : 'Production Online'}
          </span>
        </div>
      </div>
    </footer>
  );
}

import React from 'react';

/**
 * [시스템 하단 푸터]
 * 저작권 정보 및 현재 실행 환경(DEV/PROD)을 표시합니다.
 */
export default function Footer() {
  const isDev = process.env.NODE_ENV === 'development';

  return (
    <footer className="h-10 w-full bg-[#0f172a] border-t border-white/5 flex items-center justify-between px-6 text-[10px] text-slate-500 font-medium">
      <div className="flex items-center gap-4">
        <p>&copy; 2026 Account.AI Project. Modern Enterprise Resource Management.</p>
      </div>
      
      <div className="flex items-center gap-3">
        <div className="flex items-center gap-1.5 bg-slate-900 px-2 py-1 rounded-md border border-white/5">
          <span className={`w-1.5 h-1.5 rounded-full animate-pulse ${isDev ? 'bg-amber-500 shadow-[0_0_8px_rgba(245,158,11,0.5)]' : 'bg-emerald-500 shadow-[0_0_8px_rgba(16,185,129,0.5)]'}`} />
          <span className="uppercase tracking-tighter">Status: </span>
          <span className={`font-black tracking-tighter ${isDev ? 'text-amber-500' : 'text-emerald-500'}`}>
            {isDev ? 'Development Mode' : 'Production Active'}
          </span>
        </div>
      </div>
    </footer>
  );
}

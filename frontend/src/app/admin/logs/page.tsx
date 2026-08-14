"use client";

import React, { useState } from 'react';
import { 
  Search, 
  Filter, 
  Download, 
  RefreshCw, 
  AlertCircle, 
  CheckCircle2, 
  Info,
  Clock,
  Shield,
  Terminal,
  Activity,
  History
} from 'lucide-react';

// Mock 데이터: 시스템 로그
const logData = [
  { id: 1, time: '2026-04-24 17:55:12', user: 'admin', type: 'LOGIN', status: 'SUCCESS', ip: '192.168.0.1', message: '사용자 [admin] 로그인 성공' },
  { id: 2, time: '2026-04-24 17:50:05', user: 'jm.kim', type: 'PERMISSION', status: 'WARN', ip: '10.220.1.45', message: '미권한 메뉴 [/admin/users] 접근 시도' },
  { id: 3, time: '2026-04-24 17:42:33', user: 'system', type: 'BATCH', status: 'SUCCESS', ip: 'localhost', message: '월말 결산 배치 작업 완료 (Job ID: Closing_2604)' },
  { id: 4, time: '2026-04-24 17:30:11', user: 'audit.choi', type: 'DATA_EXPORT', status: 'INFO', ip: '10.220.1.88', message: '매출채권 명세서 대량 엑셀 다운로드 (5,200건)' },
  { id: 5, time: '2026-04-24 17:15:00', user: 'risk.lee', type: 'UPDATE', status: 'SUCCESS', ip: '192.168.1.12', message: 'IFRS 9 시뮬레이션 파라미터 변경 (부도율 0.5% -> 0.6%)' },
];

/**
 * [시스템 로그 조회 화면]
 * 전사 시스템의 보안 및 운영 로그를 통합 모니터링합니다.
 */
export default function SystemLogPage() {
  const [activeType, setActiveType] = useState('ALL');

  return (
    <div className="space-y-10">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
        <div className="space-y-2">
          <div className="flex items-center gap-3 text-slate-500 mb-2">
            <Terminal size={20} className="text-blue-500" />
            <span className="text-xs font-black uppercase tracking-[0.3em]">System Audit Log</span>
          </div>
          <h2 className="text-4xl font-black text-white tracking-tighter italic">
            시스템 로그 모니터링
          </h2>
          <p className="text-slate-500 font-medium max-w-2xl">
            사용자 접속 이력, 데이터 중요 변경 사항, 시스템 배치 작업 결과 등 실시간 감사 트레일을 조회합니다.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button className="px-6 py-3 bg-white/5 hover:bg-white/10 border border-white/10 rounded-2xl text-slate-400 text-sm font-black transition-all flex items-center gap-2">
            <Download size={18} /> 엑셀 출력
          </button>
          <button className="px-6 py-3 bg-blue-600 hover:bg-blue-500 rounded-2xl text-white text-sm font-black transition-all shadow-lg shadow-blue-600/20 flex items-center gap-2">
            <RefreshCw size={18} /> 실시간 갱신
          </button>
        </div>
      </div>

      {/* Log Filters */}
      <div className="flex flex-wrap gap-4">
         {['ALL', 'LOGIN', 'PERMISSION', 'UPDATE', 'DATA_EXPORT', 'BATCH'].map((type) => (
           <button 
             key={type}
             onClick={() => setActiveType(type)}
             className={`px-5 py-2.5 rounded-2xl text-xs font-black tracking-widest transition-all border ${
               activeType === type 
               ? 'bg-blue-600 text-white border-blue-600' 
               : 'bg-white/5 text-slate-500 border-white/5 hover:border-white/20'
             }`}
           >
             {type}
           </button>
         ))}
      </div>

      {/* Log Feed Table */}
      <div className="glass-panel p-8 rounded-[3rem] border border-white/10 bg-white/[0.01]">
        <div className="flex items-center justify-between mb-8">
           <div className="flex items-center gap-6">
              <div className="flex items-center gap-2 text-white font-black italic">
                 <Activity size={18} className="text-blue-500" /> Live Stream
              </div>
              <div className="h-4 w-[1px] bg-white/10" />
              <div className="text-xs text-slate-600 font-medium italic">Showing last 24 hours log entries...</div>
           </div>
           <div className="flex items-center gap-3">
              <Filter size={18} className="text-slate-600" />
              <Search size={18} className="text-slate-600" />
           </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="border-b border-white/5">
                <th className="pb-6 px-4 text-[10px] font-black text-slate-600 uppercase tracking-widest">발생 일시</th>
                <th className="pb-6 px-4 text-[10px] font-black text-slate-600 uppercase tracking-widest">사용자 / IP</th>
                <th className="pb-6 px-4 text-[10px] font-black text-slate-600 uppercase tracking-widest">유형</th>
                <th className="pb-6 px-4 text-[10px] font-black text-slate-600 uppercase tracking-widest">상태</th>
                <th className="pb-6 px-4 text-[10px] font-black text-slate-600 uppercase tracking-widest">로그 메시지</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/[0.02] font-mono">
              {logData.map((log) => (
                <tr key={log.id} className="group/row hover:bg-white/[0.02] transition-colors">
                  <td className="py-6 px-4">
                     <div className="flex items-center gap-3 text-slate-400 font-medium">
                        <Clock size={14} className="text-slate-600" />
                        <span className="text-xs">{log.time}</span>
                     </div>
                  </td>
                  <td className="py-6 px-4">
                    <div className="flex flex-col">
                       <span className="text-xs font-black text-white">{log.user}</span>
                       <span className="text-[10px] text-slate-600">{log.ip}</span>
                    </div>
                  </td>
                  <td className="py-6 px-4">
                     <span className="text-[10px] font-black text-blue-400 bg-blue-400/5 px-2 py-0.5 rounded border border-blue-400/10">
                        {log.type}
                     </span>
                  </td>
                  <td className="py-6 px-4">
                     {log.status === 'SUCCESS' ? (
                       <div className="flex items-center gap-1.5 text-emerald-500 font-black text-[10px]">
                          <CheckCircle2 size={14} /> OK
                       </div>
                     ) : log.status === 'WARN' ? (
                       <div className="flex items-center gap-1.5 text-amber-500 font-black text-[10px]">
                          <AlertCircle size={14} /> WARN
                       </div>
                     ) : (
                       <div className="flex items-center gap-1.5 text-slate-400 font-black text-[10px]">
                          <Info size={14} /> INFO
                       </div>
                     )}
                  </td>
                  <td className="py-6 px-4">
                     <p className="text-xs text-slate-400 font-medium max-w-lg truncate group-hover/row:whitespace-normal group-hover/row:overflow-visible transition-all">
                        {log.message}
                     </p>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Audit Stats */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
         {[
           { icon: Shield, label: 'Access Security', count: '99.9%', sub: 'No Critical Breach', color: 'blue' },
           { icon: Activity, label: 'System Health', count: '100%', sub: 'All Nodes Active', color: 'emerald' },
           { icon: History, label: 'Retention Policy', count: '365d', sub: 'Standard Compliance', color: 'purple' },
         ].map((stat, i) => (
           <div key={i} className="glass-panel p-6 rounded-[2.5rem] border border-white/10 flex items-center gap-5">
              <div className={`w-14 h-14 rounded-2xl bg-${stat.color}-600/10 flex items-center justify-center text-${stat.color}-500 border border-${stat.color}-600/20`}>
                 <stat.icon size={28} />
              </div>
              <div className="flex flex-col">
                 <span className="text-[10px] font-black text-slate-600 uppercase tracking-widest">{stat.label}</span>
                 <span className="text-2xl font-black text-white italic -mt-1 leading-none">{stat.count}</span>
                 <span className="text-[10px] font-bold text-slate-500 mt-1 uppercase tracking-tight">{stat.sub}</span>
              </div>
           </div>
         ))}
      </div>
    </div>
  );
}

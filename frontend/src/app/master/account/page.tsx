"use client";

import React, { useState, useEffect } from 'react';
import { 
  BookOpen, 
  Search, 
  ChevronRight, 
  ChevronDown, 
  CheckCircle2, 
  Clock, 
  AlertCircle,
  Layers,
  Send
} from 'lucide-react';
import { masterDataService, AccountSubjectDto } from '@/services/masterDataService';

interface AccountNode {
  id: string;
  name: string;
  type: 'GROUP' | 'SUBJECT';
  status?: string;
  children?: AccountNode[];
}

function buildAccountTree(subjects: AccountSubjectDto[]): AccountNode[] {
  const nodeMap = new Map<string, AccountNode>();
  const rootNodes: AccountNode[] = [];

  subjects.forEach(subject => {
    nodeMap.set(subject.code, {
      id: subject.code,
      name: subject.name,
      type: subject.category,
      status: subject.status,
      children: []
    });
  });

  subjects.forEach(subject => {
    const node = nodeMap.get(subject.code)!;
    if (subject.parentCode && nodeMap.has(subject.parentCode)) {
      nodeMap.get(subject.parentCode)!.children!.push(node);
    } else {
      rootNodes.push(node);
    }
  });

  return rootNodes;
}

/**
 * [계정 과목 관리 화면 - 리뉴얼 및 승인 요청 기능 추가]
 * 계층 구조 시각화와 일반 사용자의 '신규 요청' 기능을 통합합니다.
 * 백엔드 Master Data API 연동 완료.
 */
export default function AccountSubjectPage() {
  const [isRequestModalOpen, setIsRequestModalOpen] = useState(false);
  const [expandedGroups, setExpandedGroups] = useState<string[]>([]);
  const [treeData, setTreeData] = useState<AccountNode[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchData = async () => {
    setLoading(true);
    try {
      const data = await masterDataService.getAccountSubjects();
      const tree = buildAccountTree(data);
      setTreeData(tree);
      // 최상위 노드 자동 확장 (원한다면 로직 변경 가능)
      if (tree.length > 0) {
        setExpandedGroups(tree.map(n => n.id));
      }
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchData();
  }, []);

  const toggleGroup = (id: string) => {
    setExpandedGroups(prev => prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]);
  };

  const renderNode = (node: AccountNode, depth: number = 0) => {
    const isExpanded = expandedGroups.includes(node.id);
    const hasChildren = node.children && node.children.length > 0;

    return (
      <div key={node.id} className="space-y-4">
        <div 
          className={`flex items-center gap-4 group/item ${node.type === 'GROUP' ? 'cursor-pointer' : ''} ${depth > 0 ? 'ml-12' : ''}`} 
          onClick={() => node.type === 'GROUP' && toggleGroup(node.id)}
        >
          {node.type === 'GROUP' ? (
            <div className="p-1 rounded-lg hover:bg-white/5 text-slate-500">
              {isExpanded ? <ChevronDown size={20} /> : <ChevronRight size={20} />}
            </div>
          ) : (
            <div className="w-1.5 h-1.5 rounded-full bg-blue-500 mx-2" />
          )}
          
          <span className="text-sm font-black text-slate-500 tracking-widest uppercase">{node.id}</span>
          <span className={`text-lg font-black tracking-tight ${node.type === 'GROUP' ? 'text-slate-300' : 'text-white'}`}>{node.name}</span>
          
          {node.status === 'ACTIVE' && node.type === 'SUBJECT' && (
            <span className="text-[10px] font-black text-emerald-500 bg-emerald-500/5 px-2 py-0.5 rounded border border-emerald-500/10 ml-2">ACTIVE</span>
          )}
          {node.status === 'INACTIVE' && (
            <span className="text-[10px] font-black text-rose-500 bg-rose-500/5 px-2 py-0.5 rounded border border-rose-500/10 ml-2">INACTIVE</span>
          )}

          {node.type === 'GROUP' && <div className="h-[1px] flex-1 bg-white/5 mx-4" />}
        </div>

        {isExpanded && hasChildren && (
          <div className="animate-in slide-in-from-top-2 duration-300">
            {node.children!.map(child => renderNode(child, depth + 1))}
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="space-y-10">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
        <div className="space-y-2">
          <div className="flex items-center gap-3 text-blue-500 mb-2">
            <BookOpen size={20} />
            <span className="text-xs font-black uppercase tracking-[0.3em]">Accounting Standards</span>
          </div>
          <h2 className="text-4xl font-black text-white tracking-tighter italic">
            계정 과목 표준 체계
          </h2>
          <p className="text-slate-500 font-medium max-w-2xl">
            전사 표준 계정 과목(Chart of Accounts)을 관리합니다. 신규 계정 필요 시 관리자 승인 요청이 필요합니다.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button 
            onClick={() => setIsRequestModalOpen(true)}
            className="px-6 py-3 bg-blue-600 hover:bg-blue-500 rounded-2xl text-white text-sm font-black transition-all shadow-lg shadow-blue-600/20 flex items-center gap-2 px-8"
          >
            <Send size={18} /> 계정 신규 요청
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        {/* Left Area: Hierarchy Tree */}
        <div className="lg:col-span-12 glass-panel p-10 rounded-[3rem] border border-white/10 bg-white/[0.01]">
           <div className="flex items-center justify-between mb-10">
              <div className="flex items-center gap-4">
                 <div className="w-12 h-12 rounded-2xl bg-slate-900 border border-white/5 flex items-center justify-center text-blue-500">
                    <Layers size={24} />
                 </div>
                 <h3 className="text-2xl font-black text-white italic tracking-tight">Chart of Accounts (COA)</h3>
              </div>
              <div className="relative group/search max-w-xs w-full">
                <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-600" size={18} />
                <input 
                  type="text" 
                  placeholder="계정명, 코드 검색..."
                  className="w-full bg-slate-950 border border-white/5 focus:border-blue-500/30 rounded-xl py-3 pl-12 pr-4 text-white text-sm outline-none transition-all placeholder:text-slate-700"
                />
              </div>
           </div>

           <div className="space-y-6">
              {loading ? (
                <div className="flex justify-center items-center py-10">
                  <span className="text-white">데이터를 불러오는 중입니다...</span>
                </div>
              ) : treeData.length > 0 ? (
                treeData.map(node => renderNode(node))
              ) : (
                <div className="text-center py-10 text-slate-500">조회된 계정과목이 없습니다.</div>
              )}
           </div>
        </div>

        {/* Info Grid */}
        <div className="lg:col-span-12 grid grid-cols-1 md:grid-cols-3 gap-8">
           {[
             { icon: CheckCircle2, label: '검증 규칙', text: '재무제표 정합성을 위해 하위 계정이 없는 그룹 계정은 전표 입력이 불가합니다.', color: 'emerald' },
             { icon: Clock, label: '승인 프로세스', text: '일반 사용자 요청 시 팀장/재무팀 관리자의 승인 즉시 시스템에 반영됩니다.', color: 'blue' },
             { icon: AlertCircle, label: '주의 사항', text: '이미 사용된 전적이 있는 계정은 코드 변경이 제한되며, 사용 중지만 가능합니다.', color: 'amber' },
           ].map((item, i) => (
             <div key={i} className="glass-panel p-8 rounded-[3rem] border border-white/10 bg-white/[0.01]">
                <div className={`w-12 h-12 rounded-2xl bg-${item.color}-600/10 flex items-center justify-center text-${item.color}-500 mb-6`}>
                   <item.icon size={24} />
                </div>
                <h4 className="text-lg font-black text-white italic mb-3">{item.label}</h4>
                <p className="text-sm text-slate-500 font-medium leading-relaxed">{item.text}</p>
             </div>
           ))}
        </div>
      </div>

      {/* [MODAL] Request for New Account */}
      {isRequestModalOpen && (
        <div className="fixed inset-0 z-[1000] flex items-center justify-center p-6 sm:p-20">
           <div className="absolute inset-0 bg-slate-950/80 backdrop-blur-md" onClick={() => setIsRequestModalOpen(false)} />
           <div className="relative w-full max-w-2xl bg-[#020617] border border-white/10 rounded-[3rem] p-10 shadow-2xl animate-in zoom-in-95 duration-300">
              <div className="flex items-center gap-4 mb-8">
                 <div className="w-14 h-14 rounded-2xl bg-blue-600 flex items-center justify-center text-white">
                    <Send size={24} />
                 </div>
                 <div>
                    <h3 className="text-2xl font-black text-white italic tracking-tight uppercase">New Account Request</h3>
                    <p className="text-slate-500 text-sm font-medium">관리자에게 신규 계정 과목 등록을 요청합니다.</p>
                 </div>
              </div>

              <div className="space-y-6">
                 <div className="grid grid-cols-2 gap-6">
                    <div className="space-y-2">
                       <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">상위 그룹 계정</label>
                       <select className="w-full bg-slate-900 border border-white/5 rounded-2xl p-4 text-white text-sm outline-none focus:border-blue-500/30">
                          <option>1100 유동자산</option>
                          <option>1200 비유동자산</option>
                          <option>2100 유동부채</option>
                       </select>
                    </div>
                    <div className="space-y-2">
                       <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">요청 계정명</label>
                       <input type="text" className="w-full bg-slate-900 border border-white/5 rounded-2xl p-4 text-white text-sm outline-none focus:border-blue-500/30 font-bold" placeholder="예: 소모품비(본사)" />
                    </div>
                 </div>

                 <div className="space-y-2">
                    <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">요청 사유 및 명세</label>
                    <textarea rows={4} className="w-full bg-slate-900 border border-white/5 rounded-2xl p-4 text-white text-sm outline-none focus:border-blue-500/30 font-bold resize-none" placeholder="신규 법인 설립에 따른 별도 관리 계정 필요..." />
                 </div>

                 <div className="flex gap-4 pt-4">
                    <button 
                      onClick={() => setIsRequestModalOpen(false)}
                      className="flex-1 py-4 bg-white/5 hover:bg-white/10 rounded-2xl text-slate-300 font-black tracking-widest text-xs transition-all uppercase"
                    >
                      Cancel
                    </button>
                    <button className="flex-[2] py-4 bg-blue-600 hover:bg-blue-500 rounded-2xl text-white font-black tracking-widest text-xs transition-all uppercase shadow-xl shadow-blue-600/20">
                      Submit Request
                    </button>
                 </div>
              </div>
           </div>
        </div>
      )}
    </div>
  );
}

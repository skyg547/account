"use client";

import React, { useState } from 'react';
import { 
  Building2, 
  ChevronRight, 
  ChevronDown, 
  FolderTree, 
  Plus, 
  Search, 
  Edit3, 
  Trash2, 
  CreditCard, 
  Users, 
  ShieldAlert,
  Layers,
  Sparkles
} from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import { mockDepartments, DepartmentDto } from '@/mocks/system';

interface DepartmentNode extends DepartmentDto {
  children?: DepartmentNode[];
}

export default function SystemDepartmentsPage() {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedDeptId, setSelectedDeptId] = useState<string>('D01');
  const [expandedNodes, setExpandedNodes] = useState<Record<string, boolean>>({ D00: true });

  // Build tree structure
  const buildTree = (items: DepartmentDto[], parentId: string | null = null): DepartmentNode[] => {
    return items
      .filter(item => item.parentId === parentId)
      .map(item => ({
        ...item,
        children: buildTree(items, item.id)
      }));
  };

  const departmentTree = buildTree(mockDepartments, null);

  const toggleExpand = (id: string) => {
    setExpandedNodes(prev => ({ ...prev, [id]: !prev[id] }));
  };

  const selectedDept = mockDepartments.find(d => d.id === selectedDeptId) || mockDepartments[0];

  const renderTreeNode = (node: DepartmentNode, depth: number = 0) => {
    const hasChildren = node.children && node.children.length > 0;
    const isExpanded = expandedNodes[node.id];
    const isSelected = selectedDeptId === node.id;

    return (
      <div key={node.id} className="space-y-2">
        <div
          onClick={() => setSelectedDeptId(node.id)}
          style={{ paddingLeft: `${depth * 24 + 16}px` }}
          className={`flex items-center justify-between p-3.5 rounded-2xl cursor-pointer transition-all border ${
            isSelected
              ? 'bg-blue-600/20 border-blue-500/40 text-white shadow-lg shadow-blue-500/10'
              : 'bg-white/5 border-white/5 text-slate-300 hover:bg-white/10 hover:border-white/10'
          }`}
        >
          <div className="flex items-center gap-3">
            {hasChildren ? (
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  toggleExpand(node.id);
                }}
                className="p-1 text-slate-400 hover:text-white rounded-lg hover:bg-white/10 transition-colors"
              >
                {isExpanded ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
              </button>
            ) : (
              <div className="w-4" />
            )}

            <div className={`w-8 h-8 rounded-xl flex items-center justify-center font-bold text-xs ${
              node.parentId === null 
                ? 'bg-purple-500/20 text-purple-400 border border-purple-500/30'
                : 'bg-blue-500/20 text-blue-400 border border-blue-500/30'
            }`}>
              {node.parentId === null ? 'HQ' : 'DEPT'}
            </div>

            <div>
              <span className="font-bold text-sm tracking-tight text-white">{node.name}</span>
              <div className="flex items-center gap-2 text-xs text-slate-400 mt-0.5">
                <span className="font-mono bg-slate-900/80 px-2 py-0.5 rounded text-[11px] text-blue-400 border border-white/5">
                  {node.costCenterCode}
                </span>
                <span>ID: {node.id}</span>
              </div>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <StatusBadge status="운영중" variant="success" />
          </div>
        </div>

        {hasChildren && isExpanded && (
          <div className="space-y-2 border-l border-white/10 ml-6 pl-2">
            {node.children!.map(child => renderTreeNode(child, depth + 1))}
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="space-y-8 p-2">
      <PageHeader
        title="부서 관리"
        description="조직도 및 코스트센터 구조"
        breadcrumbs={[
          { label: 'System', href: '/system/departments' },
          { label: '부서 관리' }
        ]}
        icon={Building2}
        actions={
          <button className="px-5 py-2.5 bg-blue-600 hover:bg-blue-500 text-white rounded-2xl text-sm font-bold shadow-lg shadow-blue-600/20 transition-all flex items-center gap-2">
            <Plus size={16} /> 신규 부서 추가
          </button>
        }
      />

      {/* Quick Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-black text-slate-400 uppercase tracking-wider">등록된 부서 수</p>
              <h3 className="text-3xl font-black text-white mt-2 tracking-tight">{mockDepartments.length}개 조직</h3>
            </div>
            <div className="w-12 h-12 rounded-2xl bg-blue-500/10 border border-blue-500/20 flex items-center justify-center text-blue-400">
              <FolderTree size={22} />
            </div>
          </div>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-black text-slate-400 uppercase tracking-wider">코스트센터 매핑율</p>
              <h3 className="text-3xl font-black text-emerald-400 mt-2 tracking-tight">100% (5/5)</h3>
            </div>
            <div className="w-12 h-12 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
              <CreditCard size={22} />
            </div>
          </div>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-black text-slate-400 uppercase tracking-wider">최고 상위 조직</p>
              <h3 className="text-xl font-black text-purple-400 mt-2 tracking-tight">D00 본사</h3>
            </div>
            <div className="w-12 h-12 rounded-2xl bg-purple-500/10 border border-purple-500/20 flex items-center justify-center text-purple-400">
              <Layers size={22} />
            </div>
          </div>
        </div>
      </div>

      {/* Main Layout: Tree View + Detail Card */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* Hierarchical Tree Panel */}
        <div className="lg:col-span-7 p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md space-y-6">
          <div className="flex items-center justify-between">
            <h3 className="text-lg font-black text-white flex items-center gap-2">
              <FolderTree size={18} className="text-blue-400" /> 계층 조직도 구조
            </h3>
            <span className="text-xs font-bold text-slate-400">항목 선택 시 상세 정보 확인</span>
          </div>

          <div className="relative">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500" size={16} />
            <input
              type="text"
              placeholder="부서명 또는 코스트센터 검색..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full bg-white/5 border border-white/10 rounded-2xl py-2.5 pl-11 pr-4 text-white placeholder-slate-500 text-sm focus:outline-none focus:border-blue-500/50 transition-all"
            />
          </div>

          <div className="space-y-3">
            {departmentTree.map(rootNode => renderTreeNode(rootNode))}
          </div>
        </div>

        {/* Selected Department Details */}
        <div className="lg:col-span-5 p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-md space-y-6 h-fit">
          <div className="flex items-center justify-between border-b border-white/10 pb-4">
            <h3 className="text-lg font-black text-white flex items-center gap-2">
              <Sparkles size={18} className="text-blue-400" /> 부서 상세 정보
            </h3>
            <div className="flex items-center gap-2">
              <button className="p-2 bg-white/5 hover:bg-white/10 rounded-xl text-slate-300 transition-colors">
                <Edit3 size={16} />
              </button>
              <button className="p-2 bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 rounded-xl transition-colors">
                <Trash2 size={16} />
              </button>
            </div>
          </div>

          {selectedDept ? (
            <div className="space-y-6">
              <div className="p-4 rounded-xl bg-white/5 border border-white/5 space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-slate-400 uppercase">부서 명칭</span>
                  <StatusBadge status="활성 상태" variant="success" />
                </div>
                <h4 className="text-2xl font-black text-white tracking-tight">{selectedDept.name}</h4>
                <p className="text-xs text-slate-400 font-mono">ID: {selectedDept.id}</p>
              </div>

              <div className="space-y-4">
                <div className="flex items-center justify-between p-3.5 rounded-xl bg-white/5 border border-white/5">
                  <div className="flex items-center gap-3 text-slate-300">
                    <CreditCard size={18} className="text-blue-400" />
                    <div>
                      <p className="text-xs font-bold text-slate-400">코스트센터 코드</p>
                      <p className="text-sm font-mono font-bold text-white mt-0.5">{selectedDept.costCenterCode}</p>
                    </div>
                  </div>
                </div>

                <div className="flex items-center justify-between p-3.5 rounded-xl bg-white/5 border border-white/5">
                  <div className="flex items-center gap-3 text-slate-300">
                    <Building2 size={18} className="text-purple-400" />
                    <div>
                      <p className="text-xs font-bold text-slate-400">상위 부서 (Parent)</p>
                      <p className="text-sm font-bold text-white mt-0.5">
                        {selectedDept.parentId 
                          ? `${mockDepartments.find(d => d.id === selectedDept.parentId)?.name || selectedDept.parentId} (${selectedDept.parentId})`
                          : '최상위 부서 (Root)'}
                      </p>
                    </div>
                  </div>
                </div>

                <div className="flex items-center justify-between p-3.5 rounded-xl bg-white/5 border border-white/5">
                  <div className="flex items-center gap-3 text-slate-300">
                    <Users size={18} className="text-emerald-400" />
                    <div>
                      <p className="text-xs font-bold text-slate-400">소속 임직원 수</p>
                      <p className="text-sm font-bold text-white mt-0.5">4명 배정 (Mock)</p>
                    </div>
                  </div>
                </div>
              </div>

              <div className="p-4 rounded-xl bg-blue-500/5 border border-blue-500/10 text-xs text-slate-300 space-y-1">
                <p className="font-bold text-blue-400 flex items-center gap-1">
                  <ShieldAlert size={14} /> 내부통제 지침
                </p>
                <p className="text-slate-400">
                  코스트센터 변경 시 연동된 손익계산서 및 예금/채권 할당 계정이 일괄 재계산됩니다.
                </p>
              </div>
            </div>
          ) : (
            <div className="py-12 text-center text-slate-500">
              부서를 선택하면 상세 정보가 표시됩니다.
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

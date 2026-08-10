'use client';

import React, { useState, useEffect } from 'react';
import { ShieldAlert, Trash2, User, KeyRound, Clock, AlertTriangle, RefreshCw } from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';

interface PatItem {
  id: string;
  username: string;
  tokenName: string;
  tokenPrefix: string;
  status: 'ACTIVE' | 'REVOKED' | 'EXPIRED';
  expiresAt: string;
  lastUsedAt: string | null;
  createdAt: string;
}

const AUTH_API_BASE_URL = process.env.NEXT_PUBLIC_AUTH_API_URL || 'http://localhost:8080';

export default function SystemTokenGovernancePage() {
  const [tokens, setTokens] = useState<PatItem[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchAllTokens();
  }, []);

  const fetchAllTokens = async () => {
    setLoading(true);
    try {
      const res = await fetch(`${AUTH_API_BASE_URL}/api/auth/admin/pat`);
      if (res.ok) {
        const data = await res.json();
        setTokens(data);
      }
    } catch (e) {
      console.error('Failed to fetch admin PAT list', e);
    } finally {
      setLoading(false);
    }
  };

  const handleForceRevoke = async (id: string, username: string, tokenName: string) => {
    if (!confirm(`[관리자 경고] 사용자 '${username}'의 토큰 '${tokenName}'을 강제 폐기(Force Revoke)하시겠습니까?\n폐기 시 해당 토큰을 사용하는 외부 AI 에이전트의 접근이 1초 만에 즉시 차단됩니다.`)) return;

    try {
      const res = await fetch(`${AUTH_API_BASE_URL}/api/auth/admin/pat/${id}`, {
        method: 'DELETE',
      });
      if (res.ok) {
        fetchAllTokens();
      }
    } catch (e) {
      alert('관리자 강제 폐기 처리 실패');
    }
  };

  return (
    <div className="space-y-8 p-8 max-w-7xl mx-auto">
      <div className="flex items-center justify-between">
        <PageHeader
          title="전사 개인용 액세스 토큰 (PAT) 감시 및 거버넌스"
          description="전체 사용자가 생성한 AI 에이전트 및 외부 서비스 연결 토큰 현황을 감시하고 해킹/유출 의심 시 즉시 강제 폐기합니다."
          breadcrumbs={[{ label: 'System' }, { label: 'Access Tokens' }]}
        />
        <button
          onClick={fetchAllTokens}
          disabled={loading}
          className="px-4 py-2 bg-slate-900 hover:bg-slate-800 border border-white/10 text-white font-semibold text-xs rounded-xl transition-all flex items-center gap-2"
        >
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          <span>새로고침</span>
        </button>
      </div>

      <div className="bg-slate-900/60 backdrop-blur-xl border border-white/10 rounded-3xl p-6 shadow-2xl space-y-6">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center justify-center text-rose-400">
            <ShieldAlert className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-lg font-bold text-white">전사 발급 PAT 토큰 거버넌스 목록 ({tokens.length})</h3>
            <p className="text-xs text-slate-400">관리자는 유출되거나 이상징후가 포착된 토큰을 즉시 강제 폐기할 수 있는 최고 권한을 가집니다.</p>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-white/10 text-slate-400 uppercase font-mono">
                <th className="py-3 px-4">사용자 (Owner)</th>
                <th className="py-3 px-4">토큰 명칭 (Token Name)</th>
                <th className="py-3 px-4">식별 접두사</th>
                <th className="py-3 px-4">상태</th>
                <th className="py-3 px-4">생성일</th>
                <th className="py-3 px-4">만료 예정일</th>
                <th className="py-3 px-4">마지막 사용</th>
                <th className="py-3 px-4 text-right">관리자 액션</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5 text-slate-300">
              {tokens.map((token) => (
                <tr key={token.id} className="hover:bg-white/[0.02] transition-colors">
                  <td className="py-3 px-4 font-bold text-white flex items-center gap-2">
                    <User className="w-4 h-4 text-blue-400" />
                    <span>{token.username}</span>
                  </td>
                  <td className="py-3 px-4 font-semibold text-slate-200">{token.tokenName}</td>
                  <td className="py-3 px-4 font-mono text-slate-400">{token.tokenPrefix}</td>
                  <td className="py-3 px-4"><StatusBadge status={token.status} /></td>
                  <td className="py-3 px-4 font-mono">{new Date(token.createdAt).toLocaleDateString()}</td>
                  <td className="py-3 px-4 font-mono text-rose-300">{new Date(token.expiresAt).toLocaleDateString()}</td>
                  <td className="py-3 px-4 font-mono text-slate-400">{token.lastUsedAt ? new Date(token.lastUsedAt).toLocaleString() : '미사용'}</td>
                  <td className="py-3 px-4 text-right">
                    {token.status === 'ACTIVE' && (
                      <button
                        onClick={() => handleForceRevoke(token.id, token.username, token.tokenName)}
                        className="px-3 py-1.5 bg-rose-600 hover:bg-rose-500 text-white font-bold rounded-xl text-xs transition-all flex items-center gap-1 ml-auto shadow-md shadow-rose-600/20"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                        <span>관리자 강제 폐기</span>
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

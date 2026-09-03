'use client';

import React, { useState, useEffect } from 'react';
import { ShieldAlert, Trash2, User, KeyRound, RefreshCw, Loader2 } from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import StatusBadge from '@/components/ui/StatusBadge';
import { useToast } from '@/context/ToastContext';

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

const AUTH_API_BASE_URL = process.env.NEXT_PUBLIC_AUTH_API_URL || '';

const MOCK_ADMIN_TOKENS: PatItem[] = [
  {
    id: 'pat-001',
    username: 'admin',
    tokenName: 'Antigravity AI Agent Key',
    tokenPrefix: 'pat_live_8f3a...',
    status: 'ACTIVE',
    createdAt: '2026-08-01T09:00:00Z',
    expiresAt: '2026-11-01T09:00:00Z',
    lastUsedAt: '2026-08-18T14:20:00Z',
  },
  {
    id: 'pat-002',
    username: 'jm.kim',
    tokenName: 'Excel Financial Add-on Token',
    tokenPrefix: 'pat_live_4b2c...',
    status: 'ACTIVE',
    createdAt: '2026-07-15T11:30:00Z',
    expiresAt: '2026-10-15T11:30:00Z',
    lastUsedAt: '2026-08-12T16:00:00Z',
  },
  {
    id: 'pat-003',
    username: 'risk.lee',
    tokenName: 'ECL Risk Batch Trigger Script',
    tokenPrefix: 'pat_live_9a1e...',
    status: 'REVOKED',
    createdAt: '2026-06-10T10:00:00Z',
    expiresAt: '2026-09-10T10:00:00Z',
    lastUsedAt: '2026-07-01T08:15:00Z',
  },
];

export default function SystemTokenGovernancePage() {
  const { success: showSuccessToast, error: showErrorToast } = useToast();
  const [tokens, setTokens] = useState<PatItem[]>(MOCK_ADMIN_TOKENS);
  const [loading, setLoading] = useState(false);
  const [revokingTokenId, setRevokingTokenId] = useState<string | null>(null);

  const fetchAllTokens = async () => {
    if (!AUTH_API_BASE_URL) {
      setTokens(MOCK_ADMIN_TOKENS);
      return;
    }

    setLoading(true);
    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 3000);

      const res = await fetch(`${AUTH_API_BASE_URL}/api/auth/admin/pat`, { signal: controller.signal });
      clearTimeout(timeoutId);

      if (res.ok) {
        const data = await res.json();
        setTokens(data.length > 0 ? data : MOCK_ADMIN_TOKENS);
      } else {
        setTokens(MOCK_ADMIN_TOKENS);
      }
    } catch {
      setTokens(MOCK_ADMIN_TOKENS);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAllTokens();
  }, []);

  const handleForceRevoke = async (id: string, username: string, tokenName: string) => {
    if (revokingTokenId !== null) return;
    if (!confirm(`[관리자 경고] 사용자 '${username}'의 토큰 '${tokenName}'을 강제 폐기(Force Revoke)하시겠습니까?\n폐기 시 해당 토큰을 사용하는 외부 AI 에이전트의 접근이 즉시 차단됩니다.`)) return;

    setRevokingTokenId(id);
    if (!AUTH_API_BASE_URL) {
      setTokens(prev => prev.map(t => t.id === id ? { ...t, status: 'REVOKED' as const } : t));
      showSuccessToast(`토큰 '${tokenName}'이 강제 폐기되었습니다.`, { title: '토큰 폐기 완료' });
      setRevokingTokenId(null);
      return;
    }

    try {
      const res = await fetch(`${AUTH_API_BASE_URL}/api/auth/admin/pat/${id}`, {
        method: 'DELETE',
      });
      if (res.ok) {
        showSuccessToast(`토큰 '${tokenName}'이 강제 폐기되었습니다.`, { title: '토큰 폐기 완료' });
        await fetchAllTokens();
      } else {
        showErrorToast('토큰 폐기 요청에 실패했습니다.', { title: '폐기 실패' });
      }
    } catch (err: unknown) {
      setTokens(prev => prev.map(t => t.id === id ? { ...t, status: 'REVOKED' as const } : t));
      showErrorToast(err instanceof Error ? err.message : '토큰 폐기 처리 중 오류가 발생했습니다.');
    } finally {
      setRevokingTokenId(null);
    }
  };

  return (
    <div className="space-y-8 p-4 max-w-7xl mx-auto">
      <div className="flex items-center justify-between">
        <PageHeader
          title="전사 개인용 액세스 토큰 (PAT) 감시 및 거버넌스"
          description="전체 사용자가 생성한 AI 에이전트 및 외부 서비스 연결 토큰 현황을 감시하고 해킹/유출 의심 시 즉시 강제 폐기합니다."
          breadcrumbs={[{ label: '시스템관리' }, { label: 'PAT 토큰 거버넌스' }]}
          icon={KeyRound}
        />
        <button
          onClick={fetchAllTokens}
          disabled={loading}
          className="px-4 py-2 bg-white hover:bg-slate-50 border border-[#eaedf4] text-[#545b69] font-bold text-xs rounded-xl transition-all flex items-center gap-2 shadow-xs"
        >
          <RefreshCw className={`w-4 h-4 text-[#4262ff] ${loading ? 'animate-spin' : ''}`} />
          <span>새로고침</span>
        </button>
      </div>

      <div className="bg-white border border-[#eaedf4] rounded-2xl p-6 shadow-sm space-y-6">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-rose-50 border border-rose-100 flex items-center justify-center text-rose-600">
            <ShieldAlert className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-base font-bold text-[#17191e]">전사 발급 PAT 토큰 거버넌스 목록 ({tokens.length})</h3>
            <p className="text-xs text-[#545b69]">관리자는 유출되거나 이상징후가 포착된 토큰을 즉시 강제 폐기할 수 있는 최고 권한을 가집니다.</p>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="bg-[#f7f8fb] border-b border-[#eaedf4] text-[#17191e] uppercase font-bold">
                <th className="py-3.5 px-4">사용자 (Owner)</th>
                <th className="py-3.5 px-4">토큰 명칭 (Token Name)</th>
                <th className="py-3.5 px-4">식별 접두사</th>
                <th className="py-3.5 px-4">상태</th>
                <th className="py-3.5 px-4">생성일</th>
                <th className="py-3.5 px-4">만료 예정일</th>
                <th className="py-3.5 px-4">마지막 사용</th>
                <th className="py-3.5 px-4 text-right">관리자 액션</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#eaedf4] text-[#2a2e36]">
              {tokens.map((token) => (
                <tr key={token.id} className="hover:bg-blue-50/30 transition-colors">
                  <td className="py-3.5 px-4 font-bold text-[#17191e] flex items-center gap-2">
                    <User className="w-4 h-4 text-[#4262ff]" />
                    <span>{token.username}</span>
                  </td>
                  <td className="py-3.5 px-4 font-bold text-[#17191e]">{token.tokenName}</td>
                  <td className="py-3.5 px-4 font-mono text-[#545b69]">{token.tokenPrefix}</td>
                  <td className="py-3.5 px-4">
                    <StatusBadge 
                      status={token.status} 
                      variant={token.status === 'ACTIVE' ? 'success' : token.status === 'REVOKED' ? 'error' : 'neutral'} 
                    />
                  </td>
                  <td className="py-3.5 px-4 font-mono text-[#8c94a4]">{new Date(token.createdAt).toLocaleDateString()}</td>
                  <td className="py-3.5 px-4 font-mono text-rose-600 font-bold">{new Date(token.expiresAt).toLocaleDateString()}</td>
                  <td className="py-3.5 px-4 font-mono text-[#8c94a4]">{token.lastUsedAt ? new Date(token.lastUsedAt).toLocaleString() : '미사용'}</td>
                  <td className="py-3.5 px-4 text-right">
                    {token.status === 'ACTIVE' && (
                      <button
                        onClick={() => handleForceRevoke(token.id, token.username, token.tokenName)}
                        disabled={revokingTokenId === token.id}
                        aria-busy={revokingTokenId === token.id}
                        className="px-3 py-1.5 bg-rose-50 hover:bg-rose-100 text-rose-600 border border-rose-200 font-bold rounded-xl text-xs transition-all flex items-center gap-1.5 ml-auto shadow-xs disabled:opacity-50 disabled:cursor-not-allowed disabled:pointer-events-none cursor-pointer"
                      >
                        {revokingTokenId === token.id ? (
                          <>
                            <Loader2 className="w-3.5 h-3.5 animate-spin" />
                            <span>폐기 처리 중...</span>
                          </>
                        ) : (
                          <>
                            <Trash2 className="w-3.5 h-3.5" />
                            <span>관리자 강제 폐기</span>
                          </>
                        )}
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

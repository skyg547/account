'use client';

import React, { useState, useEffect } from 'react';
import { KeyRound, Plus, Trash2, Copy, Check, ShieldAlert, Clock, Sparkles } from 'lucide-react';
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

interface NewPatResponse extends PatItem {
  rawToken: string;
}

const AUTH_API_BASE_URL = process.env.NEXT_PUBLIC_AUTH_API_URL || '';

const MOCK_PATS: PatItem[] = [
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
    username: 'admin',
    tokenName: 'Excel Financial Add-on Token',
    tokenPrefix: 'pat_live_4b2c...',
    status: 'ACTIVE',
    createdAt: '2026-07-15T11:30:00Z',
    expiresAt: '2026-10-15T11:30:00Z',
    lastUsedAt: '2026-08-12T16:00:00Z',
  },
];

export default function PersonalAccessTokenPage() {
  const [tokens, setTokens] = useState<PatItem[]>(MOCK_PATS);
  const [tokenName, setTokenName] = useState('');
  const [expireDays, setExpireDays] = useState(90);
  const [loading, setLoading] = useState(false);
  const [createdPat, setCreatedPat] = useState<NewPatResponse | null>(null);
  const [copied, setCopied] = useState(false);
  const [username, setUsername] = useState('admin');

  const fetchTokens = async () => {
    if (!AUTH_API_BASE_URL) {
      setTokens(MOCK_PATS);
      return;
    }

    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 1500);

      const res = await fetch(`${AUTH_API_BASE_URL}/api/auth/pat?username=${username}`, {
        signal: controller.signal
      });
      clearTimeout(timeoutId);

      if (res.ok) {
        const data = await res.json();
        setTokens(data.length > 0 ? data : MOCK_PATS);
      } else {
        setTokens(MOCK_PATS);
      }
    } catch {
      setTokens(MOCK_PATS);
    }
  };

  useEffect(() => {
    fetchTokens();
  }, []);

  const handleCreatePat = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!tokenName.trim()) return;
    setLoading(true);

    if (!AUTH_API_BASE_URL) {
      // 로컬 Mock 생성 시뮬레이션
      setTimeout(() => {
        const mockNew: NewPatResponse = {
          id: `pat-${Date.now().toString().slice(-4)}`,
          username,
          tokenName,
          tokenPrefix: `pat_live_${Math.random().toString(36).slice(2, 6)}...`,
          rawToken: `pat_live_${Math.random().toString(36).slice(2, 10)}_${Math.random().toString(36).slice(2, 10)}`,
          status: 'ACTIVE',
          createdAt: new Date().toISOString(),
          expiresAt: new Date(Date.now() + expireDays * 86400000).toISOString(),
          lastUsedAt: null,
        };
        setCreatedPat(mockNew);
        setTokens(prev => [mockNew, ...prev]);
        setTokenName('');
        setLoading(false);
      }, 400);
      return;
    }

    try {
      const res = await fetch(`${AUTH_API_BASE_URL}/api/auth/pat`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, tokenName, expireDays }),
      });

      if (res.ok) {
        const newPat: NewPatResponse = await res.json();
        setCreatedPat(newPat);
        setTokenName('');
        fetchTokens();
      }
    } catch {
      // Fallback
    } finally {
      setLoading(false);
    }
  };

  const handleRevoke = async (id: string) => {
    if (!confirm('정말로 이 개인 액세스 토큰을 폐기(Revoke)하시겠습니까?\n폐기 즉시 이 키를 사용하는 AI 에이전트의 접근이 차단됩니다.')) return;

    if (!AUTH_API_BASE_URL) {
      setTokens(prev => prev.map(t => t.id === id ? { ...t, status: 'REVOKED' as const } : t));
      return;
    }

    try {
      const res = await fetch(`${AUTH_API_BASE_URL}/api/auth/pat/${id}?username=${username}`, {
        method: 'DELETE',
      });
      if (res.ok) {
        fetchTokens();
      }
    } catch {
      setTokens(prev => prev.map(t => t.id === id ? { ...t, status: 'REVOKED' as const } : t));
    }
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="space-y-8 p-4 max-w-7xl mx-auto">
      <PageHeader
        title="개인용 액세스 토큰 (PAT) 관리"
        description="AI 에이전트, 스크립트, CLI 도구에서 안전하게 사용할 수 있는 전용 API 마스터 키를 생성 및 관리합니다."
        breadcrumbs={[{ label: '내 프로필' }, { label: 'API 토큰 (PAT)' }]}
        icon={KeyRound}
      />

      {/* 1. 신규 토큰 발급 폼 카드 */}
      <div className="bg-white border border-[#eaedf4] rounded-2xl p-6 shadow-sm space-y-6">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-blue-50 border border-blue-100 flex items-center justify-center text-[#4262ff]">
            <Sparkles className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-base font-bold text-[#17191e]">새 개인용 액세스 토큰 생성 (Generate PAT)</h3>
            <p className="text-xs text-[#545b69]">AI 에이전트에 부여할 전용 키의 용도 이름과 유효기간을 설정하세요.</p>
          </div>
        </div>

        <form onSubmit={handleCreatePat} className="grid grid-cols-1 md:grid-cols-3 gap-4 items-end">
          <div className="md:col-span-1">
            <label className="block text-xs font-bold text-[#17191e] mb-1.5">토큰 용도 이름 (Token Name)</label>
            <input
              type="text"
              value={tokenName}
              onChange={(e) => setTokenName(e.target.value)}
              placeholder="예: My Antigravity AI Agent Key"
              className="w-full bg-[#f7f8fb] border border-[#eaedf4] rounded-xl py-2.5 px-4 text-sm text-[#17191e] focus:border-[#4262ff]"
              required
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-[#17191e] mb-1.5">유효기간 (Expiration Days)</label>
            <select
              value={expireDays}
              onChange={(e) => setExpireDays(Number(e.target.value))}
              className="w-full bg-[#f7f8fb] border border-[#eaedf4] rounded-xl py-2.5 px-4 text-sm text-[#17191e] focus:border-[#4262ff]"
            >
              <option value={30}>30 일</option>
              <option value={90}>90 일 (추천)</option>
              <option value={180}>180 일</option>
              <option value={365}>1 년 (365일)</option>
            </select>
          </div>

          <div>
            <button
              type="submit"
              disabled={loading}
              className="w-full py-2.5 px-5 bg-[#4262ff] hover:bg-[#3452e6] text-white font-bold text-sm rounded-xl shadow-md shadow-blue-600/20 transition-all flex items-center justify-center gap-2 disabled:opacity-50"
            >
              <Plus className="w-4 h-4" />
              <span>새 PAT 토큰 생성</span>
            </button>
          </div>
        </form>

        {/* 생성 완료 시 1회성 원문 토큰 팝업 안내 */}
        {createdPat && (
          <div className="p-5 rounded-2xl bg-emerald-50 border border-emerald-200 text-emerald-800 space-y-3 animate-fade-in">
            <div className="flex items-center gap-2 font-bold text-emerald-900 text-sm">
              <ShieldAlert className="w-5 h-5 text-emerald-600" />
              <span>토큰 생성이 완료되었습니다! (원문 토큰 1회 제공 안내)</span>
            </div>
            <p className="text-xs text-emerald-700">
              보안을 위해 **이 토큰 원문은 지금 단 1회만 노출**되며 다시 확인할 수 없습니다. 안전한 장소에 바로 복사해 두세요!
            </p>
            <div className="flex items-center gap-3 bg-white p-3 rounded-xl border border-emerald-200">
              <code className="text-xs font-mono text-emerald-700 font-bold flex-1 break-all">{createdPat.rawToken}</code>
              <button
                onClick={() => copyToClipboard(createdPat.rawToken)}
                className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs rounded-lg transition-colors flex items-center gap-1 shrink-0 shadow-sm"
              >
                {copied ? <Check className="w-4 h-4" /> : <Copy className="w-4 h-4" />}
                <span>{copied ? '복사됨' : '복사하기'}</span>
              </button>
            </div>
          </div>
        )}
      </div>

      {/* 2. 내 PAT 토큰 리스트 */}
      <div className="bg-white border border-[#eaedf4] rounded-2xl p-6 shadow-sm space-y-4">
        <h3 className="text-base font-bold text-[#17191e] flex items-center gap-2">
          <KeyRound className="w-5 h-5 text-[#4262ff]" />
          <span>보유 중인 개인 액세스 토큰 목록 ({tokens.length})</span>
        </h3>

        {tokens.length === 0 ? (
          <div className="py-12 text-center text-[#8c94a4] text-sm">
            발급받은 개인용 액세스 토큰이 없습니다. 위 폼에서 새 토큰을 생성해 보세요.
          </div>
        ) : (
          <div className="divide-y divide-[#eaedf4]">
            {tokens.map((token) => (
              <div key={token.id} className="py-4 flex items-center justify-between gap-4 hover:bg-[#f7f8fb] px-3 rounded-xl transition-colors">
                <div className="space-y-1">
                  <div className="flex items-center gap-3">
                    <span className="font-bold text-sm text-[#17191e]">{token.tokenName}</span>
                    <span className="text-xs font-mono text-[#545b69] bg-[#f7f8fb] px-2 py-0.5 rounded border border-[#eaedf4]">{token.tokenPrefix}</span>
                    <StatusBadge 
                      status={token.status} 
                      variant={token.status === 'ACTIVE' ? 'success' : token.status === 'REVOKED' ? 'error' : 'neutral'} 
                    />
                  </div>
                  <div className="flex items-center gap-4 text-xs text-[#8c94a4]">
                    <span className="flex items-center gap-1">
                      <Clock className="w-3.5 h-3.5 text-[#8c94a4]" />
                      생성일: {new Date(token.createdAt).toLocaleDateString()}
                    </span>
                    <span>만료일: {new Date(token.expiresAt).toLocaleDateString()}</span>
                    <span>마지막 사용: {token.lastUsedAt ? new Date(token.lastUsedAt).toLocaleString() : '미사용'}</span>
                  </div>
                </div>

                {token.status === 'ACTIVE' && (
                  <button
                    onClick={() => handleRevoke(token.id)}
                    className="px-3 py-1.5 bg-rose-50 hover:bg-rose-100 text-rose-600 border border-rose-200 rounded-xl text-xs font-bold transition-all flex items-center gap-1 shrink-0"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                    <span>토큰 폐기</span>
                  </button>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

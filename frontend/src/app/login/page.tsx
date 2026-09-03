'use client';

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import { KeyRound, User, CheckCircle2, ArrowRight, AlertCircle, Loader2 } from 'lucide-react';
import { authService, AuthenticatedSession } from '@/services/authService';
import { useToast } from '@/context/ToastContext';

/**
 * [K-Bank 스타일 비밀번호 로그인 페이지]
 * 
 * 맑은 소프트 그레이(#f7f8fb) 배경, 화이트 카드, KBank 시그니처 블루(#4262ff) 및 
 * 아이콘 겹침 없는 넉넉한 입력창 구조를 제공하며 다크 모드를 완벽 지원합니다.
 */
export default function LoginPage() {
  const router = useRouter();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');

  const { error: showErrorToast, success: showSuccessToast } = useToast();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [userSession, setUserSession] = useState<AuthenticatedSession | null>(null);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    const normalizedUsername = username.trim();
    if (!normalizedUsername || !password.trim()) {
      const msg = '아이디와 비밀번호를 모두 입력해 주세요.';
      setError(msg);
      showErrorToast(msg, { title: '입력 확인' });
      return;
    }

    setLoading(true);

    let data: AuthenticatedSession;
    try {
      data = await authService.login({
        username: normalizedUsername,
        password,
      });
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : '아이디 또는 비밀번호가 올바르지 않습니다.';
      setError(msg);
      showErrorToast(msg, { title: '인증 실패' });
      setLoading(false);
      return;
    }

    showSuccessToast(`${data.username} 계정으로 로그인되었습니다.`, { title: '인증 성공' });
    setUserSession(data);
    setLoading(false);
    setTimeout(() => {
      router.push('/');
    }, 1200);
  };

  return (
    <div className="min-h-[calc(100vh-140px)] flex items-center justify-center py-12 px-4">
      <div className="w-full max-w-md bg-white dark:bg-[#131b2e] border border-[#eaedf4] dark:border-slate-800 rounded-3xl p-8 sm:p-10 shadow-lg shadow-blue-600/5 dark:shadow-black/20 relative z-10 transition-all">
        {/* 상단 KBank 로고/아이콘 헤더 */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-blue-50 dark:bg-blue-950/50 border border-blue-100 dark:border-blue-800 text-[#4262ff] dark:text-blue-400 mb-4 shadow-xs">
            <KeyRound size={32} />
          </div>
          <h1 className="text-2xl font-black tracking-tight text-[#17191e] dark:text-slate-100">통합 인증 시스템</h1>
          <p className="text-xs text-[#545b69] dark:text-slate-400 font-medium mt-1.5">
            발급받은 계정의 아이디와 비밀번호로 로그인해 주세요.
          </p>
        </div>

        {/* 오류 알림 */}
        {error && (
          <div role="alert" aria-live="assertive" className="mb-6 p-4 rounded-xl bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-800 text-rose-700 dark:text-rose-300 text-xs flex items-start gap-3">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-rose-500" />
            <div>
              <p className="font-bold">인증 오류</p>
              <p className="text-[11px] text-rose-600 dark:text-rose-400 mt-0.5">{error}</p>
            </div>
          </div>
        )}

        {/* 로그인 성공 화면 */}
        {userSession ? (
          <div role="status" aria-live="polite" className="p-6 rounded-2xl bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-800 text-emerald-800 dark:text-emerald-300 text-xs text-center space-y-3 animate-fade-in">
            <CheckCircle2 className="w-12 h-12 text-emerald-500 mx-auto" />
            <h3 className="text-base font-black text-emerald-900 dark:text-emerald-200">인증 성공!</h3>
            <p className="text-xs text-emerald-700 dark:text-emerald-300">
              사용자: <span className="font-bold">{userSession.username}</span> ({userSession.departmentCode} 부서)
            </p>
            <p className="text-[11px] text-[#545b69] dark:text-slate-400 font-medium">대시보드로 자동 이동 중입니다...</p>
          </div>
        ) : (
          <form onSubmit={handleLogin} aria-busy={loading} className="space-y-4 animate-fade-in">
            {/* 사용자 아이디 입력창 */}
            <div>
              <label htmlFor="username" className="block text-xs font-bold text-[#17191e] dark:text-slate-200 mb-1.5">사용자 아이디 (Username)</label>
              <div className="relative flex items-center">
                <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-[#8c94a4] dark:text-slate-500 flex items-center justify-center pointer-events-none z-10">
                  <User size={18} />
                </span>
                <input
                  type="text"
                  id="username"
                  name="username"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  className="w-full bg-[#f7f8fb] dark:bg-slate-900 hover:bg-white dark:hover:bg-slate-900 focus:bg-white dark:focus:bg-slate-900 border border-[#eaedf4] dark:border-slate-800 focus:border-[#4262ff] rounded-xl py-3 !pl-11 pr-4 text-xs font-semibold text-[#17191e] dark:text-slate-100 placeholder-[#8c94a4] focus:outline-none transition-all shadow-xs"
                  placeholder="아이디를 입력하세요"
                  autoComplete="username"
                  disabled={loading}
                  required
                />
              </div>
            </div>

            <div className="animate-fade-in">
              <label htmlFor="password" className="block text-xs font-bold text-[#17191e] dark:text-slate-200 mb-1.5">비밀번호 (Password)</label>
              <div className="relative flex items-center">
                <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-[#8c94a4] dark:text-slate-500 flex items-center justify-center pointer-events-none z-10">
                  <KeyRound size={18} />
                </span>
                <input
                  type="password"
                  id="password"
                  name="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="w-full bg-[#f7f8fb] dark:bg-slate-900 hover:bg-white dark:hover:bg-slate-900 focus:bg-white dark:focus:bg-slate-900 border border-[#eaedf4] dark:border-slate-800 focus:border-[#4262ff] rounded-xl py-3 !pl-11 pr-4 text-xs font-semibold text-[#17191e] dark:text-slate-100 placeholder-[#8c94a4] focus:outline-none transition-all shadow-xs"
                  placeholder="비밀번호를 입력하세요"
                  autoComplete="current-password"
                  disabled={loading}
                  required
                />
              </div>
            </div>

            {/* KBank 시그니처 블루 로그인 버튼 */}
            <button
              type="submit"
              disabled={loading}
              aria-busy={loading}
              className="w-full py-3.5 px-4 mt-3 bg-[#4262ff] hover:bg-[#3452e6] active:bg-[#2b44d4] !text-white font-bold text-xs rounded-xl shadow-md shadow-blue-600/25 transition-all flex items-center justify-center gap-2 disabled:opacity-60 disabled:cursor-not-allowed disabled:pointer-events-none cursor-pointer"
            >
              {loading ? (
                <>
                  <Loader2 size={16} className="animate-spin !text-white shrink-0" />
                  <span className="!text-white">인증 확인 중...</span>
                </>
              ) : (
                <>
                  <span className="!text-white">로그인</span>
                  <ArrowRight size={15} className="!text-white shrink-0" />
                </>
              )}
            </button>
          </form>
        )}
      </div>
    </div>
  );
}

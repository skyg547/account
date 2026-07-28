'use client';

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import { KeyRound, User, Building, ShieldCheck, AlertCircle, CheckCircle2, ArrowRight } from 'lucide-react';
import { authService, LoginResponse } from '@/services/authService';

export default function LoginPage() {
  const router = useRouter();
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('1234');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [userSession, setUserSession] = useState<LoginResponse | null>(null);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      // 1. 실제 백엔드 API (http://localhost:8080/api/auth/login) 호출
      const data = await authService.login({ username, password });
      
      // 2. 브라우저 localStorage에 JWT 토큰 및 세션 정보 저장
      localStorage.setItem('auth_token', data.token);
      localStorage.setItem('user_info', JSON.stringify(data));
      setUserSession(data);

      setTimeout(() => {
        router.push('/');
      }, 1500);
    } catch (err: any) {
      setError(err.message || '백엔드 서버 연결 실패 (8080 포트를 확인하세요)');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex items-center justify-center p-4 relative overflow-hidden">
      {/* Background Glow Deco */}
      <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-blue-600/10 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-indigo-600/10 rounded-full blur-3xl pointer-events-none" />

      <div className="w-full max-w-md bg-slate-900/60 backdrop-blur-xl border border-white/10 rounded-3xl p-8 shadow-2xl relative z-10">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-blue-500/10 border border-blue-500/20 text-blue-400 mb-4 shadow-lg shadow-blue-500/10">
            <KeyRound className="w-8 h-8" />
          </div>
          <h1 className="text-2xl font-bold tracking-tight text-white">Auth 모듈 실시간 연동</h1>
          <p className="text-sm text-slate-400 mt-2">
            Spring Boot 백엔드(<span className="text-blue-400 font-mono">localhost:8080</span>)와 직접 통신하여 실제 JWT 토큰을 발급받습니다.
          </p>
        </div>

        {error && (
          <div className="mb-6 p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-300 text-sm flex items-start gap-3">
            <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
            <div>
              <p className="font-semibold">연동 오류</p>
              <p className="text-xs text-rose-400/90 mt-0.5">{error}</p>
            </div>
          </div>
        )}

        {userSession ? (
          <div className="p-6 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-300 text-sm text-center space-y-3 animate-fade-in">
            <CheckCircle2 className="w-12 h-12 text-emerald-400 mx-auto" />
            <h3 className="text-lg font-bold text-white">JWT 토큰 발급 성공!</h3>
            <p className="text-xs text-emerald-400/90">
              사용자: <span className="font-bold text-white">{userSession.username}</span> ({userSession.departmentCode} 부서)
            </p>
            <div className="bg-slate-950/80 p-3 rounded-xl text-left border border-white/5 font-mono text-[10px] text-slate-400 break-all max-h-24 overflow-y-auto">
              <span className="text-emerald-400 font-bold">Bearer Token:</span> {userSession.token}
            </div>
            <p className="text-xs text-slate-400">잠시 후 대시보드로 이동합니다...</p>
          </div>
        ) : (
          <form onSubmit={handleLogin} className="space-y-5">
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-2">사용자 아이디 (Username)</label>
              <div className="relative">
                <User className="w-5 h-5 absolute left-3.5 top-3 text-slate-400" />
                <input
                  type="text"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  className="w-full bg-slate-950/50 border border-white/10 rounded-xl py-2.5 pl-11 pr-4 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 transition-colors"
                  placeholder="예: admin"
                  required
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-2">비밀번호 (Password)</label>
              <div className="relative">
                <KeyRound className="w-5 h-5 absolute left-3.5 top-3 text-slate-400" />
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="w-full bg-slate-950/50 border border-white/10 rounded-xl py-2.5 pl-11 pr-4 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 transition-colors"
                  placeholder="예: 1234"
                  required
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3 px-4 bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 text-white font-semibold text-sm rounded-xl shadow-lg shadow-blue-600/20 transition-all flex items-center justify-center gap-2 disabled:opacity-50"
            >
              {loading ? (
                <span>JWT 토큰 요청 중...</span>
              ) : (
                <>
                  <span>실제 백엔드로 로그인</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>
        )}
      </div>
    </div>
  );
}

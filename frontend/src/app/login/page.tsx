'use client';

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import { KeyRound, User, CheckCircle2, ArrowRight, AlertCircle, Fingerprint, LockKeyhole } from 'lucide-react';
import { authService, LoginResponse } from '@/services/authService';

export default function LoginPage() {
  const router = useRouter();
  const [loginType, setLoginType] = useState<'SSO' | 'LDAP'>('SSO');
  
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('1234');
  const [otpCode, setOtpCode] = useState('');
  
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [userSession, setUserSession] = useState<LoginResponse | null>(null);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      // 1. 백엔드 연동: loginType에 따라 SSO / LDAP 분기 처리
      const data = await authService.login({ 
        username, 
        password: loginType === 'LDAP' ? password : '',
        loginType,
        otpCode: loginType === 'LDAP' ? otpCode : ''
      });
      
      // 2. 브라우저 localStorage에 JWT 토큰 및 세션 정보 저장
      localStorage.setItem('auth_token', data.token);
      localStorage.setItem('user_info', JSON.stringify(data));
      setUserSession(data);

      setTimeout(() => {
        router.push('/');
      }, 1500);
    } catch (err: any) {
      setError(err.message || '로그인 실패');
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
        <div className="text-center mb-6">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-blue-500/10 border border-blue-500/20 text-blue-400 mb-4 shadow-lg shadow-blue-500/10">
            {loginType === 'SSO' ? <Fingerprint className="w-8 h-8" /> : <KeyRound className="w-8 h-8" />}
          </div>
          <h1 className="text-2xl font-bold tracking-tight text-white">통합 인증 시스템</h1>
          <p className="text-sm text-slate-400 mt-2">
            다중 인증(Multi-Factor) 및 SSO를 지원합니다.
          </p>
        </div>

        {/* 탭 구조 UI */}
        {!userSession && (
          <div className="flex bg-slate-950/50 rounded-xl p-1 mb-8 border border-white/5">
            <button
              onClick={() => { setLoginType('SSO'); setError(null); }}
              className={`flex-1 py-2 text-sm font-bold rounded-lg transition-all ${
                loginType === 'SSO' ? 'bg-blue-600 text-white shadow-md' : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              SSO 자동 로그인
            </button>
            <button
              onClick={() => { setLoginType('LDAP'); setError(null); }}
              className={`flex-1 py-2 text-sm font-bold rounded-lg transition-all ${
                loginType === 'LDAP' ? 'bg-indigo-600 text-white shadow-md' : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              LDAP + OTP
            </button>
          </div>
        )}

        {error && (
          <div className="mb-6 p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-300 text-sm flex items-start gap-3">
            <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
            <div>
              <p className="font-semibold">인증 오류</p>
              <p className="text-xs text-rose-400/90 mt-0.5">{error}</p>
            </div>
          </div>
        )}

        {userSession ? (
          <div className="p-6 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-300 text-sm text-center space-y-3 animate-fade-in">
            <CheckCircle2 className="w-12 h-12 text-emerald-400 mx-auto" />
            <h3 className="text-lg font-bold text-white">인증 성공!</h3>
            <p className="text-xs text-emerald-400/90">
              사용자: <span className="font-bold text-white">{userSession.username}</span> ({userSession.departmentCode} 부서)
            </p>
            <div className="bg-slate-950/80 p-3 rounded-xl text-left border border-white/5 font-mono text-[10px] text-slate-400 break-all max-h-24 overflow-y-auto">
              <span className="text-emerald-400 font-bold">Bearer Token:</span> {userSession.token}
            </div>
            <p className="text-xs text-slate-400">잠시 후 대시보드로 이동합니다...</p>
          </div>
        ) : (
          <form onSubmit={handleLogin} className="space-y-5 animate-fade-in">
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

            {loginType === 'LDAP' && (
              <>
                <div className="animate-fade-in">
                  <label className="block text-xs font-semibold text-slate-300 mb-2">비밀번호 (Password)</label>
                  <div className="relative">
                    <KeyRound className="w-5 h-5 absolute left-3.5 top-3 text-slate-400" />
                    <input
                      type="password"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      className="w-full bg-slate-950/50 border border-white/10 rounded-xl py-2.5 pl-11 pr-4 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500 transition-colors"
                      placeholder="사내망 비밀번호 (예: 1234)"
                      required
                    />
                  </div>
                </div>

                <div className="animate-fade-in">
                  <label className="block text-xs font-semibold text-slate-300 mb-2">OTP 코드 (2FA 인증)</label>
                  <div className="relative">
                    <LockKeyhole className="w-5 h-5 absolute left-3.5 top-3 text-slate-400" />
                    <input
                      type="text"
                      value={otpCode}
                      onChange={(e) => setOtpCode(e.target.value)}
                      className="w-full bg-slate-950/50 border border-white/10 rounded-xl py-2.5 pl-11 pr-4 text-sm font-mono text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500 transition-colors"
                      placeholder="6자리 숫자 (예: 123456)"
                      required
                    />
                  </div>
                  <p className="text-[10px] text-slate-500 mt-2 ml-1">※ 데모 테스트를 위해 OTP는 '123456'을 입력하세요.</p>
                </div>
              </>
            )}

            <button
              type="submit"
              disabled={loading}
              className={`w-full py-3 px-4 mt-4 bg-gradient-to-r text-white font-semibold text-sm rounded-xl shadow-lg transition-all flex items-center justify-center gap-2 disabled:opacity-50 ${
                loginType === 'SSO' 
                ? 'from-blue-600 to-cyan-600 hover:from-blue-500 hover:to-cyan-500 shadow-blue-600/20' 
                : 'from-indigo-600 to-purple-600 hover:from-indigo-500 hover:to-purple-500 shadow-indigo-600/20'
              }`}
            >
              {loading ? (
                <span>인증 확인 중...</span>
              ) : (
                <>
                  <span>{loginType === 'SSO' ? 'SSO로 빠른 로그인' : 'LDAP+OTP 안전 로그인'}</span>
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

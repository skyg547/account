'use client';

import React, { createContext, useContext, useState, useCallback, useEffect } from 'react';
import { CheckCircle2, AlertCircle, AlertTriangle, Info, Loader2, X } from 'lucide-react';

export type ToastType = 'success' | 'error' | 'warning' | 'info' | 'loading';

export interface ToastItem {
  id: string;
  type: ToastType;
  title?: string;
  message: string;
  duration?: number;
}

export interface ToastOptions {
  type?: ToastType;
  title?: string;
  message: string;
  duration?: number;
}

interface ToastContextType {
  toasts: ToastItem[];
  showToast: (options: ToastOptions) => string;
  success: (message: string, options?: Omit<ToastOptions, 'message' | 'type'>) => string;
  error: (message: string, options?: Omit<ToastOptions, 'message' | 'type'>) => string;
  warning: (message: string, options?: Omit<ToastOptions, 'message' | 'type'>) => string;
  info: (message: string, options?: Omit<ToastOptions, 'message' | 'type'>) => string;
  loading: (message: string, options?: Omit<ToastOptions, 'message' | 'type'>) => string;
  dismissToast: (id: string) => void;
}

const ToastContext = createContext<ToastContextType | undefined>(undefined);

/**
 * [글로벌 토스트 이벤트 발송 유틸리티]
 * React 컴포넌트 외부(Service, API Client 등)에서도 전역 토스트를 호출할 수 있도록 브라우저 CustomEvent를 디스패치합니다.
 */
export function emitToast(options: ToastOptions) {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('app:toast', { detail: options }));
  }
}

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  const dismissToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const showToast = useCallback(
    ({ type = 'info', title, message, duration = 4000 }: ToastOptions): string => {
      const id = `toast-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`;
      const newToast: ToastItem = { id, type, title, message, duration };

      setToasts((prev) => [...prev, newToast]);

      if (duration > 0) {
        setTimeout(() => {
          dismissToast(id);
        }, duration);
      }

      return id;
    },
    [dismissToast]
  );

  const success = useCallback(
    (message: string, options?: Omit<ToastOptions, 'message' | 'type'>) =>
      showToast({ ...options, type: 'success', message }),
    [showToast]
  );

  const error = useCallback(
    (message: string, options?: Omit<ToastOptions, 'message' | 'type'>) =>
      showToast({ ...options, type: 'error', message, duration: options?.duration ?? 5000 }),
    [showToast]
  );

  const warning = useCallback(
    (message: string, options?: Omit<ToastOptions, 'message' | 'type'>) =>
      showToast({ ...options, type: 'warning', message }),
    [showToast]
  );

  const info = useCallback(
    (message: string, options?: Omit<ToastOptions, 'message' | 'type'>) =>
      showToast({ ...options, type: 'info', message }),
    [showToast]
  );

  const loading = useCallback(
    (message: string, options?: Omit<ToastOptions, 'message' | 'type'>) =>
      showToast({ ...options, type: 'loading', message, duration: 0 }),
    [showToast]
  );

  // 브라우저 CustomEvent 리스너 등록
  useEffect(() => {
    const handleCustomToast = (event: Event) => {
      const customEvent = event as CustomEvent<ToastOptions>;
      if (customEvent.detail) {
        showToast(customEvent.detail);
      }
    };

    window.addEventListener('app:toast', handleCustomToast);
    return () => window.removeEventListener('app:toast', handleCustomToast);
  }, [showToast]);

  const getToastIcon = (type: ToastType) => {
    switch (type) {
      case 'success':
        return <CheckCircle2 className="w-5 h-5 text-emerald-500 shrink-0 mt-0.5" />;
      case 'error':
        return <AlertCircle className="w-5 h-5 text-rose-500 shrink-0 mt-0.5" />;
      case 'warning':
        return <AlertTriangle className="w-5 h-5 text-amber-500 shrink-0 mt-0.5" />;
      case 'loading':
        return <Loader2 className="w-5 h-5 text-[#4262ff] dark:text-blue-400 shrink-0 mt-0.5 animate-spin" />;
      case 'info':
      default:
        return <Info className="w-5 h-5 text-blue-500 shrink-0 mt-0.5" />;
    }
  };

  const getToastStyles = (type: ToastType) => {
    switch (type) {
      case 'success':
        return 'bg-white dark:bg-[#131b2e] border-emerald-300 dark:border-emerald-800 text-emerald-950 dark:text-emerald-100 shadow-emerald-500/10';
      case 'error':
        return 'bg-white dark:bg-[#131b2e] border-rose-300 dark:border-rose-800 text-rose-950 dark:text-rose-100 shadow-rose-500/10';
      case 'warning':
        return 'bg-white dark:bg-[#131b2e] border-amber-300 dark:border-amber-800 text-amber-950 dark:text-amber-100 shadow-amber-500/10';
      case 'loading':
        return 'bg-white dark:bg-[#131b2e] border-blue-300 dark:border-blue-800 text-blue-950 dark:text-blue-100 shadow-blue-500/10';
      case 'info':
      default:
        return 'bg-white dark:bg-[#131b2e] border-[#eaedf4] dark:border-slate-800 text-[#17191e] dark:text-slate-100 shadow-blue-600/5';
    }
  };

  return (
    <ToastContext.Provider value={{ toasts, showToast, success, error, warning, info, loading, dismissToast }}>
      {children}
      {/* 고정 위치 토스트 렌더링 컨테이너 */}
      <div
        role="region"
        aria-label="알림 메시지"
        className="fixed bottom-6 right-6 z-[99999] flex flex-col gap-3 max-w-sm sm:max-w-md w-full pointer-events-none px-4 sm:px-0"
      >
        {toasts.map((toast) => (
          <div
            key={toast.id}
            role="alert"
            aria-live="assertive"
            className={`pointer-events-auto p-4 rounded-2xl border shadow-xl backdrop-blur-md flex items-start gap-3 transition-all duration-300 animate-in slide-in-from-bottom-5 fade-in ${getToastStyles(
              toast.type
            )}`}
          >
            {getToastIcon(toast.type)}
            <div className="flex-1 min-w-0">
              {toast.title && (
                <h4 className="text-xs font-bold leading-tight mb-0.5">{toast.title}</h4>
              )}
              <p className="text-xs font-semibold leading-relaxed break-words">{toast.message}</p>
            </div>
            <button
              onClick={() => dismissToast(toast.id)}
              className="text-[#8c94a4] hover:text-[#17191e] dark:text-slate-400 dark:hover:text-white p-1 rounded-lg transition-colors cursor-pointer shrink-0"
              aria-label="알림 닫기"
            >
              <X size={14} />
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error('useToast must be used within a ToastProvider');
  }
  return context;
}

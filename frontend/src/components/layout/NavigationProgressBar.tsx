'use client';

import React, { useEffect, useState, useRef, useCallback } from 'react';
import { usePathname } from 'next/navigation';

/**
 * [상단 라우트 전환 프로그레스 바]
 * 페이지 간 전환 및 Next.js 온디맨드 컴파일 지연 시 상단에 즉각적인 시각 피드백을 제공합니다.
 */
export default function NavigationProgressBar() {
  const pathname = usePathname();
  const [progress, setProgress] = useState(0);
  const [visible, setVisible] = useState(false);
  const timerRef = useRef<NodeJS.Timeout | null>(null);
  const finishTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  const startProgress = useCallback(() => {
    if (finishTimeoutRef.current) {
      clearTimeout(finishTimeoutRef.current);
      finishTimeoutRef.current = null;
    }

    setVisible(true);
    setProgress((prev) => (prev > 0 && prev < 30 ? prev : 25));

    if (timerRef.current) clearInterval(timerRef.current);

    timerRef.current = setInterval(() => {
      setProgress((current) => {
        if (current >= 85) {
          if (timerRef.current) clearInterval(timerRef.current);
          return current;
        }
        // 부드러운 점진적 증가
        const diff = (90 - current) * 0.15;
        return Math.min(88, current + Math.max(diff, 1.5));
      });
    }, 150);
  }, []);

  const completeProgress = useCallback(() => {
    if (timerRef.current) {
      clearInterval(timerRef.current);
      timerRef.current = null;
    }

    setProgress(100);

    finishTimeoutRef.current = setTimeout(() => {
      setVisible(false);
      setProgress(0);
    }, 300);
  }, []);

  // 라우트(pathname) 변경 시 프로그레스 바 완료
  useEffect(() => {
    completeProgress();
  }, [pathname, completeProgress]);

  // 클릭 이벤트 감지 (내부 링크 클릭 시 즉시 프로그레스 시작)
  useEffect(() => {
    const handleDocumentClick = (e: MouseEvent) => {
      // 보조 클릭, 메타키, 취소된 이벤트 무시
      if (e.defaultPrevented || e.button !== 0 || e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) {
        return;
      }

      const target = e.target as HTMLElement | null;
      const anchor = target?.closest('a');
      if (!anchor) return;

      const href = anchor.getAttribute('href');
      if (!href) return;

      // 외부 링크 또는 해시 앵커 무시
      if (
        href.startsWith('http:') ||
        href.startsWith('https:') ||
        href.startsWith('mailto:') ||
        href.startsWith('tel:') ||
        href.startsWith('#') ||
        anchor.target === '_blank' ||
        anchor.hasAttribute('download')
      ) {
        return;
      }

      // 현재 경로와 동일한 경우 무시
      try {
        const targetUrl = new URL(href, window.location.href);
        const currentUrl = new URL(window.location.href);
        if (targetUrl.pathname === currentUrl.pathname && targetUrl.search === currentUrl.search) {
          return;
        }
      } catch {
        // url parsing failure
      }

      startProgress();
    };

    // 커스텀 이벤트 리스너 (프로그래밍 방식 router.push 지원)
    const handleStartEvent = () => startProgress();
    const handleEndEvent = () => completeProgress();

    document.addEventListener('click', handleDocumentClick, true);
    window.addEventListener('app:navigation-start', handleStartEvent);
    window.addEventListener('app:navigation-end', handleEndEvent);

    return () => {
      document.removeEventListener('click', handleDocumentClick, true);
      window.removeEventListener('app:navigation-start', handleStartEvent);
      window.removeEventListener('app:navigation-end', handleEndEvent);
      if (timerRef.current) clearInterval(timerRef.current);
      if (finishTimeoutRef.current) clearTimeout(finishTimeoutRef.current);
    };
  }, [startProgress, completeProgress]);

  if (!visible && progress === 0) return null;

  return (
    <div
      role="progressbar"
      aria-valuenow={Math.round(progress)}
      aria-valuemin={0}
      aria-valuemax={100}
      className="fixed top-0 left-0 right-0 h-[3px] z-[99999] pointer-events-none bg-transparent overflow-hidden"
    >
      <div
        className="h-full bg-gradient-to-r from-[#4262ff] via-[#60a5fa] to-[#4262ff] transition-all ease-out shadow-[0_0_12px_rgba(66,98,255,0.8)]"
        style={{
          width: `${progress}%`,
          transitionDuration: progress === 100 ? '200ms' : '300ms',
          opacity: visible ? 1 : 0,
        }}
      />
    </div>
  );
}

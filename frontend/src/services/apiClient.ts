import { emitToast } from '@/context/ToastContext';

export class ApiTimeoutError extends Error {
  constructor(message = '서버 응답 대기 시간이 초과되었습니다 (타임아웃). 네트워크 상태를 확인하거나 잠시 후 다시 시도해 주세요.') {
    super(message);
    this.name = 'ApiTimeoutError';
  }
}

export class ApiError extends Error {
  status: number;
  statusText: string;

  constructor(status: number, statusText: string, message?: string) {
    super(message || `API 요청 처리 실패 (${status} ${statusText})`);
    this.name = 'ApiError';
    this.status = status;
    this.statusText = statusText;
  }
}

export interface FetchOptions extends RequestInit {
  timeoutMs?: number;
  silentToast?: boolean;
}

/**
 * [타임아웃 및 네트워크 오류 자동 감지 fetch 래퍼]
 * 기본 15초 타임아웃을 적용하며, 타임아웃 또는 서버 오류 발생 시 글로벌 토스트 피드백을 전달합니다.
 */
export async function fetchWithTimeout(
  url: string,
  options: FetchOptions = {}
): Promise<Response> {
  const { timeoutMs = 15000, silentToast = false, signal: externalSignal, ...fetchInit } = options;

  const controller = new AbortController();
  const timer = setTimeout(() => {
    controller.abort(new ApiTimeoutError());
  }, timeoutMs);

  // 외부 전달 signal이 있을 경우 상호 연동
  if (externalSignal) {
    externalSignal.addEventListener('abort', () => {
      controller.abort(externalSignal.reason);
    });
  }

  try {
    const response = await fetch(url, {
      ...fetchInit,
      signal: controller.signal,
    });
    return response;
  } catch (err: unknown) {
    const isTimeout =
      (err instanceof DOMException && err.name === 'AbortError') ||
      err instanceof ApiTimeoutError ||
      (err instanceof Error && err.name === 'TimeoutError');

    if (isTimeout) {
      if (!silentToast) {
        emitToast({
          type: 'error',
          title: '요청 시간 초과 (Timeout)',
          message: '백엔드 마이크로서비스 응답 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요.',
        });
      }
      throw new ApiTimeoutError();
    }

    if (!silentToast && err instanceof TypeError && err.message.includes('fetch')) {
      emitToast({
        type: 'error',
        title: '네트워크 연결 오류',
        message: '서버와의 통신에 실패했습니다. 백엔드 서비스 가동 여부를 확인해 주세요.',
      });
    }

    throw err;
  } finally {
    clearTimeout(timer);
  }
}

/**
 * [JSON 비동기 호출 및 자동 에러 피드백 헬퍼]
 */
export async function requestJsonWithTimeout<T>(
  url: string,
  options: FetchOptions = {}
): Promise<T> {
  const response = await fetchWithTimeout(url, options);

  if (!response.ok) {
    let errorMessage = `HTTP ${response.status} ${response.statusText}`;
    try {
      const errorJson = (await response.json()) as { message?: string; error?: string };
      errorMessage = errorJson.message || errorJson.error || errorMessage;
    } catch {
      try {
        const text = await response.text();
        if (text) errorMessage = text;
      } catch {
        // use default HTTP error
      }
    }

    if (!options.silentToast) {
      emitToast({
        type: 'error',
        title: `API 오류 (${response.status})`,
        message: errorMessage,
      });
    }

    throw new ApiError(response.status, response.statusText, errorMessage);
  }

  return response.json() as Promise<T>;
}

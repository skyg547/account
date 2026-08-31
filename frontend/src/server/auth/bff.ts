import { NextRequest, NextResponse } from 'next/server';

const DEVELOPMENT_SESSION_COOKIE = 'account_session';
const PRODUCTION_SESSION_COOKIE = '__Host-account_session';
const MAX_LOGIN_BODY_BYTES = 16 * 1024;
const MAX_PROXY_BODY_BYTES = 1024 * 1024;
const MAX_SESSION_AGE_SECONDS = 8 * 60 * 60;
const MAX_SESSION_TOKEN_LENGTH = 3800;
const GATEWAY_TIMEOUT_MILLISECONDS = 10_000;

const FORWARDED_REQUEST_HEADERS = [
  'accept',
  'accept-language',
  'content-type',
  'if-modified-since',
  'if-none-match',
  'range',
] as const;

const FORWARDED_RESPONSE_HEADERS = [
  'accept-ranges',
  'content-disposition',
  'content-range',
  'content-type',
  'etag',
  'last-modified',
  'ratelimit-limit',
  'ratelimit-remaining',
  'ratelimit-reset',
  'retry-after',
  'www-authenticate',
  'x-auth-error',
  'x-ratelimit-burst-capacity',
  'x-ratelimit-remaining',
  'x-ratelimit-replenish-rate',
  'x-request-id',
] as const;

interface UpstreamLoginResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
  username: string;
  departmentCode: string;
  roles: string[];
  roleVersion: number;
}

export interface AuthenticatedSession {
  expiresIn: number;
  username: string;
  departmentCode: string;
  roles: string[];
  roleVersion: number;
}

export class BffRequestError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message);
  }
}

function isProduction(): boolean {
  return process.env.NODE_ENV === 'production';
}

function sessionCookieName(): string {
  return isProduction() ? PRODUCTION_SESSION_COOKIE : DEVELOPMENT_SESSION_COOKIE;
}

function sessionCookieOptions(maxAge: number) {
  return {
    httpOnly: true,
    secure: isProduction(),
    sameSite: 'strict' as const,
    path: '/',
    maxAge,
    expires: new Date(Date.now() + maxAge * 1000),
  };
}

export function setSessionCookie(
  response: NextResponse,
  token: string,
  expiresIn: number,
): void {
  const maxAge = Math.min(Math.floor(expiresIn), MAX_SESSION_AGE_SECONDS);
  response.cookies.set({
    name: sessionCookieName(),
    value: token,
    ...sessionCookieOptions(maxAge),
  });
}

export function clearSessionCookie(response: NextResponse): void {
  response.cookies.set({
    name: sessionCookieName(),
    value: '',
    ...sessionCookieOptions(0),
    expires: new Date(0),
  });

  // A production deployment can replace a prior HTTP-only development instance
  // on the same host. Clear that weaker cookie during the upgrade as well.
  if (isProduction()) {
    response.cookies.set({
      name: DEVELOPMENT_SESSION_COOKIE,
      value: '',
      ...sessionCookieOptions(0),
      expires: new Date(0),
    });
  }
}

function isValidSessionToken(value: string | undefined): value is string {
  return Boolean(
    value &&
      value.length <= MAX_SESSION_TOKEN_LENGTH &&
      /^[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+$/.test(value),
  );
}

export function getSessionToken(request: NextRequest): string | null {
  const value = request.cookies.get(sessionCookieName())?.value;
  return isValidSessionToken(value) ? value : null;
}

export function hasSessionCookie(request: NextRequest): boolean {
  return request.cookies.has(sessionCookieName());
}

function configuredPublicOrigin(): string | null {
  const configured = process.env.FRONTEND_PUBLIC_ORIGIN?.trim();
  if (!configured) {
    return null;
  }

  let parsed: URL;
  try {
    parsed = new URL(configured);
  } catch {
    throw new BffRequestError(503, '프론트엔드 공개 주소 설정이 올바르지 않습니다.');
  }
  if (
    !['http:', 'https:'].includes(parsed.protocol) ||
    parsed.username ||
    parsed.password ||
    parsed.pathname !== '/' ||
    parsed.search ||
    parsed.hash
  ) {
    throw new BffRequestError(503, '프론트엔드 공개 주소 설정이 올바르지 않습니다.');
  }
  return parsed.origin;
}

export function requireSameOrigin(request: NextRequest): void {
  const originHeader = request.headers.get('origin');
  const fetchSite = request.headers.get('sec-fetch-site');

  if (!originHeader || (fetchSite && fetchSite !== 'same-origin')) {
    throw new BffRequestError(403, '동일 출처 요청만 허용됩니다.');
  }

  let origin: string;
  try {
    const parsedOrigin = new URL(originHeader);
    origin = parsedOrigin.origin;
    if (originHeader !== origin) {
      throw new Error('Origin header must contain only an origin.');
    }
  } catch {
    throw new BffRequestError(403, '동일 출처 요청만 허용됩니다.');
  }

  const host = request.headers.get('host');
  let requestOrigin = request.nextUrl.origin;
  if (host) {
    try {
      requestOrigin = new URL(`${request.nextUrl.protocol}//${host}`).origin;
    } catch {
      throw new BffRequestError(403, '동일 출처 요청만 허용됩니다.');
    }
  }

  const expectedOrigin = configuredPublicOrigin() ?? requestOrigin;
  if (origin !== expectedOrigin) {
    throw new BffRequestError(403, '동일 출처 요청만 허용됩니다.');
  }
}

function gatewayBaseUrl(): URL {
  const configured = process.env.GATEWAY_INTERNAL_URL?.trim();
  const value = configured || (isProduction() ? '' : 'http://localhost:8000');
  if (!value) {
    throw new BffRequestError(503, 'Gateway 내부 주소가 설정되지 않았습니다.');
  }

  let parsed: URL;
  try {
    parsed = new URL(value);
  } catch {
    throw new BffRequestError(503, 'Gateway 내부 주소 설정이 올바르지 않습니다.');
  }

  if (
    !['http:', 'https:'].includes(parsed.protocol) ||
    parsed.username ||
    parsed.password ||
    !['', '/'].includes(parsed.pathname) ||
    parsed.search ||
    parsed.hash
  ) {
    throw new BffRequestError(503, 'Gateway 내부 주소 설정이 올바르지 않습니다.');
  }
  return parsed;
}

export function gatewayApiUrl(path: readonly string[], search = ''): URL {
  if (!path.length || path.some((segment) => !segment || segment === '.' || segment === '..')) {
    throw new BffRequestError(400, 'API 경로가 올바르지 않습니다.');
  }

  const encodedPath = path.map((segment) => encodeURIComponent(segment)).join('/');
  const url = new URL(`/api/${encodedPath}`, gatewayBaseUrl());
  url.search = search;
  return url;
}

export async function readBoundedBody(
  request: NextRequest,
  maxBytes: number,
): Promise<ArrayBuffer | undefined> {
  if (request.method === 'GET' || request.method === 'HEAD') {
    return undefined;
  }

  const contentLength = request.headers.get('content-length');
  if (contentLength) {
    const parsedLength = Number(contentLength);
    if (!Number.isSafeInteger(parsedLength) || parsedLength < 0) {
      throw new BffRequestError(400, '요청 본문 길이가 올바르지 않습니다.');
    }
    if (parsedLength > maxBytes) {
      throw new BffRequestError(413, '요청 본문이 너무 큽니다.');
    }
  }

  if (!request.body) {
    return undefined;
  }

  const reader = request.body.getReader();
  const chunks: Uint8Array[] = [];
  let totalBytes = 0;
  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) {
        break;
      }
      if (!value.byteLength) {
        continue;
      }

      totalBytes += value.byteLength;
      if (totalBytes > maxBytes) {
        try {
          await reader.cancel('request body exceeds the configured limit');
        } catch {
          // Preserve the deterministic 413 response even if stream cancellation fails.
        }
        throw new BffRequestError(413, '요청 본문이 너무 큽니다.');
      }
      chunks.push(value);
    }
  } catch (error) {
    if (error instanceof BffRequestError) {
      throw error;
    }
    throw new BffRequestError(400, '요청 본문을 읽을 수 없습니다.');
  } finally {
    reader.releaseLock();
  }

  if (!totalBytes) {
    return undefined;
  }

  const body = new Uint8Array(totalBytes);
  let offset = 0;
  for (const chunk of chunks) {
    body.set(chunk, offset);
    offset += chunk.byteLength;
  }
  return body.buffer;
}

function requestHeaders(request: NextRequest, sessionToken: string): Headers {
  const headers = new Headers();
  for (const name of FORWARDED_REQUEST_HEADERS) {
    const value = request.headers.get(name);
    if (value) {
      headers.set(name, value);
    }
  }
  headers.set('authorization', `Bearer ${sessionToken}`);
  return headers;
}

function responseHeaders(upstream: Response): Headers {
  const headers = new Headers({
    'Cache-Control': 'no-store',
    Pragma: 'no-cache',
  });
  for (const name of FORWARDED_RESPONSE_HEADERS) {
    const value = upstream.headers.get(name);
    if (value) {
      headers.set(name, value);
    }
  }
  return headers;
}

export function jsonError(status: number, message: string): NextResponse {
  return NextResponse.json(
    { message },
    {
      status,
      headers: {
        'Cache-Control': 'no-store',
        Pragma: 'no-cache',
      },
    },
  );
}

function networkFailureResponse(): NextResponse {
  return jsonError(502, 'Gateway 요청을 완료할 수 없습니다.');
}

function mappedLoginFailureStatus(status: number): number {
  return [400, 401, 403, 409, 422, 429].includes(status) ? status : 502;
}

function parseLoginCredentials(body: ArrayBuffer | undefined): {
  username: string;
  password: string;
} {
  if (!body) {
    throw new BffRequestError(400, '아이디와 비밀번호를 모두 입력해 주세요.');
  }

  let candidate: unknown;
  try {
    candidate = JSON.parse(new TextDecoder().decode(body));
  } catch {
    throw new BffRequestError(400, '로그인 요청 형식이 올바르지 않습니다.');
  }

  if (!candidate || typeof candidate !== 'object') {
    throw new BffRequestError(400, '로그인 요청 형식이 올바르지 않습니다.');
  }

  const { username, password } = candidate as Record<string, unknown>;
  const normalizedUsername = typeof username === 'string' ? username.trim() : '';
  if (
    !normalizedUsername ||
    normalizedUsername.length > 100 ||
    typeof password !== 'string' ||
    !password.trim() ||
    password.length > 1024
  ) {
    throw new BffRequestError(400, '아이디와 비밀번호를 모두 입력해 주세요.');
  }
  return { username: normalizedUsername, password };
}

function parseUpstreamLoginResponse(candidate: unknown): UpstreamLoginResponse {
  if (!candidate || typeof candidate !== 'object') {
    throw new BffRequestError(502, 'Auth 로그인 응답 형식이 올바르지 않습니다.');
  }

  const response = candidate as Record<string, unknown>;
  if (
    !isValidSessionToken(typeof response.token === 'string' ? response.token : undefined) ||
    typeof response.tokenType !== 'string' ||
    response.tokenType.toLowerCase() !== 'bearer' ||
    typeof response.expiresIn !== 'number' ||
    !Number.isSafeInteger(response.expiresIn) ||
    response.expiresIn < 1 ||
    typeof response.username !== 'string' ||
    !response.username.trim() ||
    typeof response.departmentCode !== 'string' ||
    !Array.isArray(response.roles) ||
    !response.roles.every((role) => typeof role === 'string') ||
    typeof response.roleVersion !== 'number' ||
    !Number.isSafeInteger(response.roleVersion) ||
    response.roleVersion < 1
  ) {
    throw new BffRequestError(502, 'Auth 로그인 응답 형식이 올바르지 않습니다.');
  }

  return response as unknown as UpstreamLoginResponse;
}

export async function loginThroughGateway(request: NextRequest): Promise<NextResponse> {
  requireSameOrigin(request);
  if (!request.headers.get('content-type')?.toLowerCase().startsWith('application/json')) {
    throw new BffRequestError(415, 'JSON 로그인 요청만 허용됩니다.');
  }

  const credentials = parseLoginCredentials(await readBoundedBody(request, MAX_LOGIN_BODY_BYTES));
  let upstream: Response;
  try {
    upstream = await fetch(gatewayApiUrl(['auth', 'login']), {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ ...credentials, loginType: 'NORMAL' }),
      cache: 'no-store',
      redirect: 'manual',
      signal: AbortSignal.timeout(GATEWAY_TIMEOUT_MILLISECONDS),
    });
  } catch (error) {
    if (error instanceof BffRequestError) {
      throw error;
    }
    return networkFailureResponse();
  }

  if (!upstream.ok) {
    const response = jsonError(mappedLoginFailureStatus(upstream.status), '로그인에 실패했습니다.');
    clearSessionCookie(response);
    return response;
  }

  let login: UpstreamLoginResponse;
  try {
    login = parseUpstreamLoginResponse(await upstream.json());
  } catch (error) {
    if (error instanceof BffRequestError) {
      throw error;
    }
    throw new BffRequestError(502, 'Auth 로그인 응답 형식이 올바르지 않습니다.');
  }

  const session: AuthenticatedSession = {
    expiresIn: login.expiresIn,
    username: login.username,
    departmentCode: login.departmentCode,
    roles: login.roles,
    roleVersion: login.roleVersion,
  };
  const response = NextResponse.json(session, {
    headers: {
      'Cache-Control': 'no-store',
      Pragma: 'no-cache',
    },
  });
  setSessionCookie(response, login.token, login.expiresIn);
  return response;
}

export async function proxyAuthenticatedRequest(
  request: NextRequest,
  path: readonly string[],
): Promise<NextResponse> {
  if (!['GET', 'HEAD'].includes(request.method)) {
    requireSameOrigin(request);
  }

  const sessionToken = getSessionToken(request);
  if (!sessionToken) {
    const response = jsonError(401, '로그인이 필요합니다.');
    if (hasSessionCookie(request)) {
      clearSessionCookie(response);
    }
    return response;
  }

  let upstream: Response;
  try {
    upstream = await fetch(gatewayApiUrl(path, request.nextUrl.search), {
      method: request.method,
      headers: requestHeaders(request, sessionToken),
      body: await readBoundedBody(request, MAX_PROXY_BODY_BYTES),
      cache: 'no-store',
      redirect: 'manual',
      signal: AbortSignal.timeout(GATEWAY_TIMEOUT_MILLISECONDS),
    });
  } catch (error) {
    if (error instanceof BffRequestError) {
      throw error;
    }
    return networkFailureResponse();
  }

  if (upstream.status >= 300 && upstream.status < 400) {
    return jsonError(502, 'Gateway의 예상하지 못한 이동 응답을 차단했습니다.');
  }

  const body =
    request.method === 'HEAD' || [204, 304].includes(upstream.status) ? null : upstream.body;
  const response = new NextResponse(body, {
    status: upstream.status,
    headers: responseHeaders(upstream),
  });
  if (upstream.status === 401) {
    clearSessionCookie(response);
  }
  return response;
}

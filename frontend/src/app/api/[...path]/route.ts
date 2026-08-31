import { NextRequest } from 'next/server';
import { BffRequestError, jsonError, proxyAuthenticatedRequest } from '@/server/auth/bff';

export const dynamic = 'force-dynamic';
export const runtime = 'nodejs';

interface ApiRouteContext {
  params: Promise<{ path: string[] }>;
}

async function proxy(request: NextRequest, context: ApiRouteContext) {
  try {
    const { path } = await context.params;
    return await proxyAuthenticatedRequest(request, path);
  } catch (error) {
    if (error instanceof BffRequestError) {
      return jsonError(error.status, error.message);
    }
    return jsonError(500, 'API 요청 처리 중 오류가 발생했습니다.');
  }
}

export const GET = proxy;
export const POST = proxy;
export const PUT = proxy;
export const PATCH = proxy;
export const DELETE = proxy;
export const HEAD = proxy;

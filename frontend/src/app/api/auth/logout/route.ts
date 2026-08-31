import { NextRequest, NextResponse } from 'next/server';
import { BffRequestError, clearSessionCookie, jsonError, requireSameOrigin } from '@/server/auth/bff';

export const dynamic = 'force-dynamic';
export const runtime = 'nodejs';

export async function POST(request: NextRequest) {
  try {
    requireSameOrigin(request);
    const response = new NextResponse(null, {
      status: 204,
      headers: {
        'Cache-Control': 'no-store',
        Pragma: 'no-cache',
      },
    });
    clearSessionCookie(response);
    return response;
  } catch (error) {
    if (error instanceof BffRequestError) {
      return jsonError(error.status, error.message);
    }
    return jsonError(500, '로그아웃 처리 중 오류가 발생했습니다.');
  }
}

import { NextRequest } from 'next/server';
import { BffRequestError, jsonError, loginThroughGateway } from '@/server/auth/bff';

export const dynamic = 'force-dynamic';
export const runtime = 'nodejs';

export async function POST(request: NextRequest) {
  try {
    return await loginThroughGateway(request);
  } catch (error) {
    if (error instanceof BffRequestError) {
      return jsonError(error.status, error.message);
    }
    return jsonError(500, '로그인 처리 중 오류가 발생했습니다.');
  }
}

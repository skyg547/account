import { NextRequest, NextResponse } from 'next/server';
import { clearSessionCookie, getSessionToken, hasSessionCookie } from '@/server/auth/bff';

export const dynamic = 'force-dynamic';
export const runtime = 'nodejs';

export async function GET(request: NextRequest) {
  const authenticated = Boolean(getSessionToken(request));
  const response = NextResponse.json(
    { authenticated },
    {
      headers: {
        'Cache-Control': 'no-store',
        Pragma: 'no-cache',
      },
    },
  );
  if (!authenticated && hasSessionCookie(request)) {
    clearSessionCookie(response);
  }
  return response;
}

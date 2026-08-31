const LEGACY_AUTH_STORAGE_KEYS = ['auth_token', 'user_info'] as const;

/**
 * Remove only the pre-BFF browser session artifacts. The active JWT lives in an
 * HttpOnly cookie and is intentionally unavailable to this client module.
 */
export function clearLegacyBrowserSession(): void {
  if (typeof window === 'undefined') {
    return;
  }

  for (const key of LEGACY_AUTH_STORAGE_KEYS) {
    try {
      localStorage.removeItem(key);
    } catch {
      // Storage can be disabled. Cleanup is best-effort and never restores data.
    }
  }
}

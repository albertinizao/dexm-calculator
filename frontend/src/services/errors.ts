export type ApiErrorPayload = { timestamp?: string; status?: number; code?: string; message?: string; error?: string; detail?: string; requestId?: string };

export class ApiError extends Error {
  readonly status?: number;
  readonly code?: string;
  readonly requestId?: string;
  readonly method: string;
  readonly path: string;
  constructor(message: string, details: { status?: number; code?: string; requestId?: string; method: string; path: string } ) {
    super(message); this.name = 'ApiError'; Object.setPrototypeOf(this, new.target.prototype);
    this.status = details.status; this.code = details.code; this.requestId = details.requestId; this.method = details.method; this.path = details.path;
  }
}

export function safePath(input: string): string { try { return new URL(input, window.location.origin).pathname; } catch { return input.split('?')[0]; } }
export function logClientError(error: unknown, context: Record<string, unknown> = {}): void {
  const value = error instanceof Error ? error : new Error(String(error));
  const safeStack = value.stack?.split('\n').slice(1).join('\n');
  console.error(JSON.stringify({ timestamp: new Date().toISOString(), level: 'error', errorType: value.name, stack: safeStack, ...context }));
}

export async function apiErrorFromResponse(response: Response, requestId: string, method: string, originalPath: string): Promise<ApiError> {
  const body = await response.json().catch(() => ({})) as ApiErrorPayload || {};
  const errorRequestId = body.requestId || response.headers.get('X-Request-Id') || requestId;
  const detail = body.error || body.message || body.detail || response.statusText || 'La operación no se ha podido completar.';
  return new ApiError(`${response.status === 409 ? 'Conflicto: ' : ''}${detail} (ID de solicitud: ${errorRequestId})`, {
    status: response.status, code: body.code, requestId: errorRequestId, method, path: safePath(originalPath),
  });
}

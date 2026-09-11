// Only fixed categories are logged: upstream messages can contain private data.
const statuses = new Set([
  'INVALID_ARGUMENT', 'UNAUTHENTICATED', 'PERMISSION_DENIED', 'NOT_FOUND',
  'RESOURCE_EXHAUSTED', 'FAILED_PRECONDITION', 'INTERNAL', 'UNAVAILABLE',
  'DEADLINE_EXCEEDED', 'CANCELLED', 'UNKNOWN',
]);

export function upstreamCategory(body: unknown): string {
  const status = (body as { error?: { status?: unknown } } | null)?.error?.status;
  return typeof status === 'string' && statuses.has(status) ? status : 'UNKNOWN';
}

export function exceptionCategory(error: unknown): string {
  const name = error instanceof Error ? error.name : '';
  if (name === 'TimeoutError' || name === 'AbortError') return 'timeout';
  if (name === 'SyntaxError') return 'invalid_response';
  return 'request_failed';
}

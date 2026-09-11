import { describe, expect, it } from 'vitest';
import { exceptionCategory, upstreamCategory } from '../src/ai-diagnostics';

describe('Safe AI diagnostics', () => {
  it('keeps the upstream category without private error details', () => {
    expect(upstreamCategory({error: {status: 'PERMISSION_DENIED', message: 'secret key and journal text', details: ['private']}})).toBe('PERMISSION_DENIED');
  });
  it('rejects arbitrary categories and malformed responses', () => {
    for (const body of [null, {}, 'private text', {error: {status: 'secret'}}, {error: {status: 403}}]) {
      expect(upstreamCategory(body)).toBe('UNKNOWN');
    }
  });
  it('distinguishes timeouts and malformed responses without logging exception messages', () => {
    expect(exceptionCategory(new DOMException('secret', 'TimeoutError'))).toBe('timeout');
    expect(exceptionCategory(new SyntaxError('private response'))).toBe('invalid_response');
    expect(exceptionCategory(new Error('secret URL'))).toBe('request_failed');
  });
});

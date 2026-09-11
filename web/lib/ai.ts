import { auth } from './firebase';
export async function formatLog(
  rawText: string,
): Promise<{ text: string; status: string }> {
  const user = auth.currentUser;
  if (!user || !navigator.onLine)
    return { text: rawText, status: 'failed_other' };
  try {
    const token = await user.getIdToken();
    const response = await fetch(
      'https://onthejob-ai-proxy.elmntr.workers.dev',
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ rawText }),
        signal: AbortSignal.timeout(30000),
      },
    );
    if (!response.ok) throw new Error('Formatting unavailable');
    const data = (await response.json()) as {
      success?: boolean;
      formattedText?: string;
      reason?: string;
    };
    if (
      data.success &&
      typeof data.formattedText === 'string' &&
      data.formattedText.trim()
    )
      return { text: data.formattedText, status: 'done' };
    return {
      text: rawText,
      status:
        data.reason === 'quota_exhausted' ? 'failed_quota' : 'failed_other',
    };
  } catch {
    return { text: rawText, status: 'failed_other' };
  }
}

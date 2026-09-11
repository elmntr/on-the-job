/** Live attachments are authoritative; drafts only remember explicit removals. */
export function visiblePhotoUrls(
  currentUrls: string[],
  removedUrls: string[] = [],
): string[] {
  const removed = new Set(removedUrls);
  return [...new Set(currentUrls)].filter((url) => !removed.has(url));
}

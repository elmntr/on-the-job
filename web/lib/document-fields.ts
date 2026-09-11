/** Document IDs belong to Firestore paths, never to stored Android model fields. */
export function documentFields<T extends Record<string, unknown>>(
  value: T,
): Omit<T, 'id' | 'pending'> {
  return Object.fromEntries(
    Object.entries(value).filter(([key]) => key !== 'id' && key !== 'pending'),
  ) as Omit<T, 'id' | 'pending'>;
}

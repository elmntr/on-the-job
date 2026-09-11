export type Entry = {
  id: string;
  userId: string;
  rawText: string;
  text: string;
  imageUrls: string[];
  hours: number;
  formattingStatus: string;
  entryDate: string;
  ojtInstanceId: string;
  createdAt?: { toDate(): Date };
  pending?: boolean;
};
export type Placement = {
  id: string;
  name: string;
  hoursRequired: number;
  createdAt?: { toDate(): Date };
};
export const localDate = (date = new Date()) =>
  `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
export function entryDate(entry: Entry) {
  return /^\d{4}-\d{2}-\d{2}$/.test(entry.entryDate || '')
    ? entry.entryDate
    : entry.createdAt
      ? localDate(entry.createdAt.toDate())
      : '';
}
export function placementEntries(
  entries: Entry[],
  placementId: string,
  firstId: string,
) {
  return entries
    .filter(
      (e) =>
        e.ojtInstanceId === placementId ||
        (!e.ojtInstanceId && placementId === firstId),
    )
    .sort(
      (a, b) =>
        entryDate(b).localeCompare(entryDate(a)) ||
        (b.createdAt?.toDate().getTime() || 0) -
          (a.createdAt?.toDate().getTime() || 0),
    );
}
export function totalMinutes(entries: Entry[]) {
  return entries.reduce(
    (sum, e) => sum + Math.round((Number.isFinite(e.hours) ? e.hours : 0) * 60),
    0,
  );
}
export function hoursLabel(minutes: number) {
  return `${Math.floor(minutes / 60)}h${minutes % 60 ? ` ${minutes % 60}m` : ''}`;
}
export function validateLog(
  text: string,
  hours: number,
  minutes: number,
  date: string,
) {
  if (!text.trim()) return 'Write a few words about your day.';
  if (text.length > 20000)
    return 'Please keep your entry under 20,000 characters.';
  if (
    !Number.isInteger(hours) ||
    !Number.isInteger(minutes) ||
    hours < 0 ||
    minutes < 0 ||
    minutes > 59 ||
    hours * 60 + minutes > 1440 ||
    hours * 60 + minutes <= 0
  )
    return 'Enter a duration between 1 minute and 24 hours.';
  if (
    !/^\d{4}-\d{2}-\d{2}$/.test(date) ||
    localDate(new Date(`${date}T12:00:00`)) !== date ||
    date > localDate()
  )
    return 'Choose a valid date today or earlier.';
  return '';
}

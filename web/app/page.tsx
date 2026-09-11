'use client';
import { lazy, Suspense, useSyncExternalStore } from 'react';
const JournalApp = lazy(() => import('@/components/journal-app'));
const subscribe = () => () => {};
const clientSnapshot = () => true;
const serverSnapshot = () => false;
function OpeningJournal() {
  return (
    <main className="welcome">
      <div className="brand">OnTheJob</div>
      <output>Opening your journal…</output>
    </main>
  );
}
export default function Home() {
  // Keep Firebase's persistent browser cache out of prerendering, with matching hydration markup.
  const mounted = useSyncExternalStore(
    subscribe,
    clientSnapshot,
    serverSnapshot,
  );
  return mounted ? (
    <Suspense fallback={<OpeningJournal />}>
      <JournalApp />
    </Suspense>
  ) : (
    <OpeningJournal />
  );
}

'use client';
import { useEffect, useState } from 'react';
import { onAuthStateChanged, type User } from 'firebase/auth';
import { collection, onSnapshot } from 'firebase/firestore';
import { auth, db, friendlyError } from './firebase';
import type { Entry, Placement } from './journal';
import { repairDocumentIds } from './repair-document-ids';
export function useJournal() {
  const [user, setUser] = useState<User | null>(null),
    [ready, setReady] = useState(false),
    [entries, setEntries] = useState<Entry[]>([]),
    [placements, setPlacements] = useState<Placement[]>([]),
    [loaded, setLoaded] = useState(false),
    [error, setError] = useState(''),
    [cached, setCached] = useState(false);
  useEffect(
    () =>
      onAuthStateChanged(auth, (value) => {
        setEntries([]);
        setPlacements([]);
        setLoaded(false);
        setError('');
        setUser(value);
        setReady(true);
      }),
    [],
  );
  useEffect(() => {
    if (!user) return;
    const attemptedRepairs = new Set<string>();
    const repairFailed = () =>
      setError(
        'Could not sync the Android compatibility repair. Reconnect and refresh this page to retry.',
      );
    let entriesReady = false,
      placementsReady = false;
    const failed = (err: unknown) => {
      setError(friendlyError(err));
      setLoaded(false);
    };
    const a = onSnapshot(
      collection(db, 'users', user.uid, 'entries'),
      { includeMetadataChanges: true },
      (snapshot) => {
        void repairDocumentIds(snapshot, user.uid, attemptedRepairs).catch(
          repairFailed,
        );
        setEntries(
          snapshot.docs.map(
            (d) =>
              ({
                ...d.data({ serverTimestamps: 'estimate' }),
                id: d.id,
                pending: d.metadata.hasPendingWrites,
              }) as Entry,
          ),
        );
        setCached(snapshot.metadata.fromCache);
        entriesReady = true;
        setLoaded(entriesReady && placementsReady);
      },
      failed,
    );
    const b = onSnapshot(
      collection(db, 'users', user.uid, 'ojtInstances'),
      { includeMetadataChanges: true },
      (snapshot) => {
        void repairDocumentIds(snapshot, user.uid, attemptedRepairs).catch(
          repairFailed,
        );
        setPlacements(
          snapshot.docs
            .map(
              (d) =>
                ({
                  ...d.data({ serverTimestamps: 'estimate' }),
                  id: d.id,
                }) as Placement,
            )
            .sort(
              (a, b) =>
                (a.createdAt?.toDate().getTime() || 0) -
                  (b.createdAt?.toDate().getTime() || 0) ||
                a.id.localeCompare(b.id),
            ),
        );
        placementsReady = true;
        setLoaded(entriesReady && placementsReady);
      },
      failed,
    );
    return () => {
      a();
      b();
    };
  }, [user]);
  return { user, ready, entries, placements, loaded, error, cached };
}

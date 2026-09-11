'use client';
import { useCallback, useEffect, useRef, useState } from 'react';
import Image from 'next/image';
import { onAuthStateChanged, signOut } from 'firebase/auth';
import {
  collection,
  doc,
  serverTimestamp,
  setDoc,
  updateDoc,
} from 'firebase/firestore';
import {
  ArrowRight,
  BookOpen,
  CalendarDays,
  Check,
  ChevronLeft,
  ChevronRight,
  Clock,
  ImagePlus,
  List,
  LogOut,
  Plus,
  Settings2,
  WifiOff,
  X,
} from 'lucide-react';
import {
  Dialog,
  DialogContent,
  DialogTitle,
  DialogDescription,
} from '@/components/ui/dialog';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs';
import {
  Select,
  SelectTrigger,
  SelectValue,
  SelectContent,
  SelectItem,
} from '@/components/ui/select';
import { Switch } from '@/components/ui/switch';
import { Progress } from '@/components/ui/progress';
import { auth, db, friendlyError, login } from '@/lib/firebase';
import { documentFields } from '@/lib/document-fields';
import { visiblePhotoUrls } from '@/lib/photo-list';
import { photoPreviewUrl, safePhotoUrl } from '@/lib/photo-url';
import { PHOTO_ACCEPT, validateDecodablePhoto } from '@/lib/upload-policy';
import { useJournal } from '@/lib/use-journal';
import {
  entryDate,
  hoursLabel,
  localDate,
  placementEntries,
  totalMinutes,
  validateLog,
  type Entry,
  type Placement,
} from '@/lib/journal';
import {
  clearDraft,
  discardBlockedPhotos,
  photoJobs,
  queuePhotos,
  readDraft,
  saveDraft,
  uploadPhotos,
} from '@/lib/storage';
import { formatLog } from '@/lib/ai';

function SafePhoto(props: {src: string; width: number; height: number; alt: string; className?: string; unoptimized?: boolean}) {
  const url = photoPreviewUrl(props.src);
  return url ? <Image {...props} src={url} /> : <span>Photo unavailable</span>;
}

function dateLabel(value: string) {
  return value
    ? new Date(`${value}T12:00:00`).toLocaleDateString(undefined, {
        weekday: 'short',
        month: 'short',
        day: 'numeric',
        year: 'numeric',
      })
    : 'Date unavailable';
}
function statusLabel(entry: Entry) {
  if (entry.pending) return 'Waiting to sync';
  if (entry.formattingStatus === 'done') return 'AI formatted';
  if (entry.formattingStatus === 'skipped') return 'Raw entry';
  return 'Saved · AI available to retry';
}

export default function JournalApp() {
  const { user, ready, entries, placements, loaded, error, cached } =
    useJournal();
  const [notice, setNotice] = useState(''),
    [online, setOnline] = useState(true),
    [signingIn, setSigningIn] = useState(false),
    [activeId, setActiveId] = useState(''),
    [editor, setEditor] = useState<Entry | 'new' | null>(null),
    [placementDialog, setPlacementDialog] = useState<'new' | 'edit' | null>(
      null,
    ),
    [photo, setPhoto] = useState<string | null>(null),
    [pendingPhotos, setPendingPhotos] = useState(0),
    [blockedPhotos, setBlockedPhotos] = useState(0),
    [uploadError, setUploadError] = useState(false),
    [month, setMonth] = useState(
      () => new Date(new Date().getFullYear(), new Date().getMonth(), 1),
    ),
    [day, setDay] = useState(localDate()),
    [install, setInstall] = useState(false);
  const active = placements.find((p) => p.id === activeId) || placements[0];
  const firstId = placements[0]?.id || '';
  const visible = placementEntries(entries, active?.id || '', firstId);
  const total = totalMinutes(visible),
    goal = Math.round((active?.hoursRequired || 486) * 60),
    progress = Math.min(100, Math.round((total / goal) * 100));
  useEffect(() => {
    const change = () => setOnline(navigator.onLine);
    change();
    window.addEventListener('online', change);
    window.addEventListener('offline', change);
    if ('serviceWorker' in navigator && process.env.NODE_ENV === 'production') {
      navigator.serviceWorker
        .register('/sw.js')
        .then(() => navigator.serviceWorker.ready)
        .then((registration) => {
          // Resources loaded before the worker took control also belong in the offline shell.
          const urls = performance
            .getEntriesByType('resource')
            .map((resource) => resource.name);
          registration.active?.postMessage({ type: 'CACHE_SHELL', urls });
        })
        .catch(() => {});
    }
    return () => {
      window.removeEventListener('online', change);
      window.removeEventListener('offline', change);
    };
  }, []);
  useEffect(
    () =>
      onAuthStateChanged(auth, () => {
        setActiveId('');
        setEditor(null);
        setNotice('');
        setPhoto(null);
        setPendingPhotos(0);
        setBlockedPhotos(0);
      }),
    [],
  );
  const retryPhotos = useCallback(async () => {
    if (!user) return;
    const uid = user.uid;
    try {
      await uploadPhotos(uid, () => auth.currentUser?.uid === uid);
      setUploadError(false);
    } catch {
      setUploadError(true);
    } finally {
      if (auth.currentUser?.uid === uid) {
        const jobs = await photoJobs(uid).catch(() => []);
        if (auth.currentUser?.uid === uid) {
        setPendingPhotos(jobs.filter(job => !job.blocked).length);
        setBlockedPhotos(jobs.filter(job => job.blocked).length);
        }
      }
    }
  }, [user]);
  useEffect(() => {
    if (!user) return;
    queueMicrotask(() => void retryPhotos());
    const interval = setInterval(() => {
      if (document.visibilityState === 'visible') void retryPhotos();
    }, 30000);
    return () => clearInterval(interval);
  }, [user, online, retryPhotos]);
  async function doLogin() {
    setSigningIn(true);
    setNotice('');
    try {
      await login();
    } catch (e) {
      setNotice(friendlyError(e));
    } finally {
      setSigningIn(false);
    }
  }
  if (!user)
    return (
      <main className="welcome">
        <div className="brand">
          <BookOpen size={28} />
          <span>OnTheJob</span>
        </div>
        <section className="signin">
          <span className="stamp">YOUR OJT JOURNAL</span>
          <h1>
            Every day.
            <br />
            One step closer.
          </h1>
          <p>Keep your daily logs, photos, and training hours together.</p>
          <button
            className="primary"
            disabled={!ready || signingIn}
            onClick={doLogin}
          >
            {!ready
              ? 'Opening your journal…'
              : signingIn
                ? 'Signing in…'
                : 'Continue with Google'}
            <ArrowRight size={20} />
          </button>
          {notice && <p role="alert">{notice}</p>}
          <small>
            Already using OnTheJob? Use the same Google account to access your
            logs.
          </small>
        </section>
        <footer>ON THE JOB · DAILY LOG & HOURS TRACKER</footer>
      </main>
    );
  return (
    <div className="app-shell">
      <header className="app-header">
        <div className="brand">
          <BookOpen />
          <span>OnTheJob</span>
        </div>
        <div className="account">
          <span>{user.displayName?.split(' ')[0] || 'Your journal'}</span>
          <button
            className="icon-button"
            aria-label="Sign out"
            onClick={() =>
              signOut(auth).catch((e) => setNotice(friendlyError(e)))
            }
          >
            <LogOut size={20} />
          </button>
        </div>
      </header>
      <main className="journal">
        <div className="placement-bar">
          <div>
            <span className="eyebrow">MY PLACEMENT</span>
            {active ? (
              <Select
                value={active.id}
                onValueChange={(value) => {
                  if (value) setActiveId(value);
                }}
              >
                <SelectTrigger className="placement-select">
                  <SelectValue>{active.name}</SelectValue>
                </SelectTrigger>
                <SelectContent alignItemWithTrigger={false} align="start">
                  {placements.map((p) => (
                    <SelectItem key={p.id} value={p.id}>
                      {p.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            ) : (
              <h2>Your OJT journey</h2>
            )}
          </div>
          <div className="actions">
            {active && (
              <button
                className="icon-button"
                aria-label="Edit placement and hours goal"
                onClick={() => setPlacementDialog('edit')}
              >
                <Settings2 size={20} />
              </button>
            )}
            <button
              className="secondary"
              disabled={!loaded || !online}
              onClick={() => setPlacementDialog('new')}
            >
              <Plus size={18} />
              Placement
            </button>
          </div>
        </div>
        {!online && (
          <div className="banner">
            <WifiOff size={18} />
            You’re offline. Saved logs remain here; changes sync when you
            reconnect.
          </div>
        )}
        {error && (
          <div className="banner error" role="alert">
            {error}
          </div>
        )}
        {notice && (
          <output className="banner">
            {notice}
            <button
              className="icon-button"
              aria-label="Dismiss message"
              onClick={() => setNotice('')}
            >
              <X size={18} />
            </button>
          </output>
        )}
        {blockedPhotos > 0 && <div className="banner">{blockedPhotos} queued photo(s) cannot upload. Only valid JPEG, PNG, and WebP photos up to 10 MB are accepted. Your saved logs are unchanged.
          <button className="text-button" onClick={async () => {await discardBlockedPhotos(user.uid); await retryPhotos();}}>Remove blocked uploads from this device</button>
        </div>}
        {pendingPhotos > 0 && (
          <div className="banner">
            {pendingPhotos} photo{pendingPhotos === 1 ? '' : 's'} waiting to
            upload.{' '}
            {uploadError
              ? 'Check your connection and retry.'
              : 'Keep the app open until uploads finish.'}
            <button
              className="text-button"
              disabled={!online}
              onClick={() => void retryPhotos()}
            >
              Retry
            </button>
          </div>
        )}
        <section className="progress-card" aria-label="Training progress">
          <div className="progress-heading">
            <span className="eyebrow">HOURS CLOCKED</span>
            <span className="stamp">
              {progress >= 100 ? 'GOAL REACHED' : 'IN PROGRESS'}
            </span>
          </div>
          <div className="hours-total">
            {hoursLabel(total)} <span>/ {hoursLabel(goal)}</span>
          </div>
          <Progress value={progress} aria-label="Completed training hours" />
          <div className="progress-bottom">
            <span>{hoursLabel(Math.max(0, goal - total))} to go</span>
            <span>{progress}% complete</span>
          </div>
        </section>
        <div className="section-heading">
          <div>
            <span className="eyebrow">ONE DAY AT A TIME</span>
            <h2>Your daily record</h2>
          </div>
          <button
            className="primary"
            disabled={!loaded || !active}
            onClick={() => setEditor('new')}
          >
            <Plus size={20} />
            New entry
          </button>
        </div>
        {!loaded ? (
          <output>
            {error ? 'Your logs could not be loaded.' : 'Loading your journal…'}
          </output>
        ) : !active ? (
          <section className="empty">
            <BookOpen size={36} />
            <h3>Start your first placement</h3>
            <p>Give your placement a name and set your required hours.</p>
            <button
              className="primary"
              disabled={!online}
              onClick={() => setPlacementDialog('new')}
            >
              Create placement
            </button>
          </section>
        ) : (
          <Tabs defaultValue="feed">
            <TabsList className="journal-tabs">
              <TabsTrigger value="feed">
                <List size={18} />
                Log feed
              </TabsTrigger>
              <TabsTrigger value="calendar">
                <CalendarDays size={18} />
                Calendar
              </TabsTrigger>
            </TabsList>
            <TabsContent value="feed">
              <EntryList entries={visible} onOpen={setEditor} />
            </TabsContent>
            <TabsContent value="calendar">
              <section className="calendar">
                <div className="month-nav">
                  <button
                    className="icon-button"
                    aria-label="Previous month"
                    onClick={() =>
                      setMonth(
                        new Date(month.getFullYear(), month.getMonth() - 1, 1),
                      )
                    }
                  >
                    <ChevronLeft />
                  </button>
                  <h3>
                    {month.toLocaleDateString(undefined, {
                      month: 'long',
                      year: 'numeric',
                    })}
                  </h3>
                  <button
                    className="icon-button"
                    aria-label="Next month"
                    onClick={() =>
                      setMonth(
                        new Date(month.getFullYear(), month.getMonth() + 1, 1),
                      )
                    }
                  >
                    <ChevronRight />
                  </button>
                </div>
                <div className="calendar-grid">
                  {['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].map(
                    (d) => (
                      <span className="weekday" key={d}>
                        {d}
                      </span>
                    ),
                  )}
                  {Array.from({ length: month.getDay() }, (_, i) => (
                    <span key={`space-${i}`} />
                  ))}
                  {Array.from(
                    {
                      length: new Date(
                        month.getFullYear(),
                        month.getMonth() + 1,
                        0,
                      ).getDate(),
                    },
                    (_, i) => {
                      const date = localDate(
                          new Date(
                            month.getFullYear(),
                            month.getMonth(),
                            i + 1,
                          ),
                        ),
                        logs = visible.filter((e) => entryDate(e) === date);
                      return (
                        <button
                          key={date}
                          aria-label={`${dateLabel(date)}, ${logs.length} entries`}
                          aria-pressed={day === date}
                          className={`day ${day === date ? 'selected' : ''} ${date === localDate() ? 'today' : ''}`}
                          onClick={() => setDay(date)}
                        >
                          {i + 1}
                          {logs.length > 0 && <span className="day-dot" />}
                        </button>
                      );
                    },
                  )}
                </div>
              </section>
              <h3 className="day-heading">{dateLabel(day)}</h3>
              <EntryList
                entries={visible.filter((e) => entryDate(e) === day)}
                onOpen={setEditor}
              />
            </TabsContent>
          </Tabs>
        )}
        <footer className="journal-footer">
          <span>
            {cached ? 'Showing device copy' : 'Your journal is up to date'}
          </span>
          <button className="text-button" onClick={() => setInstall(true)}>
            Add to Home Screen
          </button>
        </footer>
      </main>
      {editor && active && (
        <EntryEditor
          key={typeof editor === 'string' ? 'new' : editor.id}
          entry={
            editor === 'new'
              ? null
              : entries.find((e) => e.id === editor.id) || editor
          }
          uid={user.uid}
          placement={active}
          onClose={() => setEditor(null)}
          onNotice={setNotice}
          onPhotos={() => void retryPhotos()}
          onPhoto={setPhoto}
        />
      )}
      {placementDialog && (
        <PlacementEditor
          uid={user.uid}
          placement={placementDialog === 'edit' ? active : undefined}
          onClose={() => setPlacementDialog(null)}
          onSaved={setActiveId}
          onNotice={setNotice}
        />
      )}
      <Dialog
        open={!!photo}
        onOpenChange={(open) => {
          if (!open) setPhoto(null);
        }}
      >
        <DialogContent className="photo-dialog">
          <DialogTitle>Journal photo</DialogTitle>
          <DialogDescription>
            Open the original to save it to your device.
          </DialogDescription>
          {photo && (
            <>
              <SafePhoto
                width={1200}
                height={900}
                unoptimized
                className="full-photo"
                src={photo}
                alt="OJT journal attachment"
              />
              <a
                className="secondary"
                href={safePhotoUrl(photo) || undefined}
                target="_blank"
                rel="noopener noreferrer"
              >
                Open original photo <ArrowRight size={18} />
              </a>
            </>
          )}
        </DialogContent>
      </Dialog>
      <Dialog open={install} onOpenChange={setInstall}>
        <DialogContent>
          <DialogTitle>Add OnTheJob to your Home Screen</DialogTitle>
          <DialogDescription>
            On iPhone or iPad, open this page in Safari, tap Share, then Add to
            Home Screen. On other devices, look for Install app in your browser
            menu.
          </DialogDescription>
        </DialogContent>
      </Dialog>
    </div>
  );
}
function EntryList({
  entries,
  onOpen,
}: {
  entries: Entry[];
  onOpen: (entry: Entry) => void;
}) {
  return entries.length ? (
    <div className="entry-list">
      {entries.map((entry) => (
        <button
          key={entry.id}
          className="entry-card"
          onClick={() => onOpen(entry)}
        >
          <div className="entry-top">
            <span className="entry-date">{dateLabel(entryDate(entry))}</span>
            <span className="duration">
              <Clock size={15} />
              {hoursLabel(Math.round(entry.hours * 60))}
            </span>
          </div>
          <p>{entry.text || entry.rawText}</p>
          <div className="entry-bottom">
            <span className="status-chip">{statusLabel(entry)}</span>
            {entry.imageUrls?.length > 0 && (
              <span>
                {entry.imageUrls.length} photo
                {entry.imageUrls.length === 1 ? '' : 's'}
              </span>
            )}
            <ArrowRight size={18} />
          </div>
        </button>
      ))}
    </div>
  ) : (
    <section className="empty">
      <BookOpen size={32} />
      <h3>A fresh page</h3>
      <p>
        Your daily logs will appear here. Record what you worked on and the
        hours you completed.
      </p>
    </section>
  );
}

type Draft = {
  id: string;
  text: string;
  rawText: string;
  date: string;
  hours: string;
  minutes: string;
  ai: boolean;
  imageUrls: string[];
  removedImageUrls?: string[];
  status: string;
  files: File[];
};
function EntryEditor({
  entry,
  uid,
  placement,
  onClose,
  onNotice,
  onPhotos,
  onPhoto,
}: {
  entry: Entry | null;
  uid: string;
  placement: Placement;
  onClose: () => void;
  onNotice: (s: string) => void;
  onPhotos: () => void;
  onPhoto: (s: string) => void;
}) {
  const initial: Draft = {
    id: entry?.id || doc(collection(db, 'users', uid, 'entries')).id,
    text: entry?.text || entry?.rawText || '',
    rawText: entry?.rawText || '',
    date: entry ? entryDate(entry) : localDate(),
    hours: String(Math.floor(Math.round((entry?.hours || 0) * 60) / 60)),
    minutes: String(Math.round((entry?.hours || 0) * 60) % 60),
    ai: true,
    imageUrls: entry?.imageUrls || [],
    status: entry?.formattingStatus || 'skipped',
    files: [],
  };
  const [draft, setDraft] = useState<Draft>(initial),
    [hydrated, setHydrated] = useState(false),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(''),
    [message, setMessage] = useState(''),
    [checkingPhotos, setCheckingPhotos] = useState(false);
  const key = `${uid}:${placement.id}:${entry?.id || 'new'}`,
    busyRef = useRef(false),
    storageChain = useRef(Promise.resolve());
  useEffect(() => {
    let alive = true;
    readDraft(key)
      .then((value) => {
        if (alive && value) {
          setDraft(value);
          setMessage('Recovered your unsaved draft.');
        }
      })
      .catch(() => {
        if (alive)
          setError(
            'Draft storage is unavailable. Keep this page open until your log is synced.',
          );
      })
      .finally(() => {
        if (alive) setHydrated(true);
      });
    return () => {
      alive = false;
    };
  }, [key]);
  useEffect(() => {
    if (!hydrated || busyRef.current) return;
    storageChain.current = storageChain.current
      .then(() => saveDraft(key, draft))
      .then(() => {})
      .catch(() =>
        setError(
          'Could not save a local draft. Keep this page open until your log is synced.',
        ),
      );
  }, [draft, hydrated, key, busy]);
  const remotePhotos = visiblePhotoUrls(
    entry?.imageUrls || draft.imageUrls,
    draft.removedImageUrls,
  );
  function change(patch: Partial<Draft>) {
    setDraft((d) => ({ ...d, ...patch }));
  }
  async function regenerate() {
    setBusy(true);
    busyRef.current = true;
    const result = await formatLog(draft.text);
    if (result.status === 'done') {
      change({ text: result.text, status: result.status });
      setMessage('AI formatting applied. Review it, then save.');
    } else setError('AI is unavailable right now. Your writing is unchanged.');
    busyRef.current = false;
    setBusy(false);
  }
  async function save() {
    const validation = validateLog(
      draft.text,
      Number(draft.hours),
      Number(draft.minutes),
      draft.date,
    );
    if (validation) {
      setError(validation);
      return;
    }
    if (auth.currentUser?.uid !== uid) return;
    setBusy(true);
    busyRef.current = true;
    setError('');
    try {
      await storageChain.current;
      await saveDraft(key, draft);
      let result = {
        text: draft.text,
        status: entry ? draft.status : 'skipped',
      };
      if (!entry && draft.ai) result = await formatLog(draft.text);
      if (auth.currentUser?.uid !== uid) return;
      const ref = doc(db, 'users', uid, 'entries', draft.id);
      await queuePhotos(
        draft.files.map((file, i) => ({
          id: `${uid}:${draft.id}:${i}:${file.name}:${file.size}:${file.lastModified}`,
          uid,
          entryId: draft.id,
          file,
        })),
      );
      const fields = {
        text: result.text,
        hours: (Number(draft.hours) * 60 + Number(draft.minutes)) / 60,
        formattingStatus: result.status,
      };
      // Append/remove individual URLs so a concurrent Android upload is never replaced.
      const { arrayRemove } = await import('firebase/firestore');
      const removed = draft.removedImageUrls || [];
      const write = entry
        ? updateDoc(ref, {
            ...fields,
            ...(removed.length ? { imageUrls: arrayRemove(...removed) } : {}),
          })
        : setDoc(
            ref,
            documentFields({
              ...fields,
              userId: uid,
              rawText: draft.text,
              entryDate: draft.date,
              ojtInstanceId: placement.id,
              imageUrls: [],
              createdAt: serverTimestamp(),
            }),
          );
      let acknowledged = false;
      const completion = write
        .then(async () => {
          acknowledged = true;
          await clearDraft(key);
          if (auth.currentUser?.uid === uid) {
            onNotice(
              result.status.startsWith('failed')
                ? 'Log synced. AI was unavailable; your original writing is saved.'
                : 'Log synced.',
            );
            onPhotos();
          }
        })
        .catch((e) => {
          if (auth.currentUser?.uid === uid)
            onNotice(
              `${friendlyError(e)} Reopen this entry form to recover your draft.`,
            );
          throw e;
        });
      await Promise.race([
        completion,
        new Promise<void>((resolve) => setTimeout(resolve, 1500)),
      ]);
      if (!acknowledged)
        onNotice(
          'Saved on this device, waiting for confirmation. Your draft is kept until the log syncs.',
        );
      onClose();
    } catch (e) {
      setError(friendlyError(e));
    } finally {
      busyRef.current = false;
      setBusy(false);
    }
  }
  return (
    <Dialog
      open
      onOpenChange={(open) => {
        if (!open && !busy) onClose();
      }}
    >
      <DialogContent className="editor-dialog" showCloseButton={!busy}>
        <DialogTitle>
          {entry ? 'Your daily entry' : 'Record your day'}
        </DialogTitle>
        <DialogDescription>
          {entry
            ? dateLabel(entryDate(entry))
            : `${placement.name} · Every hour counts.`}
        </DialogDescription>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            void save();
          }}
        >
          <fieldset disabled={busy || !hydrated || checkingPhotos}>
            {!entry && (
              <label>
                Date
                <input
                  type="date"
                  value={draft.date}
                  max={localDate()}
                  onChange={(e) => change({ date: e.target.value })}
                  required
                />
              </label>
            )}
            <div className="duration-fields">
              <label>
                Hours
                <input
                  type="number"
                  inputMode="numeric"
                  min="0"
                  max="24"
                  step="1"
                  value={draft.hours}
                  onChange={(e) => change({ hours: e.target.value })}
                  required
                />
              </label>
              <label>
                Minutes
                <input
                  type="number"
                  inputMode="numeric"
                  min="0"
                  max="59"
                  step="1"
                  value={draft.minutes}
                  onChange={(e) => change({ minutes: e.target.value })}
                  required
                />
              </label>
            </div>
            <label>
              {entry ? 'Journal entry' : 'What did you work on?'}
              <textarea
                rows={7}
                maxLength={20000}
                placeholder="Tasks completed, things learned, and moments worth remembering…"
                value={draft.text}
                onChange={(e) => change({ text: e.target.value })}
                required
              />
            </label>
            {!entry ? (
              <div className="toggle-row">
                <label htmlFor="ai-toggle">
                  Format with AI
                  <small>Your original notes are always kept.</small>
                </label>
                <Switch
                  id="ai-toggle"
                  checked={draft.ai}
                  onCheckedChange={(ai) => change({ ai })}
                />
              </div>
            ) : (
              <button
                className="secondary"
                type="button"
                disabled={!draft.text.trim() || !navigator.onLine}
                onClick={() => void regenerate()}
              >
                Regenerate with AI
              </button>
            )}
            {entry?.rawText && (
              <details>
                <summary>Original notes</summary>
                <p className="original-notes">{entry.rawText}</p>
              </details>
            )}
            <div className="photo-grid">
              {remotePhotos.map((url) => (
                <div key={url}>
                  <button
                    type="button"
                    className="photo-thumb"
                    onClick={() => onPhoto(url)}
                  >
                    <SafePhoto
                      width={90}
                      height={90}
                      unoptimized
                      src={url}
                      alt="View attachment"
                    />
                  </button>
                  <button
                    type="button"
                    className="text-button"
                    onClick={() =>
                      change({
                        removedImageUrls: [
                          ...(draft.removedImageUrls || []),
                          url,
                        ],
                      })
                    }
                  >
                    Remove
                  </button>
                </div>
              ))}
            </div>
            <label className="upload-label">
              <ImagePlus size={20} />
              Add photos · JPEG, PNG, WebP
              <input
                type="file"
                accept={PHOTO_ACCEPT}
                disabled={checkingPhotos}
                multiple
                onChange={async (e) => {
                  const files = Array.from(e.target.files || []);
                  e.target.value = '';
                  if (files.some((f) => f.size > 10 * 1024 * 1024)) {
                    setError('Choose photos smaller than 10 MB each.');
                    return;
                  }
                  if (
                    draft.files.length + files.length + remotePhotos.length >
                    10
                  ) {
                    setError('Attach up to 10 photos per entry.');
                    return;
                  }
                  setCheckingPhotos(true);
                  try {
                    await Promise.all(files.map(validateDecodablePhoto));
                    setDraft(current => ({...current, files: [...current.files, ...files]}));
                    setError('');
                  } catch (error) {setError(error instanceof Error ? error.message : 'Could not validate these photos.');}
                  finally {setCheckingPhotos(false);}
                }}
              />
            </label>
            {draft.files.map((file, i) => (
              <div className="selected-file" key={`${file.name}-${i}`}>
                <span>{file.name}</span>
                <button
                  type="button"
                  className="icon-button"
                  aria-label={`Remove ${file.name}`}
                  onClick={() =>
                    change({ files: draft.files.filter((_, j) => j !== i) })
                  }
                >
                  <X size={16} />
                </button>
              </div>
            ))}
            {error && (
              <p className="form-error" role="alert">
                {error}
              </p>
            )}
            {message && <output>{message}</output>}
            <div className="form-actions">
              <button type="button" className="secondary" onClick={onClose}>
                Close
              </button>
              <button className="primary" type="submit">
                <Check size={20} />
                {busy ? 'Saving…' : entry ? 'Save changes' : 'Save entry'}
              </button>
            </div>
          </fieldset>
        </form>
      </DialogContent>
    </Dialog>
  );
}
function PlacementEditor({
  uid,
  placement,
  onClose,
  onSaved,
  onNotice,
}: {
  uid: string;
  placement?: Placement;
  onClose: () => void;
  onSaved: (id: string) => void;
  onNotice: (message: string) => void;
}) {
  const [name, setName] = useState(placement?.name || ''),
    [hours, setHours] = useState(String(placement?.hoursRequired || 486)),
    [busy, setBusy] = useState(false),
    [error, setError] = useState('');
  async function submit() {
    if (
      !name.trim() ||
      !Number.isFinite(Number(hours)) ||
      Number(hours) <= 0 ||
      Number(hours) > 10000
    ) {
      setError('Enter a name and an hours goal between 1 and 10,000.');
      return;
    }
    if (!navigator.onLine) {
      setError('Reconnect to save your placement.');
      return;
    }
    setBusy(true);
    try {
      const ref = placement
        ? doc(db, 'users', uid, 'ojtInstances', placement.id)
        : doc(collection(db, 'users', uid, 'ojtInstances'));
      if (placement)
        await updateDoc(ref, {
          name: name.trim(),
          hoursRequired: Number(hours),
        });
      else
        await setDoc(
          ref,
          documentFields({
            name: name.trim(),
            hoursRequired: Number(hours),
            createdAt: serverTimestamp(),
          }),
        );
      onSaved(ref.id);
      onNotice('Placement saved.');
      onClose();
    } catch (e) {
      setError(friendlyError(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <Dialog
      open
      onOpenChange={(open) => {
        if (!open && !busy) onClose();
      }}
    >
      <DialogContent showCloseButton={!busy}>
        <DialogTitle>
          {placement ? 'Edit placement' : 'New placement'}
        </DialogTitle>
        <DialogDescription>
          Your company, training assignment, or academic term.
        </DialogDescription>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            void submit();
          }}
        >
          <fieldset disabled={busy}>
            <label>
              Placement name
              <input
                maxLength={100}
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. OJT 1 — Company name"
                required
              />
            </label>
            <label>
              Required hours
              <input
                type="number"
                min="1"
                max="10000"
                step="0.25"
                value={hours}
                onChange={(e) => setHours(e.target.value)}
                required
              />
            </label>
            {error && <p role="alert">{error}</p>}
            <button className="primary" type="submit">
              {busy ? 'Saving…' : 'Save placement'}
            </button>
          </fieldset>
        </form>
      </DialogContent>
    </Dialog>
  );
}

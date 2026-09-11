# OnTheJob web

A mobile-first browser client for the existing Android app. No Android source changes or database migration are required. Firebase Authentication and Firestore remain authoritative; Cloudinary stores photos and the existing Cloudflare Worker handles AI formatting.

## Develop and check

Node 22.13+ required. From this directory:

```sh
npm ci
npm run dev
npm run typecheck
npm test
npm run lint:app
npm run build
```

The generated Shadcn catalog has existing lint findings; `lint:app` checks the authored app separately. Production static files are emitted to `dist/client/`. Development does not register the service worker.

## Deploy for students with Firebase Hosting

The hosting configuration targets the existing `on-the-job-19c0f` project, at `https://on-the-job-19c0f.web.app`. Verify both Firebase default domains are authorized for Google sign-in before release. Hosting deploys only static files, not Firestore rules or Android releases.

Run these commands from `web/`. The included `firebase.json` and `.firebaserc` are already configured; skip `firebase init`. Use Firebase Spark and the free default domain, and verify the shared backend services also remain on free plans. Quota exhaustion can interrupt service; free hosting does not mean unlimited usage. See [Hosting quotas](https://firebase.google.com/docs/hosting/usage-quotas-pricing).

```sh
npx firebase-tools login
npm run build
npx firebase-tools deploy --only hosting --project on-the-job-19c0f
```

In Firebase Console, verify that the deployed Firestore rules allow signed-in owners to access both `users/{uid}/entries/{entryId}` and `users/{uid}/ojtInstances/{id}`. Rules documented in the parent DECISIONS.md are not proof of live rules. Do not deploy replacement rules without reviewing the existing rules. The Firebase client key is public; access is enforced by the database rules. If API-key restrictions reject browser traffic, register a Firebase Web app and configure its browser key via `NEXT_PUBLIC_FIREBASE_API_KEY` before building. Keep the same Firebase project.

For a custom domain, add it in Hosting and in Authentication → Settings → Authorized domains. Google sign-in uses a user-initiated popup to avoid Safari's cross-origin redirect-storage issue. Test it on real iPhone Safari and the installed Home Screen app.

## Maintain the AI Worker

The parent Worker now includes CORS headers on every response, including failures. Its endpoint and token verification are unchanged.

```sh
cd ../onthejob-ai-proxy
npx wrangler login
npm test -- --run
npm run deploy
```

The session handover records this compatibility fix as deployed. Verify the existing endpoint before redeploying; these commands are for Worker changes or a separate installation. A log still saves with the original writing when AI is unavailable and can be regenerated later.

## Data compatibility and offline behavior

- Uses the same Google account UID, collections, field names and formatting statuses as Android.
- Explicit `entryDate` strings preserve backdated local work days. Legacy timestamps fall back to the device timezone. Hours are calculated as integer minutes in the UI and stored as decimal hours for Android compatibility.
- Legacy entries without a placement are shown only under the oldest placement. The web app does not move entries between placements; new users explicitly create their first placement. On authenticated server snapshots it removes only the redundant top-level `id` field introduced by the initial web release, to restore compatibility with older Android versions. Document paths and log contents stay unchanged. Android's existing automatic migration still has race/interruption edge cases; test simultaneous first use separately.
- Firestore persistent cache handles offline reads and writes. Entries show pending versus synced status. IndexedDB keeps drafts and selected photos; browser storage can still be cleared or evicted, so a local save is not a backup.
- Photo uploads resume while the app is open, online, and signed into the owning account. Uploaded URLs are retained before the Firestore append, so a retry does not normally upload the file twice. URL appends/removals are atomic to preserve concurrent Android photo uploads.
- Text/hour edits use Firestore's last-write-wins behavior. Preferences remain device-local. Sign-out hides account data but retains account-scoped offline drafts and cache on the device; use a personal device for offline work.
- Offline reopening requires one successful online visit and completed app-shell caching. iOS background upload completion is not promised.
- AI keys remain on the Worker; no service-account credentials are included in the web build.

## Release checks needing real accounts/devices

Sign in on iPhone, create a placement/log, verify Android sees the same data, edit on Android and verify web updates, test timezone/backdating and multiple placements, test supported JPEG/PNG/WebP uploads, camera output, and clear rejection of new HEIC attachments, interrupt/retry uploads, save offline then reconnect, reopen the Home Screen app offline, and verify another account cannot access the first account's entries. These signed-in/device checks have not been run automatically.

A private Sites review deployment is separate from the student-facing Firebase Hosting deployment. It requires adding its domain to Firebase authorized domains if you want to sign in on that preview.

## Android document ID compatibility

Firestore document IDs are path metadata, not stored model fields. Web writes pass through `documentFields` to exclude `id` and local sync metadata. The Android repositories decode records using the snapshot path ID and tolerate records containing the legacy field, including offline cached records. Regression tests cover both entries and placements. Rebuild/install the Android update to handle cached affected records immediately.

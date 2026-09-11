# OnTheJob — session handover

Updated: 11 September 2026 (Asia/Manila).

## Start here

This is an Android app for students undergoing OJT, with a web version added so iPhone/iPad users can use the same journal. Preserve the Android app and shared data compatibility. The most recent work—security hardening and responsive web layouts—is implemented and deployed. The user is starting a new session, not requesting another redesign.

Workspace: `/home/justin/AndroidStudioProjects/OnTheJob`.

Read this document, inspect current files and Git status, then follow the user's next request. Do not assume temporary files, browser handles, credentials, local servers, or tool sessions survived. Do not rerun migrations, replace the app, or redeploy just because a new session started.

## Application structure

- `app/`: Android Kotlin/Jetpack Compose application, package `com.example.onthejob`.
- `web/`: React 19.3, Vinext beta.9, Vite 8.3, Base UI/shadcn, Firebase client SDK, IndexedDB drafts and upload queue, offline shell service worker.
- `onthejob-ai-proxy/`: Cloudflare Worker that verifies Firebase tokens and calls Gemini.
- Shared Firebase project: `on-the-job-19c0f`.
- Shared Cloudinary cloud: `dskoyv2oe`; unsigned preset: `onthejob_unsigned`.
- AI endpoint: `https://onthejob-ai-proxy.elmntr.workers.dev`.

Web functionality includes Google sign-in, placements and required hours, entry editing, daily hours, calendar/feed views, AI rewriting, photos, offline drafts/sync, and Home Screen installation guidance. The visual style follows the Android app's existing green palette and bundled fonts.

Key web files:

- `components/journal-app.tsx`: main UI and forms.
- `app/globals.css`: styling and responsive rules.
- `lib/firebase.ts`, `lib/use-journal.ts`: real authentication and Firestore subscriptions.
- `lib/journal.ts`: entry/placement types, date and duration validation.
- `lib/storage.ts`: IndexedDB drafts and queued photo uploads.
- `lib/upload-policy.ts`: allowed formats, size, signatures and decoding checks.
- `lib/photo-url.ts`, `lib/photo-list.ts`: safe previews and concurrent photo updates.
- `lib/document-fields.ts`, `lib/repair-document-ids.ts`: Android ID compatibility.
- `lib/ai.ts`: authenticated AI client.
- `public/sw.js`: offline shell caching.
- `scripts/security-headers.mjs`: post-build CSP and hosting headers.
- `SECURITY.md`: security review and remaining limitations.

## Deployment state

The web app is deployed privately through Sites:

**https://on-the-job-student-log.puyongjustin07.chatgpt.site/**

This is a real hosted deployment, not localhost. However, it is currently owner-only and may show a ChatGPT sign-in gate. Public Firebase Hosting deployment has not been completed.

Latest successful publication:

| Field | Value |
| --- | --- |
| Sites project ID | `appgprj_6aa170c28254819199bba83a08fab667` |
| Version number | `5` |
| Version ID | `appgprj_6aa170c28254819199bba83a08fab667~appgver_c3567a8b6d988191a44f4508eb95ca27` |
| Deployment ID | `appgdep_6aa35e2252bc81918f5ebc8d8f7ca21c` |
| Source commit | `76d78b796d7b60f320c82d5d94c9f8dcb8184849` |
| Verified status | `succeeded`, 11 September 2026 |

`web/.openai/hosting.json` is authoritative: reuse its project ID; do not create another Site. This is a static export (`next.config.ts` uses `output: 'export'`) with public output in `dist/client`. Do not package `dist/server` or source files as the static site.

Latest AI Worker deployment: `9a8f2911-80dd-465d-9e46-d7b0df74bf34`. It includes the `AI_RATE_LIMITER` binding (10 requests per 60 seconds per user per Cloudflare location). The deployed Gemini secret must remain private; do not print `.dev.vars`, tokens, keystore secrets, or environment credentials.

## Shared data contract and previous crash

Firestore collections:

- `users/{uid}/entries/{entryId}`
- `users/{uid}/ojtInstances/{instanceId}`

Entry fields include `userId`, `rawText`, `text`, `imageUrls`, `hours`, `formattingStatus`, `entryDate`, `ojtInstanceId`, and `createdAt`.

**Never persist the document's `id` or the web-only `pending` field.** Android uses `@DocumentId`; storing an `id` field caused a fatal startup crash. Web writes now strip these fields, and authenticated compatibility repair deletes only redundant stored IDs. Android repositories use explicit decoders in `data/firestore/DocumentDecoders.kt`, treating the document path as the authoritative ID.

Do not overwrite an entry's entire photo list from a stale draft. The web editor now uses the current remote list plus explicit removal deltas, with Firestore array operations. This prevents Android uploads disappearing or deleted photos reappearing.

## Photo fixes

The reported missing photo was an SVG. Cloudinary returned the original as `image/svg+xml`; Android's image loader did not decode it. Existing SVG/HEIC/HEIF/TIFF/AVIF links now receive Cloudinary `f_png` preview transformations on Android and web. Stored originals are preserved.

New uploads:

- Web accepts JPEG, PNG and WebP only; validates extension, MIME type, file signature, successful decoding, maximum 10 MiB and maximum 40 megapixels.
- Existing offline queued files are revalidated. Blocked files are retained until the user explicitly removes them.
- Photo preview/original links are restricted to HTTPS on this app's Cloudinary image-upload path.
- Android `CloudinaryUploader.kt` checks actual image signatures, size and bitmap dimensions and sends the correct MIME type/extension. `PhotoFormat.kt` and `PhotoUrl.kt` contain helpers.
- The user edited Cloudinary's allowed formats. A live harmless SVG upload was rejected with “An unknown file format not allowed.” The provider's 10 MiB limit was not independently verified.

## Security work and limits

Implemented:

- Firebase token verification remains mandatory for AI calls.
- AI body capped at 128 KiB of actual streamed bytes; raw text capped at 20,000 characters.
- Per-user rate limiting, upstream timeout and output-token limit.
- Gemini API key sent in a header instead of the URL.
- Internal/upstream error details no longer returned to clients; AI responses use `no-store`.
- Certificate cache refresh corrected so expired cached keys are not reused indefinitely.
- Hash-based CSP is embedded in exported HTML, including offline pages. Frame restrictions and other protections are generated as HTTP headers. Rebuild after script changes to regenerate hashes.
- Service worker caching narrowed to app-shell resources.
- Web and Worker dependency audits reported **zero known vulnerabilities** after updates on 10 September. Both use a patched `sharp` override; Worker testing dependencies were upgraded to Vitest 4.1.11 and compatible Cloudflare tooling.

The user supplied these Firestore rules; they were reviewed, not independently fetched or redeployed:

```text
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/entries/{entryId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    match /users/{userId}/ojtInstances/{instanceId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

Remaining limitations—do not claim the whole system is fully secured:

1. Cloudinary's unsigned preset allows direct uploads without Firebase authentication. Signed uploads need a server-side signing flow and Cloudinary admin credentials/configuration.
2. Cloudinary photo URLs are public links. Firestore rules protect the records, not a known photo URL. Private photo delivery needs additional architecture/configuration.
3. Supplied Firestore rules enforce ownership but not field schemas or storage quotas. No authenticated cross-account penetration test was performed.
4. Offline logs/drafts persist on the device; signing out does not securely erase browser storage.
5. Google sign-in authorized domains may need attention for the intended final host. During responsive testing the hosted browser was signed out and previously displayed the app's unauthorized-domain message. The user had successfully logged in earlier, so verify current domain settings if this recurs; do not assert that production sign-in was fully re-tested.
6. Direct automated HTTP inspection of the private Site hit its ChatGPT sign-in gate. Deployment success and packaged CSP were verified; an authenticated live HTTP header check was not completed.

## Responsive work completed

The latest version fixes:

- Dialog widths capped to the available viewport, including entry and photo dialogs.
- Dynamic viewport height and scrollable dialogs for short landscape/keyboard-sized windows.
- Dialog transitions limited to opacity/transform so rotating or resizing does not animate geometry off-screen.
- Wrapping header, placement controls, action rows and progress details.
- Long placement names wrap in the trigger and dropdown; dropdown items have touch-friendly height.
- Flexible tabs and duration columns, readable calendar cells, safe-area padding.
- File picker on its own row, filename overflow handling, responsive photo sizing, and stacked actions on very narrow screens.

Browser checks used **local sample data in the real UI components**, without saving records, requesting AI output, or uploading photos. Tested widths: 280, 320, 360, 390, 550, 768, 1024, 1440 and 1920 pixels; also 844×390 landscape and reduced-height 390×350 and 320×225 layouts. Checked sign-in, feed, calendar/empty day, placement settings/dropdown, new and existing entry forms, photo viewer, and installation help. No horizontal overflow in the checked layouts; dialogs stayed in bounds and actions were reachable by scrolling/keyboard.

Actual browser zoom shortcuts were unsupported/no-op in this browser tool. Narrow viewport reflow was checked instead; do not claim a real 200% browser zoom or Safari/iOS device test occurred.

**Temporary sample hook and Vite changes were restored before the final build.** `lib/use-journal.ts` matches deployed real Firebase source, and no `responsive-local-fixture`/`layout-entry` sample markers were found in the production bundle. Never ship a test hook replacing authentication/data subscriptions.

## Verification history

- Web: 19 unit tests passed during security work.
- Worker: 8 tests passed, including success path, invalid/oversized input, streaming body cap, quota denial and error secrecy; TypeScript check passed.
- Android: unit tests and release build passed; installed release update on the connected phone without clearing data; startup completed and no AndroidRuntime fatal errors were found for the updated process.
- Latest responsive update: browser checks above, `npm run typecheck`, `npm run lint:app`, and production build passed.
- Expected nonblocking build warnings: large client chunk and future Vite JSON-import-attribute notice.

## Local commands

Run in `web/`:

```sh
npm run dev
npm test
npm run typecheck
npm run lint:app
npm run build
npm audit
```

Use `lint:app` for authored UI/lib checks; the full bundled component catalog had unrelated lint findings earlier. `npm run start` is aimed at a Worker build and is not the preferred way to serve this static export.

Run in `onthejob-ai-proxy/`:

```sh
npm test -- --run
npx tsc --noEmit
npm audit
npm run deploy
```

Run from project root for Android:

```sh
./gradlew :app:testDebugUnitTest :app:assembleRelease
```

Android SDK: `/home/justin/Android/Sdk`. Previously connected phone serial: `10HE4YFD330003X` (V2333). Recheck connection before use. Installed APK uses release signing; debug install had a signature mismatch. Use an in-place release update if needed, never uninstall or clear app data to bypass that mismatch.

Builds and local server tools may need sandbox escalation for network access, local listening ports, or tool cache writes. Reuse authorization where supported and explain actual permission blockers rather than abandoning verification.

## Git and publishing precautions

The parent project has uncommitted Android/Worker changes, and `web/` appears untracked from the parent repository. This is expected; do not discard or reset them. Parent `.git` was read-only in this environment.

These pre-existing release-output deletions were observed and left untouched:

- `app/release/baselineProfiles/0/app-release.dm`
- `app/release/baselineProfiles/1/app-release.dm`
- `app/release/output-metadata.json`

Sites source history was managed using a separate Git directory `/tmp/onthejob-web-source.git` with `web/` as its worktree, not by committing the Android parent. **The /tmp Git directory has disappeared across session restarts before.** If missing, get a fresh Sites repository credential, fetch its branch into a new temporary Git directory, and reconstruct the index/HEAD without overwriting local source. Compare local changes against fetched history. Do not force-push a new unrelated history.

For an authorized future web update:

1. Read the currently available Sites building/hosting skills and reuse the project ID.
2. Implement, verify, and build with the real data hook restored.
3. Inspect current Site access before selecting private/shared/public publication. Last verified access was owner-only.
4. Obtain a short-lived source credential; never save it in files, remotes, Git config or this document.
5. Commit/push only the Site source. After push succeeds, read the full HEAD SHA.
6. Package validated output with the Sites plugin's `scripts/package-site.sh` helper. The last working helper was `/home/justin/.codex/plugins/cache/openai-bundled/sites/0.1.57/scripts/package-site.sh`.
7. Save a version with the exact pushed SHA and archive, deploy using the appropriate access mode, and poll to terminal success.
8. Reuse the browser tab, reset temporary viewport overrides, and report the actual deployed URL.

Treat all temporary archives, credentials, process IDs and browser-tab IDs from previous sessions as expired until checked.

## Working preferences

The user prefers direct action, plain language and minimal repeated confirmations. Preserve existing design and data. Ask only when information or access is actually missing. No subagents were requested for this work. Use the user's next message to decide the next task; the completed responsiveness request does not authorize speculative new features.

Suggested new-session prompt:

> Read HANDOVER.md in this project, inspect the current state without changing anything, and continue with my next request. Preserve the existing Android/web compatibility and deployed configuration.

# Architecture & Technical Decisions Log

## Decision 1: AI Formatting Toggle State & Status Mapping (Feature 1)

### Context & Requirements
Users require the ability to opt out of Cloudflare Worker / Gemini AI formatting per entry.
When disabled:
1. Skip network calls entirely.
2. Save raw text as-is (`text = rawText`).
3. Set `formattingStatus` to distinguish opt-out from AI failures.
4. Render a neutral status chip that conforms to `DESIGN.md` rules (never show raw/alarming technical errors).
5. Allow "Regenerate with AI" on `EntryDetailScreen` even for opted-out entries.

### Implementation
- Added `formattingStatus = "skipped"` enum value/string contract.
- Mapped `"skipped"` status in `StatusChip.kt` to a neutral chip:
  - Text: `"Raw entry"`
  - Colors: Muted background (`Muted.copy(alpha = 0.12f)`), Muted text color (`Muted`).
- In `DayStatus.kt`, `"skipped"` is treated as a completed state (`DayStatus.DONE` equivalent for calendar day aggregation) so days with skipped entries do not show alarming pending indicators.
- Added user preference `use_ai_formatting` in `OjtInstanceViewModel` (defaulting to `true`), persisted in `SharedPreferences`.
- Added a `Switch` on `NewEntryScreen` ("Format with AI").
- On `EntryDetailScreen`, raw entries (`formattingStatus = "skipped"`) retain the "Regenerate with AI" button, allowing users to opt into AI formatting retroactively.

---

## Decision 2: Multi-OJT Instance Data Model & Migration (Feature 2)

### Option Comparison & Model Choice
We evaluated two Firestore data structure models for multi-OJT instance support:

**Option (a):** Nested sub-collections: `users/{uid}/ojtInstances/{instanceId}/entries/{entryId}`
- *Pros:* Strict collection hierarchy isolation.
- *Cons:* High migration risk. Moving existing Firestore documents across sub-collections requires reading, copying, and deleting documents across sub-trees. High risk of data loss if interrupted. Requires extensive changes to security rules and index definitions.

**Option (b):** Flat collections with reference field: `users/{uid}/ojtInstances/{instanceId}` metadata collection + `users/{uid}/entries/{entryId}` with an added `ojtInstanceId: String` reference field. **(CHOSEN)**
- *Pros:* Zero migration data-loss risk. Existing entries remain at `users/{uid}/entries/{entryId}`. Backfilling `ojtInstanceId` is a single idempotent `update()` operation. Preserves existing Firestore security rules for `entries`.
- *Cons:* Requires client-side or Firestore query filtering by `ojtInstanceId`.

### Migration Strategy
- `OjtInstanceRepository.ensureDefaultInstance()` performs an idempotent migration on first app startup:
  1. Checks if `users/{uid}/ojtInstances` contains any documents.
  2. If empty, creates a default document with `name = "OJT 1"` and carries over the legacy required hours target (default 486.0 hrs).
  3. Queries all entries in `users/{uid}/entries` where `ojtInstanceId` is empty or missing, and performs batch updates to populate `ojtInstanceId = defaultInstanceId`.
  4. Safe against interruptions and multi-device race conditions.

### Firestore Security Rules Update
To support `ojtInstances`, update security rules to allow authenticated owners access to their `ojtInstances` subcollection:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      match /entries/{entryId} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
      match /ojtInstances/{instanceId} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
    }
  }
}
```

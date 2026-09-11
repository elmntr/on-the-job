# OnTheJob (OJT Daily Log & Hours Tracker) 🛠️📱

[![Release](https://img.shields.io/badge/Release-v1.3-blue?style=for-the-badge&logo=github)](https://github.com/elmntr/on-the-job/releases/tag/ver1.3)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-purple?style=for-the-badge&logo=kotlin)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material3-green?style=for-the-badge&logo=android)](https://developer.android.com/jetpack/compose)
[![Firebase](https://img.shields.io/badge/Firebase-Auth_%26_Firestore-orange?style=for-the-badge&logo=firebase)](https://firebase.google.com/)
[![Cloudflare Workers](https://img.shields.io/badge/Cloudflare_Workers-AI_Proxy-amber?style=for-the-badge&logo=cloudflare)](https://workers.cloudflare.com/)

**OnTheJob** is an Android and web app for students, interns, and professionals undertaking On-the-Job Training (OJT). Record daily activities, track hours across placements, attach photos, and optionally turn rough notes into professional narratives with AI. Sign in with the same Google account on Android and web to use the same journal.

The responsive web client supports desktop, iPhone, and iPad browsers, with Home Screen installation guidance and offline drafts.

---

## 🚀 Use OnTheJob

🌐 **[Open OnTheJob Web](https://on-the-job-19c0f.web.app)**

Use OnTheJob directly in your browser on iPhone, iPad, Android, or desktop. Sign in with the same Google account to access your existing placements, logs, hours, and photos. No APK installation is needed for the web version.

Prefer the Android app? Download the APK from the official release page:

📥 **[Download OnTheJob v1.3 APK](https://github.com/elmntr/on-the-job/releases/tag/ver1.3)**

---

## ✨ Features

* ⏱️ **Time Tracking & Goal Progress**: Log exact hours and minutes worked each day. Set custom goal targets (e.g., 500 hours) and track progress dynamically with completion status chips and progress metrics.
* 📅 **Calendar & Log Feed Views**: View all past entries via an interactive monthly calendar grid with daily detail panels or scroll through a unified timeline feed.
* ✏️ **Back-logging & Custom Time**: Easily record entries for past dates or retroactively adjust logged hours.
* 🤖 **AI-Powered Log Polish**: Converts raw bullet points or casual work notes into a polished, professional first-person narrative entry using a Cloudflare Worker backend powered by **Google Gemini 3.1 Flash Lite**.
* 🖼️ **Multi-Photo Attachments & Viewer**: Attach multiple photos to daily logs with parallel Cloudinary background uploads (powered by Android `WorkManager`). Full-screen `PhotoViewer` with image downloading capabilities.
* 🔒 **Google Sign-In**: Firebase Authentication, using Android Credential Manager on Android and a sign-in popup on web.
* 🗂️ **Multiple Placements**: Separate OJT placements with individual required-hour targets.
* 🌐 **Web Client**: Responsive feed, calendar, entry editor, photo viewer, offline drafts, and Home Screen installation guidance.
* 📶 **Offline Support**: Firestore caching and persistent photo queues. Android uses background workers; web photo uploads resume while the app is open, online, and signed in. Browser storage can be cleared or evicted, so local drafts are not a backup.
* 🎨 **Custom Design System**: Vibrant theme with custom typography (`Archivo Black`, `Big Shoulders`, `IBM Plex Mono/Sans`), stamped status badges, and styled components.

---

## 🛠️ Architecture & Tech Stack

### Android Client (`app/`)
* **Language & UI**: Kotlin, Jetpack Compose, Material 3
* **Navigation**: Jetpack Compose Navigation 3
* **Backend Integration**: Firebase Auth, Firestore DB (Offline Persistence)
* **Background Tasks & Uploads**: WorkManager, OkHttp3, Cloudinary API
* **Image Loading**: Coil Compose

### Web Client (`web/`)
* **UI**: React, TypeScript, Vinext/Vite, Tailwind CSS, Base UI/shadcn
* **Data**: Firebase Authentication and Firestore, shared with Android
* **Offline**: IndexedDB drafts/photo queue and an app-shell service worker
* **Hosting**: Static export to `dist/client/`, with Firebase Hosting configuration

### AI Proxy Backend (`onthejob-ai-proxy/`)
* **Runtime**: Cloudflare Workers (TypeScript)
* **AI Model**: Google Gemini 3.1 Flash Lite API (`generativelanguage.googleapis.com`)
* **Security & Auth Verification**: `jose` library verifying RS256-signed Firebase Auth ID tokens to protect API quota.

---

## 📂 Project Structure

```
OnTheJob/
├── app/                              # Android application source code
│   ├── src/main/java/com/example/onthejob/
│   │   ├── data/                     # Repositories, AI client, upload workers, models
│   │   ├── navigation/               # Compose Navigation 3 routes & host
│   │   ├── ui/                       # Screens (Calendar, LogFeed, NewEntry, EntryDetail, Auth, PhotoViewer)
│   │   └── util/                     # Utilities & network helpers
│   └── src/main/res/                 # Drawables, layout, fonts, values
├── web/                             # Responsive browser client
│   ├── app/                         # Routes and global styles
│   ├── components/                  # Journal UI and shared components
│   ├── lib/                         # Firebase, drafts, uploads, and data compatibility
│   ├── public/                      # Static assets and offline service worker
│   └── firebase.json                # Static hosting and security headers
├── onthejob-ai-proxy/                # Cloudflare Worker for secure Gemini API proxying
│   ├── src/index.ts                  # Worker script (Firebase JWT verification + Gemini API call)
│   └── wrangler.jsonc                # Cloudflare Wrangler configuration
├── build.gradle.kts                  # Root Gradle build script
└── README.md
```

---

## ⚙️ Building from Source

### Prerequisites
* **Android Studio**: Ladybug (2024.2.1) or newer recommended.
* **JDK**: Java 17 or higher.
* **Node.js & npm**: Node 22.13+ for the web client; also used for Worker development.

---

### Step 1: Clone the Repository

```bash
git clone https://github.com/elmntr/on-the-job.git
cd on-the-job
```

---

### Step 2: Configure Firebase Services

1. For this app, reuse the existing `on-the-job-19c0f` project in the [Firebase Console](https://console.firebase.google.com/). Create a separate project only for an independent fork, and update the client and Worker configuration accordingly.
2. Enable **Authentication** (Google Sign-In provider) and **Cloud Firestore**.
3. Register an Android App with the package name `com.example.onthejob`.
4. Download the `google-services.json` file and place it in the `app/` directory:
   ```
   app/google-services.json
   ```

---

### Step 3: Configure Signing & Build Properties (Optional for Release)

For debug builds, no extra properties are required.

If you wish to sign release builds, create a `keystore.properties` file in the root directory:

```properties
storeFile=release.jks
storePassword=YOUR_KEYSTORE_PASSWORD
keyAlias=YOUR_KEY_ALIAS
keyPassword=YOUR_KEY_PASSWORD
```

---

### Step 4: Build and Run

#### Using Android Studio:
1. Open the cloned `OnTheJob` folder in Android Studio.
2. Allow Gradle sync to complete.
3. Select an emulator or connected device, then click **Run (Shift + F10)**.

#### Using Command Line:

* **Debug Build:**
  ```bash
  ./gradlew assembleDebug
  ```
  The generated APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

* **Release Build:**
  ```bash
  ./gradlew assembleRelease
  ```

---

## 🌐 Run and deploy the web app

From the repository root:

```sh
cd web
npm ci
npm run dev
```

### Free Firebase Hosting deployment

Use the existing Firebase **Spark** plan and default domain to avoid hosting charges. Keep Cloudflare Workers, Cloudinary, and Gemini on their free plans too. Free allowances are limited and shared with Android; reaching them can interrupt hosting, syncing, uploads, or AI. Verify actual account plans before release and do not enable paid billing for a zero-cost deployment. See [Firebase Hosting quotas](https://firebase.google.com/docs/hosting/usage-quotas-pricing) and [web setup details](web/README.md).

1. In Firebase project settings, reuse an existing registered Web app or register **OnTheJob Web**. Firebase's SDK is already installed. Compare its configuration with `web/lib/firebase.ts`; the supported build-time overrides are `NEXT_PUBLIC_FIREBASE_API_KEY` and `NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN`.
2. Verify Google sign-in is enabled and the final hosting domain is authorized. Review the live Firestore rules for both user collections described below.
3. Run these commands **inside `web/`**:

```sh
npx firebase-tools login
npm run typecheck
npm test
npm run lint:app
npm run build
npx firebase-tools deploy --only hosting --project on-the-job-19c0f
```

`firebase.json` and `.firebaserc` are already included: **do not run `firebase init`** for this checkout. If you installed the CLI globally with `npm install -g firebase-tools`, use `firebase login` and `firebase deploy --only hosting --project on-the-job-19c0f` instead.

The configured deployment address is **https://on-the-job-19c0f.web.app**. Treat the release as live only after deployment succeeds and the site is checked. The command publishes only `dist/client/`; it does not deploy Firestore rules, Android releases, or the AI Worker. No running Node server is required.

Before sharing with students, test Google login on iPhone Safari, Home Screen installation, Android/web sync in both directions, offline saving/reconnection, photo uploads, AI fallback, and account isolation. See [web release checks](web/README.md#release-checks-needing-real-accountsdevices).

## 🤖 Maintain or self-host the AI Proxy Worker

The AI feature relies on a Cloudflare Worker proxy (`onthejob-ai-proxy`). To deploy your own instance:

1. Navigate to the proxy directory:
   ```bash
   cd onthejob-ai-proxy
   npm ci
   ```
2. Authenticate with `npx wrangler login`, then set your Gemini API key as a secret in Cloudflare:
   ```bash
   npx wrangler secret put GEMINI_API_KEY
   ```
3. Deploy the worker:
   ```bash
   npm run deploy
   ```
4. Update the endpoint URL in `app/src/main/java/com/example/onthejob/data/aiformat/Aiformatclient.kt` and `web/lib/ai.ts` to point to your newly deployed worker URL.

---

## Shared data and security

Both clients use `users/{uid}/entries/{entryId}` and `users/{uid}/ojtInstances/{instanceId}`. Firestore rules must restrict access to the signed-in owner. Keep these paths and field contracts compatible; never persist document `id` or web-only `pending` fields. See [architecture decisions](DECISIONS.md) and [document ID compatibility](web/README.md#android-document-id-compatibility).

Web uploads accept JPEG, PNG, and WebP up to 10 MiB and 40 megapixels. Existing unsupported photo formats have compatible previews. AI calls require Firebase tokens and have input limits, timeouts, and per-user rate limiting; Gemini credentials remain on the Worker.

Known limitations: Cloudinary uploads currently use a public unsigned preset, photo URLs are public links, and signing out does not erase offline browser storage. The AI rate limiter is not a global spending cap. Review [security notes](web/SECURITY.md) before a broad release.

## 📜 License

This project is open-source under the MIT License.

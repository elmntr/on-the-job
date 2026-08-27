
/# OnTheJob (OJT Daily Log & Hours Tracker) 🛠️📱

[![Release](https://img.shields.io/badge/Release-v1.2-blue?style=for-the-badge&logo=github)](https://github.com/elmntr/on-the-job/releases/tag/ver1.2)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-purple?style=for-the-badge&logo=kotlin)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material3-green?style=for-the-badge&logo=android)](https://developer.android.com/jetpack/compose)
[![Firebase](https://img.shields.io/badge/Firebase-Auth_%26_Firestore-orange?style=for-the-badge&logo=firebase)](https://firebase.google.com/)
[![Cloudflare Workers](https://img.shields.io/badge/Cloudflare_Workers-AI_Proxy-amber?style=for-the-badge&logo=cloudflare)](https://workers.cloudflare.com/)

**OnTheJob** is a modern Android application designed for trainees, interns, and professionals undertaking On-the-Job Training (OJT). It streamlines logging daily activities, tracking completed hours against target goals, formatting raw work notes into professional narratives using AI, and securely syncing data offline-first.

---

## 🚀 Quick Download

Get the latest pre-compiled APK directly from the official release page:

📥 **[Download OnTheJob v1.2 APK](https://github.com/elmntr/on-the-job/releases/tag/ver1.2)**

---

## ✨ Features

* ⏱️ **Time Tracking & Goal Progress**: Log exact hours and minutes worked each day. Set custom goal targets (e.g., 500 hours) and track progress dynamically with completion status chips and progress metrics.
* 📅 **Calendar & Log Feed Views**: View all past entries via an interactive monthly calendar grid with daily detail panels or scroll through a unified timeline feed.
* ✏️ **Back-logging & Custom Time**: Easily record entries for past dates or retroactively adjust logged hours.
* 🤖 **AI-Powered Log Polish**: Converts raw bullet points or casual work notes into a polished, professional first-person narrative entry using a Cloudflare Worker backend powered by **Google Gemini 3.1 Flash Lite**.
* 🖼️ **Multi-Photo Attachments & Viewer**: Attach multiple photos to daily logs with parallel Cloudinary background uploads (powered by Android `WorkManager`). Full-screen `PhotoViewer` with image downloading capabilities.
* 🔒 **Secure Authentication**: Google Sign-In via Android Credential Manager integrated with Firebase Authentication.
* 📶 **Offline-First Storage**: Powered by Firebase Firestore offline cache and persistent photo queues — work notes and photos are saved locally and synced automatically when connected.
* 🎨 **Custom Design System**: Vibrant theme with custom typography (`Archivo Black`, `Big Shoulders`, `IBM Plex Mono/Sans`), stamped status badges, and styled components.

---

## 🛠️ Architecture & Tech Stack

### Android Client (`app/`)
* **Language & UI**: Kotlin, Jetpack Compose, Material 3
* **Navigation**: Jetpack Compose Navigation 3
* **Backend Integration**: Firebase Auth, Firestore DB (Offline Persistence)
* **Background Tasks & Uploads**: WorkManager, OkHttp3, Cloudinary API
* **Image Loading**: Coil Compose

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
* **Node.js & npm** (optional, if deploying your own Cloudflare Worker): Node 18+.

---

### Step 1: Clone the Repository

```bash
git clone https://github.com/elmntr/on-the-job.git
cd on-the-job
```

---

### Step 2: Configure Firebase Services

1. Create a project in the [Firebase Console](https://console.firebase.google.com/).
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

### Step 5: (Optional) Self-Host the AI Proxy Worker

The AI feature relies on a Cloudflare Worker proxy (`onthejob-ai-proxy`). To deploy your own instance:

1. Navigate to the proxy directory:
   ```bash
   cd onthejob-ai-proxy
   npm install
   ```
2. Set your Gemini API key as a secret in Cloudflare:
   ```bash
   npx wrangler secret put GEMINI_API_KEY
   ```
3. Deploy the worker:
   ```bash
   npm run deploy
   ```
4. Update the endpoint URL in [`Aiformatclient.kt`](file:///home/justin/AndroidStudioProjects/OnTheJob/app/src/main/java/com/example/onthejob/data/aiformat/Aiformatclient.kt) to point to your newly deployed worker URL.

---

## 📜 License

This project is open-source under the MIT License.

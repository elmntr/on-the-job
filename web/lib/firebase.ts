import { getApp, getApps, initializeApp } from 'firebase/app';
import { getAuth, GoogleAuthProvider, signInWithPopup } from 'firebase/auth';
import {
  initializeFirestore,
  persistentLocalCache,
  persistentMultipleTabManager,
} from 'firebase/firestore';

// Firebase client identifiers are public. Firestore rules enforce ownership.
const app = getApps().length
  ? getApp()
  : initializeApp({
      apiKey:
        process.env.NEXT_PUBLIC_FIREBASE_API_KEY ||
        'AIzaSyBVr5FmCdLH6N0IUT_duI4_dW2W5cWEjy0',
      authDomain:
        process.env.NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN ||
        'on-the-job-19c0f.firebaseapp.com',
      projectId: 'on-the-job-19c0f',
    });
export const auth = getAuth(app);
export const db = initializeFirestore(app, {
  localCache: persistentLocalCache({
    tabManager: persistentMultipleTabManager(),
  }),
});
export function login() {
  return signInWithPopup(auth, new GoogleAuthProvider());
}
export function friendlyError(error: unknown): string {
  const code = (error as { code?: string })?.code || '';
  if (code.includes('popup-closed'))
    return 'Sign-in was closed. Tap Continue with Google to try again.';
  if (code.includes('popup-blocked'))
    return 'Allow the sign-in pop-up in your browser, then try again.';
  if (code.includes('unauthorized-domain'))
    return 'Sign-in is not enabled for this web address yet. Please contact the app owner.';
  if (code.includes('permission-denied'))
    return 'Your account could not access these logs. Please contact the app owner.';
  if (code.includes('network'))
    return 'Could not connect. Check your connection and try again.';
  return 'Something went wrong. Your writing has been kept on this device. Please try again.';
}

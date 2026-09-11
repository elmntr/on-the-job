import { openDB } from 'idb';
import { arrayUnion, doc, updateDoc } from 'firebase/firestore';
import { auth, db } from './firebase';
import { validateDecodablePhoto } from './upload-policy';
import { safePhotoUrl } from './photo-url';
export type PhotoJob = {
  id: string;
  uid: string;
  entryId: string;
  file: Blob;
  url?: string;
  blocked?: string;
};
const storage = () =>
  openDB('onthejob-web', 1, {
    upgrade(db) {
      db.createObjectStore('photos', { keyPath: 'id' });
      db.createObjectStore('drafts');
    },
  });
export async function saveDraft(key: string, value: unknown) {
  return (await storage()).put('drafts', value, key);
}
export async function readDraft(key: string) {
  return (await storage()).get('drafts', key);
}
export async function clearDraft(key: string) {
  return (await storage()).delete('drafts', key);
}
export async function queuePhotos(jobs: PhotoJob[]) {
  for (const job of jobs) await validateDecodablePhoto(job.file);
  const database = await storage();
  const tx = database.transaction('photos', 'readwrite');
  for (const job of jobs) await tx.store.put(job);
  await tx.done;
}
export async function photoJobs(uid: string): Promise<PhotoJob[]> {
  return ((await (await storage()).getAll('photos')) as PhotoJob[]).filter(
    (j) => j.uid === uid,
  );
}
let uploading = false;
export async function uploadPhotos(uid: string, isCurrentUser: () => boolean) {
  if (uploading || !navigator.onLine || auth.currentUser?.uid !== uid || !isCurrentUser()) return;
  uploading = true;
  try {
    for (const job of await photoJobs(uid)) {
      if (!isCurrentUser() || auth.currentUser?.uid !== uid) break;
      if (job.blocked) continue;
      let url = job.url;
      if (!url) {
        let format: string;
        try { format = await validateDecodablePhoto(job.file); }
        catch (error) {
          await (await storage()).put('photos', {...job, blocked: error instanceof Error ? error.message : 'Unsupported file'});
          continue;
        }
        const body = new FormData();
        body.append('file', job.file, `ojt-photo.${format}`);
        body.append('upload_preset', 'onthejob_unsigned');
        const response = await fetch(
          'https://api.cloudinary.com/v1_1/dskoyv2oe/image/upload',
          { method: 'POST', body, signal: AbortSignal.timeout(60000) },
        );
        if (!response.ok) throw new Error('Photo upload failed');
        const result = (await response.json()) as { secure_url?: string };
        url = result.secure_url;
        if (typeof url !== 'string' || !safePhotoUrl(url))
          throw new Error('Invalid image URL');
        await (await storage()).put('photos', { ...job, url });
      }
      if (!isCurrentUser() || auth.currentUser?.uid !== uid) break;
      if (!url || !safePhotoUrl(url)) throw new Error('Invalid photo URL');
      await updateDoc(doc(db, 'users', uid, 'entries', job.entryId), {
        imageUrls: arrayUnion(url),
      });
      await (await storage()).delete('photos', job.id);
    }
  } finally {
    uploading = false;
  }
}

export async function discardBlockedPhotos(uid: string) {
  if(auth.currentUser?.uid !== uid) return;
  const database=await storage();
  const jobs=await photoJobs(uid);
  const tx=database.transaction('photos','readwrite');
  for(const job of jobs.filter(job=>job.blocked)) await tx.store.delete(job.id);
  await tx.done;
}

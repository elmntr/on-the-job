import {
  deleteField,
  writeBatch,
  type QuerySnapshot,
} from 'firebase/firestore';
import { auth, db } from './firebase';

// Repair only the redundant top-level field introduced by the first web release.
// All content, document paths, timestamps and placement references remain intact.
export async function repairDocumentIds(
  snapshot: QuerySnapshot,
  uid: string,
  attempted: Set<string>,
) {
  if (snapshot.metadata.fromCache || auth.currentUser?.uid !== uid) return;
  const affected = snapshot.docs.filter(
    (document) =>
      !document.metadata.hasPendingWrites &&
      Object.hasOwn(document.data(), 'id') &&
      !attempted.has(document.ref.path),
  );
  for (let offset = 0; offset < affected.length; offset += 400) {
    if (auth.currentUser?.uid !== uid) return;
    const batch = writeBatch(db);
    for (const document of affected.slice(offset, offset + 400)) {
      attempted.add(document.ref.path);
      batch.update(document.ref, { id: deleteField() });
    }
    await batch.commit();
  }
}

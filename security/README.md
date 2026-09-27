# Security remediation rollout

Code changes are local until deployed. Existing APKs and the deployed website still use the unsigned preset.

## Owner actions

1. In Cloudinary, disable unsigned uploads for `onthejob_unsigned` immediately to close the public endpoint. Existing clients will temporarily lose photo uploads until updated. Do not re-enable the preset to support old clients. Review usage and billing alerts; remove audit asset `zej5281rbezci3ucgfpa` after checking its identity.
2. Configure the Worker's Cloudinary credentials through the interactive prompts (do not paste secrets into chat or commit them):

   ```sh
   cd /home/justin/AndroidStudioProjects/OnTheJob/onthejob-ai-proxy
   npx wrangler secret put CLOUDINARY_API_KEY
   npx wrangler secret put CLOUDINARY_API_SECRET
   npm run deploy
   ```

   Obtain these from the Cloudinary product environment for `dskoyv2oe`. Keep the existing GEMINI_API_KEY secret. The new UPLOAD_RATE_LIMITER binding is in wrangler.jsonc. Uploads fail closed with HTTP 503 if configuration is missing.
3. Publish the updated web app through its existing hosting workflow and distribute an updated signed Android release. A debug APK is only for local testing. Test new-entry, edit-entry, offline retry, sign-out/account-switch, and rate-limit behavior. The new route is `/upload`; the existing AI route is unchanged.
4. Export/review the currently deployed Firebase rules, then publish the supplied ownership rules (entries and placements only). Storage is deny-all because the app uses Cloudinary; verify no other app in this Firebase project needs Storage before publishing:

   ```sh
   cd /home/justin/AndroidStudioProjects/OnTheJob
   firebase deploy --project on-the-job-19c0f --config security/firebase.json --only firestore:rules,storage
   ```

## Verification

```sh
cd /home/justin/AndroidStudioProjects/OnTheJob
firebase emulators:exec --project demo-onthejob --config security/firebase.json --only firestore 'node security/test-firestore.mjs'
```

After rollout, check that anonymous `/upload` requests return 401 and that the old unsigned preset rejects uploads. Verify a real JPEG/PNG/WebP upload from both clients succeeds. Test Android backup/restore and device transfer on supported devices: local app data must not transfer. Unsynced photos and local settings are intentionally excluded from backup.

## Scope and remaining limits

- Backend validates actual streamed size (10 MiB), image signatures, and forces Cloudinary's image decoder with allowed_formats=jpg,png,webp. It never accepts client-provided folders, public IDs, upload URLs, or signing parameters. Client-side 40-megapixel validation remains additional UX protection, not a backend pixel limit.
- Upload rate limit: 10 requests per UID per minute per Cloudflare location. This is abuse reduction, not a global daily storage quota or billing cap. Account creation and distributed abuse remain possible; use billing alerts and consider App Check and a persistent quota ledger for stronger controls.
- Ownership rules preserve legacy document compatibility. They do not add field schema constraints. Existing Firebase configuration remains public as intended; hiding or rotating it does not replace rules.
- Cloudinary delivery URLs remain publicly readable to anyone holding the URL. This change secures uploads, not private image delivery.
- Worker uploads use random asset IDs. A lost upload response can result in a duplicate on retry; an idempotency/asset-cleanup workflow is separate follow-up work.
- Production credentials, provider preset changes, Firebase deployment, release publication, and device restore checks are not performed by the local tests.

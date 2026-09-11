# Security review — 2026-09-10

Implemented and checked:
- New web uploads accept JPEG, PNG and WebP only, verify filename, MIME type, file signature and successful image decoding, and reject files over 10 MiB or 40 megapixels. Queue processing revalidates old offline files; blocked files remain available for explicit removal.
- Photo rendering and original links accept HTTPS URLs from this app's Cloudinary image-upload path only. Existing SVG/HEIC/etc. originals receive PNG previews without changing stored records.
- AI requests require a verified Firebase token, at most 20,000 characters and 128 KiB of actual body bytes, and a Cloudflare limit of 10 requests per authenticated user per 60 seconds. This limiter is per Cloudflare location, not a global billing cap.
- Gemini calls have a timeout and output-token limit. API credentials are sent in a header, upstream error bodies are not returned, and responses are not cached.
- Static pages carry a hash-based Content Security Policy. HTTP headers add frame restrictions, MIME-sniffing protection, no-referrer and restricted browser permissions. Offline shell caching excludes identity and data requests.
- npm dependency audits of both web and AI service report zero known vulnerabilities after updates. This is a point-in-time advisory check, not a guarantee of vulnerability-free software.

Firebase rules supplied by the owner restrict both users/{uid}/entries and users/{uid}/ojtInstances to request.auth.uid == uid. Other paths default to denied. These rules were reviewed as supplied; no live rules deployment or authenticated cross-account penetration test was performed. They enforce ownership, but do not enforce field schemas or per-user storage quotas.

Remaining provider/architecture limits:
- A harmless SVG upload was rejected by the live unsigned Cloudinary preset. Its provider-side 10 MiB setting has not been independently confirmed.
- The unsigned preset is public configuration and permits direct uploads that bypass Firebase login. Signed uploads with server-side Firebase verification are needed to restrict quota usage to authenticated users; Cloudinary admin credentials are required to implement that change.
- Cloudinary image delivery URLs are public bearer links. Firestore ownership rules protect log records, not access to a photo URL already known to someone. Private/authenticated image delivery would require a storage/access redesign.
- Offline drafts and cached logs remain on the device by design. Use a personal device; signing out is not a secure erasure of browser storage.

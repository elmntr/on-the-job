export function safePhotoUrl(source: string): string | null {
  try {
    const url = new URL(source);
    if (url.protocol !== 'https:' || url.hostname !== 'res.cloudinary.com' || url.port || url.username || url.password || !url.pathname.startsWith('/dskoyv2oe/image/upload/')) return null;
    return url.toString();
  } catch { return null; }
}
/** Preserve stored originals; request raster previews for formats not shared by all clients. */
export function photoPreviewUrl(source: string): string {
  if (!safePhotoUrl(source)) return '';
  try {
    const url = new URL(source);
    if (
      url.protocol !== 'https:' ||
      url.hostname !== 'res.cloudinary.com' ||
      !url.pathname.startsWith('/dskoyv2oe/image/upload/')
    )
      return source;
    if (!/\.(svg|heic|heif|tif|tiff|avif)$/i.test(url.pathname)) return source;
    if (url.pathname.includes('/f_png/')) return source;
    url.pathname = url.pathname.replace(
      '/image/upload/',
      '/image/upload/f_png/',
    );
    return url.toString();
  } catch {
    return source;
  }
}

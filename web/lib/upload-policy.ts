export const MAX_PHOTO_BYTES = 10 * 1024 * 1024;
export const PHOTO_ACCEPT = 'image/jpeg,image/png,image/webp,.jpg,.jpeg,.png,.webp';
const allowedTypes = new Set(['image/jpeg', 'image/png', 'image/webp']);
export async function validatePhoto(file: Blob): Promise<'jpg' | 'png' | 'webp'> {
  if (!file.size || file.size > MAX_PHOTO_BYTES) throw new Error('Choose photos between 1 byte and 10 MB.');
  if (file.type && !allowedTypes.has(file.type.toLowerCase())) throw new Error('Only JPEG, PNG, and WebP photos are allowed.');
  const name = (file as File).name;
  if (name && !/\.(jpe?g|png|webp)$/i.test(name)) throw new Error('Only .jpg, .jpeg, .png, and .webp files are allowed.');
  const bytes = new Uint8Array(await file.slice(0, 16).arrayBuffer());
  let format: 'jpg' | 'png' | 'webp';
  if (bytes.length >= 3 && bytes[0]===255 && bytes[1]===216 && bytes[2]===255) format='jpg';
  else if (bytes.length >= 8 && [137,80,78,71,13,10,26,10].every((n,i)=>bytes[i]===n)) format='png';
  else if (bytes.length >= 12 && String.fromCharCode(...bytes.slice(0,4))==='RIFF' && String.fromCharCode(...bytes.slice(8,12))==='WEBP') format='webp';
  else throw new Error('This file is not a supported photo, even if its name ends in .jpg or .png.');
  const mime = {jpg:'image/jpeg',png:'image/png',webp:'image/webp'}[format];
  if (file.type && file.type.toLowerCase() !== mime) throw new Error('The photo’s file type does not match its contents.');
  if (name && !({jpg:/\.jpe?g$/i,png:/\.png$/i,webp:/\.webp$/i}[format]).test(name)) throw new Error('The photo’s extension does not match its contents.');
  return format;
}
export async function validateDecodablePhoto(file: Blob) {
  const format=await validatePhoto(file);
  // Decode only supported raster types; SVG is never passed to the browser decoder.
  const bitmap=await createImageBitmap(file).catch(()=>{throw new Error('This photo is damaged or cannot be decoded.');});
  try {if(bitmap.width*bitmap.height>40_000_000)throw new Error('Choose a photo smaller than 40 megapixels.');} finally {bitmap.close();}
  return format;
}

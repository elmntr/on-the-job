import type { Metadata } from 'next';
import './globals.css';
export const metadata: Metadata = {
  title: 'OnTheJob — OJT Journal',
  description: 'Your daily OJT logs, photos, and training hours, together.',
  manifest: '/manifest.webmanifest',
  appleWebApp: { capable: true, title: 'OnTheJob', statusBarStyle: 'default' },
};
export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}

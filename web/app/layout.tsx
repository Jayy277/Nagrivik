import type { Metadata } from 'next';
import './globals.css';
import { Header } from '../components/Header';
import { Footer } from '../components/Footer';
import { AuthProvider } from '../lib/context/AuthContext';
import { getPublicSiteUrl } from '../lib/seo';

export const metadata: Metadata = {
  metadataBase: new URL(getPublicSiteUrl()),
  title: {
    default: 'Nagrivic — Public Civic Transparency Platform',
    template: '%s | Nagrivic',
  },
  description:
    'Nagrik Ki Awaaz, Sheher Ka Sudhaar. Discover, track, and support municipal civic issues in your neighborhood with real-time resolution transparency.',
  keywords: [
    'Nagrivic',
    'Civic Issues',
    'Potholes',
    'Municipal Corporation',
    'Ahmedabad Civic',
    'Ward Transparency',
    'Civic Complaints',
    'Urban Governance',
  ],
  authors: [{ name: 'Nagrivic Civic Platform' }],
  creator: 'Nagrivic',
  openGraph: {
    type: 'website',
    locale: 'en_IN',
    url: getPublicSiteUrl(),
    siteName: 'Nagrivic',
    title: 'Nagrivic — Public Civic Transparency Platform',
    description:
      'Nagrik Ki Awaaz, Sheher Ka Sudhaar. Discover, track, and support municipal civic issues in your neighborhood.',
  },
  robots: {
    index: true,
    follow: true,
  },
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>
        <AuthProvider>
          <Header />
          <main id="main-content">{children}</main>
          <Footer />
        </AuthProvider>
      </body>
    </html>
  );
}

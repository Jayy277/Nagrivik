import React from 'react';
import Link from 'next/link';

export default function IssueNotFound() {
  return (
    <div className="container" style={{ padding: '6rem 1.25rem', textAlign: 'center' }}>
      <div style={{ fontSize: '3.5rem', marginBottom: '1rem' }}>🏛️</div>
      <h1 style={{ fontSize: '2rem', fontWeight: 800, color: 'var(--color-text)', marginBottom: '0.75rem' }}>
        Issue Not Found
      </h1>
      <p style={{ color: 'var(--color-text-secondary)', maxWidth: '480px', margin: '0 auto 2rem', lineHeight: 1.6 }}>
        The civic report you are looking for does not exist, has been removed, or is currently unavailable.
      </p>

      <div style={{ display: 'flex', justifyContent: 'center', gap: '1rem', flexWrap: 'wrap' }}>
        <Link href="/issues" className="btn btn-primary">
          Explore Public Issues →
        </Link>
        <Link href="/" className="btn btn-secondary">
          Return Home
        </Link>
      </div>
    </div>
  );
}

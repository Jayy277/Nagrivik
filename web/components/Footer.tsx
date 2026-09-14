import React from 'react';
import Link from 'next/link';

export function Footer() {
  return (
    <footer className="site-footer" role="contentinfo">
      <div className="container">
        <div className="footer-inner">
          <div className="footer-brand">
            <h3>Nagrivic</h3>
            <p style={{ color: '#94a3b8', fontSize: '0.95rem', lineHeight: 1.6, maxWidth: '420px', marginBottom: '1rem' }}>
              “Nagrik Ki Awaaz, Sheher Ka Sudhaar.”
            </p>
            <p style={{ color: '#64748b', fontSize: '0.85rem', lineHeight: 1.5, maxWidth: '420px' }}>
              An open civic transparency and public problem-discovery platform. Citizens report civic hazards, support existing community reports, and track resolution timelines with responsible municipal bodies.
            </p>
          </div>

          <div className="footer-col">
            <h4>Public Discovery</h4>
            <ul>
              <li>
                <Link href="/issues">All Civic Issues</Link>
              </li>
              <li>
                <Link href="/issues?category=roads-potholes">Roads & Potholes</Link>
              </li>
              <li>
                <Link href="/issues?category=garbage">Waste & Sanitation</Link>
              </li>
              <li>
                <Link href="/issues?category=streetlights">Street Lighting</Link>
              </li>
              <li>
                <Link href="/issues?category=water">Water & Drainage</Link>
              </li>
            </ul>
          </div>

          <div className="footer-col">
            <h4>Platform</h4>
            <ul>
              <li>
                <Link href="/accountability">Civic Accountability Dashboard</Link>
              </li>
              <li>
                <Link href="/#how-it-works">How It Works</Link>
              </li>
              <li>
                <Link href="/#report">Report via Mobile App</Link>
              </li>
              <li>
                <Link href="/sitemap.xml">Sitemap</Link>
              </li>
            </ul>
          </div>
        </div>

        <div className="footer-bottom">
          <p style={{ color: '#64748b' }}>
            © {new Date().getFullYear()} Nagrivic Civic Platform. Open civic transparency for Indian cities.
          </p>
          <p style={{ color: '#64748b', fontSize: '0.8rem' }}>
            Nagrivic is an independent civic engagement system and is not officially affiliated with or operated by any government body.
          </p>
        </div>
      </div>
    </footer>
  );
}

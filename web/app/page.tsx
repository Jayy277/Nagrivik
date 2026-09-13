import React from 'react';
import Link from 'next/link';
import { getIssues, getCategories } from '../lib/api/issues';
import { IssueCard } from '../components/IssueCard';

export const revalidate = 60; // ISR revalidate every 60 seconds

export default async function HomePage() {
  // Fetch real data on server
  const [categoriesRes, recentIssuesRes, highPriorityRes] = await Promise.allSettled([
    getCategories(),
    getIssues({ size: 6, sort: 'NEWEST' }),
    getIssues({ size: 4, priority: 'CRITICAL', sort: 'NEWEST' }),
  ]);

  const categories = categoriesRes.status === 'fulfilled' ? categoriesRes.value : [];
  const recentIssues = recentIssuesRes.status === 'fulfilled' ? recentIssuesRes.value.content : [];
  const highPriorityIssues = highPriorityRes.status === 'fulfilled' ? highPriorityRes.value.content : [];

  return (
    <div>
      {/* Hero Section */}
      <section className="hero" aria-labelledby="hero-heading">
        <div className="container">
          <div className="hero-tagline">
            <span>🛡️</span>
            <span>Nagrivic Public Civic Transparency</span>
          </div>

          <h1 id="hero-heading" className="hero-title">
            Nagrik Ki Awaaz, Sheher Ka Sudhaar.
          </h1>

          <p className="hero-subtitle">
            An open civic platform enabling citizens to report local hazards, back neighboring reports, and track real-time resolution progress with responsible municipal departments.
          </p>

          <div className="hero-actions">
            <Link href="/#report" className="btn btn-primary btn-lg">
              Report an Issue
            </Link>
            <Link href="/issues" className="btn btn-secondary btn-lg">
              Explore Issues →
            </Link>
          </div>

          {/* Quick Search Bar */}
          <form
            action="/issues"
            method="GET"
            style={{
              maxWidth: '560px',
              margin: '2.5rem auto 0',
              display: 'flex',
              gap: '0.5rem',
              backgroundColor: '#ffffff',
              padding: '0.5rem',
              borderRadius: 'var(--radius-lg)',
              border: '1px solid var(--color-border)',
              boxShadow: 'var(--shadow-md)',
            }}
          >
            <input
              type="text"
              name="q"
              placeholder="Search by keyword (e.g. pothole on SG highway, water leakage)..."
              aria-label="Search civic issues"
              style={{
                flex: 1,
                border: 'none',
                outline: 'none',
                padding: '0.65rem 0.85rem',
                fontSize: '0.95rem',
              }}
            />
            <button type="submit" className="btn btn-primary btn-sm">
              Search
            </button>
          </form>
        </div>
      </section>

      {/* Civic Categories Section */}
      <section style={{ padding: '3.5rem 0', borderBottom: '1px solid var(--color-border)' }}>
        <div className="container">
          <div style={{ textAlign: 'center', marginBottom: '2.5rem' }}>
            <h2 style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--color-text)', letterSpacing: '-0.02em' }}>
              Civic Categories
            </h2>
            <p style={{ color: 'var(--color-text-secondary)', marginTop: '0.5rem' }}>
              Browse civic challenges organized by municipal service domain
            </p>
          </div>

          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
              gap: '1.25rem',
            }}
          >
            {categories.map((cat) => {
              const iconMap: Record<string, string> = {
                'roads-potholes': '🚧',
                garbage: '🗑️',
                streetlights: '💡',
                water: '💧',
                drainage: '🔄',
              };
              const icon = iconMap[cat.slug || ''] || '📍';

              return (
                <Link
                  key={cat.id}
                  href={`/issues?category=${cat.slug || cat.id}`}
                  className="card card-hover"
                  style={{
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    textAlign: 'center',
                    padding: '1.75rem 1rem',
                  }}
                >
                  <span style={{ fontSize: '2.25rem', marginBottom: '0.75rem' }}>{icon}</span>
                  <h3 style={{ fontSize: '1.05rem', fontWeight: 700, color: 'var(--color-text)', marginBottom: '0.35rem' }}>
                    {cat.name}
                  </h3>
                  <span style={{ fontSize: '0.8rem', color: 'var(--color-primary)', fontWeight: 600 }}>
                    View Issues →
                  </span>
                </Link>
              );
            })}
          </div>
        </div>
      </section>

      {/* High Priority Issues Section */}
      {highPriorityIssues.length > 0 && (
        <section style={{ padding: '3.5rem 0', backgroundColor: 'var(--color-surface)', borderBottom: '1px solid var(--color-border)' }}>
          <div className="container">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', marginBottom: '2rem', flexWrap: 'wrap', gap: '1rem' }}>
              <div>
                <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem', color: '#b91c1c', fontSize: '0.85rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.04em', marginBottom: '0.25rem' }}>
                  <span>⚠️</span> High Community Impact
                </div>
                <h2 style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--color-text)', letterSpacing: '-0.02em' }}>
                  Critical Priority Hazards
                </h2>
                <p style={{ color: 'var(--color-text-secondary)', marginTop: '0.25rem' }}>
                  Urgent civic hazards prioritized based on public safety risk and population density
                </p>
              </div>

              <Link href="/issues?priority=CRITICAL" className="btn btn-outline btn-sm">
                View All Critical Issues →
              </Link>
            </div>

            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))',
                gap: '1.5rem',
              }}
            >
              {highPriorityIssues.map((issue) => (
                <IssueCard key={issue.id} issue={issue} />
              ))}
            </div>
          </div>
        </section>
      )}

      {/* Recent Issues Section */}
      <section style={{ padding: '3.5rem 0', borderBottom: '1px solid var(--color-border)' }}>
        <div className="container">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', marginBottom: '2rem', flexWrap: 'wrap', gap: '1rem' }}>
            <div>
              <h2 style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--color-text)', letterSpacing: '-0.02em' }}>
                Recent Public Reports
              </h2>
              <p style={{ color: 'var(--color-text-secondary)', marginTop: '0.25rem' }}>
                Latest civic issues submitted by verified residents and ward inspectors
              </p>
            </div>

            <Link href="/issues" className="btn btn-outline btn-sm">
              Explore All Issues ({recentIssues.length}) →
            </Link>
          </div>

          {recentIssues.length === 0 ? (
            <div style={{ padding: '3rem', textAlign: 'center', backgroundColor: '#fff', borderRadius: 'var(--radius-lg)', border: '1px solid var(--color-border)' }}>
              <p style={{ color: 'var(--color-text-secondary)' }}>No recent civic issues reported yet.</p>
            </div>
          ) : (
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))',
                gap: '1.5rem',
              }}
            >
              {recentIssues.map((issue) => (
                <IssueCard key={issue.id} issue={issue} />
              ))}
            </div>
          )}
        </div>
      </section>

      {/* How Nagrivic Works Section */}
      <section id="how-it-works" style={{ padding: '4.5rem 0', backgroundColor: 'var(--color-surface)', borderBottom: '1px solid var(--color-border)' }}>
        <div className="container">
          <div style={{ textAlign: 'center', maxWidth: '640px', margin: '0 auto 3.5rem' }}>
            <h2 style={{ fontSize: '1.85rem', fontWeight: 800, color: 'var(--color-text)', letterSpacing: '-0.02em' }}>
              How Nagrivic Works
            </h2>
            <p style={{ color: 'var(--color-text-secondary)', marginTop: '0.5rem', fontSize: '1rem', lineHeight: 1.6 }}>
              A transparent, closed-loop civic accountability cycle connecting citizens directly to local urban authorities.
            </p>
          </div>

          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
              gap: '2rem',
            }}
          >
            {/* Step 1 */}
            <div style={{ textAlign: 'center', padding: '1rem' }}>
              <div
                style={{
                  width: '56px',
                  height: '56px',
                  borderRadius: 'var(--radius-md)',
                  backgroundColor: 'var(--color-primary-light)',
                  color: 'var(--color-primary)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: '1.5rem',
                  fontWeight: 800,
                  margin: '0 auto 1.25rem',
                }}
              >
                1
              </div>
              <h3 style={{ fontSize: '1.15rem', fontWeight: 700, marginBottom: '0.5rem' }}>
                Capture & Geo-Tag
              </h3>
              <p style={{ fontSize: '0.9rem', color: 'var(--color-text-secondary)', lineHeight: 1.6 }}>
                Citizens report hazards using the mobile camera and hardware GPS coordinates to verify authenticity.
              </p>
            </div>

            {/* Step 2 */}
            <div style={{ textAlign: 'center', padding: '1rem' }}>
              <div
                style={{
                  width: '56px',
                  height: '56px',
                  borderRadius: 'var(--radius-md)',
                  backgroundColor: 'var(--color-primary-light)',
                  color: 'var(--color-primary)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: '1.5rem',
                  fontWeight: 800,
                  margin: '0 auto 1.25rem',
                }}
              >
                2
              </div>
              <h3 style={{ fontSize: '1.15rem', fontWeight: 700, marginBottom: '0.5rem' }}>
                Jurisdiction Routing
              </h3>
              <p style={{ fontSize: '0.9rem', color: 'var(--color-text-secondary)', lineHeight: 1.6 }}>
                Spatial polygons determine the municipal body, administrative city, ward, and engineering department.
              </p>
            </div>

            {/* Step 3 */}
            <div style={{ textAlign: 'center', padding: '1rem' }}>
              <div
                style={{
                  width: '56px',
                  height: '56px',
                  borderRadius: 'var(--radius-md)',
                  backgroundColor: 'var(--color-primary-light)',
                  color: 'var(--color-primary)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: '1.5rem',
                  fontWeight: 800,
                  margin: '0 auto 1.25rem',
                }}
              >
                3
              </div>
              <h3 style={{ fontSize: '1.15rem', fontWeight: 700, marginBottom: '0.5rem' }}>
                Open Tracking
              </h3>
              <p style={{ fontSize: '0.9rem', color: 'var(--color-text-secondary)', lineHeight: 1.6 }}>
                Status progressions and activity timestamps are made permanently visible on public web and mobile feeds.
              </p>
            </div>

            {/* Step 4 */}
            <div style={{ textAlign: 'center', padding: '1rem' }}>
              <div
                style={{
                  width: '56px',
                  height: '56px',
                  borderRadius: 'var(--radius-md)',
                  backgroundColor: 'var(--color-primary-light)',
                  color: 'var(--color-primary)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: '1.5rem',
                  fontWeight: 800,
                  margin: '0 auto 1.25rem',
                }}
              >
                4
              </div>
              <h3 style={{ fontSize: '1.15rem', fontWeight: 700, marginBottom: '0.5rem' }}>
                Citizen Verification
              </h3>
              <p style={{ fontSize: '0.9rem', color: 'var(--color-text-secondary)', lineHeight: 1.6 }}>
                Once marked resolved, the reporting citizen confirms whether work was genuinely fixed or requires rework.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Mobile App Handoff Section */}
      <section id="report" style={{ padding: '4.5rem 0', background: 'linear-gradient(135deg, #0f172a 0%, #1e293b 100%)', color: '#fff' }}>
        <div className="container" style={{ textAlign: 'center', maxWidth: '680px', margin: '0 auto' }}>
          <div style={{ fontSize: '2.5rem', marginBottom: '1rem' }}>📱</div>
          <h2 style={{ fontSize: '2rem', fontWeight: 800, color: '#f8fafc', marginBottom: '1rem', letterSpacing: '-0.02em' }}>
            Report Issues via the Nagrivic Mobile App
          </h2>
          <p style={{ fontSize: '1.05rem', color: '#cbd5e1', lineHeight: 1.6, marginBottom: '2rem' }}>
            To prevent spam and guarantee authentic ground-truth reporting, issues must be submitted through the verified Nagrivic mobile app using camera photo capture and hardware GPS coordinates.
          </p>

          <div style={{ display: 'flex', justifyContent: 'center', gap: '1rem', flexWrap: 'wrap' }}>
            <div
              style={{
                backgroundColor: 'rgba(255, 255, 255, 0.1)',
                border: '1px solid rgba(255, 255, 255, 0.2)',
                padding: '1rem 1.5rem',
                borderRadius: 'var(--radius-md)',
                fontSize: '0.9rem',
                color: '#e2e8f0',
              }}
            >
              🔒 Camera Photo Proof Required • 📍 Live GPS Coordinates
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}

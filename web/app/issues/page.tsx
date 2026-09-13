import React from 'react';
import type { Metadata } from 'next';
import Link from 'next/link';
import { getIssues, getCategories } from '../../lib/api/issues';
import { IssueCard } from '../../components/IssueCard';
import { IssueFilters } from '../../components/IssueFilters';
import { EmptyState } from '../../components/FeedbackStates';
import { Breadcrumbs } from '../../components/Breadcrumbs';
import { IssueStatus, PriorityLevel, IssueDiscoverySort } from '../../types/issue';
import { getPublicSiteUrl } from '../../lib/seo';

export const dynamic = 'force-dynamic';

interface IssuesPageProps {
  searchParams: Promise<{
    q?: string;
    category?: string;
    status?: string;
    priority?: string;
    sort?: string;
    page?: string;
  }>;
}

export async function generateMetadata({ searchParams }: IssuesPageProps): Promise<Metadata> {
  const resolvedParams = await searchParams;
  const q = resolvedParams.q;
  const category = resolvedParams.category;
  const siteUrl = getPublicSiteUrl();

  let title = 'Civic Issues Discovery';
  if (q) {
    title = `Search "${q}" — Civic Issues`;
  } else if (category) {
    title = `${category.replace(/-/g, ' ').toUpperCase()} Issues`;
  }

  return {
    title,
    description: 'Explore, track, and search verified municipal civic problems and resolution progress across urban areas.',
    alternates: {
      canonical: `${siteUrl}/issues`,
    },
  };
}

export default async function IssuesPage({ searchParams }: IssuesPageProps) {
  const resolvedParams = await searchParams;

  const q = resolvedParams.q || undefined;
  const categoryParam = resolvedParams.category || undefined;
  const statusParam = (resolvedParams.status as IssueStatus) || undefined;
  const priorityParam = (resolvedParams.priority as PriorityLevel) || undefined;
  const sortParam = (resolvedParams.sort as IssueDiscoverySort) || 'NEWEST';
  const pageParam = parseInt(resolvedParams.page || '0', 10);
  const currentPage = isNaN(pageParam) ? 0 : Math.max(0, pageParam);

  // Load categories
  const categories = await getCategories();

  // If categoryParam matches a category slug, find its UUID
  let categoryId: string | undefined = undefined;
  if (categoryParam) {
    const matched = categories.find(
      (c) => c.slug === categoryParam || c.id === categoryParam
    );
    categoryId = matched ? matched.id : categoryParam;
  }

  // Fetch paginated issues from backend
  let issuesResponse;
  let hasError = false;
  try {
    issuesResponse = await getIssues({
      q,
      categoryId,
      status: statusParam,
      priority: priorityParam,
      sort: sortParam,
      page: currentPage,
      size: 20,
    });
  } catch {
    hasError = true;
  }

  const issues = issuesResponse?.content || [];
  const totalPages = issuesResponse?.totalPages || 1;
  const totalElements = issuesResponse?.totalElements || 0;

  // Build pagination links
  const buildPageUrl = (targetPage: number) => {
    const params = new URLSearchParams();
    if (q) params.set('q', q);
    if (categoryParam) params.set('category', categoryParam);
    if (statusParam) params.set('status', statusParam);
    if (priorityParam) params.set('priority', priorityParam);
    if (sortParam && sortParam !== 'NEWEST') params.set('sort', sortParam);
    if (targetPage > 0) params.set('page', targetPage.toString());
    const qs = params.toString();
    return `/issues${qs ? `?${qs}` : ''}`;
  };

  return (
    <div className="container" style={{ padding: '2rem 1.25rem 4rem' }}>
      <Breadcrumbs
        items={[
          { label: 'Home', href: '/' },
          { label: 'Civic Issues' },
        ]}
      />

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1.75rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 style={{ fontSize: '2.25rem', fontWeight: 800, color: 'var(--color-primary-dark)', letterSpacing: '-0.02em', marginBottom: '0.5rem' }}>
            Civic Issues Discovery
          </h1>
          <p style={{ color: 'var(--color-text-secondary)', fontSize: '1.05rem' }}>
            Explore real public reports, filter by municipal department, and track verified resolution stages.
          </p>
        </div>
        <Link href="/map" className="civic-button civic-button-outline" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem' }}>
          🗺️ Switch to Map View
        </Link>
      </div>

      {/* Interactive Filters Bar */}
      <IssueFilters
        categories={categories}
        initialSearch={q}
        initialCategory={categoryParam}
        initialStatus={statusParam}
        initialPriority={priorityParam}
        initialSort={sortParam}
      />

      {/* Results Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '0.5rem' }}>
        <p style={{ fontSize: '0.95rem', color: 'var(--color-text-secondary)' }}>
          Showing <strong>{issues.length}</strong> of <strong>{totalElements}</strong> public {totalElements === 1 ? 'issue' : 'issues'}
          {q && <span> matching <em>&quot;{q}&quot;</em></span>}
        </p>
        <span style={{ fontSize: '0.85rem', color: 'var(--color-text-muted)' }}>
          Page {currentPage + 1} of {Math.max(1, totalPages)}
        </span>
      </div>

      {/* Issues Grid */}
      {hasError ? (
        <EmptyState
          title="Could not load issues"
          description="We were unable to contact the civic database. Please check your network or try again."
          actionLabel="Try Again"
          actionHref="/issues"
        />
      ) : issues.length === 0 ? (
        <EmptyState
          title="No issues found"
          description="No civic reports matched your search keyword or selected filters. Try broadening your criteria."
          actionLabel="Clear All Filters"
          actionHref="/issues"
        />
      ) : (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))',
            gap: '1.5rem',
            marginBottom: '3rem',
          }}
        >
          {issues.map((issue) => (
            <IssueCard key={issue.id} issue={issue} />
          ))}
        </div>
      )}

      {/* Pagination Controls */}
      {totalPages > 1 && (
        <nav aria-label="Issue pagination" style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '1rem', marginTop: '2rem' }}>
          {currentPage > 0 ? (
            <Link href={buildPageUrl(currentPage - 1)} className="btn btn-secondary btn-sm">
              ← Previous Page
            </Link>
          ) : (
            <span className="btn btn-secondary btn-sm" style={{ opacity: 0.5, cursor: 'not-allowed' }}>
              ← Previous Page
            </span>
          )}

          <span style={{ fontSize: '0.9rem', color: 'var(--color-text-secondary)', fontWeight: 600 }}>
            Page {currentPage + 1} of {totalPages}
          </span>

          {currentPage + 1 < totalPages ? (
            <Link href={buildPageUrl(currentPage + 1)} className="btn btn-secondary btn-sm">
              Next Page →
            </Link>
          ) : (
            <span className="btn btn-secondary btn-sm" style={{ opacity: 0.5, cursor: 'not-allowed' }}>
              Next Page →
            </span>
          )}
        </nav>
      )}
    </div>
  );
}

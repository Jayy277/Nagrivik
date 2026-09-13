'use client';

import React, { useState, useEffect } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { CategorySummary, IssueStatus, PriorityLevel, IssueDiscoverySort } from '../types/issue';

interface IssueFiltersProps {
  categories: CategorySummary[];
  initialSearch?: string;
  initialCategory?: string;
  initialStatus?: string;
  initialPriority?: string;
  initialSort?: string;
}

const STATUS_OPTIONS: { label: string; value: IssueStatus | '' }[] = [
  { label: 'All Statuses', value: '' },
  { label: 'Reported', value: 'REPORTED' },
  { label: 'Verified', value: 'VERIFIED' },
  { label: 'Acknowledged', value: 'ACKNOWLEDGED' },
  { label: 'In Progress', value: 'IN_PROGRESS' },
  { label: 'Resolved', value: 'RESOLVED' },
  { label: 'Citizen Verified', value: 'CITIZEN_VERIFIED' },
  { label: 'Not Fixed', value: 'NOT_FIXED' },
];

const PRIORITY_OPTIONS: { label: string; value: PriorityLevel | '' }[] = [
  { label: 'All Priorities', value: '' },
  { label: 'Low Priority', value: 'LOW' },
  { label: 'Medium Priority', value: 'MEDIUM' },
  { label: 'High Priority', value: 'HIGH' },
  { label: 'Critical Priority', value: 'CRITICAL' },
];

const SORT_OPTIONS: { label: string; value: IssueDiscoverySort }[] = [
  { label: 'Newest First', value: 'NEWEST' },
  { label: 'Oldest First', value: 'OLDEST' },
  { label: 'Highest Priority', value: 'PRIORITY' },
  { label: 'Most Supported', value: 'MOST_SUPPORTED' },
];

export function IssueFilters({
  categories,
  initialSearch = '',
  initialCategory = '',
  initialStatus = '',
  initialPriority = '',
  initialSort = 'NEWEST',
}: IssueFiltersProps) {
  const router = useRouter();
  const searchParams = useSearchParams();

  const [searchInput, setSearchInput] = useState(initialSearch);

  // Sync state if URL changes externally
  useEffect(() => {
    setSearchInput(searchParams.get('q') || '');
  }, [searchParams]);

  // Debounced search updating URL
  useEffect(() => {
    const handler = setTimeout(() => {
      const currentQ = searchParams.get('q') || '';
      if (searchInput.trim() !== currentQ) {
        updateFilter('q', searchInput.trim());
      }
    }, 400);

    return () => clearTimeout(handler);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchInput]);

  const updateFilter = (key: string, value: string) => {
    const params = new URLSearchParams(searchParams.toString());
    if (value) {
      params.set(key, value);
    } else {
      params.delete(key);
    }
    // Changing filters resets pagination to page 0
    params.delete('page');

    router.push(`/issues?${params.toString()}`);
  };

  const handleClearAll = () => {
    setSearchInput('');
    router.push('/issues');
  };

  const selectedCategory = searchParams.get('category') || '';
  const selectedStatus = searchParams.get('status') || '';
  const selectedPriority = searchParams.get('priority') || '';
  const selectedSort = searchParams.get('sort') || initialSort;

  const hasActiveFilters =
    Boolean(searchInput.trim()) ||
    Boolean(selectedCategory) ||
    Boolean(selectedStatus) ||
    Boolean(selectedPriority) ||
    (Boolean(selectedSort) && selectedSort !== 'NEWEST');

  return (
    <div
      style={{
        backgroundColor: 'var(--color-surface)',
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--radius-lg)',
        padding: '1.25rem',
        marginBottom: '2rem',
        boxShadow: 'var(--shadow-sm)',
      }}
    >
      {/* Search Input Row */}
      <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap', marginBottom: '1rem' }}>
        <div style={{ flex: '1 1 280px', position: 'relative' }}>
          <label htmlFor="search-input" className="sr-only" style={{ display: 'none' }}>
            Search civic issues
          </label>
          <input
            id="search-input"
            type="text"
            placeholder="Search issues by keyword (e.g. pothole, garbage, streetlight)..."
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            style={{
              width: '100%',
              padding: '0.625rem 2.5rem 0.625rem 0.85rem',
              borderRadius: 'var(--radius-md)',
              border: '1px solid var(--color-border)',
              backgroundColor: 'var(--color-bg)',
              color: 'var(--color-text)',
            }}
          />
          {searchInput && (
            <button
              onClick={() => setSearchInput('')}
              aria-label="Clear search query"
              style={{
                position: 'absolute',
                right: '10px',
                top: '50%',
                transform: 'translateY(-50%)',
                color: 'var(--color-text-muted)',
                padding: '4px',
              }}
            >
              ✕
            </button>
          )}
        </div>

        {hasActiveFilters && (
          <button
            onClick={handleClearAll}
            className="btn btn-secondary btn-sm"
            style={{ alignSelf: 'center' }}
          >
            Clear All Filters
          </button>
        )}
      </div>

      {/* Dropdown Filters Grid */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
          gap: '0.75rem',
        }}
      >
        {/* Category Filter */}
        <div>
          <label htmlFor="filter-category" style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--color-text-secondary)', marginBottom: '0.35rem', textTransform: 'uppercase', letterSpacing: '0.03em' }}>
            Category
          </label>
          <select
            id="filter-category"
            value={selectedCategory}
            onChange={(e) => updateFilter('category', e.target.value)}
            style={{
              width: '100%',
              padding: '0.5rem 0.75rem',
              borderRadius: 'var(--radius-md)',
              border: '1px solid var(--color-border)',
              backgroundColor: selectedCategory ? 'var(--color-primary-light)' : 'var(--color-bg)',
              color: 'var(--color-text)',
              fontSize: '0.9rem',
            }}
          >
            <option value="">All Categories</option>
            {categories.map((cat) => (
              <option key={cat.id} value={cat.slug || cat.id}>
                {cat.name}
              </option>
            ))}
          </select>
        </div>

        {/* Status Filter */}
        <div>
          <label htmlFor="filter-status" style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--color-text-secondary)', marginBottom: '0.35rem', textTransform: 'uppercase', letterSpacing: '0.03em' }}>
            Status
          </label>
          <select
            id="filter-status"
            value={selectedStatus}
            onChange={(e) => updateFilter('status', e.target.value)}
            style={{
              width: '100%',
              padding: '0.5rem 0.75rem',
              borderRadius: 'var(--radius-md)',
              border: '1px solid var(--color-border)',
              backgroundColor: selectedStatus ? 'var(--color-primary-light)' : 'var(--color-bg)',
              color: 'var(--color-text)',
              fontSize: '0.9rem',
            }}
          >
            {STATUS_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>

        {/* Priority Filter */}
        <div>
          <label htmlFor="filter-priority" style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--color-text-secondary)', marginBottom: '0.35rem', textTransform: 'uppercase', letterSpacing: '0.03em' }}>
            Priority
          </label>
          <select
            id="filter-priority"
            value={selectedPriority}
            onChange={(e) => updateFilter('priority', e.target.value)}
            style={{
              width: '100%',
              padding: '0.5rem 0.75rem',
              borderRadius: 'var(--radius-md)',
              border: '1px solid var(--color-border)',
              backgroundColor: selectedPriority ? 'var(--color-primary-light)' : 'var(--color-bg)',
              color: 'var(--color-text)',
              fontSize: '0.9rem',
            }}
          >
            {PRIORITY_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>

        {/* Sort Filter */}
        <div>
          <label htmlFor="filter-sort" style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--color-text-secondary)', marginBottom: '0.35rem', textTransform: 'uppercase', letterSpacing: '0.03em' }}>
            Sort By
          </label>
          <select
            id="filter-sort"
            value={selectedSort}
            onChange={(e) => updateFilter('sort', e.target.value)}
            style={{
              width: '100%',
              padding: '0.5rem 0.75rem',
              borderRadius: 'var(--radius-md)',
              border: '1px solid var(--color-border)',
              backgroundColor: selectedSort !== 'NEWEST' ? 'var(--color-primary-light)' : 'var(--color-bg)',
              color: 'var(--color-text)',
              fontSize: '0.9rem',
            }}
          >
            {SORT_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>
      </div>
    </div>
  );
}

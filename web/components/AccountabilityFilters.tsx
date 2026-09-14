'use client';

import React from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { CategoryBreakdownItem, WardBreakdownItem } from '../types/accountability';

interface AccountabilityFiltersProps {
  wards: WardBreakdownItem[];
  categories: CategoryBreakdownItem[];
  selectedWardId?: string;
  selectedCategoryId?: string;
  selectedRange?: string;
}

const RANGE_OPTIONS = [
  { label: 'Past 7 Days', value: '7d' },
  { label: 'Past 30 Days', value: '30d' },
  { label: 'Past 90 Days', value: '90d' },
  { label: 'All History', value: 'all' },
];

export function AccountabilityFilters({
  wards,
  categories,
  selectedWardId = '',
  selectedCategoryId = '',
  selectedRange = '30d',
}: AccountabilityFiltersProps) {
  const router = useRouter();
  const searchParams = useSearchParams();

  function updateFilter(key: string, value: string) {
    const params = new URLSearchParams(searchParams.toString());
    if (value && value !== 'all-wards' && value !== 'all-categories' && (key !== 'range' || value !== '30d')) {
      params.set(key, value);
    } else {
      params.delete(key);
    }
    const query = params.toString();
    router.push(`/accountability${query ? `?${query}` : ''}`);
  }

  function handleReset() {
    router.push('/accountability');
  }

  const hasActiveFilters = Boolean(selectedWardId || selectedCategoryId || (selectedRange && selectedRange !== '30d'));

  return (
    <div
      className="card"
      style={{
        padding: '1.25rem',
        marginBottom: '2rem',
        backgroundColor: '#ffffff',
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--radius-lg)',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '0.75rem', marginBottom: '1rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <span style={{ fontSize: '1.2rem' }}>🔍</span>
          <h2 style={{ fontSize: '1rem', fontWeight: 700, margin: 0, color: 'var(--color-text)' }}>
            Filter Accountability Data
          </h2>
        </div>
        {hasActiveFilters && (
          <button
            onClick={handleReset}
            className="btn btn-outline btn-sm"
            style={{ fontSize: '0.8rem', padding: '4px 10px' }}
          >
            Reset All Filters
          </button>
        )}
      </div>

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
          gap: '1rem',
          alignItems: 'flex-end',
        }}
      >
        {/* Ward Filter */}
        <div>
          <label
            htmlFor="accountability-ward-filter"
            style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--color-text-secondary)', marginBottom: '0.375rem' }}
          >
            Municipal Ward
          </label>
          <select
            id="accountability-ward-filter"
            value={selectedWardId}
            onChange={(e) => updateFilter('wardId', e.target.value)}
            className="form-input"
            style={{ width: '100%', padding: '0.5rem 0.75rem', fontSize: '0.875rem' }}
          >
            <option value="">All Authoritative Wards</option>
            {wards.map((w) => (
              <option key={w.wardId} value={w.wardId}>
                {w.wardNumber ? `Ward ${w.wardNumber} — ${w.name}` : w.name}
              </option>
            ))}
          </select>
        </div>

        {/* Category Filter */}
        <div>
          <label
            htmlFor="accountability-category-filter"
            style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--color-text-secondary)', marginBottom: '0.375rem' }}
          >
            Civic Category
          </label>
          <select
            id="accountability-category-filter"
            value={selectedCategoryId}
            onChange={(e) => updateFilter('categoryId', e.target.value)}
            className="form-input"
            style={{ width: '100%', padding: '0.5rem 0.75rem', fontSize: '0.875rem' }}
          >
            <option value="">All Civic Categories</option>
            {categories.map((c) => (
              <option key={c.categoryId} value={c.categoryId}>
                {c.name}
              </option>
            ))}
          </select>
        </div>

        {/* Time Window Buttons */}
        <div>
          <label
            style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--color-text-secondary)', marginBottom: '0.375rem' }}
          >
            Time Window
          </label>
          <div style={{ display: 'flex', gap: '0.375rem', flexWrap: 'wrap' }} role="group" aria-label="Date range filter">
            {RANGE_OPTIONS.map((opt) => {
              const isSelected = selectedRange === opt.value || (!selectedRange && opt.value === '30d');
              return (
                <button
                  key={opt.value}
                  type="button"
                  onClick={() => updateFilter('range', opt.value)}
                  style={{
                    padding: '0.45rem 0.75rem',
                    fontSize: '0.8rem',
                    fontWeight: isSelected ? 700 : 500,
                    borderRadius: 'var(--radius-md)',
                    border: isSelected ? '1px solid var(--color-primary)' : '1px solid var(--color-border)',
                    backgroundColor: isSelected ? 'var(--color-primary)' : 'var(--color-surface)',
                    color: isSelected ? '#ffffff' : 'var(--color-text-secondary)',
                    cursor: 'pointer',
                    transition: 'all 0.15s ease',
                  }}
                  aria-pressed={isSelected}
                >
                  {opt.label}
                </button>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}

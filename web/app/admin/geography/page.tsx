'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import {
  getGeographyOverview,
  validateWardBoundaries,
  triggerReResolution,
} from '@/lib/api/geography';
import {
  CivicGeographyOverview,
  BoundaryValidationSummary,
  ReResolveResponse,
} from '@/types/geography';

export default function CivicGeographyOverviewPage() {
  const [overview, setOverview] = useState<CivicGeographyOverview | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Boundary validation state
  const [isValidating, setIsValidating] = useState(false);
  const [validationResult, setValidationResult] = useState<BoundaryValidationSummary | null>(null);

  // Re-resolution state
  const [showReResolveModal, setShowReResolveModal] = useState(false);
  const [reResolveOnlyUnresolved, setReResolveOnlyUnresolved] = useState<boolean>(true);
  const [reResolveLimit, setReResolveLimit] = useState<number>(50);
  const [isReResolving, setIsReResolving] = useState(false);
  const [reResolveResult, setReResolveResult] = useState<ReResolveResponse | null>(null);

  useEffect(() => {
    let mounted = true;
    async function loadData() {
      try {
        setIsLoading(true);
        const data = await getGeographyOverview();
        if (mounted) {
          setOverview(data);
          setError(null);
        }
      } catch (err: any) {
        if (mounted) {
          setError(err.message || 'Unable to load civic geography overview.');
        }
      } finally {
        if (mounted) setIsLoading(false);
      }
    }
    loadData();
    return () => {
      mounted = false;
    };
  }, []);

  const handleValidateBoundaries = async () => {
    try {
      setIsValidating(true);
      const res = await validateWardBoundaries();
      setValidationResult(res);
    } catch (err: any) {
      alert('Boundary validation failed: ' + (err.message || String(err)));
    } finally {
      setIsValidating(false);
    }
  };

  const handleExecuteReResolution = async () => {
    try {
      setIsReResolving(true);
      const res = await triggerReResolution({
        onlyUnresolved: reResolveOnlyUnresolved,
        limit: Number(reResolveLimit) || 50,
      });
      setReResolveResult(res);
      // Refresh overview
      const updated = await getGeographyOverview();
      setOverview(updated);
    } catch (err: any) {
      alert('Re-resolution failed: ' + (err.message || String(err)));
    } finally {
      setIsReResolving(false);
    }
  };

  return (
    <div>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '32px', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px' }}>
            <span style={{ fontSize: '12px', fontWeight: '700', padding: '2px 8px', borderRadius: '4px', background: '#e0e7ff', color: '#3730a3', textTransform: 'uppercase' }}>
              ADMIN ONLY
            </span>
            <h1 style={{ fontSize: '28px', fontWeight: '800', color: '#0f172a', letterSpacing: '-0.02em', margin: 0 }}>
              Civic Geography & Responsibility
            </h1>
          </div>
          <p style={{ color: '#64748b', fontSize: '15px', margin: 0 }}>
            Inspect and manage authoritative civic bodies, cities, wards, departments, and routing rules.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '12px' }}>
          <button
            onClick={handleValidateBoundaries}
            disabled={isValidating}
            style={{
              padding: '10px 18px',
              background: '#ffffff',
              color: '#1e40af',
              border: '1px solid #cbd5e1',
              borderRadius: '8px',
              fontWeight: '600',
              fontSize: '13px',
              cursor: isValidating ? 'not-allowed' : 'pointer',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
            }}
          >
            {isValidating ? 'Validating Geometry...' : '✓ Validate Ward Boundaries'}
          </button>
          <button
            onClick={() => {
              setReResolveResult(null);
              setShowReResolveModal(true);
            }}
            style={{
              padding: '10px 18px',
              background: '#1e40af',
              color: '#ffffff',
              border: 'none',
              borderRadius: '8px',
              fontWeight: '600',
              fontSize: '13px',
              cursor: 'pointer',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
            }}
          >
            ⚡ Re-resolve Responsibility
          </button>
        </div>
      </div>

      {error && (
        <div style={{ padding: '16px', background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '8px', color: '#991b1b', marginBottom: '24px', fontSize: '14px' }}>
          {error}
        </div>
      )}

      {/* Validation Result Box */}
      {validationResult && (
        <div style={{ padding: '20px', background: '#f0fdf4', border: '1px solid #bbf7d0', borderRadius: '12px', marginBottom: '32px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
            <h3 style={{ margin: 0, fontSize: '16px', fontWeight: '700', color: '#166534' }}>
              Ward Boundary Geometry Validation Report
            </h3>
            <button
              onClick={() => setValidationResult(null)}
              style={{ background: 'transparent', border: 'none', color: '#166534', cursor: 'pointer', fontSize: '13px', fontWeight: '600' }}
            >
              Dismiss
            </button>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '16px', fontSize: '14px' }}>
            <div>
              <span style={{ color: '#64748b', display: 'block', fontSize: '12px' }}>Total Checked</span>
              <strong style={{ fontSize: '20px', color: '#0f172a' }}>{validationResult.totalWardsChecked}</strong>
            </div>
            <div>
              <span style={{ color: '#64748b', display: 'block', fontSize: '12px' }}>Valid MultiPolygons</span>
              <strong style={{ fontSize: '20px', color: '#15803d' }}>{validationResult.validBoundaries}</strong>
            </div>
            <div>
              <span style={{ color: '#64748b', display: 'block', fontSize: '12px' }}>Invalid Geometry</span>
              <strong style={{ fontSize: '20px', color: validationResult.invalidBoundaries > 0 ? '#b91c1c' : '#0f172a' }}>
                {validationResult.invalidBoundaries}
              </strong>
            </div>
            <div>
              <span style={{ color: '#64748b', display: 'block', fontSize: '12px' }}>Missing Geometry</span>
              <strong style={{ fontSize: '20px', color: validationResult.missingBoundaries > 0 ? '#d97706' : '#0f172a' }}>
                {validationResult.missingBoundaries}
              </strong>
            </div>
          </div>
          {validationResult.warnings && validationResult.warnings.length > 0 && (
            <div style={{ marginTop: '16px', paddingTop: '12px', borderTop: '1px solid #dcfce7' }}>
              <span style={{ fontSize: '12px', fontWeight: '700', color: '#b91c1c', display: 'block', marginBottom: '6px' }}>
                Warnings & Anomalies:
              </span>
              <ul style={{ margin: 0, paddingLeft: '20px', fontSize: '13px', color: '#374151' }}>
                {validationResult.warnings.map((w, idx) => (
                  <li key={idx}>{w}</li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}

      {/* Metrics Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '20px', marginBottom: '32px' }}>
        <div style={{ background: '#ffffff', padding: '20px', borderRadius: '12px', border: '1px solid #e2e8f0' }}>
          <span style={{ fontSize: '12px', fontWeight: '700', color: '#64748b', textTransform: 'uppercase' }}>Civic Bodies</span>
          <div style={{ fontSize: '32px', fontWeight: '800', color: '#0f172a', margin: '6px 0' }}>
            {isLoading ? '...' : overview?.totalCivicBodies ?? 0}
          </div>
          <div style={{ fontSize: '12px', color: '#64748b', marginBottom: '8px' }}>
            {isLoading ? '' : `${overview?.activeCivicBodies ?? 0} active`}
          </div>
          <Link href="/admin/geography/civic-bodies" style={{ fontSize: '13px', color: '#1e40af', fontWeight: '600', textDecoration: 'none' }}>
            Manage civic bodies →
          </Link>
        </div>

        <div style={{ background: '#ffffff', padding: '20px', borderRadius: '12px', border: '1px solid #e2e8f0' }}>
          <span style={{ fontSize: '12px', fontWeight: '700', color: '#64748b', textTransform: 'uppercase' }}>Cities</span>
          <div style={{ fontSize: '32px', fontWeight: '800', color: '#0f172a', margin: '6px 0' }}>
            {isLoading ? '...' : overview?.totalCities ?? 0}
          </div>
          <div style={{ fontSize: '12px', color: '#64748b', marginBottom: '8px' }}>
            {isLoading ? '' : `${overview?.activeCities ?? 0} active`}
          </div>
          <Link href="/admin/geography/cities" style={{ fontSize: '13px', color: '#1e40af', fontWeight: '600', textDecoration: 'none' }}>
            Manage cities →
          </Link>
        </div>

        <div style={{ background: '#ffffff', padding: '20px', borderRadius: '12px', border: '1px solid #e2e8f0' }}>
          <span style={{ fontSize: '12px', fontWeight: '700', color: '#64748b', textTransform: 'uppercase' }}>Wards</span>
          <div style={{ fontSize: '32px', fontWeight: '800', color: '#0f172a', margin: '6px 0' }}>
            {isLoading ? '...' : overview?.totalWards ?? 0}
          </div>
          <div style={{ fontSize: '12px', color: '#16a34a', marginBottom: '8px' }}>
            {isLoading ? '' : `${overview?.wardsWithBoundaries ?? 0} with boundaries`}
          </div>
          <Link href="/admin/geography/wards" style={{ fontSize: '13px', color: '#1e40af', fontWeight: '600', textDecoration: 'none' }}>
            Inspect & manage wards →
          </Link>
        </div>

        <div style={{ background: '#ffffff', padding: '20px', borderRadius: '12px', border: '1px solid #e2e8f0' }}>
          <span style={{ fontSize: '12px', fontWeight: '700', color: '#64748b', textTransform: 'uppercase' }}>Departments</span>
          <div style={{ fontSize: '32px', fontWeight: '800', color: '#0f172a', margin: '6px 0' }}>
            {isLoading ? '...' : overview?.totalDepartments ?? 0}
          </div>
          <div style={{ fontSize: '12px', color: '#64748b', marginBottom: '8px' }}>
            {isLoading ? '' : `${overview?.activeDepartments ?? 0} active`}
          </div>
          <Link href="/admin/geography/departments" style={{ fontSize: '13px', color: '#1e40af', fontWeight: '600', textDecoration: 'none' }}>
            Manage departments →
          </Link>
        </div>

        <div style={{ background: '#ffffff', padding: '20px', borderRadius: '12px', border: '1px solid #e2e8f0' }}>
          <span style={{ fontSize: '12px', fontWeight: '700', color: '#64748b', textTransform: 'uppercase' }}>Category Mappings</span>
          <div style={{ fontSize: '32px', fontWeight: '800', color: '#0f172a', margin: '6px 0' }}>
            {isLoading ? '...' : overview?.totalCategoryMappings ?? 0}
          </div>
          <div style={{ fontSize: '12px', color: '#64748b', marginBottom: '8px' }}>
            {isLoading ? '' : `${overview?.activeCategoryMappings ?? 0} active`}
          </div>
          <Link href="/admin/geography/mappings" style={{ fontSize: '13px', color: '#1e40af', fontWeight: '600', textDecoration: 'none' }}>
            Configure routing →
          </Link>
        </div>

        <div style={{ background: '#ffffff', padding: '20px', borderRadius: '12px', border: '1px solid #e2e8f0' }}>
          <span style={{ fontSize: '12px', fontWeight: '700', color: '#b91c1c', textTransform: 'uppercase' }}>Unresolved Issues</span>
          <div style={{ fontSize: '32px', fontWeight: '800', color: '#b91c1c', margin: '6px 0' }}>
            {isLoading ? '...' : overview?.unresolvedIssues ?? 0}
          </div>
          <div style={{ fontSize: '12px', color: '#64748b', marginBottom: '8px' }}>
            Pending routing resolution
          </div>
          <button
            onClick={() => {
              setReResolveOnlyUnresolved(true);
              setShowReResolveModal(true);
            }}
            style={{ fontSize: '13px', color: '#1e40af', fontWeight: '600', background: 'transparent', border: 'none', padding: 0, cursor: 'pointer' }}
          >
            Re-resolve unresolved →
          </button>
        </div>
      </div>

      {/* Navigation Sub-cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '20px', marginBottom: '40px' }}>
        <div style={{ background: '#ffffff', padding: '24px', borderRadius: '12px', border: '1px solid #e2e8f0' }}>
          <h3 style={{ fontSize: '18px', fontWeight: '700', color: '#0f172a', marginBottom: '8px' }}>
            Wards & Boundaries
          </h3>
          <p style={{ fontSize: '14px', color: '#64748b', lineHeight: '1.6', marginBottom: '16px' }}>
            Inspect official AMC ward boundaries, codes, and names. Safe active/inactive toggles with concurrency protection.
          </p>
          <Link
            href="/admin/geography/wards"
            style={{ display: 'inline-block', padding: '8px 16px', background: '#f1f5f9', color: '#1e293b', borderRadius: '6px', textDecoration: 'none', fontWeight: '600', fontSize: '13px' }}
          >
            Open Ward Management →
          </Link>
        </div>

        <div style={{ background: '#ffffff', padding: '24px', borderRadius: '12px', border: '1px solid #e2e8f0' }}>
          <h3 style={{ fontSize: '18px', fontWeight: '700', color: '#0f172a', marginBottom: '8px' }}>
            Departments
          </h3>
          <p style={{ fontSize: '14px', color: '#64748b', lineHeight: '1.6', marginBottom: '16px' }}>
            Maintain authoritative municipal departments. Guard against accidental deletion if referenced by historical issues.
          </p>
          <Link
            href="/admin/geography/departments"
            style={{ display: 'inline-block', padding: '8px 16px', background: '#f1f5f9', color: '#1e293b', borderRadius: '6px', textDecoration: 'none', fontWeight: '600', fontSize: '13px' }}
          >
            Open Department Management →
          </Link>
        </div>

        <div style={{ background: '#ffffff', padding: '24px', borderRadius: '12px', border: '1px solid #e2e8f0' }}>
          <h3 style={{ fontSize: '18px', fontWeight: '700', color: '#0f172a', marginBottom: '8px' }}>
            Responsibility Mappings
          </h3>
          <p style={{ fontSize: '14px', color: '#64748b', lineHeight: '1.6', marginBottom: '16px' }}>
            Configure Category → Department and Ward → Department mappings to direct issue resolution to the correct department.
          </p>
          <Link
            href="/admin/geography/mappings"
            style={{ display: 'inline-block', padding: '8px 16px', background: '#f1f5f9', color: '#1e293b', borderRadius: '6px', textDecoration: 'none', fontWeight: '600', fontSize: '13px' }}
          >
            Open Mapping Rules →
          </Link>
        </div>

        <div style={{ background: '#ffffff', padding: '24px', borderRadius: '12px', border: '1px solid #e2e8f0' }}>
          <h3 style={{ fontSize: '18px', fontWeight: '700', color: '#0f172a', marginBottom: '8px' }}>
            Audit History
          </h3>
          <p style={{ fontSize: '14px', color: '#64748b', lineHeight: '1.6', marginBottom: '16px' }}>
            Review immutable, append-only records of all civic geography and responsibility modifications by administrators.
          </p>
          <Link
            href="/admin/geography/audits"
            style={{ display: 'inline-block', padding: '8px 16px', background: '#f1f5f9', color: '#1e293b', borderRadius: '6px', textDecoration: 'none', fontWeight: '600', fontSize: '13px' }}
          >
            View Audit Log →
          </Link>
        </div>
      </div>

      {/* Re-resolution Modal */}
      {showReResolveModal && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 50 }}>
          <div style={{ background: '#ffffff', borderRadius: '12px', padding: '24px', width: '100%', maxWidth: '480px', boxShadow: '0 20px 25px -5px rgba(0,0,0,0.1)' }}>
            <h2 style={{ fontSize: '18px', fontWeight: '700', color: '#0f172a', marginBottom: '8px' }}>
              ⚡ Bounded Responsibility Re-Resolution
            </h2>
            <p style={{ fontSize: '13px', color: '#64748b', lineHeight: '1.5', marginBottom: '16px' }}>
              Safely triggers the responsibility resolver for existing citizen issues. Re-resolving does NOT alter issue workflow status, priority, or reporter data.
            </p>

            <div style={{ marginBottom: '14px' }}>
              <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13px', fontWeight: '600', color: '#374151', cursor: 'pointer' }}>
                <input
                  type="checkbox"
                  checked={reResolveOnlyUnresolved}
                  onChange={(e) => setReResolveOnlyUnresolved(e.target.checked)}
                />
                Target Only UNRESOLVED Issues (Recommended)
              </label>
            </div>

            <div style={{ marginBottom: '20px' }}>
              <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#374151', marginBottom: '4px' }}>
                Bounded Batch Limit (max 100):
              </label>
              <input
                type="number"
                min="1"
                max="100"
                value={reResolveLimit}
                onChange={(e) => setReResolveLimit(Number(e.target.value))}
                style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '14px' }}
              />
            </div>

            {reResolveResult && (
              <div style={{ padding: '12px', background: '#f0fdf4', border: '1px solid #bbf7d0', borderRadius: '6px', marginBottom: '16px', fontSize: '13px' }}>
                <strong style={{ display: 'block', color: '#166534', marginBottom: '4px' }}>Batch Finished:</strong>
                <div>Processed: {reResolveResult.totalProcessed}</div>
                <div>Resolved: {reResolveResult.resolvedCount}</div>
                <div>Unresolved: {reResolveResult.unresolvedCount}</div>
                <div>Failed: {reResolveResult.failureCount}</div>
              </div>
            )}

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
              <button
                type="button"
                onClick={() => setShowReResolveModal(false)}
                style={{ padding: '8px 16px', background: '#f1f5f9', color: '#475569', border: '1px solid #cbd5e1', borderRadius: '6px', fontWeight: '600', fontSize: '13px', cursor: 'pointer' }}
              >
                Close
              </button>
              <button
                type="button"
                onClick={handleExecuteReResolution}
                disabled={isReResolving}
                style={{ padding: '8px 16px', background: '#1e40af', color: '#ffffff', border: 'none', borderRadius: '6px', fontWeight: '600', fontSize: '13px', cursor: isReResolving ? 'not-allowed' : 'pointer' }}
              >
                {isReResolving ? 'Processing...' : 'Run Re-Resolution'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

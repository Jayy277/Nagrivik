'use client';

import React, { useEffect, useState, useTransition } from 'react';
import Link from 'next/link';
import {
  getWards,
  toggleWardActive,
  updateWard,
  createWard,
  getCities,
  ConcurrencyConflictError,
} from '@/lib/api/geography';
import { WardResponse, CityResponse } from '@/types/geography';
import { PagedResponse } from '@/types/api';

export default function AdminWardsPage() {
  const [wardsData, setWardsData] = useState<PagedResponse<WardResponse>>({
    content: [],
    page: 0,
    size: 15,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
  });
  const [cities, setCities] = useState<CityResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filters
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCity, setSelectedCity] = useState('');
  const [activeFilter, setActiveFilter] = useState<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL');
  const [page, setPage] = useState(0);

  // Modal / Edit state
  const [editingWard, setEditingWard] = useState<WardResponse | null>(null);
  const [isCreating, setIsCreating] = useState(false);
  const [formData, setFormData] = useState({
    wardNumber: '',
    wardName: '',
    wardCode: '',
    cityId: '',
    source: '',
    sourceUrl: '',
  });
  const [formError, setFormError] = useState<string | null>(null);
  const [isPending, startTransition] = useTransition();

  const loadData = async () => {
    try {
      setIsLoading(true);
      setError(null);

      const [wards, citiesList] = await Promise.all([
        getWards({
          query: searchQuery || undefined,
          cityId: selectedCity || undefined,
          active: activeFilter === 'ALL' ? undefined : activeFilter === 'ACTIVE',
          page,
          size: 15,
        }),
        getCities(),
      ]);

      setWardsData(wards);
      setCities(citiesList);
    } catch (err: any) {
      setError(err.message || 'Failed to load ward data.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [searchQuery, selectedCity, activeFilter, page]);

  const handleToggleActive = async (ward: WardResponse) => {
    const action = ward.isActive ? 'deactivate' : 'activate';
    if (!confirm(`Are you sure you want to ${action} Ward "${ward.wardName}" (${ward.wardNumber || '—'})?`)) {
      return;
    }

    try {
      await toggleWardActive(ward.id, !ward.isActive, ward.version);
      await loadData();
    } catch (err: any) {
      if (err instanceof ConcurrencyConflictError) {
        alert(err.message);
        await loadData();
      } else {
        alert(`Failed to ${action} ward: ` + (err.message || String(err)));
      }
    }
  };

  const handleOpenEdit = (ward: WardResponse) => {
    setEditingWard(ward);
    setIsCreating(false);
    setFormData({
      wardNumber: ward.wardNumber || '',
      wardName: ward.wardName,
      wardCode: ward.wardCode || '',
      cityId: ward.cityId || '',
      source: ward.source || '',
      sourceUrl: ward.sourceUrl || '',
    });
    setFormError(null);
  };

  const handleOpenCreate = () => {
    setEditingWard(null);
    setIsCreating(true);
    setFormData({
      wardNumber: '',
      wardName: '',
      wardCode: '',
      cityId: cities[0]?.id || '',
      source: 'Authoritative AMC',
      sourceUrl: 'https://ahmedabadcity.gov.in',
    });
    setFormError(null);
  };

  const handleSubmitForm = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    startTransition(async () => {
      try {
        if (isCreating) {
          await createWard({
            cityId: formData.cityId,
            wardNumber: formData.wardNumber || undefined,
            wardName: formData.wardName,
            wardCode: formData.wardCode || undefined,
            source: formData.source,
            sourceUrl: formData.sourceUrl,
          });
        } else if (editingWard) {
          await updateWard(editingWard.id, {
            wardName: formData.wardName,
            wardNumber: formData.wardNumber || undefined,
            wardCode: formData.wardCode || undefined,
            version: editingWard.version,
            source: formData.source,
            sourceUrl: formData.sourceUrl,
          });
        }
        setEditingWard(null);
        setIsCreating(false);
        await loadData();
      } catch (err: any) {
        if (err instanceof ConcurrencyConflictError) {
          setFormError('Conflict: This ward was modified by another administrator. Please close and reload.');
        } else {
          setFormError(err.message || 'Operation failed');
        }
      }
    });
  };

  return (
    <div>
      {/* Breadcrumb */}
      <div style={{ marginBottom: '16px', fontSize: '13px', color: '#64748b' }}>
        <Link href="/admin/geography" style={{ color: '#1e40af', textDecoration: 'none' }}>
          Civic Geography
        </Link>{' '}
        / <span style={{ color: '#0f172a', fontWeight: '600' }}>Wards</span>
      </div>

      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '24px', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h1 style={{ fontSize: '24px', fontWeight: '800', color: '#0f172a', margin: 0, letterSpacing: '-0.02em' }}>
            Ward Management
          </h1>
          <p style={{ color: '#64748b', fontSize: '14px', margin: '4px 0 0 0' }}>
            Authoritative municipal wards, codes, boundaries, and provenance metadata.
          </p>
        </div>
        <button
          onClick={handleOpenCreate}
          style={{
            padding: '10px 18px',
            background: '#1e40af',
            color: '#ffffff',
            border: 'none',
            borderRadius: '8px',
            fontWeight: '600',
            fontSize: '13px',
            cursor: 'pointer',
          }}
        >
          + Add Authoritative Ward
        </button>
      </div>

      {/* Filter Bar */}
      <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap', marginBottom: '20px', background: '#ffffff', padding: '16px', borderRadius: '10px', border: '1px solid #e2e8f0' }}>
        <div style={{ flex: 1, minWidth: '220px' }}>
          <input
            type="text"
            placeholder="Search by ward name or code..."
            value={searchQuery}
            onChange={(e) => {
              setSearchQuery(e.target.value);
              setPage(0);
            }}
            style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '13px' }}
          />
        </div>

        <div>
          <select
            value={selectedCity}
            onChange={(e) => {
              setSelectedCity(e.target.value);
              setPage(0);
            }}
            style={{ padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '13px' }}
          >
            <option value="">All Cities</option>
            {cities.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </div>

        <div>
          <select
            value={activeFilter}
            onChange={(e) => {
              setActiveFilter(e.target.value as any);
              setPage(0);
            }}
            style={{ padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '13px' }}
          >
            <option value="ALL">All Statuses</option>
            <option value="ACTIVE">Active Only</option>
            <option value="INACTIVE">Inactive Only</option>
          </select>
        </div>
      </div>

      {error && (
        <div style={{ padding: '14px', background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '8px', color: '#991b1b', marginBottom: '20px', fontSize: '13px' }}>
          {error}
        </div>
      )}

      {/* Table */}
      <div style={{ background: '#ffffff', borderRadius: '10px', border: '1px solid #e2e8f0', overflow: 'hidden' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
          <thead>
            <tr style={{ background: '#f8fafc', borderBottom: '1px solid #e2e8f0', color: '#475569', fontWeight: '600' }}>
              <th style={{ padding: '12px 16px' }}>Ward Code / No.</th>
              <th style={{ padding: '12px 16px' }}>Ward Name</th>
              <th style={{ padding: '12px 16px' }}>City</th>
              <th style={{ padding: '12px 16px' }}>Boundary</th>
              <th style={{ padding: '12px 16px' }}>Status</th>
              <th style={{ padding: '12px 16px' }}>Provenance</th>
              <th style={{ padding: '12px 16px', textAlign: 'right' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr>
                <td colSpan={7} style={{ padding: '32px', textAlign: 'center', color: '#64748b' }}>
                  Loading wards...
                </td>
              </tr>
            ) : !wardsData?.content || wardsData.content.length === 0 ? (
              <tr>
                <td colSpan={7} style={{ padding: '32px', textAlign: 'center', color: '#64748b' }}>
                  No wards match your filters.
                </td>
              </tr>
            ) : (
              wardsData.content.map((ward: WardResponse) => (
                <tr key={ward.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                  <td style={{ padding: '12px 16px', fontWeight: '700', color: '#0f172a' }}>
                    {ward.wardNumber || ward.wardCode || '—'}
                  </td>
                  <td style={{ padding: '12px 16px', fontWeight: '600', color: '#1e293b' }}>
                    {ward.wardName}
                  </td>
                  <td style={{ padding: '12px 16px', color: '#475569' }}>
                    {ward.cityName || '—'}
                  </td>
                  <td style={{ padding: '12px 16px' }}>
                    {ward.hasBoundary ? (
                      <span style={{ fontSize: '11px', fontWeight: '700', padding: '3px 8px', borderRadius: '9999px', background: '#dcfce7', color: '#15803d' }}>
                        Polygon OK
                      </span>
                    ) : (
                      <span style={{ fontSize: '11px', fontWeight: '700', padding: '3px 8px', borderRadius: '9999px', background: '#fef3c7', color: '#b45309' }}>
                        Missing
                      </span>
                    )}
                  </td>
                  <td style={{ padding: '12px 16px' }}>
                    {ward.isActive ? (
                      <span style={{ fontSize: '11px', fontWeight: '700', padding: '3px 8px', borderRadius: '9999px', background: '#e0f2fe', color: '#0369a1' }}>
                        ACTIVE
                      </span>
                    ) : (
                      <span style={{ fontSize: '11px', fontWeight: '700', padding: '3px 8px', borderRadius: '9999px', background: '#fee2e2', color: '#b91c1c' }}>
                        INACTIVE
                      </span>
                    )}
                  </td>
                  <td style={{ padding: '12px 16px', color: '#64748b', fontSize: '12px' }}>
                    {ward.source || 'Authoritative AMC'}
                  </td>
                  <td style={{ padding: '12px 16px', textAlign: 'right' }}>
                    <div style={{ display: 'inline-flex', gap: '8px' }}>
                      <button
                        onClick={() => handleOpenEdit(ward)}
                        style={{ padding: '5px 10px', background: '#f1f5f9', border: '1px solid #cbd5e1', borderRadius: '4px', fontSize: '12px', cursor: 'pointer' }}
                      >
                        Edit
                      </button>
                      <button
                        onClick={() => handleToggleActive(ward)}
                        style={{
                          padding: '5px 10px',
                          background: ward.isActive ? '#fee2e2' : '#dcfce7',
                          color: ward.isActive ? '#b91c1c' : '#15803d',
                          border: 'none',
                          borderRadius: '4px',
                          fontSize: '12px',
                          cursor: 'pointer',
                          fontWeight: '600',
                        }}
                      >
                        {ward.isActive ? 'Deactivate' : 'Activate'}
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>

        {/* Pagination Footer */}
        {wardsData && wardsData.totalPages > 1 && (
          <div style={{ padding: '12px 16px', background: '#f8fafc', borderTop: '1px solid #e2e8f0', display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '13px', color: '#64748b' }}>
            <span>
              Showing Page {wardsData.page + 1} of {wardsData.totalPages} ({wardsData.totalElements} total wards)
            </span>
            <div style={{ display: 'flex', gap: '6px' }}>
              <button
                disabled={wardsData.page === 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                style={{ padding: '4px 10px', borderRadius: '4px', border: '1px solid #cbd5e1', background: '#ffffff', cursor: wardsData.page === 0 ? 'not-allowed' : 'pointer' }}
              >
                Previous
              </button>
              <button
                disabled={wardsData.page >= wardsData.totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
                style={{ padding: '4px 10px', borderRadius: '4px', border: '1px solid #cbd5e1', background: '#ffffff', cursor: wardsData.page >= wardsData.totalPages - 1 ? 'not-allowed' : 'pointer' }}
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Edit / Create Modal */}
      {(editingWard || isCreating) && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 50 }}>
          <div style={{ background: '#ffffff', borderRadius: '12px', padding: '24px', width: '100%', maxWidth: '500px', boxShadow: '0 20px 25px -5px rgba(0,0,0,0.1)' }}>
            <h2 style={{ fontSize: '18px', fontWeight: '700', color: '#0f172a', marginBottom: '16px' }}>
              {isCreating ? 'Create Authoritative Ward' : `Edit Ward: ${editingWard?.wardName}`}
            </h2>

            {formError && (
              <div style={{ padding: '10px 14px', background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '6px', color: '#991b1b', marginBottom: '14px', fontSize: '13px' }}>
                {formError}
              </div>
            )}

            <form onSubmit={handleSubmitForm}>
              {isCreating && (
                <div style={{ marginBottom: '12px' }}>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#374151', marginBottom: '4px' }}>
                    City:
                  </label>
                  <select
                    value={formData.cityId}
                    onChange={(e) => setFormData({ ...formData, cityId: e.target.value })}
                    style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '13px' }}
                    required
                  >
                    {cities.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name}
                      </option>
                    ))}
                  </select>
                </div>
              )}

              <div style={{ marginBottom: '12px' }}>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#374151', marginBottom: '4px' }}>
                  Official Ward Number:
                </label>
                <input
                  type="text"
                  value={formData.wardNumber}
                  onChange={(e) => setFormData({ ...formData, wardNumber: e.target.value })}
                  placeholder="e.g. 1, 48"
                  style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '13px' }}
                />
              </div>

              <div style={{ marginBottom: '12px' }}>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#374151', marginBottom: '4px' }}>
                  Ward Code:
                </label>
                <input
                  type="text"
                  value={formData.wardCode}
                  onChange={(e) => setFormData({ ...formData, wardCode: e.target.value })}
                  placeholder="e.g. W-01, W-48"
                  style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '13px' }}
                />
              </div>

              <div style={{ marginBottom: '12px' }}>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#374151', marginBottom: '4px' }}>
                  Ward Name:
                </label>
                <input
                  type="text"
                  value={formData.wardName}
                  onChange={(e) => setFormData({ ...formData, wardName: e.target.value })}
                  placeholder="Official ward name"
                  style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '13px' }}
                  required
                />
              </div>

              <div style={{ marginBottom: '16px' }}>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#374151', marginBottom: '4px' }}>
                  Source / Provenance:
                </label>
                <input
                  type="text"
                  value={formData.source}
                  onChange={(e) => setFormData({ ...formData, source: e.target.value })}
                  style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '13px' }}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
                <button
                  type="button"
                  onClick={() => {
                    setEditingWard(null);
                    setIsCreating(false);
                  }}
                  style={{ padding: '8px 16px', background: '#f1f5f9', color: '#475569', border: '1px solid #cbd5e1', borderRadius: '6px', fontWeight: '600', fontSize: '13px', cursor: 'pointer' }}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isPending}
                  style={{ padding: '8px 16px', background: '#1e40af', color: '#ffffff', border: 'none', borderRadius: '6px', fontWeight: '600', fontSize: '13px', cursor: isPending ? 'not-allowed' : 'pointer' }}
                >
                  {isPending ? 'Saving...' : 'Save Ward'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

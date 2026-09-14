'use client';

import React, { useEffect, useState, useTransition } from 'react';
import Link from 'next/link';
import {
  getCities,
  getCivicBodies,
  createCity,
  updateCity,
  toggleCityActive,
  deleteCity,
  ConcurrencyConflictError,
} from '@/lib/api/geography';
import { CityResponse, CivicBodyResponse } from '@/types/geography';

export default function AdminCitiesPage() {
  const [cities, setCities] = useState<CityResponse[]>([]);
  const [civicBodies, setCivicBodies] = useState<CivicBodyResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filters
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCivicBodyId, setSelectedCivicBodyId] = useState<string>('ALL');
  const [activeFilter, setActiveFilter] = useState<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL');

  // Modal states
  const [editingCity, setEditingCity] = useState<CityResponse | null>(null);
  const [isCreatingCity, setIsCreatingCity] = useState(false);
  const [cityForm, setCityForm] = useState({
    civicBodyId: '',
    name: '',
    state: 'Gujarat',
    countryCode: 'IN',
    source: 'Authoritative AMC',
    sourceUrl: 'https://ahmedabadcity.gov.in',
  });
  const [modalError, setModalError] = useState<string | null>(null);
  const [isPending, startTransition] = useTransition();

  const loadData = async () => {
    try {
      setIsLoading(true);
      setError(null);
      const [citiesList, bodiesList] = await Promise.all([
        getCities(),
        getCivicBodies(),
      ]);
      setCities(citiesList);
      setCivicBodies(bodiesList);
    } catch (err: any) {
      setError(err.message || 'Failed to load cities.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleToggleActive = async (city: CityResponse) => {
    const action = city.isActive ? 'deactivate' : 'activate';
    if (!confirm(`Are you sure you want to ${action} City "${city.name}"?`)) return;

    try {
      await toggleCityActive(city.id, !city.isActive, city.version);
      await loadData();
    } catch (err: any) {
      if (err instanceof ConcurrencyConflictError) {
        alert(err.message);
        await loadData();
      } else {
        alert(`Failed to ${action} city: ` + (err.message || String(err)));
      }
    }
  };

  const handleDeleteCity = async (city: CityResponse) => {
    if (!confirm(`Are you sure you want to delete City "${city.name}"? If referenced by historical issues or wards, deletion will be rejected.`)) return;

    try {
      await deleteCity(city.id, city.version);
      await loadData();
    } catch (err: any) {
      if (err instanceof ConcurrencyConflictError) {
        alert(err.message);
        await loadData();
      } else {
        alert('Cannot delete city: ' + (err.message || String(err)));
      }
    }
  };

  const handleSaveCity = (e: React.FormEvent) => {
    e.preventDefault();
    setModalError(null);

    if (!cityForm.name.trim()) {
      setModalError('City name is required.');
      return;
    }
    if (!cityForm.state.trim()) {
      setModalError('State name is required.');
      return;
    }

    startTransition(async () => {
      try {
        if (editingCity) {
          await updateCity(editingCity.id, {
            name: cityForm.name.trim(),
            state: cityForm.state.trim(),
            countryCode: cityForm.countryCode.trim(),
            civicBodyId: cityForm.civicBodyId || undefined,
            source: cityForm.source.trim() || undefined,
            sourceUrl: cityForm.sourceUrl.trim() || undefined,
            version: editingCity.version,
          });
        } else {
          await createCity({
            name: cityForm.name.trim(),
            state: cityForm.state.trim(),
            countryCode: cityForm.countryCode.trim(),
            civicBodyId: cityForm.civicBodyId || undefined,
            source: cityForm.source.trim() || undefined,
            sourceUrl: cityForm.sourceUrl.trim() || undefined,
          });
        }
        setEditingCity(null);
        setIsCreatingCity(false);
        await loadData();
      } catch (err: any) {
        if (err instanceof ConcurrencyConflictError) {
          setModalError(err.message);
          await loadData();
        } else {
          setModalError(err.message || 'Operation failed.');
        }
      }
    });
  };

  const openCreateModal = () => {
    setCityForm({
      civicBodyId: civicBodies.length > 0 ? civicBodies[0].id : '',
      name: '',
      state: 'Gujarat',
      countryCode: 'IN',
      source: 'Authoritative AMC',
      sourceUrl: 'https://ahmedabadcity.gov.in',
    });
    setEditingCity(null);
    setModalError(null);
    setIsCreatingCity(true);
  };

  const openEditModal = (c: CityResponse) => {
    setCityForm({
      civicBodyId: c.civicBodyId || '',
      name: c.name,
      state: c.state,
      countryCode: c.countryCode || 'IN',
      source: c.source || '',
      sourceUrl: c.sourceUrl || '',
    });
    setEditingCity(c);
    setModalError(null);
    setIsCreatingCity(false);
  };

  const filteredCities = cities.filter((c) => {
    if (selectedCivicBodyId !== 'ALL' && c.civicBodyId !== selectedCivicBodyId) {
      return false;
    }
    if (activeFilter === 'ACTIVE' && !c.isActive) return false;
    if (activeFilter === 'INACTIVE' && c.isActive) return false;
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      return (
        c.name.toLowerCase().includes(q) ||
        c.state.toLowerCase().includes(q) ||
        (c.civicBodyName && c.civicBodyName.toLowerCase().includes(q))
      );
    }
    return true;
  });

  return (
    <div>
      {/* Breadcrumb / Top Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '24px', flexWrap: 'wrap', gap: '12px' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13px', color: '#64748b', marginBottom: '4px' }}>
            <Link href="/admin/geography" style={{ color: '#1e40af', textDecoration: 'none' }}>Civic Geography</Link>
            <span>/</span>
            <span>Cities</span>
          </div>
          <h1 style={{ fontSize: '24px', fontWeight: '800', color: '#0f172a', margin: 0 }}>
            City Management
          </h1>
        </div>

        <div style={{ display: 'flex', gap: '10px' }}>
          <Link
            href="/admin/geography/civic-bodies"
            style={{
              padding: '8px 16px',
              background: '#f8fafc',
              border: '1px solid #cbd5e1',
              borderRadius: '8px',
              color: '#475569',
              fontWeight: '600',
              fontSize: '13px',
              textDecoration: 'none',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
            }}
          >
            Manage Civic Bodies →
          </Link>
          <button
            onClick={openCreateModal}
            style={{
              padding: '8px 16px',
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
            + Create City
          </button>
        </div>
      </div>

      {error && (
        <div style={{ padding: '14px', background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '8px', color: '#991b1b', marginBottom: '20px', fontSize: '14px' }}>
          {error}
        </div>
      )}

      {/* Filter Bar */}
      <div style={{ background: '#ffffff', border: '1px solid #e2e8f0', borderRadius: '12px', padding: '16px', marginBottom: '20px', display: 'flex', gap: '16px', flexWrap: 'wrap', alignItems: 'center' }}>
        <div style={{ flex: '1', minWidth: '240px' }}>
          <input
            type="text"
            placeholder="Search by city or state name..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{
              width: '100%',
              padding: '8px 12px',
              border: '1px solid #cbd5e1',
              borderRadius: '6px',
              fontSize: '14px',
              outline: 'none',
            }}
          />
        </div>

        <div>
          <select
            value={selectedCivicBodyId}
            onChange={(e) => setSelectedCivicBodyId(e.target.value)}
            style={{
              padding: '8px 12px',
              border: '1px solid #cbd5e1',
              borderRadius: '6px',
              fontSize: '14px',
              background: '#ffffff',
            }}
          >
            <option value="ALL">All Civic Bodies</option>
            {civicBodies.map((b) => (
              <option key={b.id} value={b.id}>
                {b.name}
              </option>
            ))}
          </select>
        </div>

        <div>
          <select
            value={activeFilter}
            onChange={(e) => setActiveFilter(e.target.value as any)}
            style={{
              padding: '8px 12px',
              border: '1px solid #cbd5e1',
              borderRadius: '6px',
              fontSize: '14px',
              background: '#ffffff',
            }}
          >
            <option value="ALL">All Statuses</option>
            <option value="ACTIVE">Active Only</option>
            <option value="INACTIVE">Inactive Only</option>
          </select>
        </div>

        <button
          onClick={loadData}
          style={{
            padding: '8px 14px',
            background: '#f1f5f9',
            border: '1px solid #cbd5e1',
            borderRadius: '6px',
            fontSize: '13px',
            fontWeight: '600',
            color: '#475569',
            cursor: 'pointer',
          }}
        >
          ↻ Refresh
        </button>
      </div>

      {/* Cities Table */}
      <div style={{ background: '#ffffff', border: '1px solid #e2e8f0', borderRadius: '12px', overflow: 'hidden' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '14px' }}>
          <thead>
            <tr style={{ background: '#f8fafc', borderBottom: '1px solid #e2e8f0', color: '#64748b', fontSize: '12px', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              <th style={{ padding: '14px 20px' }}>City Name</th>
              <th style={{ padding: '14px 20px' }}>State / Country</th>
              <th style={{ padding: '14px 20px' }}>Governing Civic Body</th>
              <th style={{ padding: '14px 20px' }}>Provenance & Source</th>
              <th style={{ padding: '14px 20px' }}>Status</th>
              <th style={{ padding: '14px 20px', textAlign: 'right' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr>
                <td colSpan={6} style={{ padding: '40px', textAlign: 'center', color: '#64748b' }}>
                  Loading cities...
                </td>
              </tr>
            ) : filteredCities.length === 0 ? (
              <tr>
                <td colSpan={6} style={{ padding: '40px', textAlign: 'center', color: '#64748b' }}>
                  No cities found matching the search criteria.
                </td>
              </tr>
            ) : (
              filteredCities.map((city) => (
                <tr key={city.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                  <td style={{ padding: '14px 20px', fontWeight: '600', color: '#0f172a' }}>
                    {city.name}
                    <div style={{ fontSize: '11px', color: '#94a3b8', fontFamily: 'monospace' }}>v{city.version} • {city.id}</div>
                  </td>
                  <td style={{ padding: '14px 20px', color: '#475569' }}>
                    {city.state}, {city.countryCode || 'IN'}
                  </td>
                  <td style={{ padding: '14px 20px', color: '#334155' }}>
                    {city.civicBodyName || <span style={{ color: '#94a3b8', fontStyle: 'italic' }}>Unassigned</span>}
                  </td>
                  <td style={{ padding: '14px 20px', color: '#64748b', fontSize: '13px' }}>
                    <div>{city.source || 'Standard Municipal'}</div>
                    {city.sourceUrl && (
                      <a href={city.sourceUrl} target="_blank" rel="noreferrer" style={{ fontSize: '11px', color: '#1e40af', textDecoration: 'none' }}>
                        Source Link ↗
                      </a>
                    )}
                  </td>
                  <td style={{ padding: '14px 20px' }}>
                    <span
                      style={{
                        padding: '3px 8px',
                        borderRadius: '4px',
                        fontSize: '12px',
                        fontWeight: '600',
                        background: city.isActive ? '#dcfce7' : '#fee2e2',
                        color: city.isActive ? '#15803d' : '#991b1b',
                      }}
                    >
                      {city.isActive ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td style={{ padding: '14px 20px', textAlign: 'right' }}>
                    <div style={{ display: 'inline-flex', gap: '8px' }}>
                      <button
                        onClick={() => openEditModal(city)}
                        style={{
                          padding: '5px 10px',
                          background: '#ffffff',
                          border: '1px solid #cbd5e1',
                          borderRadius: '6px',
                          fontSize: '12px',
                          color: '#334155',
                          cursor: 'pointer',
                          fontWeight: '600',
                        }}
                      >
                        Edit
                      </button>
                      <button
                        onClick={() => handleToggleActive(city)}
                        style={{
                          padding: '5px 10px',
                          background: city.isActive ? '#fef2f2' : '#f0fdf4',
                          border: `1px solid ${city.isActive ? '#fecaca' : '#bbf7d0'}`,
                          borderRadius: '6px',
                          fontSize: '12px',
                          color: city.isActive ? '#dc2626' : '#16a34a',
                          cursor: 'pointer',
                          fontWeight: '600',
                        }}
                      >
                        {city.isActive ? 'Deactivate' : 'Activate'}
                      </button>
                      <button
                        onClick={() => handleDeleteCity(city)}
                        style={{
                          padding: '5px 10px',
                          background: '#ffffff',
                          border: '1px solid #fecaca',
                          borderRadius: '6px',
                          fontSize: '12px',
                          color: '#991b1b',
                          cursor: 'pointer',
                          fontWeight: '600',
                        }}
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Create / Edit City Modal */}
      {(isCreatingCity || editingCity) && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(15, 23, 42, 0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000, padding: '20px' }}>
          <div style={{ background: '#ffffff', borderRadius: '12px', width: '100%', maxWidth: '520px', padding: '24px', boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1)' }}>
            <h2 style={{ fontSize: '18px', fontWeight: '700', color: '#0f172a', marginBottom: '16px' }}>
              {editingCity ? `Edit City: ${editingCity.name}` : 'Create New City'}
            </h2>

            {modalError && (
              <div style={{ padding: '10px 14px', background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '6px', color: '#991b1b', fontSize: '13px', marginBottom: '16px' }}>
                {modalError}
              </div>
            )}

            <form onSubmit={handleSaveCity} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#334155', marginBottom: '4px' }}>
                  City Name *
                </label>
                <input
                  type="text"
                  required
                  value={cityForm.name}
                  onChange={(e) => setCityForm({ ...cityForm, name: e.target.value })}
                  placeholder="e.g. Ahmedabad"
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: '6px', fontSize: '14px' }}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#334155', marginBottom: '4px' }}>
                    State *
                  </label>
                  <input
                    type="text"
                    required
                    value={cityForm.state}
                    onChange={(e) => setCityForm({ ...cityForm, state: e.target.value })}
                    placeholder="Gujarat"
                    style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: '6px', fontSize: '14px' }}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#334155', marginBottom: '4px' }}>
                    Country Code
                  </label>
                  <input
                    type="text"
                    value={cityForm.countryCode}
                    onChange={(e) => setCityForm({ ...cityForm, countryCode: e.target.value })}
                    placeholder="IN"
                    style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: '6px', fontSize: '14px' }}
                  />
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#334155', marginBottom: '4px' }}>
                  Governing Civic Body
                </label>
                <select
                  value={cityForm.civicBodyId}
                  onChange={(e) => setCityForm({ ...cityForm, civicBodyId: e.target.value })}
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: '6px', fontSize: '14px', background: '#ffffff' }}
                >
                  <option value="">-- No Governing Civic Body --</option>
                  {civicBodies.map((b) => (
                    <option key={b.id} value={b.id}>
                      {b.name} ({b.state})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#334155', marginBottom: '4px' }}>
                  Authoritative Source / Provenance
                </label>
                <input
                  type="text"
                  value={cityForm.source}
                  onChange={(e) => setCityForm({ ...cityForm, source: e.target.value })}
                  placeholder="e.g. Ahmedabad Municipal Corporation Official Portal"
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: '6px', fontSize: '14px' }}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#334155', marginBottom: '4px' }}>
                  Source URL
                </label>
                <input
                  type="url"
                  value={cityForm.sourceUrl}
                  onChange={(e) => setCityForm({ ...cityForm, sourceUrl: e.target.value })}
                  placeholder="https://..."
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: '6px', fontSize: '14px' }}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '12px' }}>
                <button
                  type="button"
                  onClick={() => {
                    setEditingCity(null);
                    setIsCreatingCity(false);
                  }}
                  style={{
                    padding: '8px 16px',
                    background: '#f1f5f9',
                    border: '1px solid #cbd5e1',
                    borderRadius: '6px',
                    fontSize: '13px',
                    fontWeight: '600',
                    color: '#475569',
                    cursor: 'pointer',
                  }}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isPending}
                  style={{
                    padding: '8px 18px',
                    background: '#1e40af',
                    border: 'none',
                    borderRadius: '6px',
                    fontSize: '13px',
                    fontWeight: '600',
                    color: '#ffffff',
                    cursor: isPending ? 'not-allowed' : 'pointer',
                  }}
                >
                  {isPending ? 'Saving...' : editingCity ? 'Update City' : 'Create City'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

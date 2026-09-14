'use client';

import React, { useEffect, useState, useTransition } from 'react';
import Link from 'next/link';
import {
  getCivicBodies,
  createCivicBody,
  updateCivicBody,
  toggleCivicBodyActive,
  getCities,
  createCity,
  updateCity,
  toggleCityActive,
  ConcurrencyConflictError,
} from '@/lib/api/geography';
import { CivicBodyResponse, CityResponse, CivicBodyType } from '@/types/geography';

export default function AdminCivicBodiesAndCitiesPage() {
  const [civicBodies, setCivicBodies] = useState<CivicBodyResponse[]>([]);
  const [cities, setCities] = useState<CityResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Civic Body modal
  const [editingBody, setEditingBody] = useState<CivicBodyResponse | null>(null);
  const [isCreatingBody, setIsCreatingBody] = useState(false);
  const [bodyForm, setBodyForm] = useState({
    name: '',
    type: 'MUNICIPAL_CORPORATION' as CivicBodyType,
    state: 'Gujarat',
    city: 'Ahmedabad',
    officialWebsite: 'https://ahmedabadcity.gov.in',
    source: 'Authoritative AMC',
  });

  // City modal
  const [editingCity, setEditingCity] = useState<CityResponse | null>(null);
  const [isCreatingCity, setIsCreatingCity] = useState(false);
  const [cityForm, setCityForm] = useState({
    civicBodyId: '',
    name: '',
    state: 'Gujarat',
    countryCode: 'IN',
    source: 'Authoritative AMC',
  });

  const [modalError, setModalError] = useState<string | null>(null);
  const [isPending, startTransition] = useTransition();

  const loadData = async () => {
    try {
      setIsLoading(true);
      setError(null);
      const [bodies, citiesList] = await Promise.all([getCivicBodies(), getCities()]);
      setCivicBodies(bodies);
      setCities(citiesList);
    } catch (err: any) {
      setError(err.message || 'Failed to load civic bodies and cities.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleToggleBodyActive = async (b: CivicBodyResponse) => {
    const action = b.isActive ? 'deactivate' : 'activate';
    if (!confirm(`Are you sure you want to ${action} Civic Body "${b.name}"?`)) return;
    try {
      await toggleCivicBodyActive(b.id, !b.isActive, b.version);
      await loadData();
    } catch (err: any) {
      if (err instanceof ConcurrencyConflictError) {
        alert(err.message);
        await loadData();
      } else {
        alert(`Failed to ${action} civic body: ` + (err.message || String(err)));
      }
    }
  };

  const handleToggleCityActive = async (c: CityResponse) => {
    const action = c.isActive ? 'deactivate' : 'activate';
    if (!confirm(`Are you sure you want to ${action} City "${c.name}"?`)) return;
    try {
      await toggleCityActive(c.id, !c.isActive, c.version);
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

  const handleSaveBody = (e: React.FormEvent) => {
    e.preventDefault();
    setModalError(null);
    startTransition(async () => {
      try {
        if (isCreatingBody) {
          await createCivicBody(bodyForm);
        } else if (editingBody) {
          await updateCivicBody(editingBody.id, {
            name: bodyForm.name,
            type: bodyForm.type,
            state: bodyForm.state,
            city: bodyForm.city,
            officialWebsite: bodyForm.officialWebsite || undefined,
            version: editingBody.version,
            source: bodyForm.source,
          });
        }
        setEditingBody(null);
        setIsCreatingBody(false);
        await loadData();
      } catch (err: any) {
        setModalError(err.message || 'Failed to save civic body');
      }
    });
  };

  const handleSaveCity = (e: React.FormEvent) => {
    e.preventDefault();
    setModalError(null);
    startTransition(async () => {
      try {
        if (isCreatingCity) {
          await createCity(cityForm);
        } else if (editingCity) {
          await updateCity(editingCity.id, {
            name: cityForm.name,
            state: cityForm.state,
            countryCode: cityForm.countryCode,
            version: editingCity.version,
            source: cityForm.source,
          });
        }
        setEditingCity(null);
        setIsCreatingCity(false);
        await loadData();
      } catch (err: any) {
        setModalError(err.message || 'Failed to save city');
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
        / <span style={{ color: '#0f172a', fontWeight: '600' }}>Civic Bodies & Cities</span>
      </div>

      <div style={{ marginBottom: '24px' }}>
        <h1 style={{ fontSize: '24px', fontWeight: '800', color: '#0f172a', margin: 0, letterSpacing: '-0.02em' }}>
          Civic Bodies & Cities Management
        </h1>
        <p style={{ color: '#64748b', fontSize: '14px', margin: '4px 0 0 0' }}>
          Inspect and manage municipal corporations and supported administrative cities.
        </p>
      </div>

      {error && (
        <div style={{ padding: '14px', background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '8px', color: '#991b1b', marginBottom: '20px', fontSize: '13px' }}>
          {error}
        </div>
      )}

      {/* Civic Bodies Section */}
      <div style={{ background: '#ffffff', borderRadius: '10px', border: '1px solid #e2e8f0', padding: '20px', marginBottom: '32px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <h2 style={{ fontSize: '18px', fontWeight: '700', color: '#0f172a', margin: 0 }}>
            Civic Bodies ({civicBodies.length})
          </h2>
          <button
            onClick={() => {
              setIsCreatingBody(true);
              setEditingBody(null);
              setBodyForm({ name: '', type: 'MUNICIPAL_CORPORATION', state: 'Gujarat', city: 'Ahmedabad', officialWebsite: 'https://ahmedabadcity.gov.in', source: 'Authoritative AMC' });
              setModalError(null);
            }}
            style={{ padding: '8px 14px', background: '#1e40af', color: '#ffffff', border: 'none', borderRadius: '6px', fontWeight: '600', fontSize: '12px', cursor: 'pointer' }}
          >
            + Add Civic Body
          </button>
        </div>

        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
          <thead>
            <tr style={{ background: '#f8fafc', borderBottom: '1px solid #e2e8f0', color: '#475569', fontWeight: '600' }}>
              <th style={{ padding: '10px 12px' }}>Name</th>
              <th style={{ padding: '10px 12px' }}>Type</th>
              <th style={{ padding: '10px 12px' }}>City / State</th>
              <th style={{ padding: '10px 12px' }}>Status</th>
              <th style={{ padding: '10px 12px' }}>Provenance</th>
              <th style={{ padding: '10px 12px', textAlign: 'right' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {civicBodies.map((b) => (
              <tr key={b.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                <td style={{ padding: '10px 12px', color: '#1e293b', fontWeight: '700' }}>{b.name}</td>
                <td style={{ padding: '10px 12px', color: '#64748b' }}>{b.type}</td>
                <td style={{ padding: '10px 12px', color: '#475569' }}>{b.city}, {b.state}</td>
                <td style={{ padding: '10px 12px' }}>
                  <span style={{ fontSize: '11px', fontWeight: '700', padding: '2px 6px', borderRadius: '9999px', background: b.isActive ? '#e0f2fe' : '#fee2e2', color: b.isActive ? '#0369a1' : '#b91c1c' }}>
                    {b.isActive ? 'ACTIVE' : 'INACTIVE'}
                  </span>
                </td>
                <td style={{ padding: '10px 12px', color: '#64748b', fontSize: '12px' }}>{b.source || 'Authoritative AMC'}</td>
                <td style={{ padding: '10px 12px', textAlign: 'right' }}>
                  <button
                    onClick={() => {
                      setEditingBody(b);
                      setIsCreatingBody(false);
                      setBodyForm({
                        name: b.name,
                        type: b.type,
                        state: b.state,
                        city: b.city,
                        officialWebsite: b.officialWebsite || '',
                        source: b.source || '',
                      });
                      setModalError(null);
                    }}
                    style={{ padding: '4px 8px', background: '#f1f5f9', border: '1px solid #cbd5e1', borderRadius: '4px', fontSize: '11px', cursor: 'pointer', marginRight: '6px' }}
                  >
                    Edit
                  </button>
                  <button
                    onClick={() => handleToggleBodyActive(b)}
                    style={{ padding: '4px 8px', background: b.isActive ? '#fee2e2' : '#dcfce7', color: b.isActive ? '#b91c1c' : '#15803d', border: 'none', borderRadius: '4px', fontSize: '11px', cursor: 'pointer', fontWeight: '600' }}
                  >
                    {b.isActive ? 'Deactivate' : 'Activate'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Cities Section */}
      <div style={{ background: '#ffffff', borderRadius: '10px', border: '1px solid #e2e8f0', padding: '20px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <h2 style={{ fontSize: '18px', fontWeight: '700', color: '#0f172a', margin: 0 }}>
            Cities ({cities.length})
          </h2>
          <button
            onClick={() => {
              setIsCreatingCity(true);
              setEditingCity(null);
              setCityForm({ civicBodyId: civicBodies[0]?.id || '', name: '', state: 'Gujarat', countryCode: 'IN', source: 'Authoritative AMC' });
              setModalError(null);
            }}
            style={{ padding: '8px 14px', background: '#1e40af', color: '#ffffff', border: 'none', borderRadius: '6px', fontWeight: '600', fontSize: '12px', cursor: 'pointer' }}
          >
            + Add City
          </button>
        </div>

        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
          <thead>
            <tr style={{ background: '#f8fafc', borderBottom: '1px solid #e2e8f0', color: '#475569', fontWeight: '600' }}>
              <th style={{ padding: '10px 12px' }}>City Name</th>
              <th style={{ padding: '10px 12px' }}>Civic Body</th>
              <th style={{ padding: '10px 12px' }}>State / Country</th>
              <th style={{ padding: '10px 12px' }}>Status</th>
              <th style={{ padding: '10px 12px', textAlign: 'right' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {cities.map((c) => (
              <tr key={c.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                <td style={{ padding: '10px 12px', fontWeight: '700', color: '#0f172a' }}>{c.name}</td>
                <td style={{ padding: '10px 12px', color: '#1e293b' }}>{c.civicBodyName || '—'}</td>
                <td style={{ padding: '10px 12px', color: '#475569' }}>{c.state}, {c.countryCode}</td>
                <td style={{ padding: '10px 12px' }}>
                  <span style={{ fontSize: '11px', fontWeight: '700', padding: '2px 6px', borderRadius: '9999px', background: c.isActive ? '#e0f2fe' : '#fee2e2', color: c.isActive ? '#0369a1' : '#b91c1c' }}>
                    {c.isActive ? 'ACTIVE' : 'INACTIVE'}
                  </span>
                </td>
                <td style={{ padding: '10px 12px', textAlign: 'right' }}>
                  <button
                    onClick={() => {
                      setEditingCity(c);
                      setIsCreatingCity(false);
                      setCityForm({
                        civicBodyId: c.civicBodyId || '',
                        name: c.name,
                        state: c.state,
                        countryCode: c.countryCode,
                        source: c.source || '',
                      });
                      setModalError(null);
                    }}
                    style={{ padding: '4px 8px', background: '#f1f5f9', border: '1px solid #cbd5e1', borderRadius: '4px', fontSize: '11px', cursor: 'pointer', marginRight: '6px' }}
                  >
                    Edit
                  </button>
                  <button
                    onClick={() => handleToggleCityActive(c)}
                    style={{ padding: '4px 8px', background: c.isActive ? '#fee2e2' : '#dcfce7', color: c.isActive ? '#b91c1c' : '#15803d', border: 'none', borderRadius: '4px', fontSize: '11px', cursor: 'pointer', fontWeight: '600' }}
                  >
                    {c.isActive ? 'Deactivate' : 'Activate'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Body Modal */}
      {(editingBody || isCreatingBody) && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 50 }}>
          <div style={{ background: '#ffffff', borderRadius: '12px', padding: '24px', width: '100%', maxWidth: '480px', boxShadow: '0 20px 25px -5px rgba(0,0,0,0.1)' }}>
            <h2 style={{ fontSize: '18px', fontWeight: '700', color: '#0f172a', marginBottom: '16px' }}>
              {isCreatingBody ? 'Create Civic Body' : `Edit Civic Body: ${editingBody?.name}`}
            </h2>
            {modalError && (
              <div style={{ padding: '10px 14px', background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '6px', color: '#991b1b', marginBottom: '14px', fontSize: '13px' }}>
                {modalError}
              </div>
            )}
            <form onSubmit={handleSaveBody}>
              <div style={{ marginBottom: '12px' }}>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#374151', marginBottom: '4px' }}>Name:</label>
                <input
                  type="text"
                  value={bodyForm.name}
                  onChange={(e) => setBodyForm({ ...bodyForm, name: e.target.value })}
                  style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '13px' }}
                  required
                />
              </div>
              <div style={{ marginBottom: '12px' }}>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#374151', marginBottom: '4px' }}>Type:</label>
                <select
                  value={bodyForm.type}
                  onChange={(e) => setBodyForm({ ...bodyForm, type: e.target.value as CivicBodyType })}
                  style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '13px' }}
                  required
                >
                  <option value="MUNICIPAL_CORPORATION">Municipal Corporation</option>
                  <option value="MUNICIPALITY">Municipality</option>
                  <option value="GRAM_PANCHAYAT">Gram Panchayat</option>
                </select>
              </div>
              <div style={{ marginBottom: '12px' }}>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#374151', marginBottom: '4px' }}>City:</label>
                <input
                  type="text"
                  value={bodyForm.city}
                  onChange={(e) => setBodyForm({ ...bodyForm, city: e.target.value })}
                  style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '13px' }}
                  required
                />
              </div>
              <div style={{ marginBottom: '12px' }}>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#374151', marginBottom: '4px' }}>State:</label>
                <input
                  type="text"
                  value={bodyForm.state}
                  onChange={(e) => setBodyForm({ ...bodyForm, state: e.target.value })}
                  style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '13px' }}
                  required
                />
              </div>
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '16px' }}>
                <button
                  type="button"
                  onClick={() => { setEditingBody(null); setIsCreatingBody(false); }}
                  style={{ padding: '8px 16px', background: '#f1f5f9', color: '#475569', border: '1px solid #cbd5e1', borderRadius: '6px', fontWeight: '600', fontSize: '13px', cursor: 'pointer' }}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isPending}
                  style={{ padding: '8px 16px', background: '#1e40af', color: '#ffffff', border: 'none', borderRadius: '6px', fontWeight: '600', fontSize: '13px', cursor: isPending ? 'not-allowed' : 'pointer' }}
                >
                  {isPending ? 'Saving...' : 'Save'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* City Modal */}
      {(editingCity || isCreatingCity) && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 50 }}>
          <div style={{ background: '#ffffff', borderRadius: '12px', padding: '24px', width: '100%', maxWidth: '480px', boxShadow: '0 20px 25px -5px rgba(0,0,0,0.1)' }}>
            <h2 style={{ fontSize: '18px', fontWeight: '700', color: '#0f172a', marginBottom: '16px' }}>
              {isCreatingCity ? 'Create City' : `Edit City: ${editingCity?.name}`}
            </h2>
            {modalError && (
              <div style={{ padding: '10px 14px', background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '6px', color: '#991b1b', marginBottom: '14px', fontSize: '13px' }}>
                {modalError}
              </div>
            )}
            <form onSubmit={handleSaveCity}>
              {isCreatingCity && (
                <div style={{ marginBottom: '12px' }}>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#374151', marginBottom: '4px' }}>Civic Body:</label>
                  <select
                    value={cityForm.civicBodyId}
                    onChange={(e) => setCityForm({ ...cityForm, civicBodyId: e.target.value })}
                    style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '13px' }}
                    required
                  >
                    {civicBodies.map((b) => (
                      <option key={b.id} value={b.id}>
                        {b.name}
                      </option>
                    ))}
                  </select>
                </div>
              )}
              <div style={{ marginBottom: '12px' }}>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#374151', marginBottom: '4px' }}>City Name:</label>
                <input
                  type="text"
                  value={cityForm.name}
                  onChange={(e) => setCityForm({ ...cityForm, name: e.target.value })}
                  style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '13px' }}
                  required
                />
              </div>
              <div style={{ marginBottom: '12px' }}>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#374151', marginBottom: '4px' }}>State:</label>
                <input
                  type="text"
                  value={cityForm.state}
                  onChange={(e) => setCityForm({ ...cityForm, state: e.target.value })}
                  style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '13px' }}
                  required
                />
              </div>
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '16px' }}>
                <button
                  type="button"
                  onClick={() => { setEditingCity(null); setIsCreatingCity(false); }}
                  style={{ padding: '8px 16px', background: '#f1f5f9', color: '#475569', border: '1px solid #cbd5e1', borderRadius: '6px', fontWeight: '600', fontSize: '13px', cursor: 'pointer' }}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isPending}
                  style={{ padding: '8px 16px', background: '#1e40af', color: '#ffffff', border: 'none', borderRadius: '6px', fontWeight: '600', fontSize: '13px', cursor: isPending ? 'not-allowed' : 'pointer' }}
                >
                  {isPending ? 'Saving...' : 'Save'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

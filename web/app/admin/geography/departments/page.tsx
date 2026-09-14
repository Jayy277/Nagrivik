'use client';

import React, { useEffect, useState, useTransition } from 'react';
import Link from 'next/link';
import {
  getDepartments,
  toggleDepartmentActive,
  createDepartment,
  updateDepartment,
  getCivicBodies,
  ConcurrencyConflictError,
} from '@/lib/api/geography';
import { DepartmentResponse, CivicBodyResponse } from '@/types/geography';
import { PagedResponse } from '@/types/api';

export default function AdminDepartmentsPage() {
  const [deptsData, setDeptsData] = useState<PagedResponse<DepartmentResponse>>({
    content: [],
    page: 0,
    size: 15,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
  });
  const [civicBodies, setCivicBodies] = useState<CivicBodyResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filters
  const [selectedCivicBody, setSelectedCivicBody] = useState('');
  const [activeFilter, setActiveFilter] = useState<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL');
  const [page, setPage] = useState(0);

  // Modal / Create / Edit
  const [editingDept, setEditingDept] = useState<DepartmentResponse | null>(null);
  const [isCreating, setIsCreating] = useState(false);
  const [formData, setFormData] = useState({
    civicBodyId: '',
    name: '',
    code: '',
    description: '',
    source: 'Authoritative AMC',
    sourceUrl: 'https://ahmedabadcity.gov.in',
  });
  const [formError, setFormError] = useState<string | null>(null);
  const [isPending, startTransition] = useTransition();

  const loadData = async () => {
    try {
      setIsLoading(true);
      setError(null);

      const [depts, bodies] = await Promise.all([
        getDepartments({
          civicBodyId: selectedCivicBody || undefined,
          active: activeFilter === 'ALL' ? undefined : activeFilter === 'ACTIVE',
          page,
          size: 15,
        }),
        getCivicBodies(),
      ]);

      setDeptsData(depts);
      setCivicBodies(bodies);
    } catch (err: any) {
      setError(err.message || 'Failed to load department data.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [selectedCivicBody, activeFilter, page]);

  const handleToggleActive = async (dept: DepartmentResponse) => {
    const action = dept.isActive ? 'deactivate' : 'activate';
    if (!confirm(`Are you sure you want to ${action} Department "${dept.name}"?`)) {
      return;
    }

    try {
      await toggleDepartmentActive(dept.id, !dept.isActive, dept.version);
      await loadData();
    } catch (err: any) {
      if (err instanceof ConcurrencyConflictError) {
        alert(err.message);
        await loadData();
      } else {
        alert(`Failed to ${action} department: ` + (err.message || String(err)));
      }
    }
  };

  const handleOpenEdit = (dept: DepartmentResponse) => {
    setEditingDept(dept);
    setIsCreating(false);
    setFormData({
      civicBodyId: dept.civicBodyId || '',
      name: dept.name,
      code: dept.code || '',
      description: dept.description || '',
      source: dept.source || 'Authoritative AMC',
      sourceUrl: dept.sourceUrl || '',
    });
    setFormError(null);
  };

  const handleOpenCreate = () => {
    setEditingDept(null);
    setIsCreating(true);
    setFormData({
      civicBodyId: civicBodies[0]?.id || '',
      name: '',
      code: '',
      description: '',
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
          await createDepartment({
            civicBodyId: formData.civicBodyId,
            name: formData.name,
            code: formData.code || undefined,
            description: formData.description || undefined,
            source: formData.source,
            sourceUrl: formData.sourceUrl,
          });
        } else if (editingDept) {
          await updateDepartment(editingDept.id, {
            name: formData.name,
            code: formData.code || undefined,
            description: formData.description || undefined,
            version: editingDept.version,
            source: formData.source,
            sourceUrl: formData.sourceUrl,
          });
        }
        setEditingDept(null);
        setIsCreating(false);
        await loadData();
      } catch (err: any) {
        if (err instanceof ConcurrencyConflictError) {
          setFormError('Conflict: This department was modified by another administrator. Please close and reload.');
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
        / <span style={{ color: '#0f172a', fontWeight: '600' }}>Departments</span>
      </div>

      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '24px', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h1 style={{ fontSize: '24px', fontWeight: '800', color: '#0f172a', margin: 0, letterSpacing: '-0.02em' }}>
            Department Management
          </h1>
          <p style={{ color: '#64748b', fontSize: '14px', margin: '4px 0 0 0' }}>
            Authoritative municipal departments responsible for civic complaints and services.
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
          + Add Department
        </button>
      </div>

      {/* Filter Bar */}
      <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap', marginBottom: '20px', background: '#ffffff', padding: '16px', borderRadius: '10px', border: '1px solid #e2e8f0' }}>
        <div>
          <select
            value={selectedCivicBody}
            onChange={(e) => {
              setSelectedCivicBody(e.target.value);
              setPage(0);
            }}
            style={{ padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '13px' }}
          >
            <option value="">All Civic Bodies</option>
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
              <th style={{ padding: '12px 16px' }}>Code</th>
              <th style={{ padding: '12px 16px' }}>Department Name</th>
              <th style={{ padding: '12px 16px' }}>Civic Body</th>
              <th style={{ padding: '12px 16px' }}>Status</th>
              <th style={{ padding: '12px 16px' }}>Provenance</th>
              <th style={{ padding: '12px 16px', textAlign: 'right' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr>
                <td colSpan={6} style={{ padding: '32px', textAlign: 'center', color: '#64748b' }}>
                  Loading departments...
                </td>
              </tr>
            ) : !deptsData?.content || deptsData.content.length === 0 ? (
              <tr>
                <td colSpan={6} style={{ padding: '32px', textAlign: 'center', color: '#64748b' }}>
                  No departments found.
                </td>
              </tr>
            ) : (
              deptsData.content.map((dept: DepartmentResponse) => (
                <tr key={dept.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                  <td style={{ padding: '12px 16px', fontWeight: '700', color: '#0f172a' }}>
                    {dept.code || '—'}
                  </td>
                  <td style={{ padding: '12px 16px', fontWeight: '600', color: '#1e293b' }}>
                    {dept.name}
                  </td>
                  <td style={{ padding: '12px 16px', color: '#475569' }}>
                    {dept.civicBodyName || '—'}
                  </td>
                  <td style={{ padding: '12px 16px' }}>
                    {dept.isActive ? (
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
                    {dept.source || 'Authoritative AMC'}
                  </td>
                  <td style={{ padding: '12px 16px', textAlign: 'right' }}>
                    <div style={{ display: 'inline-flex', gap: '8px' }}>
                      <button
                        onClick={() => handleOpenEdit(dept)}
                        style={{ padding: '5px 10px', background: '#f1f5f9', border: '1px solid #cbd5e1', borderRadius: '4px', fontSize: '12px', cursor: 'pointer' }}
                      >
                        Edit
                      </button>
                      <button
                        onClick={() => handleToggleActive(dept)}
                        style={{
                          padding: '5px 10px',
                          background: dept.isActive ? '#fee2e2' : '#dcfce7',
                          color: dept.isActive ? '#b91c1c' : '#15803d',
                          border: 'none',
                          borderRadius: '4px',
                          fontSize: '12px',
                          cursor: 'pointer',
                          fontWeight: '600',
                        }}
                      >
                        {dept.isActive ? 'Deactivate' : 'Activate'}
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>

        {/* Pagination */}
        {deptsData && deptsData.totalPages > 1 && (
          <div style={{ padding: '12px 16px', background: '#f8fafc', borderTop: '1px solid #e2e8f0', display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '13px', color: '#64748b' }}>
            <span>
              Showing Page {deptsData.page + 1} of {deptsData.totalPages} ({deptsData.totalElements} total departments)
            </span>
            <div style={{ display: 'flex', gap: '6px' }}>
              <button
                disabled={deptsData.page === 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                style={{ padding: '4px 10px', borderRadius: '4px', border: '1px solid #cbd5e1', background: '#ffffff', cursor: deptsData.page === 0 ? 'not-allowed' : 'pointer' }}
              >
                Previous
              </button>
              <button
                disabled={deptsData.page >= deptsData.totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
                style={{ padding: '4px 10px', borderRadius: '4px', border: '1px solid #cbd5e1', background: '#ffffff', cursor: deptsData.page >= deptsData.totalPages - 1 ? 'not-allowed' : 'pointer' }}
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Edit / Create Modal */}
      {(editingDept || isCreating) && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 50 }}>
          <div style={{ background: '#ffffff', borderRadius: '12px', padding: '24px', width: '100%', maxWidth: '500px', boxShadow: '0 20px 25px -5px rgba(0,0,0,0.1)' }}>
            <h2 style={{ fontSize: '18px', fontWeight: '700', color: '#0f172a', marginBottom: '16px' }}>
              {isCreating ? 'Create Department' : `Edit Department: ${editingDept?.name}`}
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
                    Civic Body:
                  </label>
                  <select
                    value={formData.civicBodyId}
                    onChange={(e) => setFormData({ ...formData, civicBodyId: e.target.value })}
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
                <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#374151', marginBottom: '4px' }}>
                  Department Code:
                </label>
                <input
                  type="text"
                  value={formData.code}
                  onChange={(e) => setFormData({ ...formData, code: e.target.value })}
                  placeholder="e.g. SOLID_WASTE, HEALTH, ENGINEERING"
                  style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '13px' }}
                />
              </div>

              <div style={{ marginBottom: '12px' }}>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#374151', marginBottom: '4px' }}>
                  Department Name:
                </label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  placeholder="Official department name"
                  style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '13px' }}
                  required
                />
              </div>

              <div style={{ marginBottom: '12px' }}>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#374151', marginBottom: '4px' }}>
                  Description:
                </label>
                <textarea
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  rows={2}
                  style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '13px' }}
                />
              </div>

              <div style={{ marginBottom: '16px' }}>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#374151', marginBottom: '4px' }}>
                  Source:
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
                    setEditingDept(null);
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
                  {isPending ? 'Saving...' : 'Save Department'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

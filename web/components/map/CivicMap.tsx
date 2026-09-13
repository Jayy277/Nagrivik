'use client';

import React, { useEffect, useRef, useState, useCallback } from 'react';
import { IssueResponse } from '../../types/issue';
import { IssueMapPreview } from './IssueMapPreview';
import { MapLegend } from './MapLegend';
import { getDefaultCityConfig } from '../../lib/config/city';

interface CivicMapProps {
  issues: IssueResponse[];
  selectedIssue: IssueResponse | null;
  onSelectIssue: (issue: IssueResponse | null) => void;
  onViewportChange: (lat: number, lng: number, radiusMeters: number) => void;
  isLoading?: boolean;
  onSwitchToList?: () => void;
}

export function CivicMap({
  issues,
  selectedIssue,
  onSelectIssue,
  onViewportChange,
  isLoading,
  onSwitchToList,
}: CivicMapProps) {
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapInstanceRef = useRef<any>(null);
  const markersLayerRef = useRef<any>(null);
  const userPinLayerRef = useRef<any>(null);
  const debounceTimerRef = useRef<any>(null);

  const defaultCity = getDefaultCityConfig();
  const [mapReady, setMapReady] = useState(false);
  const [tileError, setTileError] = useState(false);
  const [userLocation, setUserLocation] = useState<{ lat: number; lng: number } | null>(null);
  const [showLegend, setShowLegend] = useState(false);

  // Category Icon helper
  const getCategoryIcon = (catName?: string): string => {
    if (!catName) return '📍';
    const lower = catName.toLowerCase();
    if (lower.includes('road') || lower.includes('pothole')) return '🚧';
    if (lower.includes('garbage') || lower.includes('waste')) return '🗑️';
    if (lower.includes('light')) return '💡';
    if (lower.includes('water')) return '🚰';
    if (lower.includes('drain')) return '🌊';
    return '⚠️';
  };

  // Priority color helper
  const getPriorityColor = (level?: string): string => {
    switch (level) {
      case 'CRITICAL':
        return '#DC2626';
      case 'HIGH':
        return '#EA580C';
      case 'MEDIUM':
        return '#D97706';
      case 'LOW':
        return '#16A34A';
      default:
        return '#2563EB';
    }
  };

  // Initialize Leaflet Map
  useEffect(() => {
    if (typeof window === 'undefined' || !mapContainerRef.current) return;

    let isMounted = true;

    // Dynamically import Leaflet to avoid SSR window errors
    import('leaflet').then((L) => {
      if (!isMounted || !mapContainerRef.current) return;

      // Clean up previous instance if any
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove();
        mapInstanceRef.current = null;
      }

      try {
        const initialLat = defaultCity.latitude;
        const initialLng = defaultCity.longitude;

        const map = L.map(mapContainerRef.current, {
          center: [initialLat, initialLng],
          zoom: defaultCity.defaultZoom,
          zoomControl: false,
          attributionControl: false,
        });

        // Add standard zoom control at top right
        L.control.zoom({ position: 'topright' }).addTo(map);

        // Standard OpenStreetMap Tile Layer
        const tileUrl =
          process.env.NEXT_PUBLIC_MAP_TILE_URL ||
          'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';

        const tileLayer = L.tileLayer(tileUrl, {
          maxZoom: 19,
          subdomains: ['a', 'b', 'c'],
          attribution: '&copy; OpenStreetMap contributors',
        });

        tileLayer.on('tileerror', () => {
          setTileError(true);
        });

        tileLayer.addTo(map);

        // Marker layer group
        const markersGroup = L.layerGroup().addTo(map);
        markersLayerRef.current = markersGroup;

        // User location pin layer
        const userGroup = L.layerGroup().addTo(map);
        userPinLayerRef.current = userGroup;

        // Debounced viewport movement listener
        map.on('moveend', () => {
          if (debounceTimerRef.current) {
            clearTimeout(debounceTimerRef.current);
          }

          debounceTimerRef.current = setTimeout(() => {
            const center = map.getCenter();
            const bounds = map.getBounds();
            // Calculate approximate radius from center to north-east boundary
            const ne = bounds.getNorthEast();
            const radiusMeters = Math.min(
              Math.round(center.distanceTo(ne)),
              50000 // Server maximum cap 50km
            );
            onViewportChange(center.lat, center.lng, radiusMeters);
          }, 400);
        });

        mapInstanceRef.current = map;
        setMapReady(true);
      } catch (err) {
        setTileError(true);
      }
    });

    return () => {
      isMounted = false;
      if (debounceTimerRef.current) clearTimeout(debounceTimerRef.current);
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove();
        mapInstanceRef.current = null;
      }
    };
  }, [defaultCity.latitude, defaultCity.longitude, defaultCity.defaultZoom]);

  // Update Markers when issues or selectedIssue change
  useEffect(() => {
    if (!mapReady || !mapInstanceRef.current || !markersLayerRef.current) return;

    import('leaflet').then((L) => {
      const markersGroup = markersLayerRef.current;
      markersGroup.clearLayers();

      const validIssues = issues.filter(
        (iss) =>
          iss.location &&
          typeof iss.location.latitude === 'number' &&
          typeof iss.location.longitude === 'number'
      );

      const map = mapInstanceRef.current;
      const zoom = map.getZoom();

      // Simple grid clustering: group points within ~0.015 degrees at lower zoom
      const clusterThreshold = zoom < 14 ? 0.008 : 0.002;
      const clusters: Array<{
        isCluster: boolean;
        count: number;
        lat: number;
        lng: number;
        issue?: IssueResponse;
        items: IssueResponse[];
      }> = [];

      const visited = new Set<string>();

      for (let i = 0; i < validIssues.length; i++) {
        const cur = validIssues[i];
        if (visited.has(cur.id)) continue;

        const group = [cur];
        visited.add(cur.id);

        for (let j = i + 1; j < validIssues.length; j++) {
          const cand = validIssues[j];
          if (visited.has(cand.id)) continue;

          const dLat = Math.abs(cur.location!.latitude! - cand.location!.latitude!);
          const dLng = Math.abs(cur.location!.longitude! - cand.location!.longitude!);
          if (dLat < clusterThreshold && dLng < clusterThreshold) {
            group.push(cand);
            visited.add(cand.id);
          }
        }

        if (group.length > 1) {
          const avgLat = group.reduce((sum, g) => sum + g.location!.latitude!, 0) / group.length;
          const avgLng = group.reduce((sum, g) => sum + g.location!.longitude!, 0) / group.length;
          clusters.push({
            isCluster: true,
            count: group.length,
            lat: avgLat,
            lng: avgLng,
            items: group,
          });
        } else {
          clusters.push({
            isCluster: false,
            count: 1,
            lat: cur.location!.latitude!,
            lng: cur.location!.longitude!,
            issue: cur,
            items: group,
          });
        }
      }

      // Render pins & clusters
      clusters.forEach((cluster) => {
        if (cluster.isCluster) {
          const clusterHtml = `
            <div class="web-map-cluster-badge" style="width: 38px; height: 38px; border-radius: 19px;">
              <span>${cluster.count}</span>
            </div>
          `;
          const clusterIcon = L.divIcon({
            html: clusterHtml,
            className: 'web-map-cluster-icon',
            iconSize: [38, 38],
            iconAnchor: [19, 19],
          });

          const marker = L.marker([cluster.lat, cluster.lng], { icon: clusterIcon });
          marker.on('click', () => {
            map.setView([cluster.lat, cluster.lng], Math.min(zoom + 2, 18), { animate: true });
          });
          marker.addTo(markersGroup);
        } else if (cluster.issue) {
          const iss = cluster.issue;
          const isSelected = selectedIssue?.id === iss.id;
          const priorityColor = getPriorityColor(iss.priority?.level);
          const iconSymbol = getCategoryIcon(iss.category?.name);

          const markerHtml = `
            <div class="web-map-marker-pin ${isSelected ? 'selected' : ''}" style="border-color: ${priorityColor};">
              <span class="marker-symbol">${iconSymbol}</span>
              <span class="marker-priority-dot" style="background-color: ${priorityColor};"></span>
              <span class="marker-arrow" style="border-top-color: ${priorityColor};"></span>
            </div>
          `;

          const pinIcon = L.divIcon({
            html: markerHtml,
            className: 'web-map-pin-icon',
            iconSize: [32, 38],
            iconAnchor: [16, 38],
          });

          const marker = L.marker([cluster.lat, cluster.lng], { icon: pinIcon });
          marker.on('click', () => {
            onSelectIssue(iss);
          });
          marker.addTo(markersGroup);
        }
      });
    });
  }, [issues, selectedIssue, mapReady, onSelectIssue]);

  // Center on selected issue if changed externally
  useEffect(() => {
    if (
      selectedIssue?.location?.latitude &&
      selectedIssue?.location?.longitude &&
      mapInstanceRef.current
    ) {
      mapInstanceRef.current.panTo(
        [selectedIssue.location.latitude, selectedIssue.location.longitude],
        { animate: true }
      );
    }
  }, [selectedIssue]);

  // Handle "Near Me" button
  const handleNearMe = useCallback(() => {
    if (!navigator.geolocation) {
      alert('Geolocation is not supported by your browser.');
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        const lat = position.coords.latitude;
        const lng = position.coords.longitude;
        setUserLocation({ lat, lng });

        if (mapInstanceRef.current) {
          mapInstanceRef.current.setView([lat, lng], 14, { animate: true });
        }

        // Draw user pin
        if (userPinLayerRef.current) {
          import('leaflet').then((L) => {
            userPinLayerRef.current.clearLayers();
            const userHtml = `
              <div class="web-map-user-pin">
                <div class="pulse-ring"></div>
                <div class="center-dot"></div>
              </div>
            `;
            const userIcon = L.divIcon({
              html: userHtml,
              className: 'web-user-icon',
              iconSize: [24, 24],
              iconAnchor: [12, 12],
            });
            L.marker([lat, lng], { icon: userIcon }).addTo(userPinLayerRef.current);
          });
        }

        onViewportChange(lat, lng, 5000);
      },
      () => {
        // Fallback to Ahmedabad default center if permission denied
        if (mapInstanceRef.current) {
          mapInstanceRef.current.setView(
            [defaultCity.latitude, defaultCity.longitude],
            defaultCity.defaultZoom,
            { animate: true }
          );
        }
        onViewportChange(defaultCity.latitude, defaultCity.longitude, 5000);
      },
      { timeout: 10000 }
    );
  }, [defaultCity.latitude, defaultCity.longitude, defaultCity.defaultZoom, onViewportChange]);

  return (
    <div className="civic-map-wrapper">
      {/* Map Container */}
      {!tileError ? (
        <div
          ref={mapContainerRef}
          className="civic-leaflet-container"
          aria-label="Interactive civic issue map"
        />
      ) : (
        /* Fallback when map tiles fail */
        <div className="map-fallback-view">
          <div className="map-fallback-content">
            <span className="map-fallback-icon">📍</span>
            <h3>Map Unavailable</h3>
            <p>
              Map tiles could not be loaded at this time. You can continue
              discovering and filtering civic issues using the list view.
            </p>
            {onSwitchToList && (
              <button
                type="button"
                className="civic-button civic-button-primary"
                onClick={onSwitchToList}
              >
                Switch to List View
              </button>
            )}
          </div>
        </div>
      )}

      {/* Loading Overlay */}
      {isLoading && (
        <div className="map-loading-indicator" aria-live="polite">
          <span className="spinner-icon"></span>
          <span>Loading visible issues...</span>
        </div>
      )}

      {/* Floating Action Controls */}
      <div className="map-floating-controls">
        <button
          type="button"
          className="map-control-btn"
          onClick={handleNearMe}
          title="Center on my current location"
          aria-label="Near Me: Center on my current location"
        >
          📍
        </button>
        <button
          type="button"
          className="map-control-btn"
          onClick={() => setShowLegend(!showLegend)}
          title="Toggle map legend"
          aria-label="Toggle map legend"
        >
          ℹ️
        </button>
      </div>

      {/* Optional Legend Popup */}
      {showLegend && <MapLegend />}

      {/* Compact Issue Preview Bottom Sheet */}
      {selectedIssue && (
        <div className="map-preview-overlay">
          <IssueMapPreview
            issue={selectedIssue}
            onClose={() => onSelectIssue(null)}
          />
        </div>
      )}
    </div>
  );
}

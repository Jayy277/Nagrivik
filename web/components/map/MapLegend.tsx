import React from 'react';

export function MapLegend() {
  return (
    <aside className="map-legend-card" aria-label="Map Legend and Indicators">
      <h4 className="map-legend-title">Legend</h4>
      <div className="map-legend-grid">
        <div className="map-legend-item">
          <span className="map-legend-marker-sample">📍</span>
          <div>
            <strong>Civic Issue</strong>
            <p>Category icon with priority border</p>
          </div>
        </div>

        <div className="map-legend-item">
          <span className="map-legend-cluster-sample">12</span>
          <div>
            <strong>Issue Cluster</strong>
            <p>Groups dense issues (click to zoom)</p>
          </div>
        </div>
      </div>

      <div className="map-legend-priorities">
        <span className="priority-tag critical">
          <span className="dot"></span> Critical
        </span>
        <span className="priority-tag high">
          <span className="dot"></span> High
        </span>
        <span className="priority-tag medium">
          <span className="dot"></span> Medium
        </span>
        <span className="priority-tag low">
          <span className="dot"></span> Low
        </span>
      </div>
    </aside>
  );
}

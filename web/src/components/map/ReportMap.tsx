import { Component, lazy, Suspense, type ReactNode } from 'react';
import type { MapPoint } from './ReportMapView';

const ReportMapView = lazy(() =>
  import('./ReportMapView').then((module) => ({ default: module.ReportMapView })),
);

interface ReportMapProps {
  points: MapPoint[];
  height?: string;
  onLocationPick?: (latitude: number, longitude: number) => void;
  onBoundsChange?: (bbox: string) => void;
  center?: [number, number];
  zoom?: number;
  ariaLabel?: string;
  /** Hide the text marker list (wizard picker mode). */
  hideMarkerList?: boolean;
}

class MapErrorBoundary extends Component<{ fallback: ReactNode; children: ReactNode }, { failed: boolean }> {
  state = { failed: false };

  static getDerivedStateFromError() {
    return { failed: true };
  }

  render() {
    if (this.state.failed) return this.props.fallback;
    return this.props.children;
  }
}

/**
 * Public wrapper around the Leaflet map. The heavy Leaflet bundle is loaded
 * lazily so unit tests (jsdom) never import it; on failure the component
 * degrades to a plain list. An always-present text list of markers keeps the
 * data accessible without the map.
 */
export function ReportMap({
  points,
  height,
  onLocationPick,
  onBoundsChange,
  center,
  zoom,
  ariaLabel,
  hideMarkerList = false,
}: ReportMapProps) {
  const fallback = (
    <p className="rounded-card border border-line bg-surface p-3 text-sm text-muted">
      Map view is unavailable right now — all locations are listed below.
    </p>
  );
  return (
    <div>
      <MapErrorBoundary fallback={fallback}>
        <Suspense
          fallback={
            <div
              className="flex items-center justify-center rounded-card border border-line bg-surface text-sm text-muted"
              style={{ height: height ?? '20rem' }}
            >
              Loading map…
            </div>
          }
        >
          <ReportMapView
            points={points}
            height={height}
            onLocationPick={onLocationPick}
            onBoundsChange={onBoundsChange}
            center={center}
            zoom={zoom}
            ariaLabel={ariaLabel}
          />
        </Suspense>
      </MapErrorBoundary>
      {!hideMarkerList && points.length > 0 && <MarkerList points={points} />}
    </div>
  );
}

function MarkerList({ points }: { points: MapPoint[] }) {
  return (
    <ul className="sr-only" aria-label="Map markers">
      {points.map((point) => (
        <li key={point.code}>
          {point.title} ({point.code}) at {point.latitude}, {point.longitude}
        </li>
      ))}
    </ul>
  );
}

import { useEffect, useRef, useState } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

export interface MapPoint {
  code: string;
  title: string;
  latitude: number;
  longitude: number;
}

interface ReportMapViewProps {
  points: MapPoint[];
  height?: string;
  /** Clicking the map emits rounded (6 dp) coordinates — used by the report wizard. */
  onLocationPick?: (latitude: number, longitude: number) => void;
  /** Emits "minLon,minLat,maxLon,maxLat" whenever the user pans/zooms the map. */
  onBoundsChange?: (bbox: string) => void;
  /** Initial center; defaults to the mean of the points, else Dar es Salaam. */
  center?: [number, number];
  zoom?: number;
  tileUrl?: string;
  ariaLabel?: string;
}

const DEFAULT_CENTER: [number, number] = [-6.7924, 39.2083];
const DEFAULT_TILE_URL = 'https://tile.openstreetmap.org/{z}/{x}/{y}.png';
const TILE_ATTRIBUTION =
  '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors';

/**
 * Renders a Leaflet map. Loaded lazily via ReportMap so jsdom test runs never
 * import Leaflet (it requires a real browser canvas).
 */
export function ReportMapView({
  points,
  height = '20rem',
  onLocationPick,
  onBoundsChange,
  center,
  zoom,
  tileUrl = import.meta.env.VITE_TILE_URL ?? DEFAULT_TILE_URL,
  ariaLabel = 'Map of report locations',
}: ReportMapViewProps) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<L.Map | null>(null);
  const markersRef = useRef<L.Marker[]>([]);
  const [initFailed, setInitFailed] = useState(false);

  useEffect(() => {
    if (!containerRef.current || mapRef.current) return;
    const initialCenter: [number, number] =
      center ??
      (points.length > 0
        ? ([
            points.reduce((sum, p) => sum + p.latitude, 0) / points.length,
            points.reduce((sum, p) => sum + p.longitude, 0) / points.length,
          ] as [number, number])
        : DEFAULT_CENTER);
    let map: L.Map;
    try {
      map = L.map(containerRef.current, {
        center: initialCenter,
        zoom: zoom ?? (points.length > 0 ? 12 : 11),
        attributionControl: true,
      });
    } catch {
      // Leaflet needs a real layout engine (e.g. jsdom has none) — render nothing;
      // the surrounding ReportMap keeps the accessible marker list available.
      setInitFailed(true);
      return;
    }
    L.tileLayer(tileUrl, { attribution: TILE_ATTRIBUTION, maxZoom: 19 }).addTo(map);
    if (onLocationPick) {
      map.on('click', (event: L.LeafletMouseEvent) => {
        onLocationPick(
          Number(event.latlng.lat.toFixed(6)),
          Number(event.latlng.lng.toFixed(6)),
        );
      });
    }
    mapRef.current = map;
    return () => {
      map.remove();
      mapRef.current = null;
      markersRef.current = [];
    };
    // Mount-once: marker updates are handled by the effects below.
  }, []);

  useEffect(() => {
    const map = mapRef.current;
    if (!map || !onBoundsChange) return;
    const emit = () => {
      const bounds = map.getBounds();
      onBoundsChange(
        [
          bounds.getWest().toFixed(6),
          bounds.getSouth().toFixed(6),
          bounds.getEast().toFixed(6),
          bounds.getNorth().toFixed(6),
        ].join(','),
      );
    };
    map.on('moveend', emit);
    return () => {
      map.off('moveend', emit);
    };
  }, [onBoundsChange]);

  useEffect(() => {
    const map = mapRef.current;
    if (!map || initFailed) return;
    markersRef.current.forEach((marker) => marker.remove());
    markersRef.current = points.map((point) =>
      L.marker([point.latitude, point.longitude])
        .addTo(map)
        .bindPopup(
          `<a href="/reports/${encodeURIComponent(point.code)}">${escapeHtml(point.title)}</a>`,
        ),
    );
    if (points.length > 0 && !onLocationPick) {
      const group = L.latLngBounds(points.map((p) => [p.latitude, p.longitude] as [number, number]));
      map.fitBounds(group.pad(0.2));
    }
  }, [points, onLocationPick, initFailed]);

  if (initFailed) return null;

  return (
    <div
      ref={containerRef}
      role="application"
      aria-label={ariaLabel}
      data-testid="report-map"
      style={{ height, width: '100%' }}
    />
  );
}

function escapeHtml(value: string): string {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;');
}

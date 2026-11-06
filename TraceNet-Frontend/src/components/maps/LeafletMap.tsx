/**
 * @file LeafletMap.tsx
 * @description Leaflet map component with OpenStreetMap cartography tiles,
 * dynamic color-coded incident status markers (Blue = Missing, Green = Found, Gold = Resolved),
 * interactive click-to-preview popups, and click-to-pin location picker mode.
 */

import { useEffect, useMemo, useRef } from "react";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { STATUS_HEX } from "../../lib/report-utils";
import type { Report, ReportStatus } from "../../services/types";

function makeIcon(color: string) {
  const svg = `<svg xmlns='http://www.w3.org/2000/svg' width='30' height='42' viewBox='0 0 30 42'>
    <path d='M15 0C6.7 0 0 6.7 0 15c0 11.2 15 27 15 27s15-15.8 15-27C30 6.7 23.3 0 15 0z' fill='${color}'/>
    <circle cx='15' cy='15' r='6' fill='white'/></svg>`;
  return L.divIcon({
    className: "",
    html: svg,
    iconSize: [30, 42],
    iconAnchor: [15, 42],
    popupAnchor: [0, -38],
  });
}

type Props = {
  reports?: Report[];
  center?: [number, number];
  zoom?: number;
  height?: number | string;
  onSelect?: (r: Report) => void;
  // Picker mode
  pickable?: boolean;
  marker?: [number, number] | null;
  onPick?: (lat: number, lng: number) => void;
};

export default function LeafletMap({
  reports = [],
  center = [20.5937, 78.9629],
  zoom = 5,
  height = 480,
  onSelect,
  pickable = false,
  marker = null,
  onPick,
}: Props) {
  const ref = useRef<HTMLDivElement>(null);
  const mapRef = useRef<L.Map | null>(null);
  const markerRef = useRef<L.Marker | null>(null);

  const icons = useMemo(
    () =>
      ({
        missing: makeIcon(STATUS_HEX.missing),
        found: makeIcon(STATUS_HEX.found),
        resolved: makeIcon(STATUS_HEX.resolved),
      }) as Record<ReportStatus, L.DivIcon>,
    [],
  );

  useEffect(() => {
    if (!ref.current) return;
    if (mapRef.current) {
      mapRef.current.remove();
      mapRef.current = null;
    }
    if ((ref.current as any)._leaflet_id) {
      delete (ref.current as any)._leaflet_id;
    }
    const map = L.map(ref.current, { center, zoom, scrollWheelZoom: true });
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      attribution: "&copy; OpenStreetMap contributors",
      maxZoom: 19,
    }).addTo(map);
    mapRef.current = map;

    if (pickable) {
      map.on("click", (e) => {
        const { lat, lng } = e.latlng;
        if (markerRef.current) markerRef.current.setLatLng([lat, lng]);
        else markerRef.current = L.marker([lat, lng], { icon: icons.missing }).addTo(map);
        onPick?.(lat, lng);
      });
    }

    return () => {
      if (mapRef.current) {
        mapRef.current.remove();
        mapRef.current = null;
      }
      if (ref.current && (ref.current as any)._leaflet_id) {
        delete (ref.current as any)._leaflet_id;
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Sync markers for reports
  useEffect(() => {
    const map = mapRef.current;
    if (!map || pickable) return;
    const group = L.layerGroup().addTo(map);
    reports.forEach((r) => {
      const m = L.marker([r.lat, r.lng], { icon: icons[r.status] }).addTo(group);
      m.bindPopup(`<strong>${r.title}</strong><br/><span style="color:#6B7280">${r.city}</span>`);
      if (onSelect) m.on("click", () => onSelect(r));
    });
    if (reports.length) {
      const bounds = L.latLngBounds(reports.map((r) => [r.lat, r.lng] as [number, number]));
      map.fitBounds(bounds.pad(0.2));
    }
    return () => {
      group.remove();
    };
  }, [reports, icons, onSelect, pickable]);

  // Sync picker marker prop
  useEffect(() => {
    const map = mapRef.current;
    if (!map || !pickable) return;
    if (marker) {
      if (markerRef.current) markerRef.current.setLatLng(marker);
      else markerRef.current = L.marker(marker, { icon: icons.missing }).addTo(map);
      map.setView(marker, Math.max(map.getZoom(), 12));
    }
  }, [marker, pickable, icons]);

  return (
    <div
      ref={ref}
      style={{ height, width: "100%", zIndex: 0 }}
      className="overflow-hidden rounded-2xl border border-border/60 relative"
    />
  );
}

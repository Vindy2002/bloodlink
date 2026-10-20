import { useEffect } from 'react';
import { Circle, CircleMarker, MapContainer, Popup, TileLayer, useMap } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import { bgLabel } from '../data.js';

function Recenter({ center, radiusKm }) {
  const map = useMap();
  useEffect(() => {
    map.setView(center, radiusKm > 40 ? 8 : radiusKm > 15 ? 10 : 11);
  }, [center[0], center[1], radiusKm]); // eslint-disable-line
  return null;
}

export default function ResultsMap({ center, radiusKm, results }) {
  return (
    <MapContainer center={center} zoom={11} className="map" scrollWheelZoom={false}>
      <TileLayer attribution="&copy; OpenStreetMap contributors" url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
      <Recenter center={center} radiusKm={radiusKm} />
      <Circle center={center} radius={radiusKm * 1000} pathOptions={{ color: '#15203b', weight: 1, fillOpacity: 0.04 }} />
      <CircleMarker center={center} radius={7} pathOptions={{ color: '#15203b', fillColor: '#15203b', fillOpacity: 1 }}>
        <Popup>Search centre</Popup>
      </CircleMarker>
      {results.map((r) => (
        <CircleMarker key={r.id} center={[r.latitude, r.longitude]} radius={9}
          pathOptions={{ color: '#fff', weight: 2, fillColor: '#b3122b', fillOpacity: 0.9 }}>
          <Popup>{r.name} · {bgLabel(r.bloodGroup)}<br />{r.city}{r.distanceKm != null && ` · ${r.distanceKm} km`}</Popup>
        </CircleMarker>
      ))}
    </MapContainer>
  );
}

import { CITIES } from '../data.js';

/** City dropdown + "use my location" button. Calls onChange({city, district, lat, lng}). */
export default function CitySelect({ value, onChange, withGps = true }) {
  const gps = () => {
    if (!navigator.geolocation) return alert('Location is not available in this browser.');
    navigator.geolocation.getCurrentPosition(
      (p) => onChange({ city: value || 'My location', district: '', lat: p.coords.latitude, lng: p.coords.longitude, gps: true }),
      () => alert('Could not read your location. Please allow location access or pick a city.')
    );
  };
  return (
    <div style={{ display: 'flex', gap: 8 }}>
      <select value={value || ''} onChange={(e) => {
        const c = CITIES.find((x) => x.name === e.target.value);
        if (c) onChange({ city: c.name, district: c.district, lat: c.lat, lng: c.lng });
      }}>
        <option value="" disabled>Choose city</option>
        {CITIES.map((c) => <option key={c.name}>{c.name}</option>)}
      </select>
      {withGps && <button type="button" className="btn sm" onClick={gps}>Use GPS</button>}
    </div>
  );
}

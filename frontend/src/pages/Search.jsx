import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import api, { errMsg } from '../api.js';
import { useAuth } from '../AuthContext.jsx';
import BloodBadge from '../components/BloodBadge.jsx';
import CitySelect from '../components/CitySelect.jsx';
import ResultsMap from '../components/ResultsMap.jsx';
import { BLOOD_GROUPS, CITIES } from '../data.js';

export default function Search() {
  const [params] = useSearchParams();
  const { user } = useAuth();
  const [bg, setBg] = useState(params.get('bg') || 'O_POS');
  const [loc, setLoc] = useState({ city: 'Colombo', lat: CITIES[0].lat, lng: CITIES[0].lng });
  const [radius, setRadius] = useState(25);
  const [compatible, setCompatible] = useState(true);
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    let live = true;
    setLoading(true);
    setError('');
    api.get('/search/donors', { params: { bloodGroup: bg, lat: loc.lat, lng: loc.lng, radiusKm: radius, compatible } })
      .then((r) => live && setResults(r.data))
      .catch((e) => live && setError(errMsg(e)))
      .finally(() => live && setLoading(false));
    return () => { live = false; };
  }, [bg, loc, radius, compatible]);

  return (
    <main className="wrap page">
      <h2>Find donors</h2>
      <div className="split">
        <aside className="card">
          <div className="lbl" style={{ marginBottom: 6 }}>Blood group needed</div>
          <div className="picker" style={{ marginBottom: 16 }}>
            {BLOOD_GROUPS.map((g) => (
              <button key={g.key} className={bg === g.key ? 'on' : ''} onClick={() => setBg(g.key)}>{g.label}</button>
            ))}
          </div>
          <div className="field">
            <label>Where is the patient?</label>
            <CitySelect value={loc.city} onChange={setLoc} />
          </div>
          <div className="field">
            <label>Search radius: {radius} km</label>
            <input type="range" min="5" max="150" step="5" value={radius} onChange={(e) => setRadius(+e.target.value)} />
          </div>
          <label className="check">
            <input type="checkbox" checked={compatible} onChange={(e) => setCompatible(e.target.checked)} />
            <span>Include compatible groups<small>e.g. O− and O+ can give to A+. Confirm with the hospital.</small></span>
          </label>
          <p className="small muted" style={{ marginBottom: 0 }}>
            Donor names are shortened and locations are blurred for privacy.
            {user?.role === 'HOSPITAL' ? ' As a verified hospital you can send an alert to everyone nearby from your dashboard.' : ' To reach donors, ask a verified hospital to post a request.'}
          </p>
        </aside>

        <section>
          <ResultsMap center={[loc.lat, loc.lng]} radiusKm={radius} results={results} />
          {error && <div className="err">{error}</div>}
          <div className="card">
            <h3>{loading ? 'Searching…' : `${results.length} donor${results.length === 1 ? '' : 's'} available`}</h3>
            {!loading && results.length === 0 && <p className="muted">No available donors in this radius. Try a larger radius or include compatible groups.</p>}
            <div className="list">
              {results.map((r) => (
                <div className="item-row" key={r.id}>
                  <BloodBadge group={r.bloodGroup} />
                  <div className="grow">
                    <b>{r.name}</b> {r.verifiedDonor && <span className="tag ok">Verified donor</span>}{' '}
                    {!r.exactMatch && <span className="tag">Compatible</span>}
                    <div className="small muted">{r.city}{r.distanceKm != null && ` · ${r.distanceKm} km away`}</div>
                  </div>
                  {r.phone && <a href={`tel:${r.phone}`} className="btn sm">Call {r.phone}</a>}
                </div>
              ))}
            </div>
          </div>
        </section>
      </div>
    </main>
  );
}

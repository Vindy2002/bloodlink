import { useCallback, useEffect, useState } from 'react';
import api, { errMsg } from '../api.js';
import BloodBadge from '../components/BloodBadge.jsx';
import CitySelect from '../components/CitySelect.jsx';
import { BLOOD_GROUPS, fmtAgo, fmtDate } from '../data.js';

const urgencyTag = { CRITICAL: 'bad', URGENT: 'warn', PLANNED: '' };

export default function DonorDashboard() {
  const [tab, setTab] = useState('alerts');
  const [p, setP] = useState(null);
  const [alerts, setAlerts] = useState([]);
  const [history, setHistory] = useState([]);
  const [msg, setMsg] = useState({ type: '', text: '' });

  const load = useCallback(async () => {
    try {
      const [a, b, c] = await Promise.all([api.get('/donor/profile'), api.get('/donor/alerts'), api.get('/donor/donations')]);
      setP(a.data); setAlerts(b.data); setHistory(c.data);
    } catch (e) { setMsg({ type: 'err', text: errMsg(e) }); }
  }, []);
  useEffect(() => { load(); }, [load]);

  const flash = (type, text) => { setMsg({ type, text }); setTimeout(() => setMsg({ type: '', text: '' }), 4000); };
  const run = async (fn, ok) => { try { await fn(); flash('ok', ok); load(); } catch (e) { flash('err', errMsg(e)); } };

  if (!p) return <main className="wrap page">{msg.text ? <div className="err">{msg.text}</div> : 'Loading…'}</main>;
  const openAlerts = alerts.filter((a) => a.requestStatus === 'OPEN' && a.myResponse === 'NOTIFIED').length;

  return (
    <main className="wrap page">
      <div className="card" style={{ display: 'flex', gap: 18, alignItems: 'center', flexWrap: 'wrap' }}>
        <BloodBadge group={p.bloodGroup} />
        <div style={{ flex: 1, minWidth: 220 }}>
          <h2 style={{ marginBottom: 2 }}>{p.fullName}</h2>
          <span className="muted small">{p.city} · {p.verifiedDonor ? 'Verified donor' : 'Not yet verified by a hospital'}</span>
          <div className="small" style={{ marginTop: 4 }}>
            {!p.available ? <span className="tag">Paused: you won't get alerts</span>
              : p.eligible ? <span className="tag ok">Eligible to donate</span>
              : <span className="tag warn">Eligible again on {fmtDate(p.nextEligibleDate)}</span>}
          </div>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <span className="lbl">{p.available ? 'Available' : 'Unavailable'}</span>
          <button className={`toggle ${p.available ? 'on' : ''}`} role="switch" aria-checked={p.available} aria-label="Availability"
            onClick={() => {
              if (!p.available && !window.confirm('Please confirm: you are in good health, have no fever or infection, are not taking antibiotics, and have had no recent tattoo, piercing, surgery or vaccination.')) return;
              run(() => api.patch('/donor/availability', { available: !p.available, confirmFit: true }), p.available ? 'You are now hidden from alerts.' : 'You are available again.');
            }} />
        </div>
      </div>

      {msg.text && <div className={msg.type === 'err' ? 'err' : 'ok-msg'}>{msg.text}</div>}

      <div className="tabs">
        <button className={tab === 'alerts' ? 'on' : ''} onClick={() => setTab('alerts')}>Alerts{openAlerts > 0 && ` (${openAlerts})`}</button>
        <button className={tab === 'history' ? 'on' : ''} onClick={() => setTab('history')}>Donation history</button>
        <button className={tab === 'profile' ? 'on' : ''} onClick={() => setTab('profile')}>Profile &amp; privacy</button>
      </div>

      {tab === 'alerts' && (
        <div className="card">
          {alerts.length === 0 && <p className="muted">No requests yet. When a hospital near you needs your blood group, it will show up here and reach you by SMS and email.</p>}
          <div className="list">
            {alerts.map((a) => (
              <div className="item-row" key={a.requestId}>
                <BloodBadge group={a.bloodGroup} />
                <div className="grow">
                  <b>{a.hospitalName}</b>, {a.hospitalCity} <span className={`tag ${urgencyTag[a.urgency]}`}>{a.urgency.toLowerCase()}</span>
                  <div className="small muted">{a.unitsNeeded} unit(s) · {a.distanceKm != null ? `${a.distanceKm} km away · ` : ''}{fmtAgo(a.createdAt)}</div>
                  {a.note && <div className="small">{a.note}</div>}
                </div>
                {a.requestStatus !== 'OPEN' ? <span className="tag">Closed</span>
                  : a.myResponse === 'NOTIFIED' ? (
                    <>
                      <button className="btn primary sm" onClick={() => run(() => api.post(`/donor/alerts/${a.requestId}/accept`), 'Thank you! The hospital can now see your phone number.')}>I can donate</button>
                      <button className="btn sm" onClick={() => run(() => api.post(`/donor/alerts/${a.requestId}/decline`), 'Marked as unavailable for this request.')}>Can't this time</button>
                    </>
                  ) : <span className={`tag ${a.myResponse === 'ACCEPTED' ? 'ok' : ''}`}>{a.myResponse === 'ACCEPTED' ? 'You accepted' : 'You declined'}</span>}
              </div>
            ))}
          </div>
        </div>
      )}

      {tab === 'alerts' && <Tips />}
      {tab === 'history' && <History history={history} run={run} />}
      {tab === 'profile' && <Profile p={p} run={run} />}
    </main>
  );
}

function History({ history, run }) {
  const [f, setF] = useState({ date: '', place: '', note: '' });
  return (
    <>
      <div className="card">
        <h3>Your donations</h3>
        {history.length === 0 && <p className="muted">Nothing recorded yet.</p>}
        <div className="list">
          {history.map((h) => (
            <div className="item-row" key={h.id}>
              <div className="grow"><b>{fmtDate(h.date)}</b> · {h.place || 'Unknown place'}<div className="small muted">{h.units} unit(s){h.note ? ` · ${h.note}` : ''}</div></div>
              <span className={`tag ${h.status === 'CONFIRMED' ? 'ok' : ''}`}>{h.status === 'CONFIRMED' ? 'Confirmed by hospital' : 'Self-reported'}</span>
            </div>
          ))}
        </div>
      </div>
      <form className="card" onSubmit={(e) => { e.preventDefault(); run(() => api.post('/donor/donations', f), 'Donation added.').then?.(() => setF({ date: '', place: '', note: '' })); }}>
        <h3>Add an earlier donation</h3>
        <div className="row">
          <div className="field"><label>Date</label><input type="date" required value={f.date} onChange={(e) => setF({ ...f, date: e.target.value })} /></div>
          <div className="field"><label>Where</label><input value={f.place} onChange={(e) => setF({ ...f, place: e.target.value })} /></div>
        </div>
        <button className="btn sm">Add donation</button>
        <p className="small muted" style={{ marginTop: 10 }}>Self-reported donations count for your waiting period, but only hospital-confirmed ones make you a verified donor.</p>
      </form>
    </>
  );
}

function Profile({ p, run }) {
  const [f, setF] = useState(p);
  const set = (k, v) => setF({ ...f, [k]: v });
  const chk = (k, title, hint) => (
    <label className="check"><input type="checkbox" checked={!!f[k]} onChange={(e) => set(k, e.target.checked)} />
      <span>{title}<small>{hint}</small></span></label>
  );
  return (
    <form className="card" onSubmit={(e) => { e.preventDefault(); run(() => api.put('/donor/profile', f), 'Profile saved.'); }}>
      <h3>Profile</h3>
      <div className="row">
        <div className="field"><label>Full name</label><input value={f.fullName || ''} onChange={(e) => set('fullName', e.target.value)} /></div>
        <div className="field"><label>Mobile</label><input value={f.phone || ''} onChange={(e) => set('phone', e.target.value)} /></div>
        <div className="field"><label>Blood group {p.verifiedDonor && '(locked after verification)'}</label>
          <select disabled={p.verifiedDonor} value={f.bloodGroup} onChange={(e) => set('bloodGroup', e.target.value)}>
            {BLOOD_GROUPS.map((g) => <option key={g.key} value={g.key}>{g.label}</option>)}</select></div>
      </div>
      <div className="field"><label>Location</label>
        <CitySelect value={f.city} onChange={(l) => setF({ ...f, city: l.city, district: l.district, latitude: l.lat, longitude: l.lng })} /></div>
      <div className="field" style={{ maxWidth: 260 }}><label>Travel up to {f.alertRadiusKm} km for a request</label>
        <input type="range" min="5" max="100" step="5" value={f.alertRadiusKm} onChange={(e) => set('alertRadiusKm', +e.target.value)} /></div>

      <h3 style={{ marginTop: 20 }}>Privacy</h3>
      {chk('visibleInSearch', 'Show me in donor search', 'Turn off to receive hospital alerts only, without appearing in search results.')}
      {chk('hideExactLocation', 'Blur my location more', 'Search shows a wider random offset instead of a point near your home.')}
      {chk('showPhone', 'Let verified hospitals see my phone in search', 'Off by default. Hospitals always get your number if you accept a request.')}
      <h3 style={{ marginTop: 20 }}>Alerts</h3>
      {chk('receiveSms', 'SMS alerts', 'Fastest way to reach you in an emergency.')}
      {chk('receiveEmail', 'Email alerts', '')}
      <button className="btn primary">Save changes</button>
    </form>
  );
}

function Tips() {
  return (
    <div className="card">
      <h3>Before you donate</h3>
      <ul className="small" style={{ margin: 0, paddingLeft: 18 }}>
        <li>Eat a proper meal first. Do not go with an empty stomach.</li>
        <li>Drink plenty of water, and sleep 6 to 8 hours the night before.</li>
        <li>Bring your NIC, driving licence or passport.</li>
        <li>Your haemoglobin (12.5 g/dl or more) is tested at the blood bank. Tell the doctor about any medicines or health concerns.</li>
        <li>You can donate every 4 months (up to 3 times a year).</li>
      </ul>
    </div>
  );
}

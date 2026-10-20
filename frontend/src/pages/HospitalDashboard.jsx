import { useCallback, useEffect, useState } from 'react';
import api, { errMsg } from '../api.js';
import BloodBadge from '../components/BloodBadge.jsx';
import { BLOOD_GROUPS, fmtAgo, fmtDate } from '../data.js';

export default function HospitalDashboard() {
  const [me, setMe] = useState(null);
  const [reqs, setReqs] = useState([]);
  const [donations, setDonations] = useState([]);
  const [tab, setTab] = useState('requests');
  const [msg, setMsg] = useState({ type: '', text: '' });

  const load = useCallback(async () => {
    try {
      const [a, b, c] = await Promise.all([api.get('/hospital/me'), api.get('/hospital/requests'), api.get('/hospital/donations')]);
      setMe(a.data); setReqs(b.data); setDonations(c.data);
    } catch (e) { setMsg({ type: 'err', text: errMsg(e) }); }
  }, []);
  useEffect(() => { load(); const t = setInterval(load, 20000); return () => clearInterval(t); }, [load]);

  const flash = (type, text) => { setMsg({ type, text }); setTimeout(() => setMsg({ type: '', text: '' }), 5000); };
  const run = async (fn, ok) => { try { await fn(); if (ok) flash('ok', ok); load(); return true; } catch (e) { flash('err', errMsg(e)); return false; } };

  if (!me) return <main className="wrap page">{msg.text ? <div className="err">{msg.text}</div> : 'Loading…'}</main>;
  const verified = me.status === 'VERIFIED';

  return (
    <main className="wrap page">
      <div className="card" style={{ display: 'flex', gap: 14, alignItems: 'center', flexWrap: 'wrap' }}>
        <div style={{ flex: 1 }}>
          <h2 style={{ marginBottom: 2 }}>{me.name}</h2>
          <span className="muted small">{me.city} · Reg. {me.registrationNo}</span>
        </div>
        <span className={`tag ${verified ? 'ok' : me.status === 'REJECTED' ? 'bad' : 'warn'}`}>{me.status}</span>
      </div>
      {!verified && <div className="err" style={{ background: 'var(--warn-bg)', color: '#6b4100' }}>
        {me.status === 'PENDING' ? 'An admin is reviewing your registration. You can send blood requests once you are verified.' : `Verification declined: ${me.rejectionReason}`}</div>}
      {msg.text && <div className={msg.type === 'err' ? 'err' : 'ok-msg'}>{msg.text}</div>}

      <div className="tabs">
        <button className={tab === 'requests' ? 'on' : ''} onClick={() => setTab('requests')}>Blood requests</button>
        <button className={tab === 'donations' ? 'on' : ''} onClick={() => setTab('donations')}>Record donation</button>
      </div>

      {tab === 'requests' && (
        <>
          {verified && <NewRequest run={run} />}
          {reqs.map((r) => <RequestCard key={r.id} r={r} run={run} />)}
          {reqs.length === 0 && <p className="muted">No requests yet.</p>}
        </>
      )}
      {tab === 'donations' && <Donations donations={donations} run={run} verified={verified} />}
    </main>
  );
}

function NewRequest({ run }) {
  const [f, setF] = useState({ bloodGroup: 'O_POS', unitsNeeded: 2, urgency: 'URGENT', radiusKm: 15, note: '' });
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  return (
    <form className="card" onSubmit={async (e) => {
      e.preventDefault();
      await run(() => api.post('/hospital/requests', { ...f, unitsNeeded: +f.unitsNeeded, radiusKm: +f.radiusKm }), 'Request sent. Nearby eligible donors are being alerted.');
    }}>
      <h3>Send a new request</h3>
      <div className="row">
        <div className="field"><label>Blood group</label><select value={f.bloodGroup} onChange={set('bloodGroup')}>{BLOOD_GROUPS.map((g) => <option key={g.key} value={g.key}>{g.label}</option>)}</select></div>
        <div className="field"><label>Units</label><input type="number" min="1" max="20" value={f.unitsNeeded} onChange={set('unitsNeeded')} /></div>
        <div className="field"><label>Urgency</label><select value={f.urgency} onChange={set('urgency')}><option value="CRITICAL">Critical</option><option value="URGENT">Urgent</option><option value="PLANNED">Planned</option></select></div>
        <div className="field"><label>Radius (km)</label><input type="number" min="1" max="200" value={f.radiusKm} onChange={set('radiusKm')} /></div>
      </div>
      <div className="field"><label>Note for donors (no patient names)</label><input maxLength={500} value={f.note} onChange={set('note')} placeholder="e.g. Ward 7, ask for the blood bank counter" /></div>
      <button className="btn primary">Alert donors</button>
    </form>
  );
}

function RequestCard({ r, run }) {
  const [rows, setRows] = useState(null);
  const open = r.status === 'OPEN';
  const toggle = async () => { if (rows) return setRows(null); const { data } = await api.get(`/hospital/requests/${r.id}/responses`); setRows(data); };
  return (
    <div className="card">
      <div className="item-row" style={{ paddingTop: 0 }}>
        <BloodBadge group={r.bloodGroup} />
        <div className="grow"><b>{r.unitsNeeded} unit(s)</b> · {r.urgency.toLowerCase()} · {r.radiusKm} km radius
          <div className="small muted">{fmtAgo(r.createdAt)} · {r.notified + r.accepted + r.declined} alerted · <b style={{ color: 'var(--ok)' }}>{r.accepted} accepted</b> · {r.declined} declined</div></div>
        <span className={`tag ${open ? 'warn' : r.status === 'FULFILLED' ? 'ok' : ''}`}>{r.status}</span>
      </div>
      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
        <button className="btn sm" onClick={toggle}>{rows ? 'Hide donors' : 'View donors'}</button>
        {open && <>
          <button className="btn sm" onClick={() => run(() => api.post(`/hospital/requests/${r.id}/escalate`), 'Radius widened by 10 km and new donors alerted.')}>Widen +10 km</button>
          <button className="btn sm" onClick={() => run(() => api.patch(`/hospital/requests/${r.id}/status`, { status: 'FULFILLED' }), 'Marked as fulfilled.')}>Mark fulfilled</button>
          <button className="btn sm" onClick={() => run(() => api.patch(`/hospital/requests/${r.id}/status`, { status: 'CANCELLED' }), 'Request cancelled.')}>Cancel</button>
        </>}
      </div>
      {rows && (
        <div className="list" style={{ marginTop: 12 }}>
          {rows.length === 0 && <p className="muted">No eligible donors were found in range yet. Try widening the radius.</p>}
          {rows.map((d) => (
            <div className="item-row" key={d.donorId}>
              <BloodBadge group={d.bloodGroup} soft />
              <div className="grow"><b>{d.name}</b> {d.verifiedDonor && <span className="tag ok">Verified</span>}
                <div className="small muted">{d.distanceKm != null && `${d.distanceKm} km`}</div></div>
              {d.phone && <a className="btn sm" href={`tel:${d.phone}`}>Call {d.phone}</a>}
              {d.status === 'ACCEPTED' && <button className="btn sm primary" onClick={() => run(() => api.post('/hospital/donations', { donorId: d.donorId, requestId: r.id, units: 1 }), 'Donation recorded.')}>Record donation</button>}
              <span className={`tag ${d.status === 'ACCEPTED' ? 'ok' : d.status === 'DECLINED' ? 'bad' : ''}`}>{d.status === 'NOTIFIED' ? 'Waiting' : d.status.toLowerCase()}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function Donations({ donations, run, verified }) {
  const [f, setF] = useState({ donorEmail: '', units: 1 });
  return (
    <>
      {verified && (
        <form className="card" onSubmit={async (e) => { e.preventDefault(); if (await run(() => api.post('/hospital/donations', { ...f, units: +f.units }), 'Donation recorded. The donor is now verified.')) setF({ donorEmail: '', units: 1 }); }}>
          <h3>Record a walk-in donation</h3>
          <div className="row">
            <div className="field"><label>Donor's BloodLink email</label><input type="email" required value={f.donorEmail} onChange={(e) => setF({ ...f, donorEmail: e.target.value })} /></div>
            <div className="field"><label>Units</label><input type="number" min="1" max="3" value={f.units} onChange={(e) => setF({ ...f, units: e.target.value })} /></div>
          </div>
          <button className="btn primary">Confirm donation</button>
        </form>
      )}
      <div className="card"><h3>Confirmed by your hospital</h3>
        {donations.length === 0 && <p className="muted">No donations recorded yet.</p>}
        <div className="list">{donations.map((d) => (
          <div className="item-row" key={d.id}><div className="grow"><b>{d.donorName}</b><div className="small muted">{fmtDate(d.date)} · {d.units} unit(s)</div></div></div>))}</div>
      </div>
    </>
  );
}

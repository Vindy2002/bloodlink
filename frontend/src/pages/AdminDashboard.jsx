import { useCallback, useEffect, useState } from 'react';
import api, { errMsg } from '../api.js';
import { fmtDate } from '../data.js';

export default function AdminDashboard() {
  const [status, setStatus] = useState('PENDING');
  const [rows, setRows] = useState([]);
  const [stats, setStats] = useState(null);
  const [msg, setMsg] = useState('');

  const load = useCallback(async () => {
    try {
      const [h, s] = await Promise.all([api.get('/admin/hospitals', { params: { status } }), api.get('/admin/stats')]);
      setRows(h.data); setStats(s.data);
    } catch (e) { setMsg(errMsg(e)); }
  }, [status]);
  useEffect(() => { load(); }, [load]);

  const act = async (fn) => { try { await fn(); setMsg(''); load(); } catch (e) { setMsg(errMsg(e)); } };
  const reject = (id) => {
    const reason = window.prompt('Reason for declining (the hospital will see this):');
    if (reason && reason.trim()) act(() => api.post(`/admin/hospitals/${id}/reject`, { reason }));
  };

  return (
    <main className="wrap page">
      <h2>Admin</h2>
      {stats && (
        <div className="stats">
          <div className="stat"><b>{stats.totalDonors}</b>donors</div>
          <div className="stat"><b>{stats.verifiedHospitals}</b>verified hospitals</div>
          <div className="stat"><b>{stats.pendingHospitals}</b>awaiting review</div>
          <div className="stat"><b>{stats.openRequests}</b>open requests</div>
          <div className="stat"><b>{stats.confirmedDonations}</b>confirmed donations</div>
        </div>
      )}
      {msg && <div className="err">{msg}</div>}
      <div className="tabs">
        {['PENDING', 'VERIFIED', 'REJECTED'].map((s) => (
          <button key={s} className={status === s ? 'on' : ''} onClick={() => setStatus(s)}>{s.charAt(0) + s.slice(1).toLowerCase()}</button>
        ))}
      </div>
      <div className="card">
        {rows.length === 0 && <p className="muted">No {status.toLowerCase()} hospitals.</p>}
        <div className="list">
          {rows.map((h) => (
            <div className="item-row" key={h.id}>
              <div className="grow">
                <b>{h.name}</b> <span className="muted small">Reg. {h.registrationNo}</span>
                <div className="small muted">{h.city} · {h.contactName} · {h.contactPhone} · {h.contactEmail} · applied {fmtDate(h.createdAt)}</div>
                {h.rejectionReason && <div className="small">Reason: {h.rejectionReason}</div>}
              </div>
              {h.status !== 'VERIFIED' && <button className="btn primary sm" onClick={() => act(() => api.post(`/admin/hospitals/${h.id}/verify`))}>Approve</button>}
              {h.status === 'PENDING' && <button className="btn sm" onClick={() => reject(h.id)}>Decline</button>}
            </div>
          ))}
        </div>
      </div>
      <p className="small muted">Before approving, check the registration number with the hospital's official phone line, not the number given at sign-up.</p>
    </main>
  );
}

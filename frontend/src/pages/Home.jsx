import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../api.js';
import { BLOOD_GROUPS } from '../data.js';

export default function Home() {
  const [stats, setStats] = useState(null);
  const nav = useNavigate();
  useEffect(() => { api.get('/public/stats').then((r) => setStats(r.data)).catch(() => {}); }, []);

  return (
    <>
      <section className="hero">
        <div className="wrap">
          <div>
            <h1>Blood needed now? Find a donor close by.</h1>
            <p className="lead">
              BloodLink matches patients with eligible, willing donors around the hospital, and alerts them by SMS and email within seconds.
            </p>
            <div className="cta">
              <Link className="btn primary" to="/register">Register as a donor</Link>
              <Link className="btn ghost-light" to="/register?type=hospital">Register a hospital</Link>
            </div>
          </div>
          <div>
            <p className="lead" style={{ marginBottom: 12 }}>Pick the blood group you need:</p>
            <div className="tiles">
              {BLOOD_GROUPS.map((g) => (
                <button key={g.key} className="tile" onClick={() => nav(`/search?bg=${g.key}`)}>
                  <b>{g.label}</b>
                  <span>{stats ? `${stats.donorsByGroup[g.key]} donors` : '…'}</span>
                </button>
              ))}
            </div>
          </div>
        </div>
      </section>

      <main className="wrap page">
        {stats && (
          <div className="stats">
            <div className="stat"><b>{stats.availableDonors}</b>available donors</div>
            <div className="stat"><b>{stats.verifiedHospitals}</b>verified hospitals</div>
            <div className="stat"><b>{stats.openRequests}</b>open requests</div>
          </div>
        )}
        <h2>How it works</h2>
        <div className="how">
          <div className="card"><h3>Donors sign up once</h3><p className="muted">Add your blood group and city. Switch availability off any time, and decide what other people can see.</p></div>
          <div className="card"><h3>Hospitals are verified</h3><p className="muted">Only hospitals approved by an admin can send requests, so donors are never spammed by strangers.</p></div>
          <div className="card"><h3>Nearest donors are alerted</h3><p className="muted">Compatible donors inside the radius get an SMS, email and in-app alert. If nobody answers, widen the radius in one tap.</p></div>
          <div className="card"><h3>Donations are recorded</h3><p className="muted">The hospital confirms each donation. Donors build a verified history and are not asked again until they are eligible.</p></div>
        </div>
      </main>
    </>
  );
}

import { useEffect, useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import api from '../api.js';
import { useAuth } from '../AuthContext.jsx';
import { fmtAgo } from '../data.js';

function Bell() {
  const [data, setData] = useState({ unread: 0, items: [] });
  const [open, setOpen] = useState(false);

  const load = () => api.get('/notifications').then((r) => setData(r.data)).catch(() => {});
  useEffect(() => {
    load();
    const t = setInterval(load, 30000); // simple polling; swap for WebSocket/SSE later
    return () => clearInterval(t);
  }, []);

  const toggle = async () => {
    const next = !open;
    setOpen(next);
    if (next && data.unread > 0) {
      await api.post('/notifications/read-all').catch(() => {});
      setTimeout(load, 1500);
    }
  };

  return (
    <>
      <button className="bell" onClick={toggle} aria-label={`Notifications, ${data.unread} unread`}>
        🔔{data.unread > 0 && <span className="count">{data.unread}</span>}
      </button>
      {open && (
        <div className="panel" role="dialog" aria-label="Notifications">
          {data.items.length === 0 && <div className="item muted">No notifications yet.</div>}
          {data.items.map((n) => (
            <div key={n.id} className={`item ${n.seen ? '' : 'new'}`}>
              <b>{n.title}</b>
              <span className="small">{n.message}</span>
              <div className="small muted">{fmtAgo(n.createdAt)}</div>
            </div>
          ))}
        </div>
      )}
    </>
  );
}

export default function Navbar() {
  const { user, logout } = useAuth();
  const nav = useNavigate();
  const home = user ? { DONOR: '/donor', HOSPITAL: '/hospital', ADMIN: '/admin' }[user.role] : null;

  return (
    <header className="nav">
      <div className="wrap">
        <Link to="/" className="brand"><span className="drop" />BloodLink</Link>
        <NavLink to="/search" className="link">Find donors</NavLink>
        {home && <NavLink to={home} className="link">Dashboard</NavLink>}
        <span className="spacer" />
        {user ? (
          <>
            <Bell />
            <span className="hide-sm small">{user.fullName}</span>
            <button className="btn sm ghost-light" onClick={() => { logout(); nav('/'); }}>Log out</button>
          </>
        ) : (
          <>
            <NavLink to="/login" className="link">Log in</NavLink>
            <Link to="/register" className="btn sm primary">Become a donor</Link>
          </>
        )}
      </div>
    </header>
  );
}

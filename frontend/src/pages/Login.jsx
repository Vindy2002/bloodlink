import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api, { errMsg } from '../api.js';
import { useAuth } from '../AuthContext.jsx';

export const homeFor = (role) => ({ DONOR: '/donor', HOSPITAL: '/hospital', ADMIN: '/admin' }[role] || '/');

export default function Login() {
  const { login } = useAuth();
  const nav = useNavigate();
  const [f, setF] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      const { data } = await api.post('/auth/login', f);
      login(data);
      nav(homeFor(data.user.role));
    } catch (err) {
      setError(errMsg(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <main className="wrap page">
      <form className="card narrow" onSubmit={submit}>
        <h2>Log in</h2>
        {error && <div className="err">{error}</div>}
        <div className="field"><label htmlFor="em">Email</label>
          <input id="em" type="email" required autoComplete="username" value={f.email} onChange={(e) => setF({ ...f, email: e.target.value })} /></div>
        <div className="field"><label htmlFor="pw">Password</label>
          <input id="pw" type="password" required autoComplete="current-password" value={f.password} onChange={(e) => setF({ ...f, password: e.target.value })} /></div>
        <button className="btn primary" disabled={busy}>{busy ? 'Logging in…' : 'Log in'}</button>
        <p className="small muted" style={{ marginTop: 16 }}>New here? <Link to="/register">Create an account</Link></p>
        <p className="small muted">Demo: donor1@demo.lk / Donor@123 · hospital@kurunegala.lk / Hospital@123 · admin@bloodlink.lk / Admin@123</p>
      </form>
    </main>
  );
}

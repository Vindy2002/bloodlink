import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import api, { errMsg } from '../api.js';
import { useAuth } from '../AuthContext.jsx';
import CitySelect from '../components/CitySelect.jsx';
import { BLOOD_GROUPS, LONG_TERM, TEMPORARY, ageOf, eligibilityProblems } from '../data.js';
import { homeFor } from './Login.jsx';

function Checks({ title, hint, items, value, onToggle }) {
  return (
    <fieldset style={{ border: '1.5px solid var(--line)', borderRadius: 10, padding: '12px 14px', marginBottom: 14 }}>
      <legend className="lbl" style={{ padding: '0 6px' }}>{title}</legend>
      <p className="small muted">{hint}</p>
      {items.map((i) => (
        <label className="check" key={i.key}>
          <input type="checkbox" checked={value.includes(i.key)} onChange={() => onToggle(i.key)} />
          <span>{i.label}</span>
        </label>
      ))}
    </fieldset>
  );
}

export default function Register() {
  const [params] = useSearchParams();
  const [type, setType] = useState(params.get('type') === 'hospital' ? 'hospital' : 'donor');
  const { login } = useAuth();
  const nav = useNavigate();
  const [f, setF] = useState({ bloodGroup: 'O_POS', gender: 'Male', conditions: [], regularDonor: false });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  const setLoc = (l) => setF({ ...f, city: l.city, district: l.district, latitude: l.lat, longitude: l.lng });
  const toggle = (key) => setF({ ...f, conditions: f.conditions.includes(key) ? f.conditions.filter((c) => c !== key) : [...f.conditions, key] });

  const age = ageOf(f.dateOfBirth);
  const problems = type === 'donor'
    ? eligibilityProblems({ dob: f.dateOfBirth, weight: f.weightKg, regular: f.regularDonor, conditions: f.conditions })
    : [];

  const submit = async (e) => {
    e.preventDefault();
    if (f.latitude == null) return setError('Please choose your city (or use GPS).');
    if (problems.length) return setError('You are not eligible to register yet. See the checklist below.');
    setBusy(true);
    setError('');
    try {
      const { data } = await api.post(`/auth/register/${type}`, { ...f, weightKg: f.weightKg ? +f.weightKg : null });
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
      <form className="card narrow" onSubmit={submit} style={{ maxWidth: 680 }}>
        <h2>Create your account</h2>
        <div className="tabs" role="tablist">
          <button type="button" className={type === 'donor' ? 'on' : ''} onClick={() => setType('donor')}>I'm a donor</button>
          <button type="button" className={type === 'hospital' ? 'on' : ''} onClick={() => setType('hospital')}>I'm a hospital / blood bank</button>
        </div>
        {error && <div className="err">{error}</div>}

        {type === 'donor' ? (
          <>
            <div className="field"><label>Full name</label><input required onChange={set('fullName')} /></div>
            <div className="row">
              <div className="field"><label>Blood group</label>
                <select value={f.bloodGroup} onChange={set('bloodGroup')}>{BLOOD_GROUPS.map((g) => <option key={g.key} value={g.key}>{g.label}</option>)}</select></div>
              <div className="field"><label>Gender</label>
                <select value={f.gender} onChange={set('gender')}><option>Male</option><option>Female</option><option>Other</option></select></div>
            </div>
            <div className="row">
              <div className="field"><label>Date of birth</label><input type="date" required onChange={set('dateOfBirth')} /></div>
              <div className="field"><label>Weight (kg)</label><input type="number" min="1" max="250" required onChange={set('weightKg')} /></div>
            </div>
            {age != null && age > 55 && age <= 60 && (
              <label className="check"><input type="checkbox" checked={f.regularDonor} onChange={(e) => setF({ ...f, regularDonor: e.target.checked })} />
                <span>I have donated blood regularly before<small>People aged 56 to 60 can donate only if they were regular donors.</small></span></label>
            )}
            <div className="field"><label>Mobile (for SMS alerts)</label><input type="tel" required placeholder="07XXXXXXXX" onChange={set('phone')} /></div>

            <h3 style={{ marginTop: 8 }}>Health check</h3>
            <p className="small muted">Tick anything that applies to you. Be honest: this protects the patient and you. Your haemoglobin (12.5 g/dl or more) is tested at the blood bank before every donation.</p>
            <Checks title="Long-term conditions" hint="Do you have any of these?" items={LONG_TERM} value={f.conditions} onToggle={toggle} />
            <Checks title="Temporary reasons" hint="Does any of these apply right now?" items={TEMPORARY} value={f.conditions} onToggle={toggle} />

            {problems.length === 0 && f.dateOfBirth && f.weightKg
              ? <div className="ok-msg">You meet the basic requirements. You can donate every 4 months.</div>
              : problems.length > 0 && f.dateOfBirth && f.weightKg && (
                <div className="err"><b>You cannot register right now:</b>
                  <ul style={{ margin: '6px 0 0', paddingLeft: 18 }}>{problems.map((p) => <li key={p}>{p}</li>)}</ul></div>
              )}
          </>
        ) : (
          <>
            <div className="field"><label>Hospital name</label><input required onChange={set('hospitalName')} /></div>
            <div className="row">
              <div className="field"><label>Registration number</label><input required onChange={set('registrationNo')} /></div>
              <div className="field"><label>Contact person</label><input required onChange={set('contactName')} /></div>
            </div>
            <div className="row">
              <div className="field"><label>Blood bank phone</label><input type="tel" required onChange={set('phone')} /></div>
              <div className="field"><label>Address</label><input onChange={set('address')} /></div>
            </div>
          </>
        )}

        <div className="field"><label>{type === 'donor' ? 'Your city' : 'Hospital city'}</label><CitySelect value={f.city} onChange={setLoc} /></div>
        <div className="row">
          <div className="field"><label>Email</label><input type="email" required onChange={set('email')} /></div>
          <div className="field"><label>Password (8+ characters)</label><input type="password" minLength={8} required onChange={set('password')} /></div>
        </div>
        {type === 'hospital' && <p className="small muted">An admin will check your registration before you can send blood requests.</p>}
        <button className="btn primary" disabled={busy || (type === 'donor' && problems.length > 0)}>{busy ? 'Creating…' : 'Create account'}</button>
      </form>
    </main>
  );
}

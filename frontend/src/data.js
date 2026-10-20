export const BLOOD_GROUPS = [
  { key: 'A_POS', label: 'A+' }, { key: 'A_NEG', label: 'A−' },
  { key: 'B_POS', label: 'B+' }, { key: 'B_NEG', label: 'B−' },
  { key: 'AB_POS', label: 'AB+' }, { key: 'AB_NEG', label: 'AB−' },
  { key: 'O_POS', label: 'O+' }, { key: 'O_NEG', label: 'O−' },
];
export const bgLabel = (key) => BLOOD_GROUPS.find((b) => b.key === key)?.label ?? key;

export const CITIES = [
  { name: 'Colombo', district: 'Colombo', lat: 6.9271, lng: 79.8612 },
  { name: 'Gampaha', district: 'Gampaha', lat: 7.0873, lng: 79.9925 },
  { name: 'Negombo', district: 'Gampaha', lat: 7.2008, lng: 79.8737 },
  { name: 'Kalutara', district: 'Kalutara', lat: 6.5854, lng: 79.9607 },
  { name: 'Kandy', district: 'Kandy', lat: 7.2906, lng: 80.6337 },
  { name: 'Matale', district: 'Matale', lat: 7.4675, lng: 80.6234 },
  { name: 'Nuwara Eliya', district: 'Nuwara Eliya', lat: 6.9497, lng: 80.7891 },
  { name: 'Galle', district: 'Galle', lat: 6.0535, lng: 80.221 },
  { name: 'Matara', district: 'Matara', lat: 5.9549, lng: 80.555 },
  { name: 'Hambantota', district: 'Hambantota', lat: 6.1241, lng: 81.1185 },
  { name: 'Jaffna', district: 'Jaffna', lat: 9.6615, lng: 80.0255 },
  { name: 'Kurunegala', district: 'Kurunegala', lat: 7.4863, lng: 80.3647 },
  { name: 'Chilaw', district: 'Puttalam', lat: 7.5758, lng: 79.7953 },
  { name: 'Puttalam', district: 'Puttalam', lat: 8.0408, lng: 79.8394 },
  { name: 'Anuradhapura', district: 'Anuradhapura', lat: 8.3114, lng: 80.4037 },
  { name: 'Polonnaruwa', district: 'Polonnaruwa', lat: 7.9403, lng: 81.0188 },
  { name: 'Trincomalee', district: 'Trincomalee', lat: 8.5874, lng: 81.2152 },
  { name: 'Batticaloa', district: 'Batticaloa', lat: 7.731, lng: 81.6747 },
  { name: 'Badulla', district: 'Badulla', lat: 6.9934, lng: 81.055 },
  { name: 'Ratnapura', district: 'Ratnapura', lat: 6.6828, lng: 80.3992 },
  { name: 'Kegalle', district: 'Kegalle', lat: 7.2513, lng: 80.3464 },
];

export const fmtDate = (d) => (d ? new Date(d).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' }) : '—');
export const fmtAgo = (iso) => {
  const m = Math.round((Date.now() - new Date(iso)) / 60000);
  if (m < 1) return 'just now';
  if (m < 60) return `${m} min ago`;
  if (m < 1440) return `${Math.round(m / 60)} h ago`;
  return fmtDate(iso);
};

// ---- donor eligibility (mirrors backend Eligibility.java; the server enforces it too) ----
export const LONG_TERM = [
  { key: 'BLOODBORNE', label: 'Blood-borne infection (HIV/AIDS, Hepatitis B or C, Syphilis)' },
  { key: 'DIABETES', label: 'Diabetes' },
  { key: 'BLOOD_PRESSURE', label: 'High blood pressure' },
  { key: 'HEART', label: 'Heart disease' },
  { key: 'ASTHMA', label: 'Asthma' },
  { key: 'KIDNEY', label: 'Kidney disease' },
  { key: 'LIVER', label: 'Liver disease' },
  { key: 'EPILEPSY', label: 'Epilepsy / fits' },
];
export const TEMPORARY = [
  { key: 'PREGNANT_OR_BREASTFEEDING', label: 'Pregnant or breastfeeding' },
  { key: 'TATTOO_PIERCING_12M', label: 'Tattoo, ear or body piercing in the last 12 months' },
  { key: 'RECENT_SURGERY', label: 'Recent surgery (minor or major)' },
  { key: 'ON_MEDICATION', label: 'Taking antibiotics or other special medicines' },
  { key: 'FEVER_OR_INFECTION', label: 'Fever, cold or other infection (or only recently recovered)' },
  { key: 'RECENT_VACCINE', label: 'Had a vaccine recently' },
  { key: 'NOT_WELL', label: 'Not in good physical or mental health' },
];

export const ageOf = (dob) => {
  if (!dob) return null;
  const d = new Date(dob), n = new Date();
  let a = n.getFullYear() - d.getFullYear();
  if (n < new Date(n.getFullYear(), d.getMonth(), d.getDate())) a--;
  return a;
};

/** Returns a list of reasons the person cannot register right now (empty = OK). */
export function eligibilityProblems({ dob, weight, regular, conditions }) {
  const p = [];
  const age = ageOf(dob);
  if (age == null) p.push('Enter your date of birth.');
  else if (age < 18) p.push('You must be at least 18 years old.');
  else if (age > 60) p.push('Donation is not possible above 60 years.');
  else if (age > 55 && !regular) p.push('Above 55, only people who have donated regularly before can donate (up to 60).');
  if (!weight || +weight <= 50) p.push('Weight must be more than 50 kg.');
  conditions.forEach((c) => {
    const lt = LONG_TERM.find((x) => x.key === c);
    const tm = TEMPORARY.find((x) => x.key === c);
    if (lt) p.push(`${lt.label}: not eligible. If it is well controlled, ask the doctor at a blood donation centre.`);
    if (tm) p.push(`${tm.label}: not right now. Register again once you are clear and the waiting period is over.`);
  });
  return p;
}

import { bgLabel } from '../data.js';

export default function BloodBadge({ group, soft }) {
  return <span className={`badge ${soft ? 'soft' : ''}`}>{bgLabel(group)}</span>;
}

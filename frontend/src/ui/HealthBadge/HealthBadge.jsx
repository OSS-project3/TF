export default function HealthBadge({ health }) {
  if (health === 'ok')   return <span className="badge ok">on track</span>
  if (health === 'warn') return <span className="badge warn">at risk</span>
  if (health === 'bad')  return <span className="badge bad">late</span>
  return <span className="badge">idle</span>
}

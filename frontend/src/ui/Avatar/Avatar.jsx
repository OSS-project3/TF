import './Avatar.css'

export default function Avatar({ member, size = 28 }) {
  return (
    <span
      className="avatar"
      style={{ width: size, height: size, fontSize: Math.max(10, size * 0.4) }}
      title={member?.name}
    >
      {member?.init ?? '?'}
    </span>
  )
}

import './ProgressBar.css'

export default function ProgressBar({ value, kind }) {
  return (
    <div className="bar">
      <div
        className={'bar-fill' + (kind ? ' ' + kind : '')}
        style={{ width: `${Math.round((value || 0) * 100)}%` }}
      />
    </div>
  )
}

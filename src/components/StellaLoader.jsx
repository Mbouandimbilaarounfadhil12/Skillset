import './StellaLoader.css'

export default function StellaLoader({ message = 'Chargement…', sub = '' }) {
  return (
    <div className="stella-loader-shell">
      <div className="stella-loader-card">
        <div className="stella-s">S</div>
        {message && <p className="stella-message">{message}</p>}
        {sub && <p className="stella-sub">{sub}</p>}
      </div>
    </div>
  )
}

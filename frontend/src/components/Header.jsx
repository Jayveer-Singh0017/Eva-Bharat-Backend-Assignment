export default function Header({ cycleSeconds, activeSync }) {
  return (
    <header className="wall-header">
      <div className="header-branding">
        <h1>Media Sequencer</h1>
        <p className="header-status">
          Cycle {cycleSeconds}s
          {activeSync ? ' · sync overlay active' : ''}
        </p>
      </div>
    </header>
  )
}

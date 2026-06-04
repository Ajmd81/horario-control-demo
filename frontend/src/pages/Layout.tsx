import { Outlet, NavLink, useNavigate } from 'react-router-dom'
import { Clock, Users, Calendar, LogOut, LayoutDashboard } from 'lucide-react'
import { logout, getSession, isAdmin } from '../services/auth'
import { DemoBanner } from '../components/DemoBanner'

export function Layout() {
  const navigate = useNavigate()
  const session = getSession()

  function handleLogout() {
    logout()
    navigate('/login')
  }

  const linkStyle = ({ isActive }: { isActive: boolean }) => ({
    display: 'flex', alignItems: 'center', gap: 10,
    padding: '10px 16px', borderRadius: 8, fontSize: 14, fontWeight: 500,
    color: isActive ? '#2563eb' : '#64748b',
    background: isActive ? '#eff6ff' : 'transparent',
    textDecoration: 'none', transition: 'all .15s',
  })

  return (
    <div style={{ display: 'flex', minHeight: '100vh' }}>
      {/* Sidebar */}
      <aside style={{
        width: 220, background: '#fff', borderRight: '1px solid #e2e8f0',
        display: 'flex', flexDirection: 'column', padding: '20px 12px',
        position: 'fixed', top: 0, bottom: 0,
      }}>
        <div style={{ padding: '0 8px 24px', borderBottom: '1px solid #e2e8f0', marginBottom: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <Clock size={20} color="#2563eb" />
            <span style={{ fontWeight: 700, fontSize: 15 }}>Control Horario</span>
          </div>
          <div style={{ fontSize: 12, color: '#64748b', marginTop: 4 }}>
            {session?.empresaNombre}
          </div>
        </div>

        <nav style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 4 }}>
          <NavLink to="/dashboard" style={linkStyle}>
            <LayoutDashboard size={16} /> Dashboard
          </NavLink>
          {isAdmin() && (
            <NavLink to="/empleados" style={linkStyle}>
              <Users size={16} /> Empleados
            </NavLink>
          )}
          <NavLink to="/fichajes" style={linkStyle}>
            <Calendar size={16} /> {isAdmin() ? 'Fichajes equipo' : 'Mis fichajes'}
          </NavLink>
        </nav>

        <div style={{ borderTop: '1px solid #e2e8f0', paddingTop: 16 }}>
          <div style={{ fontSize: 13, color: '#64748b', padding: '0 8px 10px' }}>
            👤 {session?.username}
          </div>
          <button className="ghost" style={{ width: '100%', display: 'flex', alignItems: 'center', gap: 8, justifyContent: 'center' }}
            onClick={handleLogout}>
            <LogOut size={15} /> Salir
          </button>
        </div>
      </aside>

      {/* Main */}
      <div style={{ marginLeft: 220, flex: 1, display: 'flex', flexDirection: 'column' }}>
        <DemoBanner />
        <main style={{ padding: 28, flex: 1 }}>
          <Outlet />
        </main>
      </div>
    </div>
  )
}

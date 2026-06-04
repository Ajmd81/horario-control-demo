import { useEffect, useState } from 'react'
import { api } from '../services/api'
import { getSession, isAdmin } from '../services/auth'
import { Users, Clock, CheckCircle } from 'lucide-react'
import { useDemoStatus } from '../hooks/useDemoStatus'

export function DashboardPage() {
  const session = getSession()
  const { isDemo, diasRestantes } = useDemoStatus()
  const [empleados, setEmpleados] = useState<any[]>([])
  const [fichajes, setFichajes] = useState<any[]>([])

  useEffect(() => {
    if (isAdmin()) {
      api.get('/empleados').then(r => setEmpleados(r.data)).catch(() => {})
      api.get('/fichajes/equipo').then(r => setFichajes(r.data)).catch(() => {})
    } else {
      api.get('/fichajes/mis-fichajes').then(r => setFichajes(r.data)).catch(() => {})
    }
  }, [])

  const fichajesHoy = fichajes.filter(f => f.horaEntrada?.startsWith(
    new Date().toLocaleDateString('es-ES', { day:'2-digit', month:'2-digit', year:'numeric' })
  ))

  return (
    <div>
      <h1 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: 6 }}>
        Bienvenido, {session?.username} 👋
      </h1>
      <p style={{ color: '#64748b', marginBottom: 28 }}>{session?.empresaNombre}</p>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 16, marginBottom: 28 }}>
        {isAdmin() && (
          <StatCard icon={<Users size={20} />} label="Empleados activos"
            value={empleados.length} color="#2563eb" />
        )}
        <StatCard icon={<Clock size={20} />} label="Fichajes totales"
          value={fichajes.length} color="#16a34a" />
        <StatCard icon={<CheckCircle size={20} />} label="Fichajes hoy"
          value={fichajesHoy.length} color="#d97706" />
        {isDemo && (
          <StatCard icon={<Clock size={20} />} label="Días de demo restantes"
            value={diasRestantes} color={diasRestantes <= 3 ? '#dc2626' : '#2563eb'} />
        )}
      </div>

      <div className="card">
        <h2 style={{ marginBottom: 16, fontSize: '1rem', fontWeight: 600 }}>
          {isAdmin() ? 'Últimos fichajes del equipo' : 'Mis últimos fichajes'}
        </h2>
        {fichajes.length === 0
          ? <p style={{ color: '#94a3b8', fontSize: 14 }}>Sin fichajes registrados todavía.</p>
          : (
            <table>
              <thead><tr>
                {isAdmin() && <th>Empleado</th>}
                <th>Entrada</th><th>Salida</th><th>Estado</th>
              </tr></thead>
              <tbody>
                {fichajes.slice(0, 10).map((f: any) => (
                  <tr key={f.id}>
                    {isAdmin() && <td>{f.empleadoNombre}</td>}
                    <td>{f.horaEntrada}</td>
                    <td>{f.horaSalida ?? '—'}</td>
                    <td>
                      <span className={`badge ${f.cerrado ? 'green' : 'blue'}`}>
                        {f.cerrado ? 'Completo' : 'En curso'}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
      </div>
    </div>
  )
}

function StatCard({ icon, label, value, color }: any) {
  return (
    <div className="card" style={{ display: 'flex', gap: 14, alignItems: 'center' }}>
      <div style={{ background: color + '18', color, padding: 10, borderRadius: 8 }}>{icon}</div>
      <div>
        <div style={{ fontSize: 22, fontWeight: 700 }}>{value}</div>
        <div style={{ fontSize: 12, color: '#64748b' }}>{label}</div>
      </div>
    </div>
  )
}

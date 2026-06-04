import { useEffect, useState } from 'react'
import { api } from '../services/api'
import { isAdmin } from '../services/auth'

export function FichajesPage() {
  const [fichajes, setFichajes] = useState<any[]>([])
  const admin = isAdmin()

  function load() {
    const url = admin ? '/fichajes/equipo' : '/fichajes/mis-fichajes'
    api.get(url).then(r => setFichajes(r.data)).catch(() => {})
  }

  useEffect(() => { load() }, [])

  return (
    <div>
      <h1 style={{ fontSize: '1.4rem', fontWeight: 700, marginBottom: 20 }}>
        {admin ? 'Fichajes del equipo' : 'Mis fichajes'}
      </h1>
      <div className="card">
        <table>
          <thead><tr>
            {admin && <th>Empleado</th>}
            <th>Entrada</th><th>Salida</th><th>Estado</th>
          </tr></thead>
          <tbody>
            {fichajes.map((f: any) => (
              <tr key={f.id}>
                {admin && <td>{f.empleadoNombre}</td>}
                <td>{f.horaEntrada}</td>
                <td>{f.horaSalida ?? '—'}</td>
                <td><span className={`badge ${f.cerrado ? 'green' : 'blue'}`}>
                  {f.cerrado ? 'Completo' : 'En curso'}
                </span></td>
              </tr>
            ))}
            {fichajes.length === 0 && (
              <tr><td colSpan={4} style={{ textAlign: 'center', color: '#94a3b8', padding: 28 }}>
                Sin fichajes todavía.
              </td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}

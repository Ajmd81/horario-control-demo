import { useEffect, useState } from 'react'
import { api } from '../services/api'
import { useDemoStatus } from '../hooks/useDemoStatus'
import { Smartphone, UserX, Plus } from 'lucide-react'

interface Empleado {
  id: number; nombre: string; apellido: string; dni: string
  telefono: string; username: string; role: string
  activo: boolean; dispositivoVinculado: boolean
}

const emptyForm = { nombre: '', apellido: '', dni: '', telefono: '', username: '', password: '', role: 'EMPLOYEE' }

export function EmpleadosPage() {
  const [empleados, setEmpleados] = useState<Empleado[]>([])
  const [modal, setModal] = useState(false)
  const [form, setForm] = useState(emptyForm)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const { isDemo } = useDemoStatus()

  const totalTrabajadores = empleados.filter(e => e.role === 'EMPLOYEE' && e.activo).length
  const limiteAlcanzado = isDemo && totalTrabajadores >= 3

  function load() {
    api.get('/empleados').then(r => setEmpleados(r.data))
  }

  useEffect(() => { load() }, [])

  async function handleCrear(e: React.FormEvent) {
    e.preventDefault()
    setError(''); setSaving(true)
    try {
      await api.post('/empleados', form)
      setModal(false); setForm(emptyForm); load()
    } catch (err: any) {
      const msg: string = err?.response?.data?.message ?? ''
      if (msg.includes('DEMO_LIMITE_EMPLEADOS')) {
        setError('Has alcanzado el límite de 3 trabajadores en el plan demo. Activa la licencia completa para añadir más.')
      } else if (err?.response?.status === 409) {
        setError('Ese nombre de usuario ya existe en esta empresa.')
      } else {
        setError('Error al crear el empleado.')
      }
    } finally { setSaving(false) }
  }

  async function resetDispositivo(id: number) {
    if (!confirm('¿Desvincular dispositivo de este empleado?')) return
    await api.delete(`/empleados/${id}/dispositivo`)
    load()
  }

  async function desactivar(id: number) {
    if (!confirm('¿Desactivar este empleado?')) return
    await api.patch(`/empleados/${id}/desactivar`)
    load()
  }

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 20 }}>
        <div>
          <h1 style={{ fontSize: '1.4rem', fontWeight: 700 }}>Empleados</h1>
          {isDemo && (
            <p style={{ fontSize: 13, color: '#64748b', marginTop: 2 }}>
              {totalTrabajadores}/3 trabajadores usados en demo
            </p>
          )}
        </div>
        <div>
          <button className="primary" style={{ display: 'flex', alignItems: 'center', gap: 6 }}
            onClick={() => { setError(''); setModal(true) }}
            disabled={limiteAlcanzado}
            title={limiteAlcanzado ? 'Límite demo alcanzado (3/3 trabajadores)' : 'Añadir empleado'}>
            <Plus size={15} /> Nuevo empleado
          </button>
          {limiteAlcanzado && (
            <p style={{ fontSize: 12, color: '#b45309', marginTop: 6, textAlign: 'right' }}>
              ⚠️ Límite demo alcanzado.{' '}
              <a href="mailto:info@controlhorario.es?subject=Licencia">Activar licencia →</a>
            </p>
          )}
        </div>
      </div>

      <div className="card">
        <table>
          <thead><tr>
            <th>Nombre</th><th>Usuario</th><th>Rol</th>
            <th>Dispositivo</th><th>Estado</th><th>Acciones</th>
          </tr></thead>
          <tbody>
            {empleados.map(e => (
              <tr key={e.id}>
                <td><strong>{e.nombre} {e.apellido}</strong><br />
                  <span style={{ fontSize: 12, color: '#94a3b8' }}>{e.dni}</span></td>
                <td>{e.username}</td>
                <td><span className={`badge ${e.role === 'ADMIN' ? 'blue' : 'green'}`}>{e.role}</span></td>
                <td>
                  {e.dispositivoVinculado
                    ? <span className="badge green">Vinculado</span>
                    : <span className="badge red">Sin vincular</span>}
                </td>
                <td><span className={`badge ${e.activo ? 'green' : 'red'}`}>{e.activo ? 'Activo' : 'Inactivo'}</span></td>
                <td>
                  <div style={{ display: 'flex', gap: 6 }}>
                    {e.dispositivoVinculado && (
                      <button className="ghost" style={{ padding: '4px 8px', fontSize: 12 }}
                        title="Resetear dispositivo" onClick={() => resetDispositivo(e.id)}>
                        <Smartphone size={13} />
                      </button>
                    )}
                    {e.activo && (
                      <button className="ghost" style={{ padding: '4px 8px', fontSize: 12, color: '#dc2626' }}
                        title="Desactivar" onClick={() => desactivar(e.id)}>
                        <UserX size={13} />
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
            {empleados.length === 0 && (
              <tr><td colSpan={6} style={{ textAlign: 'center', color: '#94a3b8', padding: 28 }}>
                Sin empleados todavía. Crea el primero.
              </td></tr>
            )}
          </tbody>
        </table>
      </div>

      {modal && (
        <div className="modal-backdrop" onClick={e => e.target === e.currentTarget && setModal(false)}>
          <div className="modal">
            <h2>Nuevo empleado</h2>
            <form onSubmit={handleCrear}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                {[['nombre','Nombre'],['apellido','Apellido'],['dni','DNI'],['telefono','Teléfono']].map(([k,l]) => (
                  <div className="form-group" key={k} style={{ margin: 0 }}>
                    <label>{l}</label>
                    <input value={(form as any)[k]} onChange={e => setForm(p => ({ ...p, [k]: e.target.value }))}
                      required={k==='nombre'||k==='apellido'} />
                  </div>
                ))}
              </div>
              <div className="form-group" style={{ marginTop: 12 }}>
                <label>Nombre de usuario</label>
                <input value={form.username} onChange={e => setForm(p => ({ ...p, username: e.target.value }))} required />
              </div>
              <div className="form-group">
                <label>Contraseña</label>
                <input type="password" value={form.password}
                  onChange={e => setForm(p => ({ ...p, password: e.target.value }))} required minLength={6} />
              </div>
              <div className="form-group">
                <label>Rol</label>
                <select value={form.role} onChange={e => setForm(p => ({ ...p, role: e.target.value }))}>
                  <option value="EMPLOYEE">Trabajador</option>
                  <option value="ADMIN">Administrador</option>
                </select>
              </div>
              {error && <div className="error-msg">{error}</div>}
              <div className="form-actions">
                <button type="button" className="ghost" onClick={() => setModal(false)}>Cancelar</button>
                <button type="submit" className="primary" disabled={saving}>{saving ? 'Guardando…' : 'Crear'}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}

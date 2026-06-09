import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { login } from '../services/auth'
import { Clock } from 'lucide-react'

export function LoginPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ empresaSlug: '', username: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await login(form.empresaSlug, form.username, form.password)
      navigate('/dashboard')
    } catch (err: any) {
      const msg: string = err?.response?.data?.message ?? ''
      if (err?.response?.status === 403 && msg.includes('DEMO_EXPIRADA')) {
        setError('El periodo de demo de 15 días ha finalizado. Contacta con el administrador para activar tu licencia completa.')
      } else if (err?.response?.status === 401) {
        setError('Usuario o contraseña incorrectos.')
      } else {
        setError('Error de conexión. Inténtalo de nuevo.')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{
      minHeight: '100vh', display: 'flex', alignItems: 'center',
      justifyContent: 'center', background: 'linear-gradient(135deg, #eff6ff 0%, #f8fafc 100%)',
    }}>
      <div className="card" style={{ width: '100%', maxWidth: 380 }}>
        <div style={{ textAlign: 'center', marginBottom: 28 }}>
          <div style={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
            background: '#eff6ff', borderRadius: '50%', padding: 12, marginBottom: 12 }}>
            <Clock size={28} color="#2563eb" />
          </div>
          <h1 style={{ fontSize: '1.4rem', fontWeight: 700 }}>Control Horario</h1>
          <p style={{ color: '#64748b', fontSize: 14, marginTop: 4 }}>Accede a tu empresa</p>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Empresa (slug)</label>
            <input placeholder="mi-empresa" value={form.empresaSlug}
              onChange={e => setForm(p => ({ ...p, empresaSlug: e.target.value }))} required />
          </div>
          <div className="form-group">
            <label>Email</label>
            <input placeholder="admin@tuempresa.com" value={form.username}
              onChange={e => setForm(p => ({ ...p, username: e.target.value }))} required />
          </div>
          <div className="form-group">
            <label>Contraseña</label>
            <input type="password" placeholder="••••••" value={form.password}
              onChange={e => setForm(p => ({ ...p, password: e.target.value }))} required />
          </div>

          {error && <div className="error-msg">{error}</div>}

          <button className="primary" type="submit" disabled={loading}
            style={{ width: '100%', marginTop: 18, padding: '11px' }}>
            {loading ? 'Entrando…' : 'Entrar'}
          </button>
        </form>
      </div>
    </div>
  )
}

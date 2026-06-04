import { api } from './api'

export interface Session {
  token: string
  username: string
  role: string
  empresaSlug: string
  empresaNombre: string
  usuarioId: number
  empleadoId?: number
  demo?: boolean
  diasRestantesDemo?: number
}

export async function login(empresaSlug: string, username: string, password: string, deviceId?: string): Promise<Session> {
  const { data } = await api.post<Session>('/auth/login', { empresaSlug, username, password, deviceId })

  localStorage.setItem('token', data.token)
  localStorage.setItem('username', data.username)
  localStorage.setItem('role', data.role)
  localStorage.setItem('empresaSlug', data.empresaSlug)
  localStorage.setItem('empresaNombre', data.empresaNombre)
  localStorage.setItem('usuarioId', String(data.usuarioId))
  if (data.empleadoId) localStorage.setItem('empleadoId', String(data.empleadoId))
  if (data.demo) {
    localStorage.setItem('demo', 'true')
    localStorage.setItem('diasRestantesDemo', String(data.diasRestantesDemo ?? 15))
  } else {
    localStorage.removeItem('demo')
    localStorage.removeItem('diasRestantesDemo')
  }

  return data
}

export function logout() {
  localStorage.clear()
}

export function getSession(): Session | null {
  const token = localStorage.getItem('token')
  if (!token) return null
  return {
    token,
    username: localStorage.getItem('username') ?? '',
    role: localStorage.getItem('role') ?? '',
    empresaSlug: localStorage.getItem('empresaSlug') ?? '',
    empresaNombre: localStorage.getItem('empresaNombre') ?? '',
    usuarioId: Number(localStorage.getItem('usuarioId')),
    empleadoId: localStorage.getItem('empleadoId') ? Number(localStorage.getItem('empleadoId')) : undefined,
    demo: localStorage.getItem('demo') === 'true',
    diasRestantesDemo: Number(localStorage.getItem('diasRestantesDemo')) || undefined,
  }
}

export function isAdmin() { return localStorage.getItem('role') === 'ADMIN' }
export function isEmployee() { return localStorage.getItem('role') === 'EMPLOYEE' }

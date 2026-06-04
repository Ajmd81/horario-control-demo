import * as SecureStore from 'expo-secure-store'
import * as Crypto from 'expo-crypto'
import { api } from './api'

const DEVICE_KEY = 'device_id'
const TOKEN_KEY = 'token'
const SESSION_KEY = 'session'

async function getOrCreateDeviceId(): Promise<string> {
  let id = await SecureStore.getItemAsync(DEVICE_KEY)
  if (!id) {
    id = Crypto.randomUUID()
    await SecureStore.setItemAsync(DEVICE_KEY, id)
  }
  return id
}

export async function login(empresaSlug: string, username: string, password: string) {
  const deviceId = await getOrCreateDeviceId()
  const { data } = await api.post('/auth/login', { empresaSlug, username, password, deviceId })

  await SecureStore.setItemAsync(TOKEN_KEY, data.token)
  await SecureStore.setItemAsync(SESSION_KEY, JSON.stringify(data))

  return data
}

export async function logout() {
  await SecureStore.deleteItemAsync(TOKEN_KEY)
  await SecureStore.deleteItemAsync(SESSION_KEY)
}

export async function getSession() {
  const raw = await SecureStore.getItemAsync(SESSION_KEY)
  return raw ? JSON.parse(raw) : null
}

export async function isLoggedIn(): Promise<boolean> {
  const token = await SecureStore.getItemAsync(TOKEN_KEY)
  return !!token
}

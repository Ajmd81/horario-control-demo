import axios from 'axios'
import * as SecureStore from 'expo-secure-store'

export const api = axios.create({
  baseURL: process.env.EXPO_PUBLIC_API_URL ?? 'http://localhost:8080/api',
  timeout: 10000,
})

api.interceptors.request.use(async cfg => {
  const token = await SecureStore.getItemAsync('token')
  if (token) cfg.headers.Authorization = `Bearer ${token}`
  return cfg
})

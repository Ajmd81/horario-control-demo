import { useState } from 'react'
import {
  View, Text, TextInput, TouchableOpacity,
  StyleSheet, KeyboardAvoidingView, Platform, ScrollView, ActivityIndicator,
} from 'react-native'
import { router } from 'expo-router'
import { login } from '../services/auth'

export default function LoginScreen() {
  const [form, setForm] = useState({ empresaSlug: '', username: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleLogin() {
    setError('')
    if (!form.empresaSlug || !form.username || !form.password) {
      setError('Rellena todos los campos.')
      return
    }
    setLoading(true)
    try {
      await login(form.empresaSlug, form.username, form.password)
      router.replace('/(tabs)/fichaje')
    } catch (err: any) {
      const msg: string = err?.response?.data?.message ?? ''
      const status: number = err?.response?.status ?? 0
      if (status === 403 && msg.includes('DEMO_EXPIRADA')) {
        setError('El periodo de demo ha finalizado.\nContacta con el administrador para activar la licencia.')
      } else if (status === 403 && msg.includes('DEVICE_NOT_AUTHORIZED')) {
        setError('Este usuario ya está vinculado a otro dispositivo.\nContacta con el administrador.')
      } else if (status === 401) {
        setError('Usuario o contraseña incorrectos.')
      } else {
        setError('Error de conexión. Comprueba tu internet.')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={s.container} keyboardShouldPersistTaps="handled">
        {/* Logo */}
        <View style={s.logoWrap}>
          <View style={s.logoCircle}>
            <Text style={s.logoIcon}>🕐</Text>
          </View>
          <Text style={s.title}>Control Horario</Text>
          <Text style={s.subtitle}>Accede con tu cuenta</Text>
        </View>

        {/* Formulario */}
        <View style={s.card}>
          <Text style={s.label}>Empresa (slug)</Text>
          <TextInput style={s.input} placeholder="mi-empresa"
            autoCapitalize="none" autoCorrect={false}
            value={form.empresaSlug}
            onChangeText={v => setForm(p => ({ ...p, empresaSlug: v }))} />

          <Text style={s.label}>Usuario</Text>
          <TextInput style={s.input} placeholder="usuario"
            autoCapitalize="none" autoCorrect={false}
            value={form.username}
            onChangeText={v => setForm(p => ({ ...p, username: v }))} />

          <Text style={s.label}>Contraseña</Text>
          <TextInput style={s.input} placeholder="••••••"
            secureTextEntry
            value={form.password}
            onChangeText={v => setForm(p => ({ ...p, password: v }))} />

          {!!error && <View style={s.errorBox}><Text style={s.errorText}>{error}</Text></View>}

          <TouchableOpacity style={[s.btn, loading && s.btnDisabled]}
            onPress={handleLogin} disabled={loading} activeOpacity={0.8}>
            {loading
              ? <ActivityIndicator color="#fff" />
              : <Text style={s.btnText}>Entrar</Text>}
          </TouchableOpacity>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  )
}

const s = StyleSheet.create({
  container: { flexGrow: 1, justifyContent: 'center', padding: 24, backgroundColor: '#f1f5f9' },
  logoWrap: { alignItems: 'center', marginBottom: 28 },
  logoCircle: { width: 72, height: 72, borderRadius: 36, backgroundColor: '#dbeafe', justifyContent: 'center', alignItems: 'center', marginBottom: 12 },
  logoIcon: { fontSize: 34 },
  title: { fontSize: 24, fontWeight: '700', color: '#1e293b' },
  subtitle: { fontSize: 14, color: '#64748b', marginTop: 4 },
  card: { backgroundColor: '#fff', borderRadius: 16, padding: 24, shadowColor: '#000', shadowOpacity: 0.06, shadowRadius: 8, elevation: 3 },
  label: { fontSize: 13, fontWeight: '600', color: '#64748b', marginBottom: 5, marginTop: 14 },
  input: { borderWidth: 1, borderColor: '#e2e8f0', borderRadius: 10, padding: 12, fontSize: 15, backgroundColor: '#f8fafc' },
  errorBox: { backgroundColor: '#fef2f2', borderRadius: 8, padding: 12, marginTop: 14, borderLeftWidth: 3, borderLeftColor: '#ef4444' },
  errorText: { color: '#991b1b', fontSize: 13, lineHeight: 18 },
  btn: { backgroundColor: '#2563eb', borderRadius: 12, padding: 15, alignItems: 'center', marginTop: 22 },
  btnDisabled: { opacity: 0.6 },
  btnText: { color: '#fff', fontWeight: '700', fontSize: 16 },
})

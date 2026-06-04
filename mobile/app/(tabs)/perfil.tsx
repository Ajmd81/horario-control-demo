import { useEffect, useState } from 'react'
import { View, Text, TouchableOpacity, StyleSheet, Alert, ActivityIndicator } from 'react-native'
import { router } from 'expo-router'
import { getSession, logout } from '../../services/auth'

export default function PerfilScreen() {
  const [session, setSession] = useState<any>(null)

  useEffect(() => {
    getSession().then(setSession)
  }, [])

  function handleLogout() {
    Alert.alert('Cerrar sesión', '¿Seguro que quieres salir?', [
      { text: 'Cancelar', style: 'cancel' },
      {
        text: 'Salir', style: 'destructive', onPress: async () => {
          await logout()
          router.replace('/login')
        }
      }
    ])
  }

  if (!session) {
    return <View style={s.center}><ActivityIndicator color="#2563eb" /></View>
  }

  const isDemo = session.demo === true
  const diasRestantes = session.diasRestantesDemo ?? 0

  return (
    <View style={s.container}>
      {/* Banner demo */}
      {isDemo && (
        <View style={[s.demoBanner, diasRestantes <= 3 ? s.demoUrgente : diasRestantes <= 7 ? s.demoAdvertencia : s.demoNormal]}>
          <Text style={s.demoText}>
            {diasRestantes > 0
              ? `🕐 Demo · ${diasRestantes} ${diasRestantes === 1 ? 'día' : 'días'} restantes · Máx. 3 trabajadores`
              : '⚠️ ¡Demo expirada! Contacta con el administrador.'}
          </Text>
        </View>
      )}

      {/* Avatar */}
      <View style={s.avatarWrap}>
        <View style={s.avatar}>
          <Text style={s.avatarText}>{session.username?.[0]?.toUpperCase() ?? '?'}</Text>
        </View>
        <Text style={s.username}>{session.username}</Text>
        <View style={[s.roleBadge, session.role === 'ADMIN' ? s.roleAdmin : s.roleEmployee]}>
          <Text style={s.roleText}>{session.role}</Text>
        </View>
      </View>

      {/* Info */}
      <View style={s.infoCard}>
        <InfoRow label="Empresa" value={session.empresaNombre ?? session.empresaSlug} />
        <InfoRow label="Slug" value={session.empresaSlug} />
        <InfoRow label="Rol" value={session.role} />
        {isDemo && <InfoRow label="Plan" value={`Demo (${diasRestantes} días restantes)`} />}
      </View>

      <TouchableOpacity style={s.logoutBtn} onPress={handleLogout} activeOpacity={0.8}>
        <Text style={s.logoutText}>Cerrar sesión</Text>
      </TouchableOpacity>
    </View>
  )
}

function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <View style={{ flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 12, borderBottomWidth: 1, borderBottomColor: '#f1f5f9' }}>
      <Text style={{ fontSize: 14, color: '#64748b' }}>{label}</Text>
      <Text style={{ fontSize: 14, fontWeight: '600', color: '#1e293b' }}>{value}</Text>
    </View>
  )
}

const s = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f8fafc', padding: 20 },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  demoBanner: { borderRadius: 12, padding: 12, marginBottom: 16 },
  demoNormal: { backgroundColor: '#dbeafe' },
  demoAdvertencia: { backgroundColor: '#fef9c3' },
  demoUrgente: { backgroundColor: '#fee2e2' },
  demoText: { fontSize: 13, fontWeight: '600', textAlign: 'center', color: '#1e293b' },
  avatarWrap: { alignItems: 'center', marginBottom: 24 },
  avatar: { width: 80, height: 80, borderRadius: 40, backgroundColor: '#2563eb', justifyContent: 'center', alignItems: 'center', marginBottom: 12 },
  avatarText: { fontSize: 32, color: '#fff', fontWeight: '700' },
  username: { fontSize: 20, fontWeight: '700', color: '#1e293b', marginBottom: 6 },
  roleBadge: { paddingHorizontal: 12, paddingVertical: 4, borderRadius: 99 },
  roleAdmin: { backgroundColor: '#dbeafe' },
  roleEmployee: { backgroundColor: '#dcfce7' },
  roleText: { fontSize: 12, fontWeight: '700', color: '#1e40af' },
  infoCard: { backgroundColor: '#fff', borderRadius: 16, padding: 16, marginBottom: 24, shadowColor: '#000', shadowOpacity: 0.04, shadowRadius: 6, elevation: 2 },
  logoutBtn: { backgroundColor: '#fee2e2', borderRadius: 12, padding: 16, alignItems: 'center' },
  logoutText: { color: '#dc2626', fontWeight: '700', fontSize: 15 },
})

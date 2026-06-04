import { useEffect, useState, useCallback } from 'react'
import {
  View, Text, TouchableOpacity, StyleSheet,
  ActivityIndicator, Alert, ScrollView, RefreshControl,
} from 'react-native'
import * as Location from 'expo-location'
import { api } from '../../services/api'

interface FichajeActivo {
  id: number
  horaEntrada: string
  latitud?: number
  longitud?: number
}

export default function FichajeScreen() {
  const [fichajeActivo, setFichajeActivo] = useState<FichajeActivo | null>(null)
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState(false)
  const [refreshing, setRefreshing] = useState(false)
  const [tiempo, setTiempo] = useState('')
  const [hora, setHora] = useState('')

  // Reloj en tiempo real
  useEffect(() => {
    const tick = () => {
      const now = new Date()
      setHora(now.toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit', second: '2-digit' }))
      if (fichajeActivo) {
        // Calcular tiempo transcurrido desde entrada
        const [day, month, yearTime] = fichajeActivo.horaEntrada.split('/')
        const [year, time] = yearTime.split(' ')
        const entrada = new Date(`${year}-${month}-${day}T${time}:00`)
        const diff = Math.floor((Date.now() - entrada.getTime()) / 1000)
        if (diff > 0) {
          const h = Math.floor(diff / 3600)
          const m = Math.floor((diff % 3600) / 60)
          const s = diff % 60
          setTiempo(`${String(h).padStart(2,'0')}:${String(m).padStart(2,'0')}:${String(s).padStart(2,'0')}`)
        }
      }
    }
    tick()
    const id = setInterval(tick, 1000)
    return () => clearInterval(id)
  }, [fichajeActivo])

  const cargar = useCallback(async () => {
    try {
      const { data } = await api.get('/fichajes/activo')
      setFichajeActivo(data)
    } catch (err: any) {
      if (err?.response?.status === 404) setFichajeActivo(null)
    } finally {
      setLoading(false)
      setRefreshing(false)
    }
  }, [])

  useEffect(() => { cargar() }, [cargar])

  async function getLocation() {
    try {
      const { status } = await Location.requestForegroundPermissionsAsync()
      if (status !== 'granted') return null
      const loc = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.Balanced })
      return { latitud: loc.coords.latitude, longitud: loc.coords.longitude }
    } catch {
      return null
    }
  }

  async function handleEntrada() {
    setActionLoading(true)
    try {
      const loc = await getLocation()
      await api.post('/fichajes/entrada', loc ?? {})
      await cargar()
    } catch (err: any) {
      const msg = err?.response?.data?.message ?? 'Error al registrar entrada'
      Alert.alert('Error', msg)
    } finally {
      setActionLoading(false)
    }
  }

  async function handleSalida() {
    Alert.alert('Registrar salida', '¿Confirmas que quieres registrar tu salida?', [
      { text: 'Cancelar', style: 'cancel' },
      {
        text: 'Confirmar', style: 'destructive', onPress: async () => {
          setActionLoading(true)
          try {
            await api.post('/fichajes/salida')
            setFichajeActivo(null)
            setTiempo('')
          } catch (err: any) {
            Alert.alert('Error', err?.response?.data?.message ?? 'Error al registrar salida')
          } finally {
            setActionLoading(false)
          }
        }
      }
    ])
  }

  if (loading) {
    return <View style={s.center}><ActivityIndicator size="large" color="#2563eb" /></View>
  }

  const activo = !!fichajeActivo

  return (
    <ScrollView
      contentContainerStyle={s.container}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); cargar() }} />}
    >
      {/* Reloj */}
      <View style={s.clockCard}>
        <Text style={s.clockTime}>{hora}</Text>
        <Text style={s.clockDate}>
          {new Date().toLocaleDateString('es-ES', { weekday: 'long', day: 'numeric', month: 'long' })}
        </Text>
      </View>

      {/* Estado actual */}
      <View style={[s.statusCard, activo ? s.statusActivo : s.statusInactivo]}>
        <Text style={[s.statusDot, { color: activo ? '#16a34a' : '#94a3b8' }]}>
          {activo ? '●' : '○'}
        </Text>
        <View style={{ flex: 1 }}>
          <Text style={[s.statusLabel, { color: activo ? '#15803d' : '#64748b' }]}>
            {activo ? 'Trabajando' : 'Sin fichar'}
          </Text>
          {activo && fichajeActivo && (
            <>
              <Text style={s.statusSub}>Entrada: {fichajeActivo.horaEntrada}</Text>
              {!!tiempo && <Text style={s.statusTimer}>{tiempo}</Text>}
            </>
          )}
        </View>
      </View>

      {/* Botón principal */}
      <TouchableOpacity
        style={[s.mainBtn, activo ? s.btnSalida : s.btnEntrada, actionLoading && s.btnDisabled]}
        onPress={activo ? handleSalida : handleEntrada}
        disabled={actionLoading}
        activeOpacity={0.85}
      >
        {actionLoading
          ? <ActivityIndicator color="#fff" size="large" />
          : <>
              <Text style={s.mainBtnIcon}>{activo ? '⏹' : '▶'}</Text>
              <Text style={s.mainBtnText}>{activo ? 'Registrar salida' : 'Registrar entrada'}</Text>
            </>
        }
      </TouchableOpacity>

      <Text style={s.hint}>
        {activo
          ? 'Pulsa para registrar tu hora de salida'
          : 'Pulsa para iniciar tu jornada laboral'}
      </Text>
    </ScrollView>
  )
}

const s = StyleSheet.create({
  container: { flexGrow: 1, padding: 20, backgroundColor: '#f8fafc', alignItems: 'center' },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  clockCard: {
    backgroundColor: '#fff', borderRadius: 20, padding: 28, alignItems: 'center',
    width: '100%', marginBottom: 16,
    shadowColor: '#000', shadowOpacity: 0.05, shadowRadius: 10, elevation: 2,
  },
  clockTime: { fontSize: 48, fontWeight: '700', color: '#1e293b', letterSpacing: 2 },
  clockDate: { fontSize: 14, color: '#64748b', marginTop: 4, textTransform: 'capitalize' },
  statusCard: {
    flexDirection: 'row', alignItems: 'center', gap: 12,
    borderRadius: 14, padding: 16, width: '100%', marginBottom: 28,
  },
  statusActivo: { backgroundColor: '#dcfce7', borderWidth: 1, borderColor: '#bbf7d0' },
  statusInactivo: { backgroundColor: '#f1f5f9', borderWidth: 1, borderColor: '#e2e8f0' },
  statusDot: { fontSize: 22 },
  statusLabel: { fontSize: 16, fontWeight: '700' },
  statusSub: { fontSize: 13, color: '#64748b', marginTop: 2 },
  statusTimer: { fontSize: 22, fontWeight: '700', color: '#2563eb', marginTop: 4, letterSpacing: 1 },
  mainBtn: {
    width: 200, height: 200, borderRadius: 100,
    justifyContent: 'center', alignItems: 'center',
    shadowColor: '#000', shadowOpacity: 0.15, shadowRadius: 20, elevation: 6,
    marginBottom: 16,
  },
  btnEntrada: { backgroundColor: '#2563eb' },
  btnSalida: { backgroundColor: '#dc2626' },
  btnDisabled: { opacity: 0.6 },
  mainBtnIcon: { fontSize: 36, color: '#fff', marginBottom: 8 },
  mainBtnText: { fontSize: 16, fontWeight: '700', color: '#fff', textAlign: 'center' },
  hint: { fontSize: 13, color: '#94a3b8', textAlign: 'center' },
})

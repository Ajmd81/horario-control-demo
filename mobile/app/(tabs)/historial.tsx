import { useEffect, useState, useCallback } from 'react'
import {
  View, Text, FlatList, StyleSheet,
  ActivityIndicator, RefreshControl,
} from 'react-native'
import { api } from '../../services/api'

interface Fichaje {
  id: number
  horaEntrada: string
  horaSalida: string | null
  cerrado: boolean
}

export default function HistorialScreen() {
  const [fichajes, setFichajes] = useState<Fichaje[]>([])
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)

  const cargar = useCallback(async () => {
    try {
      const { data } = await api.get('/fichajes/mis-fichajes')
      setFichajes(data)
    } catch { /* silencioso */ }
    finally { setLoading(false); setRefreshing(false) }
  }, [])

  useEffect(() => { cargar() }, [cargar])

  if (loading) {
    return <View style={s.center}><ActivityIndicator size="large" color="#2563eb" /></View>
  }

  return (
    <FlatList
      data={fichajes}
      keyExtractor={f => String(f.id)}
      contentContainerStyle={s.list}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); cargar() }} />}
      ListEmptyComponent={
        <View style={s.empty}>
          <Text style={s.emptyIcon}>📋</Text>
          <Text style={s.emptyText}>Sin fichajes todavía</Text>
        </View>
      }
      renderItem={({ item }) => (
        <View style={s.card}>
          <View style={s.row}>
            <View style={{ flex: 1 }}>
              <View style={s.rowInline}>
                <Text style={s.emoji}>▶</Text>
                <Text style={s.label}>Entrada</Text>
              </View>
              <Text style={s.time}>{item.horaEntrada}</Text>
            </View>
            <View style={{ flex: 1 }}>
              <View style={s.rowInline}>
                <Text style={s.emoji}>⏹</Text>
                <Text style={s.label}>Salida</Text>
              </View>
              <Text style={s.time}>{item.horaSalida ?? '—'}</Text>
            </View>
            <View style={[s.badge, item.cerrado ? s.badgeGreen : s.badgeBlue]}>
              <Text style={[s.badgeText, { color: item.cerrado ? '#166534' : '#1e40af' }]}>
                {item.cerrado ? 'Completo' : 'En curso'}
              </Text>
            </View>
          </View>
        </View>
      )}
    />
  )
}

const s = StyleSheet.create({
  center: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  list: { padding: 16, gap: 10 },
  empty: { alignItems: 'center', paddingTop: 80 },
  emptyIcon: { fontSize: 48, marginBottom: 12 },
  emptyText: { fontSize: 16, color: '#94a3b8' },
  card: {
    backgroundColor: '#fff', borderRadius: 14, padding: 16,
    shadowColor: '#000', shadowOpacity: 0.04, shadowRadius: 6, elevation: 2,
  },
  row: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  rowInline: { flexDirection: 'row', alignItems: 'center', gap: 4, marginBottom: 2 },
  emoji: { fontSize: 11, color: '#64748b' },
  label: { fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5 },
  time: { fontSize: 14, fontWeight: '600', color: '#1e293b' },
  badge: { paddingHorizontal: 10, paddingVertical: 4, borderRadius: 99 },
  badgeGreen: { backgroundColor: '#dcfce7' },
  badgeBlue: { backgroundColor: '#dbeafe' },
  badgeText: { fontSize: 11, fontWeight: '700' },
})

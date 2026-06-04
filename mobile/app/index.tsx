import { useEffect } from 'react'
import { router } from 'expo-router'
import { View, ActivityIndicator } from 'react-native'
import { isLoggedIn } from '../services/auth'

export default function Index() {
  useEffect(() => {
    isLoggedIn().then(ok => {
      router.replace(ok ? '/(tabs)/fichaje' : '/login')
    })
  }, [])

  return (
    <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: '#f8fafc' }}>
      <ActivityIndicator size="large" color="#2563eb" />
    </View>
  )
}

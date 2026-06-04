import { Tabs } from 'expo-router'
import { Text } from 'react-native'

function Icon({ emoji, focused }: { emoji: string; focused: boolean }) {
  return <Text style={{ fontSize: 20, opacity: focused ? 1 : 0.5 }}>{emoji}</Text>
}

export default function TabsLayout() {
  return (
    <Tabs screenOptions={{
      tabBarActiveTintColor: '#2563eb',
      tabBarInactiveTintColor: '#94a3b8',
      tabBarStyle: { borderTopColor: '#e2e8f0', paddingBottom: 4 },
      headerStyle: { backgroundColor: '#fff' },
      headerTitleStyle: { fontWeight: '700', color: '#1e293b' },
      headerShadowVisible: false,
    }}>
      <Tabs.Screen
        name="fichaje"
        options={{
          title: 'Fichar',
          tabBarIcon: ({ focused }) => <Icon emoji="🕐" focused={focused} />,
        }}
      />
      <Tabs.Screen
        name="historial"
        options={{
          title: 'Historial',
          tabBarIcon: ({ focused }) => <Icon emoji="📋" focused={focused} />,
        }}
      />
      <Tabs.Screen
        name="perfil"
        options={{
          title: 'Perfil',
          tabBarIcon: ({ focused }) => <Icon emoji="👤" focused={focused} />,
        }}
      />
    </Tabs>
  )
}

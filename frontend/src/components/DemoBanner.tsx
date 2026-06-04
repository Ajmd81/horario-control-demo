import { useDemoStatus } from '../hooks/useDemoStatus'

export function DemoBanner() {
  const { isDemo, diasRestantes, diasTotales, porcentaje } = useDemoStatus()
  if (!isDemo) return null

  const urgente = diasRestantes <= 3
  const advertencia = diasRestantes <= 7

  const bg = urgente ? '#fef2f2' : advertencia ? '#fffbeb' : '#eff6ff'
  const border = urgente ? '#ef4444' : advertencia ? '#f59e0b' : '#3b82f6'
  const color = urgente ? '#991b1b' : advertencia ? '#92400e' : '#1e40af'

  return (
    <div style={{
      background: bg, borderLeft: `4px solid ${border}`, color,
      padding: '10px 20px', fontSize: 13, display: 'flex',
      alignItems: 'center', gap: 12, flexWrap: 'wrap',
      position: 'sticky', top: 0, zIndex: 100,
    }}>
      <span>{urgente ? '⚠️' : '🕐'}</span>
      <span style={{ flex: 1 }}>
        <strong>Modo Demo</strong> — {diasRestantes > 0
          ? <>Te quedan <strong>{diasRestantes} {diasRestantes === 1 ? 'día' : 'días'}</strong> · Máx. 3 trabajadores</>
          : <strong>¡Demo expirada!</strong>}
      </span>
      <div style={{ width: 100, height: 5, background: 'rgba(0,0,0,.12)', borderRadius: 3, overflow: 'hidden' }}>
        <div style={{ width: `${porcentaje}%`, height: '100%', background: border, borderRadius: 3 }} />
      </div>
      <a href="mailto:info@controlhorario.es?subject=Licencia"
         style={{ padding: '5px 12px', background: border, color: '#fff', borderRadius: 6, fontWeight: 600, fontSize: 12 }}>
        Activar licencia →
      </a>
    </div>
  )
}

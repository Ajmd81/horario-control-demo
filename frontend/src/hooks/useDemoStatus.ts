export function useDemoStatus() {
  const isDemo = localStorage.getItem('demo') === 'true'
  const diasRestantes = isDemo ? (Number(localStorage.getItem('diasRestantesDemo')) || 0) : -1
  const diasTotales = isDemo ? (Number(localStorage.getItem('diasTotalesDemo')) || 15) : 15
  const isExpirada = isDemo && diasRestantes <= 0
  const pct = isDemo && diasTotales > 0 ? Math.round(((diasTotales - diasRestantes) / diasTotales) * 100) : 0

  return { isDemo, diasRestantes, diasTotales, isExpirada, porcentaje: pct }
}
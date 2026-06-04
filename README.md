# Control Horario Lite

Sistema de fichaje con **modo demo de 15 días** (1 admin + máx. 3 trabajadores).

---

## Stack

| Capa | Tecnología | Deploy |
|------|-----------|--------|
| Backend | Spring Boot 3.3 / Java 21 | Railway |
| Web | React 18 + Vite + TypeScript | Vercel |
| Mobile | React Native + Expo SDK 53 | EAS Build |

---

## Despliegue paso a paso

### 1 — Backend en Railway

1. Crea un nuevo proyecto en Railway, añade un servicio **PostgreSQL**.
2. Añade un servicio **Web** apuntando a este repo (carpeta raíz: `backend/`).
3. Variables de entorno obligatorias:

```
JDBC_DATABASE_URL=jdbc:postgresql://<host>:<puerto>/<db>
DB_USER=postgres
DB_PASS=<password>
JWT_SECRET=<genera con: openssl rand -base64 64 | tr -d '\n'>
ALLOWED_ORIGINS=https://<tu-app>.vercel.app
SPRING_PROFILES_ACTIVE=prod
```

4. Al arrancar, el backend crea automáticamente el usuario **superadmin / superadmin123**.
   ⚠️ Cámbialo inmediatamente desde la base de datos.

---

### 2 — Crear una empresa demo

Con Postman (o cualquier cliente HTTP):

```http
POST https://<tu-api>.up.railway.app/api/superadmin/empresas/demo
Authorization: Bearer <token_superadmin>

{
  "nombreEmpresa": "Burger Río",
  "slug": "burger-rio",
  "adminUsername": "admin",
  "adminPassword": "Admin123",
  "diasDemo": 15,
  "maxEmpleados": 3
}
```

Para obtener el token del superadmin:

```http
POST /api/auth/login
{ "empresaSlug": "", "username": "superadmin", "password": "superadmin123" }
```
> El superadmin no tiene empresa, deja `empresaSlug` vacío o `"superadmin"`.

---

### 3 — Frontend Web en Vercel

1. Importa el repo en Vercel, **Root Directory**: `frontend/`
2. Variable de entorno:
   ```
   VITE_API_URL=https://<tu-api>.up.railway.app/api
   ```
3. Deploy automático.

---

### 4 — App Mobile (APK de prueba)

1. Edita `mobile/eas.json` → cambia `EXPO_PUBLIC_API_URL` por tu URL de Railway.
2. Crea el build:
   ```bash
   cd mobile
   npm install
   npx eas-cli@latest build --platform android --profile preview
   ```
3. Descarga el APK desde el panel de EAS y compártelo por QR.

---

## API — Endpoints principales

### Auth
| Método | Ruta | Acceso |
|--------|------|--------|
| POST | `/api/auth/login` | Público |

### Empleados
| Método | Ruta | Acceso |
|--------|------|--------|
| GET | `/api/empleados` | ADMIN |
| POST | `/api/empleados` | ADMIN |
| DELETE | `/api/empleados/{id}/dispositivo` | ADMIN |
| PATCH | `/api/empleados/{id}/desactivar` | ADMIN |

### Fichajes
| Método | Ruta | Acceso |
|--------|------|--------|
| POST | `/api/fichajes/entrada` | EMPLOYEE |
| POST | `/api/fichajes/salida` | EMPLOYEE |
| GET | `/api/fichajes/activo` | EMPLOYEE |
| GET | `/api/fichajes/mis-fichajes` | EMPLOYEE |
| GET | `/api/fichajes/equipo` | ADMIN |

### Superadmin
| Método | Ruta | Acceso |
|--------|------|--------|
| POST | `/api/superadmin/empresas/demo` | SUPERADMIN |
| GET | `/api/superadmin/empresas` | SUPERADMIN |
| PATCH | `/api/superadmin/empresas/{id}/activar-licencia` | SUPERADMIN |

---

## Lógica demo

| Restricción | Dónde se aplica |
|-------------|-----------------|
| 15 días desde creación | Backend: `AuthService.login()` → HTTP 403 `DEMO_EXPIRADA` |
| Máx. 3 trabajadores | Backend: `EmpleadoService.crear()` → HTTP 400 `DEMO_LIMITE_EMPLEADOS` |
| Banner con cuenta atrás | Web: `DemoBanner.tsx` (sticky, cambia de color) |
| Botón deshabilitado | Web: `EmpleadosPage.tsx` al llegar a 3/3 |
| Banner en app | Mobile: pantalla Perfil |
| Errores claros en login | Web + Mobile: mensajes distintos por código de error |

---

## Activar licencia completa (quitar demo)

```http
PATCH /api/superadmin/empresas/{id}/activar-licencia
Authorization: Bearer <token_superadmin>
```

Esto elimina todas las restricciones de la empresa.

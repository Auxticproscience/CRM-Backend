# CRM Gestión por Zonas — Backend

Spring Boot 3.4 + PostgreSQL (NeonDB) para la carga diaria de actividades desde Excel.

---

## Requisitos

| Herramienta | Versión mínima |
|-------------|---------------|
| Java        | 21            |
| Maven       | 3.9 (o usar el wrapper `mvnw`) |

---

## Arrancar el proyecto

```bash
# Clonar / descomprimir el proyecto y entrar a la carpeta
cd crm-zonas

# Compilar y levantar
./mvnw spring-boot:run
```

El servidor queda en `http://localhost:8080`.

---

## Endpoints disponibles

### Cargar archivo Excel diario

```
POST /api/excel/cargar
Content-Type: multipart/form-data
Param: file  →  archivo .xlsx
```

**Ejemplo con curl:**
```bash
curl -X POST http://localhost:8080/api/excel/cargar \
  -F "file=@reporte_diario.xlsx"
```

**Respuesta exitosa:**
```json
{
  "nombreArchivo":      "reporte_diario.xlsx",
  "registrosCargados":  42,
  "registrosOmitidos":  3,
  "fechaCarga":         "2025-07-01T14:30:00Z",
  "notas":              "Fila 5: fecha vacía | Fila 12: propietario vacío"
}
```

---

### Consultar actividades

```
GET /api/actividades
GET /api/actividades?propietarioId=1    ← filtrar por asesor
```

---

### Historial de cargas (últimas 10)

```
GET /api/actividades/historial-cargas
```

---

## Estructura esperada del Excel

La primera fila debe ser el encabezado (se omite automáticamente).
Las columnas deben estar **en este orden**:

| Columna | Campo            | Obligatorio |
|---------|------------------|-------------|
| A (0)   | Fecha de creación | ✅          |
| B (1)   | Nombre / Asunto   | ✅          |
| C (2)   | Descripción       |             |
| D (3)   | Estado            |             |
| E (4)   | Tipo              |             |
| F (5)   | Propietario       | ✅          |
| G (6)   | Cliente           |             |
| H (7)   | Lugar             |             |

> Si las columnas están en otro orden, cambia las constantes `COL_*`
> en `ExcelParserService.java`.

**Formatos de fecha aceptados:**
`yyyy-MM-dd`, `dd/MM/yyyy`, `MM/dd/yyyy`, `dd-MM-yyyy`, `yyyy/MM/dd`
y celdas con formato de fecha nativo de Excel.

---

## Deduplicación

Una fila se omite (sin error) si ya existe en la base de datos
una actividad con la misma combinación de **fecha + nombre + propietario**.
Esto permite cargar el mismo archivo varias veces sin crear duplicados.

---

## Catálogos auto-creados

Estados, Tipos, Clientes y Lugares que aparezcan en el Excel
pero no existan en la base de datos **se crean automáticamente**.
Lo mismo aplica para nuevos Propietarios.

---

## Variables de entorno (opcional para producción)

En lugar de dejar credenciales en `application.properties`,
puedes usar variables de entorno:

```bash
export DB_URL=jdbc:postgresql://...
export DB_USER=neondb_owner
export DB_PASS=tu_password
```

Y en `application.properties`:
```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASS}
```

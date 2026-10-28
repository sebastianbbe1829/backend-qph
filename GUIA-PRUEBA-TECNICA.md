# Guía de bolsillo para la prueba técnica

## Archivos clave

### Java / backend

- `src/main/java/com/example/demo/controller/`: endpoints REST de productos, categorías y autenticación.
- `src/main/java/com/example/demo/service/`: lógica de negocio.
- `src/main/java/com/example/demo/repository/`: acceso a datos con Spring Data JPA.
- `src/main/java/com/example/demo/entity/`: entidades y mapeo de tablas. Productos y categorías usan el esquema `qph_prueba`.
- `src/main/java/com/example/demo/dto/`: contratos de entrada y salida; las restricciones de entrada se validan con Jakarta Validation.
- `src/main/java/com/example/demo/security/` y `config/SecurityConfig.java`: filtro JWT y configuración de acceso.
- `src/main/java/com/example/demo/exception/GlobalExceptionHandler.java`: traducción de excepciones a respuestas HTTP.
- `src/main/resources/application.properties`: configuración local de PostgreSQL, esquema y JWT.
- `database/product_db.sql` y `database/categories.sql`: creación de las tablas.
- `src/test/java/com/example/demo/controller/ProductControllerIntegrationTest.java`: pruebas de integración de productos.

### Angular / frontend

No hay código Angular en este repositorio. En el repositorio frontend, busca los archivos equivalentes a `src/app/app.routes.ts` o `app-routing.module.ts` (rutas), el servicio HTTP de productos, los componentes de listado/formulario y los interceptores HTTP (token JWT y errores). Los nombres dependen de la versión y estructura de ese proyecto.

## Comandos PowerShell

Desde la raíz del backend:

```powershell
# Ejecutar todas las pruebas; usan H2 y no necesitan PostgreSQL local
.\mvnw.cmd test

# Iniciar el backend
.\mvnw.cmd spring-boot:run

# Empaquetar
.\mvnw.cmd package
```

Ejemplo de login y consulta con PowerShell:

```powershell
$login = Invoke-RestMethod -Method Post `
  -Uri 'http://localhost:8080/api/auth/login' `
  -ContentType 'application/json' `
  -Body (@{ username = 'admin'; password = '<contraseña-configurada>' } | ConvertTo-Json)

$headers = @{ Authorization = "Bearer $($login.token)" }
Invoke-RestMethod -Method Get -Uri 'http://localhost:8080/api/products' -Headers $headers
```

## Flujo de una petición

1. El cliente llama a `/api/auth/login` y recibe un JWT.
2. Envía el token en `Authorization: Bearer <token>`.
3. `JwtAuthFilter` valida el token y establece la autenticación.
4. El controller recibe y valida el DTO, y delega en el service.
5. El service aplica la lógica y usa el repository.
6. JPA consulta PostgreSQL: productos en `qph_prueba.products` y categorías en `qph_prueba.categories`.
7. El controller responde con JSON; `GlobalExceptionHandler` convierte errores conocidos a HTTP.

## Diagnóstico rápido

| Síntoma | Revisar |
|---|---|
| `401 Unauthorized` | Login, token expirado o formato del header `Bearer`. |
| `400 Bad Request` | Campos requeridos y restricciones como precio positivo o stock no negativo. |
| `404 Not Found` | ID solicitado y mensaje del handler de recursos inexistentes. |
| `409 Conflict` al crear categoría | Nombre duplicado, ignorando mayúsculas/minúsculas. |
| Error SQL de tabla/esquema | Que existan `qph_prueba.products` y `qph_prueba.categories`; `ddl-auto=none` no crea tablas. |
| La API no inicia | Revisa logs, puerto 8080, URL/credenciales de PostgreSQL y conectividad. |

## Lista de chequeo

- [ ] Base de datos accesible y tablas creadas con los SQL del repositorio.
- [ ] Backend inicia sin errores y las pruebas terminan con `BUILD SUCCESS`.
- [ ] Login devuelve un token y las peticiones protegidas lo envían como `Bearer`.
- [ ] Probar listado, detalle, creación, actualización y eliminación de un producto.
- [ ] Probar una entrada inválida y confirmar respuesta `400`.
- [ ] Probar un ID inexistente y confirmar respuesta `404`.
- [ ] Verificar en el frontend que la URL base, el servicio HTTP y el interceptor coinciden con la API.
- [ ] No compartir ni subir secretos o credenciales de entorno.

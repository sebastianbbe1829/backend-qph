# Guía rápida del backend

## Estructura

- `controller`: endpoints REST.
- `service`: casos de uso y reglas de negocio.
- `repository`: persistencia Spring Data JPA.
- `entity`: entidades relacionales.
- `dto`: contratos HTTP y validaciones.
- `security` / `config`: autenticación JWT y permisos.
- `exception`: traducción de errores a HTTP.
- `database/schema.sql`: esquema limpio de instalación.

## Preparación

1. Crea `products_db` en PostgreSQL.
2. Ejecuta `database/schema.sql` en esa base.
3. Configura `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` y `JWT_SECRET`.
4. Inicializa el primer administrador con `APP_BOOTSTRAP_ADMIN_ENABLED=true`, `APP_BOOTSTRAP_ADMIN_USERNAME` y `APP_BOOTSTRAP_ADMIN_PASSWORD`.
5. Desactiva el bootstrap y elimina las variables de credenciales luego de crear la cuenta.

El bootstrap solo funciona cuando `users` está vacío y guarda la contraseña con BCrypt. No hay cuentas ni registros de prueba insertados por el script SQL.

## Flujo de solicitud

1. El cliente inicia sesión en `POST /api/auth/login` y recibe un JWT.
2. Envía el token con las llamadas protegidas.
3. `JwtAuthFilter` valida el token y establece la identidad del usuario.
4. El controller valida el DTO y delega en el service.
5. El service aplica las reglas de negocio y usa los repositories.
6. JPA persiste en PostgreSQL; las claves foráneas y restricciones refuerzan la integridad relacional.
7. El handler global traduce errores conocidos a respuestas HTTP.

## Venta y stock

`POST /api/sales` recibe una lista de productos y cantidades. El backend usa el usuario autenticado y los precios actuales leídos del catálogo. En una transacción bloquea los productos en orden de ID, valida stock, descuenta las unidades y crea la venta y sus detalles. Si una línea falla, la operación completa se revierte. El cliente no determina el precio, el usuario ni el stock final.

## Pruebas y ejecución (PowerShell)

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Errores comunes:

| HTTP | Causa frecuente |
|---|---|
| `400` | DTO inválido, cantidades no positivas o campos requeridos ausentes |
| `401` | Credenciales inválidas o token ausente/expirado |
| `403` | Rol sin permiso para la operación |
| `404` | Producto no encontrado |
| `409` | Stock insuficiente o violación de una restricción de integridad |

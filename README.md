# API de ventas y productos

API REST con Spring Boot, PostgreSQL, Spring Security, JWT y JPA. Gestiona productos, usuarios y ventas de varios productos por operación. La venta y el descuento de stock se procesan en una única transacción.

## Arquitectura

El backend está organizado en capas:

- `controller`: endpoints HTTP y validación de entrada.
- `service`: casos de uso y reglas de negocio.
- `repository`: acceso a datos con Spring Data JPA.
- `entity`: modelo persistente.
- `dto`: contratos de entrada y salida.
- `security` y `config`: autenticación JWT y autorización.
- `exception`: traducción de errores a respuestas HTTP.

Para el alcance actual, un monolito modular por dominio es más sencillo de operar que microservicios. La separación por capas ya evita acoplar los controladores directamente a JPA; el siguiente paso de organización debería ser agrupar las clases por dominio (`users`, `products`/`inventory`, `sales`) cuando el tamaño lo amerite, sin añadir capas vacías.

## Modelo relacional

El esquema mínimo consta de cuatro tablas:

```text
users 1 ─── * sales 1 ─── * sale_items * ─── 1 products
```

- `users`: usuario, hash BCrypt y rol.
- `products`: nombre, precio vigente y stock disponible.
- `sales`: usuario autenticado que registró la operación y fecha.
- `sale_items`: producto, cantidad y precio unitario histórico de cada línea.

Las claves foráneas están definidas en la base de datos: ventas requieren un usuario; las líneas requieren una venta y un producto. Borrar una venta elimina sus líneas; el usuario y el producto no se pueden borrar si tienen historial asociado. También existen restricciones para unicidad de usuario/producto por venta, roles permitidos, precios positivos, cantidades positivas y stock no negativo. El script no carga datos iniciales.

Cada línea conserva su precio de venta para no alterar el historial cuando cambie el precio del catálogo. El total se deriva de las líneas y no se duplica como dato persistido.

Consulta [database/README.md](database/README.md) y ejecuta [database/schema.sql](database/schema.sql) solo al crear una instalación nueva.

## Crear la base e iniciar

Requisitos: JDK 21, Maven 3.9+ y PostgreSQL 15+.

```sql
CREATE DATABASE products_db;
```

Conecta a `products_db` y ejecuta `database/schema.sql`. Luego configura estas variables de entorno:

| Variable | Uso |
|---|---|
| `DB_URL` | URL JDBC; por ejemplo `jdbc:postgresql://localhost:5432/products_db?currentSchema=qph_prueba` |
| `DB_USERNAME` | Usuario de la base |
| `DB_PASSWORD` | Contraseña de la base |
| `JWT_SECRET` | Secreto Base64 seguro para firmar tokens |
| `APP_BOOTSTRAP_ADMIN_ENABLED` | `true` solo durante la inicialización del primer administrador |
| `APP_BOOTSTRAP_ADMIN_USERNAME` | Nombre inicial del administrador |
| `APP_BOOTSTRAP_ADMIN_PASSWORD` | Contraseña inicial; mínimo 8 y máximo 72 caracteres |

En PowerShell, ejecuta la aplicación con:

```powershell
.\mvnw.cmd spring-boot:run
```

Después de que la primera cuenta se haya creado, desactiva `APP_BOOTSTRAP_ADMIN_ENABLED` y elimina las variables de credenciales de inicialización. El proceso de bootstrap falla de forma explícita si la tabla ya contiene cuentas: no reemplaza ni modifica usuarios existentes. No guardes estas variables en el repositorio.

## API

| Método | Ruta | Acceso | Función |
|---|---|---|---|
| `POST` | `/api/auth/login` | Público | Iniciar sesión y obtener JWT |
| `POST` | `/api/users` | Administrador | Crear usuario |
| `GET` | `/api/products` | Administrador | Listar productos |
| `GET` | `/api/products/{id}` | Usuario o administrador | Consultar producto |
| `POST` | `/api/products` | Administrador | Crear producto |
| `PUT` | `/api/products/{id}` | Administrador | Actualizar producto |
| `DELETE` | `/api/products/{id}` | Administrador | Eliminar producto sin ventas asociadas |
| `POST` | `/api/sales` | Usuario o administrador | Registrar venta |

El cuerpo de venta contiene `items`, una lista de `{ "productId": id, "quantity": cantidad }`. El servidor obtiene el usuario del JWT y el precio del producto persistido; no acepta como autoridad el stock, el usuario o el total enviados por el cliente.

## Integridad y concurrencia del stock

`POST /api/sales` ejecuta la operación dentro de una transacción. Valida la lista y las cantidades, bloquea los productos en orden de ID para evitar interbloqueos, verifica disponibilidad, descuenta el stock y guarda la venta con sus líneas. Si algún producto no existe o no tiene stock suficiente, no se guarda ninguna parte de la venta. La falta de stock responde `409 Conflict`.

El frontend solo pre-valida cantidades para dar una mejor experiencia; la base de datos y el backend son la autoridad. Los `CHECK` protegen las invariantes básicas incluso ante escrituras fuera de la API. El bloqueo pesimista serializa ventas concurrentes del mismo producto.

## Mejoras recomendadas

1. Mantener el stock solo en operaciones del dominio de inventario; hoy el formulario de edición puede sobrescribirlo. En una siguiente iteración, separar datos de catálogo y ajustes de existencias, y registrar movimientos de inventario si se requiere auditoría.
2. Añadir pruebas de integración de venta con PostgreSQL (por ejemplo, Testcontainers), incluyendo dos ventas concurrentes y rollback en una venta de varios productos. Las pruebas actuales unitarias no demuestran el comportamiento real de bloqueo/constraints del motor.
3. Añadir versionado/migraciones de esquema con Flyway o Liquibase cuando el modelo deje de ser exclusivamente de instalación limpia; no usar `ddl-auto=update` en producción.
4. Añadir paginación y filtros del lado servidor si el catálogo crece, además de consultas de historial de ventas.
5. Centralizar secretos y configuración por entorno; validar que JWT tenga un secreto no vacío y suficientemente seguro fuera de pruebas.
6. Usar `Instant`/`TIMESTAMPTZ` para fechas si se requiere operación en varias zonas horarias; acordar el formato de API antes del cambio.
7. Eliminar RabbitMQ del stack actual: no había productores ni consumidores implementados, por lo que la dependencia no entregaba funcionalidad. Incorporar mensajería solo con un caso de uso y estrategia de consistencia definidos, como transactional outbox.

## Pruebas

```powershell
.\mvnw.cmd test
```

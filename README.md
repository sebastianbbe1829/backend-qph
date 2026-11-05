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

## Requisitos

- Windows 10/11.
- JDK 21 en `PATH`.
- Node.js `^20.19.0`, `^22.12.0` o `>=24.0.0`, y npm (rangos aceptados por Angular CLI 21).
- Acceso de red a la base PostgreSQL configurada.
- Puertos locales 8080 (backend) y 4200 (frontend) disponibles.

Maven se ejecuta mediante el wrapper incluido; no es necesario instalarlo por separado.

## Levantar el proyecto desde cero

1. Clona ambos repositorios en carpetas hermanas y conserva los nombres `backend-qph` y `frontend-qph`. Puedes elegir la ubicación; por ejemplo, `C:\git\backend-qph` y `C:\git\frontend-qph`.
2. Inicializa la base vacía y el primer administrador ejecutando `bootstrap_admin_qph.bat` desde la carpeta `backend-qph`. El script crea el esquema de las cuatro tablas si hace falta, pide la contraseña actual de PostgreSQL y solicita el nombre y la contraseña del administrador. Las contraseñas no se muestran. La cuenta se guarda con BCrypt. Solo funciona cuando la tabla de usuarios está vacía; no borra ni reinicia datos existentes.
3. Cuando la base y el administrador estén listos, ejecuta `iniciar_prueba_qph.bat` desde la carpeta `backend-qph`. El lanzador instala automáticamente las dependencias del frontend con `npm ci` si no están instaladas o si cambió el archivo `package-lock.json`. Luego abre consolas separadas para backend y frontend y abre `/login` en el navegador cuando Angular responde por HTTP.
4. Inicia sesión con las credenciales de administrador. Desde la aplicación puedes registrar productos y usuarios y probar ventas.

No vuelvas a ejecutar el bootstrap después de crear el administrador. Para los siguientes usos, ejecuta directamente `iniciar_prueba_qph.bat`; no es necesario ejecutar `npm ci` manualmente.

El esquema es idempotente para esta instalación: se crean las tablas que falten sin vaciar tablas que ya existan. Para preparar manualmente una base PostgreSQL local, crea `products_db`, conecta a esa base y ejecuta `database/schema.sql`.

## Configuración de conexión y secretos

Los scripts usan por defecto el endpoint Neon de este proyecto. El lanzador pide la contraseña de PostgreSQL de forma interactiva; no la escribas en los archivos `.bat`, `.ps1` ni en Git. Puedes establecer `DB_URL` y `DB_USERNAME` antes de iniciar si necesitas otra base o usuario. HikariCP recicla sus conexiones cada cuatro minutos y comprueba las conexiones ociosas cada minuto para evitar reutilizar conexiones cerradas por el pooler de Neon.

Si `JWT_SECRET` no está definido, el lanzador genera una clave temporal. Para conservar sesiones JWT después de reiniciar, define una clave segura y persistente como variable de entorno. La contraseña nueva de Neon se ingresa en el prompt del script; no uses la credencial que fue expuesta previamente.

Si la espera se agota, deja abiertas las consolas y revisa el error original que muestran. Angular se inicia en `localhost:4200` y el lanzador comprueba esa misma dirección antes de abrir `/login`.

La aplicación también puede configurarse directamente con estas variables de entorno, definidas en `src/main/resources/application.yml`:

| Variable | Uso |
|---|---|
| `DB_URL` | URL JDBC; por ejemplo `jdbc:postgresql://localhost:5432/products_db?currentSchema=qph_prueba` |
| `DB_USERNAME` | Usuario de la base |
| `DB_PASSWORD` | Contraseña de la base |
| `APP_CORS_ALLOWED_ORIGINS` | Orígenes frontend separados por coma; por defecto `http://localhost:4200` |
| `JWT_SECRET` | Secreto Base64 seguro para firmar tokens |
| `APP_BOOTSTRAP_ADMIN_ENABLED` | `true` solo durante la inicialización del primer administrador |
| `APP_BOOTSTRAP_ADMIN_USERNAME` | Nombre inicial del administrador |
| `APP_BOOTSTRAP_ADMIN_PASSWORD` | Contraseña inicial; mínimo 8 y máximo 72 caracteres |
| `LOG_FILE_PATH` | Ruta del archivo de logs; por defecto `logs/prueba-tecnica.log` relativa al directorio desde donde inicia el backend |

En PowerShell, ejecuta la aplicación con:

```powershell
.\mvnw.cmd spring-boot:run
```

Después de que la primera cuenta se haya creado, desactiva `APP_BOOTSTRAP_ADMIN_ENABLED` y elimina las variables de credenciales de inicialización. El proceso de bootstrap falla de forma explícita si la tabla ya contiene cuentas: no reemplaza ni modifica usuarios existentes. No guardes estas variables en el repositorio.

Los logs se escriben en consola y en archivo, con rotación diaria y retención de 365 días. Por defecto, el archivo queda en `logs/prueba-tecnica.log` bajo el directorio de trabajo del backend. En el servidor configura `LOG_FILE_PATH` con una ruta absoluta en un directorio persistente y asegúrate de que la cuenta que ejecuta la aplicación tenga permisos de escritura.

## API

| Método | Ruta | Acceso | Función |
|---|---|---|---|
| `POST` | `/api/auth/login` | Público | Iniciar sesión y obtener JWT |
| `GET` | `/api/users` | Administrador | Listar usuarios (ID, nombre y rol) |
| `POST` | `/api/users` | Administrador | Crear usuario |
| `GET` | `/api/products` | Administrador | Listar productos |
| `GET` | `/api/products/{id}` | Usuario o administrador | Consultar producto |
| `POST` | `/api/products` | Administrador | Crear producto |
| `PUT` | `/api/products/{id}` | Administrador | Actualizar producto |
| `DELETE` | `/api/products/{id}` | Administrador | Eliminar producto sin ventas asociadas |
| `POST` | `/api/sales` | Usuario o administrador | Registrar venta |

El cuerpo de venta contiene `items`, una lista de `{ "productId": id, "quantity": cantidad }`. El servidor obtiene el usuario del JWT y el precio del producto persistido; no acepta como autoridad el stock, el usuario o el total enviados por el cliente.

Los endpoints `GET /api/products` y `GET /api/users` devuelven resultados paginados. Aceptan `page` (base cero, por defecto `0`), `size` (por defecto `20`, máximo `100`), `sort`, `direction` (`ASC` o `DESC`) y `search`. Productos permiten ordenar por `id`, `nombre`, `precio` o `stock`; usuarios por `id`, `username` o `role`. La respuesta contiene `content`, `page`, `size`, `totalElements`, `totalPages`, `first` y `last`. La búsqueda de productos coincide con el nombre; la de usuarios, con nombre de usuario o rol. Por ejemplo: `/api/products?page=0&size=20&sort=nombre&direction=ASC&search=teclado`.

Cada solicitud a `/api/**` recibe un `X-Request-Id` generado por el servidor. El log de acceso registra el identificador, método, ruta sin query string, estado HTTP y duración; no registra headers de autorización, credenciales ni cuerpos. Los errores internos incluyen ese mismo identificador en la respuesta para poder correlacionarlos con el log del servidor.

Los endpoints `GET /api/products` y `GET /api/users` devuelven resultados paginados. Aceptan `page` (base cero, por defecto `0`), `size` (por defecto `20`, máximo `100`), `sort`, `direction` (`ASC` o `DESC`) y `search`. Productos permiten ordenar por `id`, `nombre`, `precio` o `stock`; usuarios por `id`, `username` o `role`. La respuesta contiene `content`, `page`, `size`, `totalElements`, `totalPages`, `first` y `last`. La búsqueda de productos coincide con el nombre; la de usuarios, con nombre de usuario o rol. Por ejemplo: `/api/products?page=0&size=20&sort=nombre&direction=ASC&search=teclado`.

## Swagger / OpenAPI

Con el backend en ejecución, la documentación interactiva está disponible en `http://localhost:8080/swagger-ui/index.html` y el documento OpenAPI en `http://localhost:8080/v3/api-docs`. La interfaz y el documento son accesibles sin iniciar sesión; las operaciones de la API mantienen sus permisos actuales. Usa **Authorize** en Swagger UI e ingresa el JWT para probar rutas protegidas. El token se obtiene con `POST /api/auth/login`.

## CORS

En desarrollo, el navegador llama a Angular en `http://localhost:4200` y el proxy de Angular reenvía `/api` al backend; no se necesita habilitar otros orígenes. El backend acepta por defecto únicamente `http://localhost:4200`, y la lista puede cambiarse con `APP_CORS_ALLOWED_ORIGINS`, separando los orígenes exactos por coma (por ejemplo, `https://app.example.com,https://staging.example.com`). No incluyas rutas ni comodines.

CORS aplica únicamente a `/api/**`, permite los métodos HTTP usados por la API y los headers `Authorization`, `Content-Type` y `Accept`. Las credenciales cross-origin están deshabilitadas porque la autenticación usa JWT en el header y no cookies. En producción, si frontend y API están bajo el mismo origen, sirve `/api` mediante reverse proxy y conserva CORS cerrado; si tienen orígenes distintos, configura solo el origen del frontend.

## Integridad y concurrencia del stock

`POST /api/sales` ejecuta la operación dentro de una transacción. Valida la lista y las cantidades, bloquea los productos en orden de ID para evitar interbloqueos, verifica disponibilidad, descuenta el stock y guarda la venta con sus líneas. Si algún producto no existe o no tiene stock suficiente, no se guarda ninguna parte de la venta. La falta de stock responde `409 Conflict`.

El frontend solo pre-valida cantidades para dar una mejor experiencia; la base de datos y el backend son la autoridad. Los `CHECK` protegen las invariantes básicas incluso ante escrituras fuera de la API. El bloqueo pesimista serializa ventas concurrentes del mismo producto.

## Mejoras recomendadas

1. Mantener el stock solo en operaciones del dominio de inventario; hoy el formulario de edición puede sobrescribirlo. En una siguiente iteración, separar datos de catálogo y ajustes de existencias, y registrar movimientos de inventario si se requiere auditoría.
2. Añadir pruebas de integración de venta con PostgreSQL (por ejemplo, Testcontainers) para validar bloqueo y constraints específicos del motor. Las pruebas actuales cubren concurrencia con H2 y rollback transaccional, pero no sustituyen esa verificación en PostgreSQL.
3. Las migraciones de esquema quedan fuera del alcance actual. Evaluar Flyway o Liquibase si se incorporan cambios incrementales del modelo; no usar `ddl-auto=update` en producción.
4. Añadir filtros adicionales y consultas de historial de ventas según las necesidades del negocio.
5. Centralizar secretos y configuración por entorno; validar que JWT tenga un secreto no vacío y suficientemente seguro fuera de pruebas.
6. Usar `Instant`/`TIMESTAMPTZ` para fechas si se requiere operación en varias zonas horarias; acordar el formato de API antes del cambio.
7. Eliminar RabbitMQ del stack actual: no había productores ni consumidores implementados, por lo que la dependencia no entregaba funcionalidad. Incorporar mensajería solo con un caso de uso y estrategia de consistencia definidos, como transactional outbox.

## Pruebas

```powershell
.\mvnw.cmd test
```

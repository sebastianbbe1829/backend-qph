# Esquema de base de datos

El proyecto inicia con una base vacía y usa cuatro tablas: `users`, `products`, `sales` y `sale_items`.

1. Crea la base de datos PostgreSQL `products_db`.
2. Ejecuta `schema.sql` conectado a esa base.
3. Configura conexión y secreto JWT mediante variables de entorno.
4. Inicializa el primer administrador con el procedimiento descrito en el README principal.

El script no inserta usuarios, productos ni ventas de demostración. Es para una instalación limpia; no es una migración ni debe ejecutarse encima de una base con datos.

## Relaciones y restricciones

- `sales.user_id` referencia obligatoriamente `users.id`; no se permite borrar un usuario con ventas.
- `sale_items.sale_id` referencia `sales.id` y sus líneas se borran junto con la venta.
- `sale_items.product_id` referencia `products.id`; no se permite borrar productos que forman parte del historial.
- Claves `PRIMARY KEY`, `UNIQUE` y `CHECK` refuerzan la identidad, el nombre de usuario, los roles, los precios positivos, las cantidades positivas y el stock no negativo.
- La restricción única `(sale_id, product_id)` evita repetir el mismo producto dentro de una venta. La aplicación valida también esa condición para devolver un error legible.
- No se persiste el total de línea: se calcula a partir de cantidad y precio unitario para evitar valores redundantes que puedan contradecirse.

Las ventas registran el usuario, fecha, producto, cantidad y precio unitario histórico. El precio guardado no cambia si luego se actualiza el precio vigente del producto.

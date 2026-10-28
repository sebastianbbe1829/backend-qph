-- Ejecutar en la base de datos y esquema usados por la aplicación.
-- La tabla se crea separada de products: este MVP no cambia la tabla existente.
CREATE TABLE IF NOT EXISTS qph_prueba.categories (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(300),
    CONSTRAINT uq_categories_nombre UNIQUE (nombre)
);